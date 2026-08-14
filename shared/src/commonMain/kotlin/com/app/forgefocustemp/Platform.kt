package com.app.forgefocustemp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform