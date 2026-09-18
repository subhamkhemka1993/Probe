#!/usr/bin/env python3
'''Structural check for the "no mid-block comments" guardrail (see CLAUDE.md#Conventions).

A comment is allowed at class-level, property/variable-level, and function-level:
  - immediately before a declaration (class/object/interface/fun/val/var), at any nesting depth
  - as the first content of a block, right after its opening `{` (explains the block as a whole)
  - trailing on the same line as code (a same-line note, like a variable-level annotation)

A *standalone* comment (alone on its own line(s)) that sits after at least one real statement has
already run in the current block, and that isn't immediately followed by a declaration, is a
violation: it's narrating one specific line/block partway through a function/class body instead
of living at a declaration boundary, exactly the pattern CLAUDE.md's guardrail bans.

This is a heuristic line-oriented scanner, not a real Kotlin parser. Known limitations:
  - A brace or quote character inside a string template (like a `${...}` interpolation) is not
    treated as nested code -- it's scanned as opaque string content. Rare enough not to matter.
  - A multi-line block comment or multi-line raw (triple-quoted) string is tracked across
    lines, but a line-comment marker appearing textually inside either is correctly ignored.
'''
from __future__ import annotations

import re
import sys
from dataclasses import dataclass, field

DECLARATION_RE = re.compile(
    r"^(private\s+|internal\s+|public\s+|protected\s+|actual\s+|expect\s+|override\s+|"
    r"open\s+|final\s+|abstract\s+|sealed\s+|data\s+|inline\s+|crossinline\s+|noinline\s+|"
    r"suspend\s+|lateinit\s+|const\s+|external\s+|annotation\s+|companion\s+|"
    r"@\w+(\([^)]*\))?\s+)*"
    r"(fun\s|val\s|var\s|class\s|object\s|interface\s|enum\s+class\s)"
)


@dataclass
class BlockFrame:
    has_statement: bool = False


@dataclass
class ScanResult:
    violations: list[tuple[int, int]] = field(default_factory=list)  # (start_line, end_line), 1-indexed


