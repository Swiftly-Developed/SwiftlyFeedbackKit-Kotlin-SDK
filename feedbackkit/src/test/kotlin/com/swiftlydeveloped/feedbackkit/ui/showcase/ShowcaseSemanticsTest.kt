package com.swiftlydeveloped.feedbackkit.ui.showcase

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.swiftlydeveloped.feedbackkit.models.FeedbackCategory
import com.swiftlydeveloped.feedbackkit.models.FeedbackStatus
import com.swiftlydeveloped.feedbackkit.state.FeedbackListState
import com.swiftlydeveloped.feedbackkit.ui.FeedbackList
import com.swiftlydeveloped.feedbackkit.ui.FeedbackListStyle
import com.swiftlydeveloped.feedbackkit.ui.FeedbackSortFilterButton
import com.swiftlydeveloped.feedbackkit.ui.SORT_FILTER_BUTTON_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = PHONE)
class ShowcaseSemanticsTest {

    @get:Rule
    val compose = createComposeRule()

    private val approved = ShowcaseFixtures.first { it.id == "2" }
    private val completed = ShowcaseFixtures.first { it.id == "4" }

    private fun card(item: com.swiftlydeveloped.feedbackkit.models.Feedback, actions: VoteActions = FakeVoteActions()) {
        compose.setContent {
            ShowcaseTestHost(voteActions = actions) { ShowcaseFeedbackCard(feedback = item, onClick = {}) }
        }
    }

    /**
     * Material components keep a 32-40dp visual size and extend the touch bounds to 48dp, so
     * touch targets are measured on [touchBoundsInRoot][androidx.compose.ui.semantics.SemanticsNode.touchBoundsInRoot].
     */
    private fun SemanticsNodeInteraction.assertTouchTargetAtLeast(min: Dp = 48.dp): SemanticsNodeInteraction {
        val node = fetchSemanticsNode()
        val bounds = node.touchBoundsInRoot
        val density = node.layoutInfo.density
        val width = with(density) { bounds.width.toDp() }
        val height = with(density) { bounds.height.toDp() }
        assertTrue("touch target is $width x $height", width >= min - 0.5.dp && height >= min - 0.5.dp)
        return this
    }

    private fun SemanticsNodeInteraction.onClickLabel(): String? =
        fetchSemanticsNode().config.getOrElseNullable(SemanticsActions.OnClick) { null }?.label

    private fun SemanticsNodeInteraction.customActionLabels(): List<String> =
        fetchSemanticsNode().config.getOrElseNullable(SemanticsActions.CustomActions) { null }.orEmpty().map { it.label }

    @Test
    fun `the vote capsule is a labelled button with a vote state`() {
        card(approved)

        compose.onNodeWithTag(VOTE_CAPSULE_TAG)
            .assertContentDescriptionEquals("42 votes")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Not voted"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast()
        assertEquals("Vote", compose.onNodeWithTag(VOTE_CAPSULE_TAG).onClickLabel())
    }

    @Test
    fun `tapping the capsule votes optimistically and offers removal`() {
        val actions = FakeVoteActions().apply { seed("2", 42) }
        card(approved, actions)

        compose.onNodeWithTag(VOTE_CAPSULE_TAG).performClick()
        compose.waitForIdle()

        assertEquals(listOf("vote:2"), actions.calls)
        compose.onNodeWithTag(VOTE_CAPSULE_TAG)
            .assertContentDescriptionEquals("43 votes")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Voted"))
        assertEquals("Remove vote", compose.onNodeWithTag(VOTE_CAPSULE_TAG).onClickLabel())
    }

    @Test
    fun `a closed request shows its count without a control`() {
        card(completed)

        compose.onAllNodesWithTag(VOTE_CAPSULE_TAG, useUnmergedTree = true).assertCountEquals(0)
        compose.onNodeWithTag(VOTE_CLOSED_TAG, useUnmergedTree = true)
            .assertContentDescriptionEquals("96 votes")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Voting is closed"))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
    }

    @Test
    fun `the card opens details and carries the vote as a custom action`() {
        card(approved)

        val node = compose.onNodeWithTag(SHOWCASE_CARD_TAG)
        assertEquals("View details", node.onClickLabel())
        assertEquals(listOf("Vote"), node.customActionLabels())
    }

    @Test
    fun `a closed card has no vote action`() {
        card(completed)

        assertTrue(compose.onNodeWithTag(SHOWCASE_CARD_TAG).customActionLabels().isEmpty())
    }

    @Test
    fun `the sort button reports the sort, then the active filters`() {
        lateinit var state: FeedbackListState
        compose.setContent {
            ShowcaseTestHost {
                state = rememberFixtureState()
                FeedbackSortFilterButton(state = state)
            }
        }

        compose.onNodeWithTag(SORT_FILTER_BUTTON_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Sorted by Votes"))
            .assertTouchTargetAtLeast()

        compose.runOnIdle {
            state.selectStatus(FeedbackStatus.APPROVED)
            state.selectCategory(FeedbackCategory.BUG_REPORT)
        }

        compose.onNodeWithTag(SORT_FILTER_BUTTON_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Filtered by Bug Reports, Approved"))
    }

    @Test
    fun `selecting a filter shows the summary row and Clear restores the list`() {
        lateinit var state: FeedbackListState
        compose.setContent {
            ShowcaseTestHost {
                state = rememberFixtureState()
                FeedbackList(state = state)
            }
        }
        compose.onAllNodesWithTag(FILTER_SUMMARY_TAG).assertCountEquals(0)

        compose.runOnIdle { state.selectStatus(FeedbackStatus.APPROVED) }

        compose.onNodeWithContentDescription("Filtered by Approved").let { summary ->
            assertEquals(listOf("Clear filters"), summary.customActionLabels())
        }
        compose.onAllNodesWithTag(SHOWCASE_CARD_TAG).assertCountEquals(1)
        compose.onNodeWithTag(FILTER_SUMMARY_CLEAR_TAG)
            .assertContentDescriptionEquals("Clear filters")
            .assertTouchTargetAtLeast()
            .performClick()

        compose.waitForIdle()
        assertFalse(state.isFiltered)
        compose.onAllNodesWithTag(FILTER_SUMMARY_TAG).assertCountEquals(0)
    }

    @Test
    fun `sheet chips are radio buttons with full touch targets`() {
        compose.setContent {
            ShowcaseTestHost {
                SortFilterSheetContent(state = rememberFixtureState(), showStatusFilter = true, showCategoryFilter = true)
            }
        }

        val chips = compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        val count = chips.fetchSemanticsNodes().size
        assertEquals(4 + 1 + 6 + 1 + 4, count)
        repeat(count) { chips[it].assertTouchTargetAtLeast() }
    }

    @Test
    fun `classic style renders no showcase cards`() {
        compose.setContent {
            ShowcaseTestHost {
                FeedbackList(state = rememberFixtureState(), listStyle = FeedbackListStyle.CLASSIC)
            }
        }

        compose.onAllNodesWithTag(SHOWCASE_CARD_TAG).assertCountEquals(0)
        compose.onAllNodesWithTag(VOTE_CAPSULE_TAG).assertCountEquals(0)
    }
}
