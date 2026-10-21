package com.nidhi.ai.platform.actions

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import com.nidhi.ai.core.result.ActionResult

/** Turns the phone flashlight on or off. Success is returned only after the system call succeeds. */
object TorchController {

    fun set(context: Context, on: Boolean): ActionResult {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return try {
            val cameraId = manager.cameraIdList.firstOrNull { id ->
                manager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return ActionResult.Failed("Is phone mein flash available nahi hai.")

            manager.setTorchMode(cameraId, on)
            ActionResult.Success(if (on) "Torch on kar di." else "Torch band kar di.")
        } catch (e: CameraAccessException) {
            ActionResult.Failed("Torch control nahi ho paya: ${e.message}")
        } catch (e: IllegalArgumentException) {
            ActionResult.Failed("Torch control nahi ho paya: ${e.message}")
        }
    }
}
