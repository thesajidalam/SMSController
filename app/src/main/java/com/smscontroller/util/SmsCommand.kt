package com.smscontroller.util

sealed class SmsCommand {
    data object Lock : SmsCommand()
    data object Beep : SmsCommand()
    data object BeepStop : SmsCommand()
    data object BeepStatus : SmsCommand()
    data object Gps : SmsCommand()
    data object Battery : SmsCommand()
    data object Flash : SmsCommand()
    data object Callme : SmsCommand()
    data object Data : SmsCommand()
    data object Help : SmsCommand()

    companion object {
        private val COMMAND_MAP = mapOf(
            "lock" to Lock,
            "beep" to Beep,
            "beepstop" to BeepStop,
            "beepstatus" to BeepStatus,
            "gps" to Gps,
            "battery" to Battery,
            "flash" to Flash,
            "torch" to Flash,
            "callme" to Callme,
            "call" to Callme,
            "data" to Data,
            "mobile" to Data,
            "help" to Help,
            "commands" to Help
        )

        fun fromMessage(body: String): SmsCommand? {
            val text = body.trim().lowercase()
            val words = text.split("\\s+".toRegex()).filter { it.isNotBlank() }

            for (word in words) {
                val command = COMMAND_MAP[word]
                if (command != null) {
                    if (command == Beep) {
                        if (words.contains("stop")) return BeepStop
                        if (words.contains("status")) return BeepStatus
                    }
                    if (command == Help && words.contains("list")) return Help
                    return command
                }
            }
            return null
        }
    }
}
