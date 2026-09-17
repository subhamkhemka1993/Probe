package com.dev.probe

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform