package com.swiftlydeveloped.feedbackkit.ui.showcase

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.swiftlydeveloped.feedbackkit.models.FeedbackCategory
import com.swiftlydeveloped.feedbackkit.models.FeedbackStatus
import com.swiftlydeveloped.feedbackkit.state.FeedbackListState
import com.swiftlydeveloped.feedbackkit.ui.FeedbackList
import com.swiftlydeveloped.feedbackkit.ui.FeedbackListScreen
import com.swiftlydeveloped.feedbackkit.ui.FeedbackListStyle
import kotlinx.coroutines.awaitCancellation
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.IOException

/** Every Showcase state the spec reviews besides the plain list (FEEDBACK01 §8.4). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = PHONE)
class ShowcaseStateScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    private fun capture(name: String) {
        rule.waitForIdle()
        rule.onRoot().captureRoboImage(shot(name))
    }

    @Test
    fun filteredList() {
        rule.setContent {
            ShowcaseTestHost {
                FeedbackListScreen(state = rememberFixtureState { selectCategory(FeedbackCategory.BUG_REPORT); selectStatus(FeedbackStatus.APPROVED) })
            }
        }
        capture("filtered_light_fs100")
    }

    @Test
    fun filteredListLargeText() {
        rule.setContent {
            ShowcaseTestHost(dark = true, fontScale = 2f) {
                FeedbackListScreen(state = rememberFixtureState { selectCategory(FeedbackCategory.FEATURE_REQUEST); selectStatus(FeedbackStatus.IN_PROGRESS) })
            }
        }
        capture("filtered_dark_fs200")
    }

    @Test
    fun filteredEmpty() {
        rule.setContent {
            ShowcaseTestHost {
                FeedbackListScreen(state = rememberFixtureState { selectCategory(FeedbackCategory.BUG_REPORT); selectStatus(FeedbackStatus.REJECTED) })
            }
        }
        capture("filtered_empty_light_fs100")
    }

    @Test
    fun searchEmpty() {
        rule.setContent {
            ShowcaseTestHost(dark = true) {
                FeedbackListScreen(state = rememberFixtureState { updateSearchQuery("kangaroo") })
            }
        }
        capture("search_empty_dark_fs100")
    }

    @Test
    fun projectEmpty() {
        rule.setContent {
            ShowcaseTestHost {
                FeedbackListScreen(state = rememberFixtureState(items = emptyList()), onAddFeedback = {})
            }
        }
        capture("project_empty_light_fs100")
    }

    @Test
    fun loadError() {
        rule.setContent {
            val scope = rememberCoroutineScope()
            val state = remember {
                FeedbackListState(scope, loadItems = { throw IOException("offline") }).also { it.load() }
            }
            ShowcaseTestHost(dark = true, fontScale = 1.5f) { FeedbackListScreen(state = state) }
        }
        capture("error_dark_fs150")
    }

    @Test
    fun skeleton() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val scope = rememberCoroutineScope()
            val state = remember {
                FeedbackListState(scope, loadItems = { awaitCancellation() }).also { it.load() }
            }
            ShowcaseTestHost { FeedbackListScreen(state = state) }
        }
        rule.mainClock.advanceTimeBy(SKELETON_REVEAL_DELAY_MILLIS + 100)
        rule.onRoot().captureRoboImage(shot("skeleton_light_fs100"))
    }

    @Test
    fun skeletonDarkLargeText() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val scope = rememberCoroutineScope()
            val state = remember {
                FeedbackListState(scope, loadItems = { awaitCancellation() }).also { it.load() }
            }
            ShowcaseTestHost(dark = true, fontScale = 2f) { FeedbackListScreen(state = state) }
        }
        rule.mainClock.advanceTimeBy(SKELETON_REVEAL_DELAY_MILLIS + 100)
        rule.onRoot().captureRoboImage(shot("skeleton_dark_fs200"))
    }

    @Test
    fun sortFilterSheet() {
        rule.setContent {
            ShowcaseTestHost { SheetPreview(rememberFixtureState { selectCategory(FeedbackCategory.BUG_REPORT) }) }
        }
        capture("sheet_light_fs100")
    }

    @Test
    fun sortFilterSheetDarkLargeText() {
        rule.setContent {
            ShowcaseTestHost(dark = true, fontScale = 2f) {
                SheetPreview(rememberFixtureState { selectStatus(FeedbackStatus.IN_PROGRESS) })
            }
        }
        capture("sheet_dark_fs200")
    }

    @Test
    fun dynamicColorScheme() {
        rule.setContent {
            ShowcaseTestHost(colorScheme = DynamicLikeLightScheme) {
                FeedbackListScreen(state = rememberFixtureState(), onAddFeedback = {})
            }
        }
        capture("dynamic_color_light_fs100")
    }

    @Test
    fun classicStyle() {
        rule.setContent {
            ShowcaseTestHost {
                FeedbackListScreen(state = rememberFixtureState(), listStyle = FeedbackListStyle.CLASSIC)
            }
        }
        capture("classic_light_fs100")
    }

    @Test
    @Config(qualifiers = "ar-$PHONE")
    fun arabicRightToLeftLargeText() {
        rule.setContent {
            ShowcaseTestHost(fontScale = 2f) {
                FeedbackListScreen(state = rememberFixtureState(ArabicFixtures) { selectCategory(FeedbackCategory.FEATURE_REQUEST) }, onAddFeedback = {})
            }
        }
        capture("rtl_ar_light_fs200")
    }

    @Test
    @Config(qualifiers = "ar-$PHONE")
    fun arabicRightToLeft() {
        rule.setContent {
            ShowcaseTestHost(dark = true) {
                FeedbackListScreen(state = rememberFixtureState(ArabicFixtures), onAddFeedback = {})
            }
        }
        capture("rtl_ar_dark_fs100")
    }

    /** Every card variant at 200%, on a tall canvas so nothing is cut off by the viewport. */
    @Test
    @Config(qualifiers = "w360dp-h2600dp-xhdpi")
    fun allCardsLargeText() {
        rule.setContent {
            ShowcaseTestHost(fontScale = 2f) { FeedbackList(state = rememberFixtureState()) }
        }
        capture("cards_light_fs200_tall")
    }

    @Composable
    private fun SheetPreview(state: FeedbackListState) {
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp)
        ) {
            SortFilterSheetContent(
                state = state,
                showStatusFilter = true,
                showCategoryFilter = true,
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}

private val ArabicFixtures = listOf(
    feedback("a1", title = "الوضع الداكن في التطبيق كله", description = "شاشة الإعدادات لا تزال ساطعة في الليل.", status = FeedbackStatus.IN_PROGRESS, category = FeedbackCategory.FEATURE_REQUEST, votes = 1234, voted = true, comments = 18),
    feedback("a2", title = "تصدير إلى CSV", description = "تم الإصدار في 3.2.", status = FeedbackStatus.COMPLETED, category = FeedbackCategory.FEATURE_REQUEST, votes = 96, comments = 1),
    feedback("a3", title = "بحث أسرع", description = "تظهر النتائج ببطء على الهواتف القديمة.", status = FeedbackStatus.TESTFLIGHT, category = FeedbackCategory.IMPROVEMENT, votes = 7)
)
