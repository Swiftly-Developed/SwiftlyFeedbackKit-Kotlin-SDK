package com.swiftlydeveloped.feedbackkit.ui.showcase

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.getfeedbackkit.feedbackkit.R

internal const val VOTE_CAPSULE_TAG = "fbk-list-vote"
internal const val VOTE_CLOSED_TAG = "fbk-list-vote-closed"

private val CapsulePadding = PaddingValues(start = 12.dp, top = 8.dp, end = 16.dp, bottom = 8.dp)

/**
 * The Showcase vote control (FEEDBACK01 §6.4). Tonal while not voted, filled while voted, and
 * plain text, not a control, once voting is closed. TalkBack reads one element: "12 votes",
 * then "Voted" or "Not voted", then the action ("Vote" / "Remove vote").
 *
 * The arrow is not mirrored in right-to-left layouts: it means "up", not "forward".
 */
@Composable
internal fun VoteCapsule(
    presentation: VotePresentation,
    voteCount: Int,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null
) {
    val countText = formatCount(voteCount)
    val countStyle = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum")
    val iconSize = iconSizeFor(countStyle.fontSize, 1.25f)

    when (presentation) {
        VotePresentation.Placeholder -> {
            Surface(
                shape = CircleShape,
                color = placeholderColor(),
                modifier = modifier
                    .padding(vertical = 4.dp)
                    .heightIn(min = 40.dp)
                    .clearAndSetSemantics {}
            ) {
                Row(Modifier.padding(CapsulePadding), verticalAlignment = Alignment.CenterVertically) {
                    CapsuleContent(countText, countStyle, iconSize, Color.Transparent)
                }
            }
        }

        VotePresentation.Closed -> {
            val label = quantityString(R.plurals.feedbackkit_vote_count, voteCount)
            val closed = stringResource(R.string.feedbackkit_vote_closed)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier
                    .testTag(VOTE_CLOSED_TAG)
                    .clearAndSetSemantics {
                        contentDescription = label
                        stateDescription = closed
                    }
                    .heightIn(min = 48.dp)
                    .padding(CapsulePadding)
            ) {
                CapsuleContent(countText, countStyle, iconSize, MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        is VotePresentation.Votable -> {
            val label = quantityString(R.plurals.feedbackkit_vote_count, voteCount)
            val state = stringResource(
                if (presentation.voted) R.string.feedbackkit_vote_state_voted else R.string.feedbackkit_vote_state_not_voted
            )
            val action = stringResource(
                if (presentation.voted) R.string.feedbackkit_remove_vote else R.string.feedbackkit_vote
            )
            // clearAndSetSemantics alone does not make a merge boundary, so without the explicit
            // mergeDescendants the capsule would fold into the clickable card and TalkBack could
            // not focus it on its own.
            val semanticsModifier = modifier
                .testTag(VOTE_CAPSULE_TAG)
                .semantics(mergeDescendants = true) {}
                .clearAndSetSemantics {
                    contentDescription = label
                    stateDescription = state
                    role = Role.Button
                    onClick(label = action) { onToggle(); true }
                    if (errorMessage != null) error(errorMessage)
                }
            if (presentation.voted) {
                Button(onClick = onToggle, modifier = semanticsModifier, contentPadding = CapsulePadding) {
                    CapsuleContent(countText, countStyle, iconSize, null)
                }
            } else {
                FilledTonalButton(onClick = onToggle, modifier = semanticsModifier, contentPadding = CapsulePadding) {
                    CapsuleContent(countText, countStyle, iconSize, null)
                }
            }
        }
    }
}

/** Arrow and count. A null [tint] inherits the button's content colour. */
@Composable
private fun RowScope.CapsuleContent(
    countText: String,
    style: TextStyle,
    iconSize: Dp,
    tint: Color?
) {
    if (tint == null) {
        Icon(Icons.Filled.ArrowUpward, contentDescription = null, modifier = Modifier.size(iconSize))
        Spacer(Modifier.width(6.dp))
        Text(countText, style = style)
    } else {
        Icon(Icons.Filled.ArrowUpward, contentDescription = null, modifier = Modifier.size(iconSize), tint = tint)
        Spacer(Modifier.width(6.dp))
        Text(countText, style = style, color = tint)
    }
}
