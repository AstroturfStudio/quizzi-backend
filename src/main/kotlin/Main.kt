package com.alicankorkmaz

import util.Logger

fun main() {
    val name = "Kotlin"
    Logger.i("Hello, " + name + "!")

    for (i in 1..5) {
        Logger.i("i = $i")
    }
}