package com.nidhi.ai.core.result

/** Result of any device action. RAVAN may only say "done" when the result is Success. */
sealed interface ActionResult {
    data class Success(val spokenText: String) : ActionResult
    data class NotInstalled(val appName: String) : ActionResult
    data class PermissionMissing(val permission: String, val reason: String) : ActionResult
    data class Failed(val reason: String) : ActionResult
}
