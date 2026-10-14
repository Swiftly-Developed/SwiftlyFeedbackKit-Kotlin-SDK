# SwiftlyFeedbackKit-Kotlin

> **Install `com.getfeedbackkit:feedbackkit:1.0.1` from Maven Central** (published 9 February 2026). The Kotlin package inside that artifact is `com.swiftlydeveloped.feedbackkit`. This checkout is **2.1.0**: git tag `2.1.0` on the public mirror, not on Maven Central. Detail and submit composables match 1.0.1. The Showcase list (`FeedbackListScreen`, search, sort and filter, and `listStyle`) is 2.1.0 only. Anonymous user ids, `votes.vote(email, notifyStatusChange)`, `users.register(userId, mrr)`, and `FeedbackKit.shared.metrics` are 2.1.0 only. 1.0.1's `vote` has `notifyOnStatusChange` only, and that build wrote a JSON key the server does not read. Step-by-step samples: [docs/getting-started.md](docs/getting-started.md).
>
> The public mirror [SwiftlyFeedbackKit-Kotlin-SDK](https://github.com/Swiftly-Developed/SwiftlyFeedbackKit-Kotlin-SDK) has tags `1.1.0`, `1.1.1`, `1.2.0` and `2.1.0`. `1.2.0` and `2.1.0` were tagged 14 October 2026. Its `main` is this tree. Tag `1.2.0` is 1.1.1 plus fixes (the `R` import compile fix, nullable `Comment.feedbackId`, the `is_admin` and `user_email` keys, the committed Gradle wrapper jar). It has no metrics API, no anonymous id and no Showcase list. Its README shows `com.swiftlydeveloped:feedbackkit:1.2.0`, which was never the Maven group and is not on Maven Central. No tag is on Maven Central. To use `1.2.0` or `2.1.0`, check out the tag and depend on its `:feedbackkit` module.
>
> There is no pull-to-refresh gesture. `FeedbackListState.refresh()` sets `isRefreshing`, and the list draws a progress indicator while that flag is true.

Android SDK for the Swiftly Feedback platform. Built with Kotlin, Coroutines, and Jetpack Compose.

## Installation

The library is published on Maven Central as `com.getfeedbackkit:feedbackkit`. **1.0.1** is the latest published version. `1.1.x`, `1.2.0` and `2.1.0` are git tags on the public mirror only. They are not on Maven Central.

Declare Maven Central, then add the dependency in your app module's `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.getfeedbackkit:feedbackkit:1.0.1")
}
```

Groovy (`build.gradle`):

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'com.getfeedbackkit:feedbackkit:1.0.1'
}
```

Or add via Maven:

```xml
<dependency>
    <groupId>com.getfeedbackkit</groupId>
    <artifactId>feedbackkit</artifactId>
    <version>1.0.1</version>
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
            userId = "user-123" // Optional: omit to use a persisted anonymous id
            debug = true // Optional: enable logging
        }
    }
}
```

### 2. Add FeedbackKit UI

Use the pre-built Compose components inside your app's `MaterialTheme`:

```kotlin
import com.swiftlydeveloped.feedbackkit.state.FeedbackKitProvider
import com.swiftlydeveloped.feedbackkit.ui.FeedbackListScreen

@Composable
fun FeedbackScreen() {
    FeedbackKitProvider {
        FeedbackListScreen(
            onFeedbackClick = { feedback ->
                // Navigate to detail view
            },
            onAddFeedback = { /* Open SubmitFeedbackView */ }
        )
    }
}
```

`FeedbackListScreen` is a full screen: a top app bar with a sort and filter button, a search field, the list and an add button. Use `FeedbackList` instead when you already have your own scaffold.

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

- **Pre-built UI Components**: FeedbackListScreen, FeedbackList, FeedbackCard, FeedbackDetailView, SubmitFeedbackView
- **Showcase list**: Material 3 cards with a status eyebrow, a vote button, search, sort, and status and category filters. It follows your `MaterialTheme`, including dynamic colour and dark theme, and supports TalkBack, right-to-left layouts and font scales up to 200%
- **Optimistic Updates**: Vote buttons update immediately for a responsive UX
- **Theming**: Light and dark themes with full customization
- **State Management**: Built-in state holders with Compose integration
- **Error Handling**: Comprehensive error types with user-friendly messages
- **Persistent Storage**: The anonymous user id is kept in private SharedPreferences. Email and name still use DataStore.

