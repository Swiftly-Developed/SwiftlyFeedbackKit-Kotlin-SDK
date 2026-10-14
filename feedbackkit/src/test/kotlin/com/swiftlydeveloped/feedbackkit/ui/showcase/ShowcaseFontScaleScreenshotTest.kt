package com.swiftlydeveloped.feedbackkit.ui.showcase

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.swiftlydeveloped.feedbackkit.ui.FeedbackListScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Showcase screen at the five font scales FEEDBACK01 reviews, in light and dark. Record
 * with `./gradlew :feedbackkit:recordRoborazziDebug`; verify with `verifyRoborazziDebug`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PHONE)
class ShowcaseFontScaleScreenshotTest(
    private val fontScale: Float,
    private val dark: Boolean
) {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun showcaseList() {
        rule.setContent {
            ShowcaseTestHost(dark = dark, fontScale = fontScale) {
                FeedbackListScreen(state = rememberFixtureState(), onFeedbackClick = {}, onAddFeedback = {})
            }
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage(shot("list_${if (dark) "dark" else "light"}_fs${(fontScale * 100).toInt()}"))
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "fontScale={0} dark={1}")
        fun parameters(): List<Array<Any>> = listOf(0.85f, 1f, 1.3f, 1.5f, 2f).flatMap { scale ->
            listOf(arrayOf<Any>(scale, false), arrayOf<Any>(scale, true))
        }
    }
}

internal const val PHONE = "w360dp-h800dp-xhdpi"

internal fun shot(name: String) = "src/test/screenshots/showcase_$name.png"
