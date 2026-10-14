package com.swiftlydeveloped.feedbackkit.ui.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.getfeedbackkit.feedbackkit.R
import com.swiftlydeveloped.feedbackkit.errors.FeedbackKitError
import com.swiftlydeveloped.feedbackkit.errors.localizedUserMessage
import com.swiftlydeveloped.feedbackkit.models.Feedback
import com.swiftlydeveloped.feedbackkit.models.VoteResponse
import com.swiftlydeveloped.feedbackkit.state.LocalFeedbackKitTheme
import kotlinx.coroutines.launch

internal const val SHOWCASE_CARD_TAG = "fbk-list-card"

/**
 * One Showcase request card (FEEDBACK01 §6.3): status eyebrow, title, the whole description,
 * then a footer with the category and comment facts and the vote capsule.
 *
 * Colours come from the host's [MaterialTheme], so dynamic colour and dark theme follow the
 * app. Only the status colours come from [FeedbackKitTheme.statusColors][com.swiftlydeveloped.feedbackkit.theme.FeedbackKitTheme.statusColors],
 * lifted to 4.5:1 against the card so a light host colour stays legible.
 *
 * TalkBack reads the card as one element ending in "double-tap to view details", with the
 * vote as a custom action. The capsule is also its own focusable element.
 */
@Composable
internal fun ShowcaseFeedbackCard(
    feedback: Feedback,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    placeholder: Boolean = false,
    onVoteChange: ((VoteResponse) -> Unit)? = null,
    onVoteError: ((FeedbackKitError) -> Unit)? = null
) {
    val actions = LocalVoteActions.current
    val controller = remember(feedback.id, feedback.hasVoted, feedback.voteCount, actions) {
        VoteController(feedback.id, feedback.hasVoted, feedback.voteCount, actions)
    }
    val scope = rememberCoroutineScope()
    val presentation = resolveVotePresentation(placeholder, feedback.canVote, controller.hasVoted)
    val toggle: () -> Unit = {
        scope.launch {
            controller.toggle(
                onApplied = { onVoteChange?.invoke(it) },
                onFailed = { onVoteError?.invoke(it) }
            )
        }
    }
    val voteActionLabel = stringResource(
        if (controller.hasVoted) R.string.feedbackkit_remove_vote else R.string.feedbackkit_vote
    )
    val viewDetailsLabel = stringResource(R.string.feedbackkit_view_details)

    val cardModifier = modifier
        .fillMaxWidth()
        .testTag(SHOWCASE_CARD_TAG)
        .semantics(mergeDescendants = onClick == null) {
            if (onClick != null) {
                onClick(label = viewDetailsLabel) { onClick(); true }
            }
            if (presentation is VotePresentation.Votable) {
                customActions = listOf(CustomAccessibilityAction(voteActionLabel) { toggle(); true })
            }
        }

    if (onClick != null && !placeholder) {
        ElevatedCard(onClick = onClick, modifier = cardModifier) {
            ShowcaseCardBody(feedback, placeholder, presentation, controller, toggle)
        }
    } else {
        ElevatedCard(modifier = cardModifier) {
            ShowcaseCardBody(feedback, placeholder, presentation, controller, toggle)
        }
    }
}

@Composable
private fun ShowcaseCardBody(
    feedback: Feedback,
    placeholder: Boolean,
    presentation: VotePresentation,
    controller: VoteController,
    onToggle: () -> Unit
) {
    val typography = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme
    val statusColor = readableOn(
        LocalFeedbackKitTheme.current.statusColors.forStatus(feedback.status),
        colors.surfaceContainerLow
    )
    val eyebrowStyle = typography.labelLarge

    // The capsule's 48dp touch target adds 4dp of invisible space above and below it, so the
    // footer gap and bottom padding are 4dp short of the visual 12dp and 16dp they produce.
    Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusGlyph(
                status = feedback.status,
                color = if (placeholder) placeholderColor() else statusColor,
                size = iconSizeFor(eyebrowStyle.fontSize, 1.25f)
            )
            Spacer(Modifier.size(6.dp))
            ShowcaseText(
                text = stringResource(feedback.status.displayNameRes),
                style = eyebrowStyle,
                color = statusColor,
                placeholder = placeholder
            )
        }
        Spacer(Modifier.height(8.dp))
        ShowcaseText(
            text = feedback.title,
            style = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.onSurface,
            placeholder = placeholder
        )
        Spacer(Modifier.height(4.dp))
        ShowcaseText(
            text = feedback.description,
            style = typography.bodyMedium,
            color = colors.onSurfaceVariant,
            placeholder = placeholder
        )
        Spacer(Modifier.height(8.dp))
        AdaptiveFooter(
            spacing = 12.dp,
            modifier = Modifier.fillMaxWidth(),
            facts = {
                WrappingRow(horizontalSpacing = 16.dp, verticalSpacing = 4.dp) {
                    CardFact(
                        icon = feedback.category.icon,
                        text = stringResource(feedback.category.displayNameRes),
                        placeholder = placeholder,
                        contentDescription = null
                    )
                    if (feedback.commentCount > 0) {
                        CardFact(
                            icon = Icons.Outlined.ChatBubbleOutline,
                            text = formatCount(feedback.commentCount),
                            placeholder = placeholder,
                            contentDescription = quantityString(R.plurals.feedbackkit_comment_count, feedback.commentCount)
                        )
                    }
                }
            },
            trailing = {
                VoteCapsule(
                    presentation = presentation,
                    voteCount = controller.voteCount,
                    onToggle = onToggle,
                    errorMessage = controller.error?.localizedUserMessage()
                )
            }
        )
    }
}

/** A footer fact: a glyph and a short label, in the secondary colour, on one line. */
@Composable
private fun CardFact(
    icon: ImageVector,
    text: String,
    placeholder: Boolean,
    contentDescription: String?
) {
    val style = MaterialTheme.typography.bodySmall
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    val semantics = if (contentDescription != null) {
        Modifier.clearAndSetSemantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    Row(
        modifier = semantics,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (placeholder) placeholderColor() else color,
            modifier = Modifier.size(iconSizeFor(style.fontSize, 1.34f))
        )
        ShowcaseText(text = text, style = style, color = color, placeholder = placeholder)
    }
}