## Configuration Options

```kotlin
FeedbackKit.configure(context) {
    apiKey = "your-api-key"           // Required: Your API key
    userId = "user-123"               // Optional: omit to use a persisted anonymous id
    environment = Environment.PRODUCTION  // Or STAGING, LOCAL
    timeout = 30_000L                 // Optional: Request timeout in ms
    debug = false                     // Optional: Enable debug logging
}
```

### Anonymous user id

Leave `userId` unset and `configure()` generates one id in the form `local_<uuid>` (a lowercase UUID v4, the same shape Flutter stores and the same `local_` prefix Swift uses). It is written to SharedPreferences in private mode, under the name and key `feedbackkit_user_id`, so the next launch reuses it. Maven Central 1.0.1 does not generate this id. A vote with no id on that artifact throws `FeedbackKitError.ValidationError`.

Assigning `FeedbackKit.shared.userId` replaces that id and stores the new value in SharedPreferences. An id passed to `configure()` does the same, and it wins over whatever was already stored. Assigning a blank value clears SharedPreferences and does not mint a replacement. `configure()` returns the instance it already created, so a later call in the same process does not generate an id.

`logout()` clears SharedPreferences and DataStore. The next process start, with both stores empty, generates a fresh `local_<uuid>`. If you only blank `userId` and leave DataStore populated, that next start copies the DataStore id back.

`feedback.list`, `feedback.create`, `votes.vote`, `votes.unvote`, and `comments.create` send the id. List puts it on `user_id` and `X-User-Id`. Submit, vote, unvote, and comment also send it as `user_id` in the body when that call omits one.

`setUserIdAndPersist` writes DataStore as well as SharedPreferences. On `configure()`, if SharedPreferences has no id and the host did not pass one, a non-blank id already in DataStore is copied into SharedPreferences and used, so an upgraded app keeps the same user. A new `local_<uuid>` is generated only when both stores are empty.

`users.getCurrentUser()` requests `GET /users/{id}`. The server does not register that route, so the call 404s. Read `FeedbackKit.shared.userId` after `configure()` instead.

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

// Vote with a status-change email opt-in
val response = FeedbackKit.shared.votes.vote(
    feedbackId = "feedback-id",
    email = "user@example.com",
    notifyStatusChange = true
)

// Remove vote. VoteButton uses this overload and does not pass email
// or notifyStatusChange. 1.0.1 has notifyOnStatusChange only, and no email parameter.
// 1.0.1 and tag 1.2.0 unvote put user_id in the query string. The server reads the DELETE
// body, so that call returns 400. 2.1.0 sends user_id in the JSON body.
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
// Register a user. The body is user_id plus an optional mrr.
// email and name are ignored by the server.
// SdkUser.id is the row id. Votes and comments use SdkUser.userId.
val user = FeedbackKit.shared.users.register(
    userId = "your-system-user-id",
    mrr = 9.99
)

// After a successful register, keep the SDK user id, not the row id.
// Assigning userId persists it in SharedPreferences. setUserIdAndPersist
// also writes DataStore.
FeedbackKit.shared.userId = user.userId
FeedbackKit.shared.setUserIdAndPersist(user.userId)
```

### Event Tracking

```kotlin
// Track an event
FeedbackKit.shared.events.track(
    name = "feedback_viewed",
    properties = mapOf("feedback_id" to "123")
)

// The three names FeedbackKit itself defines. Everything else is a CUSTOM event,
// and the distinction decides what a Free-tier metrics read can count.
FeedbackKit.shared.events.track(PredefinedEventNames.FEEDBACK_LIST)
```

`events.track` works on **every** plan, including Free. Events you track while your plan
cannot read them back are still ingested and retained — they become readable the moment you
upgrade, with no re-tracking.

### Metrics

Four read operations, all on the API key, all rate limited to **60 requests per 60 seconds
shared across the family**. The SDK does not retry or back off; a retry loop would hide the
cap from you.

```kotlin
val series = FeedbackKit.shared.metrics.timeseries(
    MetricTimeseriesOptions(
        measures = listOf(MetricMeasure.EVENT_COUNT, MetricMeasure.FEEDBACK_COUNT),
        granularity = MetricGranularity.DAY,
        days = 7
    )
)

