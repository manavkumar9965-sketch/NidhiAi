package com.nidhi.ai.platform.actions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import com.nidhi.ai.core.result.ActionResult
import com.nidhi.ai.domain.model.Command
import com.nidhi.ai.domain.router.CommandRouter
import com.nidhi.ai.domain.router.RouteDecision

/** Runs a local command if one matches. Returns null when the text should go to Gemini instead. */
object LocalCommandRunner {

    private val packages = mapOf(
        "youtube" to "com.google.android.youtube",
        "facebook" to "com.facebook.katana",
        "whatsapp" to "com.whatsapp",
        "instagram" to "com.instagram.android",
        "chrome" to "com.android.chrome",
        "maps" to "com.google.android.apps.maps",
        "spotify" to "com.spotify.music"
    )

    fun run(context: Context, text: String): String? =
        when (val decision = CommandRouter.route(text)) {
            is RouteDecision.SendToAi -> null
            is RouteDecision.Clarify -> decision.question
            is RouteDecision.Local -> execute(context, decision.command)
        }

    private fun execute(context: Context, command: Command): String = when (command) {
        Command.TorchOn -> describe(TorchController.set(context, true))
        Command.TorchOff -> describe(TorchController.set(context, false))
        is Command.OpenApp -> openApp(context, command.appName)
        is Command.OpenWebsite -> openUrl(context, command.site)
        else -> "Ye action abhi Nidhi mein add nahi hua hai."
    }

    private fun openApp(context: Context, name: String): String {
        val intent: Intent? = when (name) {
            "settings" -> Intent(Settings.ACTION_SETTINGS)
            "camera" -> Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            else -> packages[name]?.let { context.packageManager.getLaunchIntentForPackage(it) }
        }
        if (intent == null) return "$name install nahi hai ya pehchana nahi gaya."
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            "$name khol diya."
        } catch (e: Exception) {
            "$name nahi khul paya: ${e.message}"
        }
    }

    private fun openUrl(context: Context, site: String): String {
        val url = if (site.startsWith("http")) site else "https://$site"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            "$site khol diya."
        } catch (e: Exception) {
            "Website nahi khul paayi: ${e.message}"
        }
    }

    private fun describe(result: ActionResult): String = when (result) {
        is ActionResult.Success -> result.spokenText
        is ActionResult.NotInstalled -> "${result.appName} install nahi hai."
        is ActionResult.PermissionMissing -> "Permission chahiye: ${result.permission}. ${result.reason}"
        is ActionResult.Failed -> result.reason
    }
}
