package com.swiftlydeveloped.feedbackkit.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.swiftlydeveloped.feedbackkit.Environment
import com.swiftlydeveloped.feedbackkit.FeedbackKit
import com.swiftlydeveloped.feedbackkit.configure
import com.swiftlydeveloped.feedbackkit.models.Feedback
import com.swiftlydeveloped.feedbackkit.state.FeedbackKitProvider
import com.swiftlydeveloped.feedbackkit.state.rememberFeedbackListState
import com.swiftlydeveloped.feedbackkit.theme.FeedbackKitTheme
import com.swiftlydeveloped.feedbackkit.ui.FeedbackDetailView
import com.swiftlydeveloped.feedbackkit.ui.FeedbackList
import com.swiftlydeveloped.feedbackkit.ui.SubmitFeedbackView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure FeedbackKit
        FeedbackKit.configure(this) {
            // Replace with your actual API key
            apiKey = "your-api-key-here"

            // Use local environment for development
            // For Android emulator, use LOCAL (10.0.2.2)
            // For physical device on same network, use LOCAL_DEVICE or custom baseUrl
            environment = Environment.LOCAL

            // Optional: Set a user ID for voting
            userId = "test-user-123"

            // Enable debug logging
            debug = true
        }

        setContent {
            FeedbackKitExampleApp()
        }
    }
}

@Composable
fun FeedbackKitExampleApp() {
    val isDarkTheme = isSystemInDarkTheme()
    val theme = if (isDarkTheme) FeedbackKitTheme.Dark else FeedbackKitTheme.Light

    FeedbackKitProvider(theme = theme) {
        val navController = rememberNavController()
        var selectedFeedback by remember { mutableStateOf<Feedback?>(null) }
        val listState = rememberFeedbackListState()

        NavHost(
            navController = navController,
            startDestination = "list"
        ) {
            composable("list") {
                Scaffold(
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { navController.navigate("submit") },
                            containerColor = theme.primaryColor
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Submit Feedback",
                                tint = theme.backgroundColor
                            )
                        }
                    },
                    containerColor = theme.backgroundColor
                ) { padding ->
                    FeedbackList(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        state = listState,
                        onFeedbackClick = { feedback ->
                            selectedFeedback = feedback
                            navController.navigate("detail")
                        },
                        theme = theme
                    )
                }
            }

            composable("detail") {
                selectedFeedback?.let { feedback ->
                    FeedbackDetailView(
                        feedback = feedback,
                        onBack = { navController.popBackStack() },
                        onVoteChange = { response ->
                            // Update the feedback in the list
                            selectedFeedback = feedback.withVote(
                                response.hasVoted,
                                response.voteCount
                            )
                            listState.updateFeedback(selectedFeedback!!)
                        },
                        theme = theme
                    )
                }
            }

            composable("submit") {
                SubmitFeedbackView(
                    onBack = { navController.popBackStack() },
                    onSubmitSuccess = { newFeedback ->
                        // Add the new feedback to the list and go back
                        listState.addFeedback(newFeedback)
                        navController.popBackStack()
                    },
                    theme = theme
                )
            }
        }
    }
}