// Bucket keys are expressed in the reporting zone the server actually applied — print it
// beside them rather than re-interpreting a bucket in the device's zone.
println(series.meta.timezone)
for (point in series.series) {
    println("${point.bucket}: ${point.measure(MetricMeasure.EVENT_COUNT) ?: 0.0}")
}
```

**Empty buckets are present and zero-valued, never omitted.** `series.series.isEmpty()` is a
branch that never fires for a valid window — handle the all-zero case instead.

**`values` is sparse.** Every typed accessor returns `Double?`, and that is not defensive
padding: a breakdown past the server's cardinality ceiling collapses its tail into one
overflow row whose `values` omits `unique_event_users` entirely, because a distinct-user
count is not additive across the names that were elided.

```kotlin
val byEvent = FeedbackKit.shared.metrics.breakdown(
    MetricBreakdownOptions(
        measures = listOf(MetricMeasure.EVENT_COUNT, MetricMeasure.UNIQUE_EVENT_USERS),
        dimension = MetricDimension.EVENT_NAME,
        days = 30
    )
)

for (item in byEvent.items) {
    val users = item.measure(MetricMeasure.UNIQUE_EVENT_USERS)   // null on the overflow row
    println("${item.key}: ${users?.toString() ?: "n/a"}")
}
```

#### What your plan changes — and what it does not

Reading metrics is **not** a paid feature, and none of the three sentences below is
"upgrade for metrics". All three are **`200`s**, and each one tells you what it did:

1. Every plan reads metrics.
2. Below Pro, only the three predefined event names are counted. The response says so:
   `meta.eventScope == MetricEventScope.PREDEFINED_ONLY`. Your own event names keep
   accruing while unreadable.
3. A window wider than your plan allows comes back **clamped**, not refused, with
   `meta.retentionClamped == true` and `meta.retentionWindowDays` carrying the allowance
   (Free 30 days, Pro 90, Team 365). Read that number; never assume one.

```kotlin
val summary = FeedbackKit.shared.metrics.summary(
    MetricSummaryOptions(measures = listOf(MetricMeasure.EVENT_COUNT), days = 400)
)

