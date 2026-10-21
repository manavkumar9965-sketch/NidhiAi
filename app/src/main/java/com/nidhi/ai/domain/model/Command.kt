package com.nidhi.ai.domain.model

/** Every action RAVAN can perform locally, without the internet. */
sealed interface Command {
    data class OpenApp(val appName: String) : Command
    data class OpenWebsite(val site: String) : Command
    data object TorchOn : Command
    data object TorchOff : Command
    data object VolumeUp : Command
    data object VolumeDown : Command
    data object BatteryStatus : Command
    data class SetAlarm(val hour: Int, val minute: Int) : Command
    data class Reminder(val minutes: Int, val message: String) : Command
    data object GoBack : Command
    data object GoHome : Command
}
