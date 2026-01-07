package io.github.alelk.apps.challengetgbot.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.subcommands

/**
 * Main CLI command that serves as entry point
 */
class ChallengeBotCommand : CliktCommand(name = "challenge-bot") {

    override fun help(context: Context): String = "Telegram Challenge Bot - управление челленджами в Telegram группах"

    override fun run() {
        // If no subcommand is provided, show help
    }
}

/**
 * Create main command with all subcommands
 */
fun createMainCommand(): CliktCommand {
    return ChallengeBotCommand().subcommands(
        RunCommand(),
        PostChallengeCommand()
    )
}

