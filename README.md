# SwiftlyFeedbackKit-Kotlin

Android SDK for the Swiftly Feedback platform. Built with Kotlin, Coroutines, and Jetpack Compose.

## Installation

Add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.swiftlydeveloped:feedbackkit:1.0.0")
}
```

Or add via Maven:

```xml
<dependency>
    <groupId>com.swiftlydeveloped</groupId>
    <artifactId>feedbackkit</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

### 1. Configure the SDK

Initialize FeedbackKit in your Application class or MainActivity:

```kotlin
import com.swiftlydeveloped.feedbackkit.FeedbackKit
import com.swiftlydeveloped.feedbackkit.configure

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        FeedbackKit.configure(this) {
            apiKey = "your-api-key"
            userId = "user-123" // Optional: for voting
            debug = true // Optional: enable logging
        }
    }
}
```

### 2. Add FeedbackKit UI

Use the pre-built Compose components:

```kotlin
import com.swiftlydeveloped.feedbackkit.ui.FeedbackList
import com.swiftlydeveloped.feedbackkit.state.FeedbackKitProvider

@Composable
fun FeedbackScreen() {
    FeedbackKitProvider {
        FeedbackList(
            onFeedbackClick = { feedback ->
                // Navigate to detail view
            }
        )
    }
}
```

### 3. Submit Feedback Form

```kotlin
import com.swiftlydeveloped.feedbackkit.ui.SubmitFeedbackView

@Composable
fun SubmitScreen() {
    SubmitFeedbackView(
        onBack = { /* Navigate back */ },
        onSubmitSuccess = { feedback ->
            // Handle successful submission
        }
    )
}
```

## Features

- **Pre-built UI Components**: FeedbackList, FeedbackCard, FeedbackDetailView, SubmitFeedbackView
- **Optimistic Updates**: Vote buttons update immediately for a responsive UX
- **Pull-to-Refresh**: Built-in refresh functionality
- **Theming**: Light and dark themes with full customization
- **State Management**: Built-in state holders with Compose integration
- **Error Handling**: Comprehensive error types with user-friendly messages
- **Persistent Storage**: User preferences persisted with DataStore

## Configuration Options

```kotlin
FeedbackKit.configure(context) {
    apiKey = "your-api-key"           // Required: Your API key
    userId = "user-123"               // Optional: User ID for voting
    environment = Environment.PRODUCTION  // Or STAGING, LOCAL
    baseUrl = "https://custom.api.com"   // Optional: Custom base URL
    timeout = 30_000L                 // Optional: Request timeout in ms
    debug = false                     // Optional: Enable debug logging
}
```

## API Usage

### Feedback Operations

```kotlin
// List feedback
val feedbackList = FeedbackKit.shared.feedback.list()

// With filters
val filtered = FeedbackKit.shared.feedback.list(
    ListFeedbackOptions(
        status = FeedbackStatus.APPROVED,
        category = FeedbackCategory.FEATURE_REQUEST
    )
)

// Get single feedback
val feedback = FeedbackKit.shared.feedback.get("feedback-id")

// Create feedback
val newFeedback = FeedbackKit.shared.feedback.create(
    title = "New Feature Request",
    description = "Please add dark mode",
    category = FeedbackCategory.FEATURE_REQUEST,
    email = "user@example.com" // Optional
)
```

### Voting

```kotlin
// Vote for feedback
val response = FeedbackKit.shared.votes.vote("feedback-id")

// Remove vote
val response = FeedbackKit.shared.votes.unvote("feedback-id")

// Toggle vote
val response = FeedbackKit.shared.votes.toggleVote(
    feedbackId = "feedback-id",
    hasVoted = currentState
)
```

### Comments

```kotlin
// List comments
val comments = FeedbackKit.shared.comments.list("feedback-id")

// Add comment
val comment = FeedbackKit.shared.comments.create(
    feedbackId = "feedback-id",
    content = "Great idea!",
    userName = "John"
)
```

### User Management

