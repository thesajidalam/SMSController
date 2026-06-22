package com.smscontroller.util

sealed class SmsCommand {
    data object Lock : SmsCommand()
    data object Beep : SmsCommand()
    data object Gps : SmsCommand()
    data object Battery : SmsCommand()
    data object Photo : SmsCommand()
    data object Wipe : SmsCommand()
    data object Flash : SmsCommand()
    data object Callme : SmsCommand()
    data object Wifi : SmsCommand()
    data object Data : SmsCommand()
    data object RecordStart : SmsCommand()
    data object RecordStop : SmsCommand()
    data object Screenshot : SmsCommand()

    companion object {
        private val COMMAND_MAP = mapOf(
            "lock" to Lock,
            "beep" to Beep,
            "gps" to Gps,
            "battery" to Battery,
            "photo" to Photo,
            "camera" to Photo,
            "wipe" to Wipe,
            "factory" to Wipe,
            "reset" to Wipe,
            "flash" to Flash,
            "torch" to Flash,
            "light" to Flash,
            "callme" to Callme,
            "call" to Callme,
            "wifi" to Wifi,
            "data" to Data,
            "mobile" to Data,
            "recordstart" to RecordStart,
            "record" to RecordStart,
            "recordstop" to RecordStop,
            "screenshot" to Screenshot,
            "screen" to Screenshot,
            "capture" to Screenshot
        )

        fun fromMessage(body: String): SmsCommand? {
            val text = body.trim().lowercase()
            val words = text.split("\\s+".toRegex()).filter { it.isNotBlank() }

            for (word in words) {
                val command = COMMAND_MAP[word]
                if (command != null) {
                    if (word == "record" && words.contains("stop")) return RecordStop
                    if (word == "recordstop") return RecordStop
                    return command
                }
            }
            return null
        }
    }
}