if (summary.meta.eventScope == MetricEventScope.PREDEFINED_ONLY) {
    // Render an upsell from THIS, not from a catch block — nothing was thrown.
    showUpgradePrompt()
}
if (summary.meta.retentionClamped) {
    showNotice("Showing the last ${summary.meta.retentionWindowDays} days on your plan.")
}
```

Only three requests have no honest narrower answer and do raise a **402**
(`METRICS_CUSTOM_EVENTS_REQUIRE_PRO`): a breakdown by event name, a custom `eventName`
filter, and the raw event feed.

#### Paging the raw event feed

The feed's response carries **two** meta blocks: `meta` is pagination, `metrics` is the
window-and-scope block every other operation puts under `meta`.

Pass an explicit `dateTo` so the window stays still while you page, then advance it on the
next sync. There is no automatic page following, for the same reason there is no retry.

```kotlin
var page = 1
val until = "2023-11-14T05:00:00Z"
while (true) {
    val feed = FeedbackKit.shared.metrics.events(
        MetricEventsOptions(
            measures = listOf(MetricMeasure.EVENT_COUNT),
            dateFrom = lastSyncedAt,
            dateTo = until,
            page = page,
            perPage = 100
        )
    )
    store(feed.items)
    if (page >= feed.meta.totalPages) break
    page += 1
}
```

## UI Components

### FeedbackListScreen

```kotlin
FeedbackListScreen(
    state = rememberFeedbackListState(),
    onFeedbackClick = { feedback -> /* Open detail */ },
    onAddFeedback = { /* Open submit */ },
    onBack = null,                     // Shows a back arrow when set
    showStatusFilter = true,           // Sections in the sort and filter sheet
    showCategoryFilter = true,
    listStyle = FeedbackListStyle.SHOWCASE
)
```

Sorting is Votes (default), Newest, Oldest or Comments. The choice is saved across launches. Search and the status and category filters are applied to the loaded items, last for the session only, and survive a reload. While a filter is active the list shows a "Filtered by … Clear" row.

### FeedbackList

```kotlin
FeedbackList(
    modifier = Modifier.fillMaxSize(),
    statusFilter = FeedbackStatus.APPROVED,
    categoryFilter = FeedbackCategory.FEATURE_REQUEST,
    onFeedbackClick = { feedback -> /* Handle click */ },
    contentPadding = PaddingValues(bottom = 88.dp)
)
```

`FeedbackList` has no app bar of its own. To add search and the sort and filter sheet to your own app bar, pass the same `FeedbackListState` to `FeedbackListSearchField(state)` and `FeedbackSortFilterButton(state)`.

### List styles

```kotlin
FeedbackList(listStyle = FeedbackListStyle.SHOWCASE) // Default from 2.1.0
FeedbackList(listStyle = FeedbackListStyle.CLASSIC)  // The 1.x look
```

Showcase takes its surfaces, text and vote colours from your `MaterialTheme`, so dynamic colour (Android 12+) and dark theme follow the app. Only the status colours come from `FeedbackKitTheme.statusColors`. Each is darkened or lightened as needed to reach 4.5:1 contrast against the card. Classic keeps using `FeedbackKitTheme` for everything.

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
        is FeedbackKitError.RateLimitError -> // 429 — metrics are capped at 60 / 60s
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

### Branching on the server's own error code

`e.message` now carries the server's explanation, and `e.code` carries its machine-readable
token when the response had one. Branch on `e.code`, never on the message text:

```kotlin
catch (e: FeedbackKitError.PaymentRequiredError) {
    if (e.code == "METRICS_CUSTOM_EVENTS_REQUIRE_PRO") showUpgradePrompt()
}
```

Two error bodies exist and they are not interchangeable. Requests the metrics API rejects
itself carry a typed envelope with a `code`; the shared middleware (401, a suspended-project
403, 404, 429, 500) carries only a human-readable reason and **no code at all**, so `e.code`
falls back to this SDK's own class-level token there. A 403 arrives in **both** shapes: a
measure your key cannot read carries `METRICS_MEASURE_REQUIRES_BEARER`, while a role or
suspension denial carries no code — which is exactly why you cannot tell them apart from the
status alone.

## Privacy

Data this SDK sends is collected on your behalf. FeedbackKit is the processor. `users.register` sends `user_id` and, when you pass it, `mrr`. Email and a display name leave the device only when a submit, vote, or comment call includes them. Store-form answers: [App Store privacy](../docs/privacy/APP-STORE-PRIVACY.md), [Google Play Data safety](../docs/privacy/GOOGLE-PLAY-DATA-SAFETY.md).

## Plan limits

The Free plan keeps 10 feedback items visible per project. Create still returns the new item. The server stores the rest with `hiddenOverLimit` and omits them from list reads, so the board does not grow and nothing throws. That cap is not `PaymentRequiredError`.

`PaymentRequiredError` with `code == "METRICS_CUSTOM_EVENTS_REQUIRE_PRO"` is the metrics 402: a breakdown by event name, a custom `eventName` filter, or the raw event feed, on a plan below Pro. Other metrics reads succeed and say what they narrowed (`meta.eventScope`, `meta.retentionClamped`, `meta.retentionWindowDays`). Documented windows are Free 30 days, Pro 90, Team 365.

An archived project still lists. Create, vote, and comment come back as `ForbiddenError`. `completed` and `rejected` items cannot be voted. `VoteButton` stays disabled. A direct `votes.vote` is rejected by the server.

## Requirements

- Android SDK 24+ (Android 7.0+)
- Kotlin 1.9+
- Jetpack Compose BOM 2024.06.00+ (Material3 1.2.1). Gradle raises an older BOM in your app to this version.

## License

Copyright 2024 Swiftly Developed. All rights reserved.
