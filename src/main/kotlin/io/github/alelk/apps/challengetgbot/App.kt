package io.github.alelk.apps.challengetgbot

import com.github.ajalt.clikt.core.main
import io.github.alelk.apps.challengetgbot.command.createMainCommand

/**
 * Main application entry point
 */
fun main(args: Array<String>) {
    createMainCommand().main(args)
}

