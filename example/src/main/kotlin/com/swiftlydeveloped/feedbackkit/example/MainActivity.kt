package com.swiftlydeveloped.feedbackkit.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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

        val apiKey = intent.getStringExtra("api_key") ?: "your-api-key-here"
        val baseUrl = intent.getStringExtra("base_url")
        val screen = intent.getStringExtra("screen") ?: "list"
        val detailTitle = intent.getStringExtra("feedback_title") ?: ""
        val themeName = intent.getStringExtra("theme")

        // Configure FeedbackKit. Intent extras let the screenshot script point
        // at a local server without editing this file again.
        FeedbackKit.configure(this) {
            this.apiKey = apiKey
            environment = Environment.LOCAL
            if (!baseUrl.isNullOrBlank()) {
                this.baseUrl = baseUrl
            }
            userId = "priya-nair"
            debug = true
        }

        val darkBackground = themeName == "dark"
        window.decorView.setBackgroundColor(if (darkBackground) 0xFF111827.toInt() else 0xFFFFFFFF.toInt())

        setContent {
            FeedbackKitExampleApp(
                startScreen = screen,
                detailTitle = detailTitle,
                themeName = themeName
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackKitExampleApp(
    startScreen: String = "list",
    detailTitle: String = "",
    themeName: String? = null
) {
    val systemDark = isSystemInDarkTheme()
    val isDarkTheme = when (themeName) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }
    val theme = if (isDarkTheme) FeedbackKitTheme.Dark else FeedbackKitTheme.Light

    FeedbackKitProvider(theme = theme) {
        val navController = rememberNavController()
        var selectedFeedback by remember { mutableStateOf<Feedback?>(null) }
        val listState = rememberFeedbackListState()
        val startDestination = if (startScreen == "list") "list" else "boot"

        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable("boot") {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (listState.error != null) {
                        Text(text = "Could not load feedback", color = theme.textColor)
                    } else if (
                        startScreen == "detail" &&
                        !listState.isLoading &&
                        listState.feedbackItems.none { it.title == detailTitle }
                    ) {
                        Text(text = "Missing feedback", color = theme.textColor)
                    } else {
                        CircularProgressIndicator(color = theme.primaryColor)
                    }
                }
                LaunchedEffect(startScreen, listState.isLoading, listState.feedbackItems) {
                    if (startScreen == "submit") {
                        navController.navigate("submit") {
                            popUpTo("boot") { inclusive = true }
                        }
                    } else if (startScreen == "detail" && !listState.isLoading) {
                        val match = listState.feedbackItems.firstOrNull { it.title == detailTitle }
                        if (match != null) {
                            selectedFeedback = match
                            navController.navigate("detail") {
                                popUpTo("boot") { inclusive = true }
                            }
                        }
                    }
                }
            }

            composable("list") {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Trailmate") },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = theme.backgroundColor,
                                titleContentColor = theme.textColor
                            )
                        )
                    },
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
                        listState.addFeedback(newFeedback)
                        navController.popBackStack()
                    },
                    theme = theme
                )
            }
        }
    }
}
