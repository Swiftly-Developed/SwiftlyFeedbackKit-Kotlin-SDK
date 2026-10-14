package com.swiftlydeveloped.feedbackkit.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.swiftlydeveloped.feedbackkit.ui.FeedbackListScreen
import com.swiftlydeveloped.feedbackkit.ui.FeedbackListStyle
import com.swiftlydeveloped.feedbackkit.ui.SubmitFeedbackView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val apiKey = intent.getStringExtra("api_key") ?: "your-api-key-here"
        val baseUrl = intent.getStringExtra("base_url")
        val screen = intent.getStringExtra("screen") ?: "list"
        val detailTitle = intent.getStringExtra("feedback_title") ?: ""
        val themeName = intent.getStringExtra("theme")
        val listStyle = if (intent.getStringExtra("list_style") == "classic") {
            FeedbackListStyle.CLASSIC
        } else {
            FeedbackListStyle.SHOWCASE
        }

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
                themeName = themeName,
                listStyle = listStyle
            )
        }
    }
}

@Composable
fun FeedbackKitExampleApp(
    startScreen: String = "list",
    detailTitle: String = "",
    themeName: String? = null,
    listStyle: FeedbackListStyle = FeedbackListStyle.SHOWCASE
) {
    val systemDark = isSystemInDarkTheme()
    val isDarkTheme = when (themeName) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }
    val theme = if (isDarkTheme) FeedbackKitTheme.Dark else FeedbackKitTheme.Light
    // The Showcase list takes its colours from MaterialTheme, so wallpaper colours (Android 12+)
    // flow straight into it.
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        isDarkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(colorScheme = colorScheme) {
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
                    FeedbackListScreen(
                        state = listState,
                        title = "Trailmate",
                        listStyle = listStyle,
                        onFeedbackClick = { feedback ->
                            selectedFeedback = feedback
                            navController.navigate("detail")
                        },
                        onAddFeedback = { navController.navigate("submit") }
                    )
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
}