```kotlin
// Register a user
val user = FeedbackKit.shared.users.register(
    email = "user@example.com",
    name = "John Doe",
    externalId = "your-system-user-id"
)

// Set user ID
FeedbackKit.shared.userId = user.id

// Persist user ID across sessions
FeedbackKit.shared.setUserIdAndPersist(user.id)
```

### Event Tracking

```kotlin
// Track an event
FeedbackKit.shared.events.track(
    name = "feedback_viewed",
    properties = mapOf("feedback_id" to "123")
)
```

## UI Components

### FeedbackList

```kotlin
FeedbackList(
    modifier = Modifier.fillMaxSize(),
    statusFilter = FeedbackStatus.APPROVED,
    categoryFilter = FeedbackCategory.FEATURE_REQUEST,
    onFeedbackClick = { feedback -> /* Handle click */ }
)
```

### FeedbackDetailView

```kotlin
FeedbackDetailView(
    feedback = selectedFeedback,
    onBack = { /* Navigate back */ },
    onVoteChange = { response -> /* Update state */ },
    showAppBar = true
)
```

### SubmitFeedbackView

```kotlin
SubmitFeedbackView(
    onBack = { /* Navigate back */ },
    onSubmitSuccess = { feedback -> /* Handle success */ },
    showAppBar = true
)
```

### Individual Components

```kotlin
// Status badge
StatusBadge(status = FeedbackStatus.APPROVED)

// Category badge
CategoryBadge(category = FeedbackCategory.BUG_REPORT)

// Vote button
VoteButton(
    feedback = feedback,
    onVoteChange = { response -> /* Update state */ }
)

// Feedback card
FeedbackCard(
    feedback = feedback,
    onClick = { /* Handle click */ },
    onVoteChange = { response -> /* Update state */ }
)
```

## Theming

### Using Built-in Themes

```kotlin
// Light theme (default)
FeedbackKitProvider(theme = FeedbackKitTheme.Light) {
    FeedbackList()
}

// Dark theme
FeedbackKitProvider(theme = FeedbackKitTheme.Dark) {
    FeedbackList()
}

// System theme
val theme = if (isSystemInDarkTheme()) {
    FeedbackKitTheme.Dark
} else {
    FeedbackKitTheme.Light
}
```

### Custom Theme

```kotlin
val customTheme = FeedbackKitTheme(
    primaryColor = Color(0xFF6200EE),
    backgroundColor = Color(0xFFF5F5F5),
    cardBackgroundColor = Color.White,
    textColor = Color.Black,
    secondaryTextColor = Color.Gray,
    borderRadius = 16f,
    statusColors = StatusColors(
        approved = Color(0xFF4CAF50),
        inProgress = Color(0xFFFF9800)
    )
)

FeedbackKitProvider(theme = customTheme) {
    FeedbackList()
}
```

## Error Handling

All errors are subclasses of `FeedbackKitError`:

```kotlin
try {
    val feedback = FeedbackKit.shared.feedback.get("id")
} catch (e: FeedbackKitError) {
    when (e) {
        is FeedbackKitError.AuthenticationError -> // Invalid API key
        is FeedbackKitError.PaymentRequiredError -> // Subscription needed
        is FeedbackKitError.ForbiddenError -> // No permission
        is FeedbackKitError.NotFoundError -> // Resource not found
        is FeedbackKitError.ConflictError -> // Already voted, etc.
        is FeedbackKitError.ValidationError -> // Invalid input
        is FeedbackKitError.NetworkError -> // Network issue
        is FeedbackKitError.ServerError -> // Server error
        is FeedbackKitError.UnknownError -> // Unknown error
    }

    // User-friendly message
    showToast(e.userMessage)

    // Check if retryable
    if (e.isRecoverable) {
        showRetryButton()
    }
}
```

## Requirements

- Android SDK 24+ (Android 7.0+)
- Kotlin 1.9+
- Jetpack Compose BOM 2024.01.00+

## License

Copyright 2024 Swiftly Developed. All rights reserved.
