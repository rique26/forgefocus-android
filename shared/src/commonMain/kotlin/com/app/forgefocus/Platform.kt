package com.app.forgefocus

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform