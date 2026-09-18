/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-FileCopyrightText: 2026 Anshuman_X (maxxcodebug)
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.doOnLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import com.google.android.material.button.MaterialButton
import org.lineageos.aperture.R
import org.lineageos.aperture.ext.px
import org.lineageos.aperture.models.CameraMode
import org.lineageos.aperture.models.CameraState
import org.lineageos.aperture.utils.TimeUtils
import kotlin.reflect.cast

class CameraModeSelectorLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    // Views
    private val cameraModeButtonsLinearLayout by lazy { findViewById<LinearLayout>(R.id.cameraModeButtonsLinearLayout) }
    private val cameraModeHighlightButton by lazy { findViewById<MaterialButton>(R.id.cameraModeHighlightButton) }
    private val videoDurationButton by lazy { findViewById<MaterialButton>(R.id.videoDurationButton) }

    // System services
    private val layoutInflater by lazy { context.getSystemService(LayoutInflater::class.java) }

    private val cameraToButton = mutableMapOf<CameraMode, MaterialButton>()

    private var inSingleCaptureMode = false
    private var cameraState = CameraState.IDLE

    var onModeSelectedCallback: (cameraMode: CameraMode) -> Unit = {}

    // MaxxSpring: custom width property, no stock DynamicAnimation property covers view width
    private val widthProperty = object : FloatPropertyCompat<MaterialButton>("highlightWidth") {
        override fun getValue(button: MaterialButton) = button.width.toFloat()
        override fun setValue(button: MaterialButton, value: Float) {
            button.layoutParams = button.layoutParams.apply {
                width = value.toInt().coerceAtLeast(1)
            }
        }
    }

    init {
        inflate(context, R.layout.camera_mode_selector_layout, this)

        for (cameraMode in CameraMode.entries) {
            cameraToButton[cameraMode] = MaterialButton::class.cast(
                layoutInflater.inflate(
                    R.layout.camera_mode_button, this, false
                )
            ).apply {
                setText(
                    when (cameraMode) {
                        CameraMode.PHOTO -> R.string.camera_mode_photo
                        CameraMode.VIDEO -> R.string.camera_mode_video
                        CameraMode.QR -> R.string.camera_mode_qr
                    }
                )
                setOnClickListener { onModeSelectedCallback(cameraMode) }
            }.also {
                cameraModeButtonsLinearLayout.addView(it)
            }
        }
    }

    fun setCurrentCameraMode(cameraMode: CameraMode) {
        val currentCameraModeButton =
            cameraToButton[cameraMode] ?: throw Exception("No button for $cameraMode")

        cameraToButton.forEach {
            it.value.isEnabled = cameraMode != it.key
        }

        // MaxxSpring transition: bouncy position + settled width + micro squash pop.
        // Not a stock ValueAnimator interpolation — three springs layered together.
        doOnLayout {
            val targetX = currentCameraModeButton.x + 16.px
            val targetWidth = currentCameraModeButton.width.toFloat()

            SpringAnimation(cameraModeHighlightButton, DynamicAnimation.X, targetX).apply {
                spring = SpringForce(targetX).apply {
                    dampingRatio = 0.55f
                    stiffness = 700f
                }
            }.start()

            SpringAnimation(cameraModeHighlightButton, widthProperty, targetWidth).apply {
                spring = SpringForce(targetWidth).apply {
                    dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
                    stiffness = SpringForce.STIFFNESS_MEDIUM
                }
            }.start()

            cameraModeHighlightButton.scaleY = 0.82f
            SpringAnimation(cameraModeHighlightButton, DynamicAnimation.SCALE_Y, 1f).apply {
                spring = SpringForce(1f).apply {
                    dampingRatio = 0.35f
                    stiffness = 900f
                }
            }.start()
        }
    }

    fun setInSingleCaptureMode(inSingleCaptureMode: Boolean) {
        this.inSingleCaptureMode = inSingleCaptureMode

        updateButtons()
    }

    fun setCameraState(cameraState: CameraState) {
        this.cameraState = cameraState

        // Update video duration button
        videoDurationButton.isVisible = cameraState.isRecordingVideo

        updateButtons()
    }

    fun setVideoRecordingDuration(duration: Long) {
        videoDurationButton.text = TimeUtils.convertNanosToString(duration)
    }

    private fun updateButtons() {
        cameraModeHighlightButton.isInvisible = cameraState.isRecordingVideo || inSingleCaptureMode
        cameraToButton.forEach {
            it.value.isInvisible = cameraState.isRecordingVideo || inSingleCaptureMode
        }
    }
}
