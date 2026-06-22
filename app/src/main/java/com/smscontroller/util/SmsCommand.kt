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
        fun fromMessage(body: String): SmsCommand? {
            val text = body.trim().lowercase()
            return when {
                text.contains("record stop") || text == "recordstop" -> RecordStop
                text.contains("record start") || text == "recordstart" -> RecordStart
                text == "record" -> RecordStart
                text.contains("lock") -> Lock
                text.contains("beep") -> Beep
                text.contains("gps") -> Gps
                text.contains("battery") -> Battery
                text.contains("photo") || text.contains("camera") -> Photo
                text.contains("wipe") || text.contains("factory") || text.contains("reset") -> Wipe
                text.contains("flash") || text.contains("torch") || text.contains("light") -> Flash
                text.contains("callme") || text.contains("call me") || text.contains("call") -> Callme
                text.contains("wifi") -> Wifi
                text.contains("data") || text.contains("mobile") -> Data
                text.contains("screenshot") || text.contains("screen") || text.contains("capture") -> Screenshot
                else -> null
            }
        }
    }
}
