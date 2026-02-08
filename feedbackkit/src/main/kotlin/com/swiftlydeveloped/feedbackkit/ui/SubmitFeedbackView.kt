package com.swiftlydeveloped.feedbackkit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swiftlydeveloped.feedbackkit.FeedbackKit
import com.swiftlydeveloped.feedbackkit.errors.FeedbackKitError
import com.swiftlydeveloped.feedbackkit.errors.userMessage
import com.swiftlydeveloped.feedbackkit.models.CreateFeedbackRequest
import com.swiftlydeveloped.feedbackkit.models.Feedback
import com.swiftlydeveloped.feedbackkit.models.FeedbackCategory
import com.swiftlydeveloped.feedbackkit.state.LocalFeedbackKitTheme
import com.swiftlydeveloped.feedbackkit.theme.FeedbackKitTheme
import kotlinx.coroutines.launch

/**
 * Form composable for submitting new feedback.
 *
 * @param modifier Modifier for the view.
 * @param onBack Callback when the back button is pressed.
 * @param onSubmitSuccess Callback when feedback is successfully submitted.
 * @param showAppBar Whether to show the app bar with back button.
 * @param theme Optional theme override.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitFeedbackView(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onSubmitSuccess: ((Feedback) -> Unit)? = null,
    showAppBar: Boolean = true,
    theme: FeedbackKitTheme = LocalFeedbackKitTheme.current
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(FeedbackCategory.FEATURE_REQUEST) }
    var email by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<FeedbackKitError?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val isValid = title.isNotBlank() && description.isNotBlank()

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = theme.primaryColor,
        unfocusedBorderColor = theme.borderColor,
        focusedLabelColor = theme.primaryColor,
        unfocusedLabelColor = theme.secondaryTextColor,
        cursorColor = theme.primaryColor,
        focusedTextColor = theme.textColor,
        unfocusedTextColor = theme.textColor,
        errorBorderColor = theme.errorColor,
        errorLabelColor = theme.errorColor
    )

    fun submit() {
        if (!isValid || isSubmitting) return

        scope.launch {
            isSubmitting = true
            error = null

            try {
                val request = CreateFeedbackRequest(
                    title = title.trim(),
                    description = description.trim(),
                    category = category,
                    email = email.takeIf { it.isNotBlank() }?.trim()
                )

                val feedback = FeedbackKit.shared.feedback.create(request)
                onSubmitSuccess?.invoke(feedback)
            } catch (e: Exception) {
                error = FeedbackKitError.fromException(e)
            } finally {
                isSubmitting = false
            }
        }
    }

    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(theme.spacing.dp * 2),
            verticalArrangement = Arrangement.spacedBy(theme.spacing.dp * 2)
        ) {
            // Title field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                placeholder = { Text("Brief summary of your feedback") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = textFieldColors,
                shape = RoundedCornerShape(theme.borderRadius.dp),
                enabled = !isSubmitting
            )

            // Category dropdown
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { if (!isSubmitting) categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = category.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = textFieldColors,
                    shape = RoundedCornerShape(theme.borderRadius.dp),
                    enabled = !isSubmitting
                )

                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    FeedbackCategory.entries.forEach { cat ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(theme.spacing.dp)
                                ) {
                                    CategoryBadge(category = cat, theme = theme)
                                    Text(cat.displayName)
                                }
                            },
                            onClick = {
                                category = cat
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // Description field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                placeholder = { Text("Provide details about your feedback...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                colors = textFieldColors,
                shape = RoundedCornerShape(theme.borderRadius.dp),
                enabled = !isSubmitting
            )

            // Email field (optional)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email (optional)") },
                placeholder = { Text("Get notified about updates") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = textFieldColors,
                shape = RoundedCornerShape(theme.borderRadius.dp),
                enabled = !isSubmitting
            )

            // Error message
            error?.let {
                Text(
                    text = it.userMessage,
                    color = theme.errorColor,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(theme.spacing.dp))

            // Submit button
            Button(
                onClick = { submit() },
                modifier = Modifier.fillMaxWidth(),
                enabled = isValid && !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.primaryColor
                ),
                shape = RoundedCornerShape(theme.borderRadius.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = theme.backgroundColor,
                        modifier = Modifier.height(24.dp)
                    )
                } else {
                    Text(
                        text = "Submit Feedback",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = theme.spacing.dp)
                    )
                }
            }
        }
    }

    if (showAppBar) {
        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = { Text("Submit Feedback") },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = theme.backgroundColor
                    )
                )
            },
            containerColor = theme.backgroundColor
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                content()
            }
        }
    } else {
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun SubmitFeedbackViewPreview() {
    SubmitFeedbackView(showAppBar = false)
}
