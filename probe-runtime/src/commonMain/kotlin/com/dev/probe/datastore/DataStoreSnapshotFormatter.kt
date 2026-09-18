package com.dev.probe.datastore

internal data class DataStoreRow(val name: String, val value: String)

internal object DataStoreSnapshotFormatter {
    fun format(name: String, value: Any?, redactor: (Any?) -> String): DataStoreRow = DataStoreRow(name = name, value = redactor(value))
}