class LineScanner:
    """Scans a Kotlin source file line by line, tracking:
      - brace depth (a stack of BlockFrame, each remembering if a real statement has run in it)
      - whether we're currently inside a multi-line block comment or multi-line raw string
    and reports standalone comment runs that violate the mid-block rule.
    """

    def __init__(self, lines: list[str]):
        self.lines = lines
        self.stack: list[BlockFrame] = [BlockFrame()]
        self.in_block_comment = False
        self.in_raw_string = False
        self.result = ScanResult()
        self._pending_start: int | None = None
        self._pending_standalone = True

    def run(self) -> ScanResult:
        for line_no, raw_line in enumerate(self.lines, start=1):
            self._process_line(line_no, raw_line)
        if self._pending_start is not None:
            self._flush(len(self.lines))
        return self.result

    def _is_declaration_start(self, from_line: int) -> bool:
        idx = from_line - 1
        n = len(self.lines)
        while idx < n:
            stripped = self.lines[idx].strip()
            if stripped == "":
                idx += 1
                continue
            if stripped.startswith("//"):
                idx += 1
                continue
            if stripped.startswith("/*"):
                if "*/" in stripped[2:]:
                    idx += 1
                    continue
                idx += 1
                while idx < n and "*/" not in self.lines[idx]:
                    idx += 1
                idx += 1
                continue
            if stripped.startswith("@"):
                idx += 1
                continue
            return bool(DECLARATION_RE.match(stripped))
        return False

    def _flush(self, end_line: int):
        start_line = self._pending_start
        self._pending_start = None
        if start_line is None:
            return
        if not self._pending_standalone:
            self._pending_standalone = True
            return
        top = self.stack[-1]
        if top.has_statement and not self._is_declaration_start(end_line + 1):
            self.result.violations.append((start_line, end_line))
        self._pending_standalone = True

    def _process_line(self, line_no: int, raw_line: str):
        if self.in_block_comment:
            end = raw_line.find("*/")
            if end == -1:
                self._note_comment_line(line_no, raw_line, standalone=True)
                return
            self.in_block_comment = False
            self._note_comment_line(line_no, raw_line, standalone=raw_line[:end].strip() == "" or self._before_block_comment_start_was_ws)
            remainder = raw_line[end + 2 :]
            self._scan_code_segment(remainder)
            return

        if self.in_raw_string:
            end = raw_line.find('"""')
            if end == -1:
                return
            self.in_raw_string = False
            self._scan_code_segment(raw_line[end + 3 :])
            return

        stripped = raw_line.strip()
        if stripped == "":
            self._flush(line_no - 1)
            return

        code_col, comment_col, is_block, is_raw_unterminated = self._find_comment_or_raw(raw_line)

        if comment_col is not None:
            standalone = raw_line[:comment_col].strip() == ""
            if code_col is not None and code_col < comment_col:
                self.stack[-1].has_statement = True
                self._apply_braces(raw_line[:comment_col])
                self._flush(line_no - 1)
            self._note_comment_line(line_no, raw_line, standalone=standalone)
            if is_block:
                end = raw_line.find("*/", comment_col + 2)
                if end == -1:
                    self.in_block_comment = True
                    self._before_block_comment_start_was_ws = standalone
                else:
                    self._scan_code_segment(raw_line[end + 2 :])
            return

        if is_raw_unterminated:
            self._flush(line_no - 1)
            self.stack[-1].has_statement = True
            triple_idx = raw_line.find('"""')
            self._apply_braces(raw_line[:triple_idx])
            self.in_raw_string = True
            return

        self._flush(line_no - 1)
        self.stack[-1].has_statement = True
        self._apply_braces(raw_line)

    def _note_comment_line(self, line_no: int, raw_line: str, standalone: bool):
        if self._pending_start is None:
            self._pending_start = line_no
            self._pending_standalone = standalone
        elif not standalone:
            self._pending_standalone = False

    def _scan_code_segment(self, segment: str):
        if segment.strip() != "":
            self.stack[-1].has_statement = True
        self._apply_braces(segment)

    @staticmethod
    def _find_comment_or_raw(raw_line: str):
        """Returns (first_code_col_or_None, comment_start_col_or_None, is_block_comment, hit_unterminated_raw_string)."""
        i = 0
        n = len(raw_line)
        in_str: str | None = None
        first_code_col = None
        while i < n:
            ch = raw_line[i]
            nxt = raw_line[i + 1] if i + 1 < n else ""
            if in_str:
                if in_str == '"' and ch == "\\":
                    i += 2
                    continue
                if ch == in_str:
                    in_str = None
                i += 1
                continue
            if ch == "/" and nxt == "/":
                return first_code_col, i, False, False
            if ch == "/" and nxt == "*":
                return first_code_col, i, True, False
            if raw_line[i : i + 3] == '"""':
                end = raw_line.find('"""', i + 3)
                if end == -1:
                    return first_code_col, None, False, True
                i = end + 3
                if first_code_col is None:
                    first_code_col = i
                continue
            if ch == '"' or ch == "'":
                in_str = ch
                i += 1
                if first_code_col is None:
                    first_code_col = i - 1
                continue
            if not ch.isspace() and first_code_col is None:
                first_code_col = i
            i += 1
        return first_code_col, None, False, False

    def _apply_braces(self, segment: str):
        i = 0
        n = len(segment)
        in_str: str | None = None
        while i < n:
            ch = segment[i]
            nxt = segment[i + 1] if i + 1 < n else ""
            if in_str:
                if in_str == '"' and ch == "\\":
                    i += 2
                    continue
                if ch == in_str:
                    in_str = None
                i += 1
                continue
            if ch == '"' or ch == "'":
                in_str = ch
                i += 1
                continue
            if ch == "{":
                self.stack.append(BlockFrame())
            elif ch == "}":
                if len(self.stack) > 1:
                    self.stack.pop()
            i += 1


def scan(source: str) -> ScanResult:
    lines = source.split("\n")
    return LineScanner(lines).run()


def main(argv: list[str]) -> int:
    if not argv:
        print("usage: check_no_midblock_comments.py <file.kt> [<file.kt> ...]", file=sys.stderr)
        return 2
    fail = 0
    for path in argv:
        try:
            with open(path, "r", encoding="utf-8") as f:
                source = f.read()
        except OSError as exc:
            print(f"check-no-midblock-comments: cannot read {path}: {exc}", file=sys.stderr)
            fail = 1
            continue
        result = scan(source)
        for start, end in result.violations:
            span = f"{start}" if start == end else f"{start}-{end}"
            print(
                f"{path}:{span}: mid-block comment after an existing statement — "
                f"move this explanation to the function/property/class declaration instead."
            )
            fail = 1
    return fail


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
