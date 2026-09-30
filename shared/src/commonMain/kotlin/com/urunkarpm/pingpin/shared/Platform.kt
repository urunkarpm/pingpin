package com.urunkarpm.pingpin.shared

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
