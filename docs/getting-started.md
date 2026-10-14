# Getting started

Add a feedback board to an Android app with the FeedbackKit Kotlin SDK: one Gradle line, one `configure` call, and the Jetpack Compose screens. The samples below are the **2.1.0** API in this repository.

## Which version you are installing

| Source | Coordinates | What it contains |
| --- | --- | --- |
| Maven Central (latest published) | `com.getfeedbackkit:feedbackkit:1.0.1` | List, detail, and submit composables. No anonymous user id. `vote` has `notifyOnStatusChange` only, and that build wrote the wrong JSON key. `users.register` has no `userId` or `mrr` parameter. No metrics API. |
| This repository (`VERSION_NAME` in `gradle.properties`) | `com.getfeedbackkit:feedbackkit:2.1.0` | Everything below. Git tag `2.1.0` on the public mirror. **Not on Maven Central.** |
| Public mirror [SwiftlyFeedbackKit-Kotlin-SDK](https://github.com/Swiftly-Developed/SwiftlyFeedbackKit-Kotlin-SDK) tag `1.2.0` | That tree's `gradle.properties` says `1.2.0`. Tagged 14 October 2026 | 1.1.1 plus fixes: it compiles, `Comment.feedbackId` is nullable, `is_admin` and `user_email` are read, the Gradle wrapper jar is committed. No metrics API, no anonymous id, no Showcase list. `vote` has `notifyOnStatusChange` only, `unvote` puts `user_id` in the query, and `users.register` has no `userId` or `mrr`. Its README shows `com.swiftlydeveloped:feedbackkit:1.2.0`, which is not the Maven group and was never published. Do not install from that README. |
| Public mirror tag `2.1.0` and `main` | `2.1.0`. Tagged 14 October 2026 | This checkout. Not on Maven Central. |

The Kotlin package inside the artifact is `com.swiftlydeveloped.feedbackkit`. The Maven group is `com.getfeedbackkit`.

`FeedbackList`, `FeedbackDetailView`, `SubmitFeedbackView` and `VoteButton` exist in 1.0.1. `FeedbackListScreen`, `FeedbackListStyle`, search, and sort and filter are 2.1.0 only. Anonymous ids, `votes.vote(email, notifyStatusChange)`, `users.register(userId, mrr)`, and `FeedbackKit.shared.metrics` are not in 1.0.1 either.

## Install

`minSdk` 24, Java 17, Kotlin 1.9 (this repo builds with 1.9.22), Jetpack Compose BOM `2024.06.00` or later (Material3 1.2.1).

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.getfeedbackkit:feedbackkit:1.0.1")
}
```

Use `1.0.1` from Maven. No mirror tag is on Maven Central. To exercise the 2.1.0 samples, depend on this module (`:feedbackkit`) from the repository, or from the mirror at tag `2.1.0`, instead of Maven.

The library manifest declares `INTERNET`. There is no extra manifest entry.

## Configure

Call `configure` once, from `Application.onCreate`. A second call in the same process returns the instance the first call created. A key changed later does not take effect.

```kotlin
import android.app.Application
import com.swiftlydeveloped.feedbackkit.Environment
import com.swiftlydeveloped.feedbackkit.FeedbackKit
import com.swiftlydeveloped.feedbackkit.configure

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FeedbackKit.configure(this) {
            apiKey = "your-api-key"
            environment = Environment.PRODUCTION
            debug = false
        }
    }
}
```

`configure` with a trailing lambda is an extension on the companion, so import it next to `FeedbackKit`. There is a second overload, `configure(context, apiKey) { userId("…"); environment(Environment.PRODUCTION) }`, whose lambda is `FeedbackKitConfig.Builder` and uses methods rather than properties. Both compile.

| `Environment` | Base URL |
| --- | --- |
| `PRODUCTION` | `https://api.prod.getfeedbackkit.com` |
| `STAGING` | `https://api.testflight.getfeedbackkit.com` |
| `LOCAL` | `http://10.0.2.2:8080` (emulator) |
| `LOCAL_DEVICE` | `http://localhost:8080` |

`Environment.LOCAL` is cleartext. A debug-only network security config has to allow `10.0.2.2`, or Android blocks it.

The API key is a project client credential. It belongs in the app binary. It must not be a key that can administer the project. Keep it out of git (`local.properties` or CI env, copied into `BuildConfig`).

Every request sends `Content-Type: application/json`, `Accept: application/json`, and `X-API-Key`. When a user id is set, requests also send `X-User-Id`.

## User id

Leave `userId` unset. On 2.1.0, `configure()` stores one anonymous id, `local_` plus a lowercase UUID, in private SharedPreferences under the name and key `feedbackkit_user_id`. The next launch reuses it. List, create, vote, unvote, and create-comment send that id.

```kotlin
FeedbackKit.configure(this) {
    apiKey = "your-api-key"
    // userId omitted on purpose
}
```

On Maven 1.0.1, omitting `userId` leaves it null. Vote and unvote then throw `FeedbackKitError.ValidationError` ("User ID is required for voting"). Set an id yourself on that artifact.

Replace the anonymous id when the person signs in. Assignment writes SharedPreferences:

```kotlin
FeedbackKit.shared.userId = "your-system-user-id"
```

`setUserIdAndPersist` writes DataStore as well. DataStore is where builds before SharedPreferences kept the id. On `configure()`, if SharedPreferences is empty and the host passed no id, a non-blank DataStore value is copied across and reused.

```kotlin
FeedbackKit.shared.setUserIdAndPersist("your-system-user-id")
```

Assigning a blank string clears SharedPreferences and does not mint a replacement. `configure()` does not run again while an instance exists, so the same process keeps a null id. `logout()` clears SharedPreferences and DataStore. The next process start, with both stores empty, generates a new `local_` id. If DataStore still holds an id, that start copies it back instead.

`users.register` sends `user_id` and an optional `mrr`. `email`, `name`, `externalId`, and `metadata` still compile and are ignored. They are not on the wire. Keep `SdkUser.userId` (the id you passed). `SdkUser.id` is the server row id.

```kotlin
val user = FeedbackKit.shared.users.register(
    userId = "your-system-user-id",
    mrr = 9.99
)
FeedbackKit.shared.userId = user.userId
```

`users.getCurrentUser()` calls `GET /users/{id}`. The server does not register that route, so the call 404s. Do not use it to restore a session. Read the id from `FeedbackKit.shared.userId` after `configure()`.

## Show the board

```kotlin
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.swiftlydeveloped.feedbackkit.models.Feedback
import com.swiftlydeveloped.feedbackkit.models.FeedbackStatus
import com.swiftlydeveloped.feedbackkit.state.FeedbackKitProvider
import com.swiftlydeveloped.feedbackkit.state.rememberFeedbackListState
import com.swiftlydeveloped.feedbackkit.theme.FeedbackKitTheme
import com.swiftlydeveloped.feedbackkit.ui.FeedbackDetailView
import com.swiftlydeveloped.feedbackkit.ui.FeedbackListScreen
import com.swiftlydeveloped.feedbackkit.ui.SubmitFeedbackView

@Composable
fun FeedbackScreen() {
    val theme = if (isSystemInDarkTheme()) FeedbackKitTheme.Dark else FeedbackKitTheme.Light
    FeedbackKitProvider(theme = theme) {
        val navController = rememberNavController()
        val listState = rememberFeedbackListState()
        var selected by remember { mutableStateOf<Feedback?>(null) }

        NavHost(navController = navController, startDestination = "list") {
            composable("list") {
                FeedbackListScreen(
                    state = listState,
                    onFeedbackClick = { feedback ->
                        selected = feedback
                        navController.navigate("detail")
                    },
                    onAddFeedback = { navController.navigate("submit") }
                )
            }
            composable("detail") {
                selected?.let { feedback ->
                    FeedbackDetailView(
                        feedback = feedback,
                        onBack = { navController.popBackStack() },
                        onVoteChange = { response ->
                            selected = feedback.withVote(response.hasVoted, response.voteCount)
                            listState.updateFeedback(selected!!)
                        }
                    )
                }
            }
            composable("submit") {
                SubmitFeedbackView(
                    onBack = { navController.popBackStack() },
                    onSubmitSuccess = { newFeedback ->
                        listState.addFeedback(newFeedback)
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
```

Navigation Compose is your dependency. `FeedbackDetailView` takes a `Feedback`, not an id. `rememberFeedbackListState` loads on first composition.

`FeedbackListScreen` draws a top app bar (title, an optional back arrow, and a sort and filter button), a search field, the list, and an add button when `onAddFeedback` is set. Call it inside your app's `MaterialTheme`: the Showcase list takes its colours from it, so dynamic colour and dark theme follow the app. Call `enableEdgeToEdge()` in your activity; the screen pads itself for the system bars.

The sort and filter button opens a bottom sheet with Sort by (Votes, Newest, Oldest, Comments), Status and Category. The sort is saved across launches. The status and category choices and the search text are applied to the loaded items, are kept for the session only, and survive a reload. While a status or category is selected the list shows a "Filtered by … Clear" row. Hide a sheet section with `showStatusFilter = false` or `showCategoryFilter = false`.

`listStyle = FeedbackListStyle.CLASSIC` keeps the 1.x cards. The default is `FeedbackListStyle.SHOWCASE`.

`FeedbackList` has no pull-to-refresh gesture. `listState.refresh()` sets `isRefreshing`, and the list draws a progress indicator while that is true. Wire the gesture yourself. `isRefreshing` is not a swipe detector.

A filter-only list manages its own state:

```kotlin
FeedbackList(
    statusFilter = FeedbackStatus.APPROVED,
    onFeedbackClick = { feedback ->
        // open detail
    }
)
```

`FeedbackList` has no app bar. With your own scaffold, put `FeedbackListSearchField(state)` and `FeedbackSortFilterButton(state)` in it and pass the same `state` to `FeedbackList`. Pass your scaffold's inner padding as `contentPadding` so the last card clears the bars. `statusFilter` and `categoryFilter` above are sent to the server. The sheet's choices are applied on the device.

`SubmitFeedbackView(showAppBar = false)` fits a sheet that already has chrome. The form collects title, description, category, and an optional email with a mailing-list checkbox. It does not collect a status-change opt-in. That opt-in is a vote parameter, below.

`FeedbackKitProvider`, `FeedbackCard`, `VoteButton`, `StatusBadge`, and `CategoryBadge` are the other composables. `VoteButton` calls `votes.vote(feedbackId)` and `votes.unvote(feedbackId)`. It does not pass an email or `notifyStatusChange`.

```kotlin
import com.swiftlydeveloped.feedbackkit.models.FeedbackCategory
import com.swiftlydeveloped.feedbackkit.models.FeedbackStatus
import com.swiftlydeveloped.feedbackkit.ui.CategoryBadge
import com.swiftlydeveloped.feedbackkit.ui.StatusBadge
import com.swiftlydeveloped.feedbackkit.ui.VoteButton

StatusBadge(status = FeedbackStatus.APPROVED)
CategoryBadge(category = FeedbackCategory.BUG_REPORT)
VoteButton(
    feedback = feedback,
    onVoteChange = { response ->
        // response.voteCount, response.hasVoted
    }
)
```

`VoteButton.onVoteChange` receives a `VoteResponse`, not a `Boolean`. The button is disabled when `feedback.canVote` is false (`completed` and `rejected`).

## List, submit, vote, comments, events

These are suspend functions on `FeedbackKit.shared`. Call them from a coroutine.

```kotlin
import com.swiftlydeveloped.feedbackkit.FeedbackKit
import com.swiftlydeveloped.feedbackkit.models.FeedbackCategory
import com.swiftlydeveloped.feedbackkit.models.FeedbackStatus
import com.swiftlydeveloped.feedbackkit.models.ListFeedbackOptions
import com.swiftlydeveloped.feedbackkit.models.PredefinedEventNames

val feedbackList = FeedbackKit.shared.feedback.list()

val filtered = FeedbackKit.shared.feedback.list(
    ListFeedbackOptions(
        status = FeedbackStatus.APPROVED,
        category = FeedbackCategory.FEATURE_REQUEST
    )
)

val feedback = FeedbackKit.shared.feedback.get("feedback-id")

val created = FeedbackKit.shared.feedback.create(
    title = "New Feature Request",
    description = "Please add dark mode",
    category = FeedbackCategory.FEATURE_REQUEST,
    email = "user@example.com"
)

val voted = FeedbackKit.shared.votes.vote(
    feedbackId = feedback.id,
    email = "user@example.com",
    notifyStatusChange = true
)

val removed = FeedbackKit.shared.votes.unvote(feedback.id)

val comments = FeedbackKit.shared.comments.list(feedback.id)
val comment = FeedbackKit.shared.comments.create(
    feedbackId = feedback.id,
    content = "Great idea!",
    userName = "Ada"
)

FeedbackKit.shared.events.track(
    name = "feedback_viewed",
    properties = mapOf("feedback_id" to feedback.id)
)
FeedbackKit.shared.events.track(PredefinedEventNames.FEEDBACK_LIST)
```

`list` takes `ListFeedbackOptions` (defaulted), not a nullable options object. `get` takes `id`. The configured user id is added for you on list (`user_id` query), create, vote, unvote, and comment when the call omits one.

`notifyStatusChange = true` with `email` asks the server to mail that address on status changes. The wire key is `notify_status_change`. The older parameter name `notifyOnStatusChange` still compiles and means the same thing. On 1.0.1 the only parameter is `notifyOnStatusChange`, and that artifact serialized a key the server does not read, so the opt-in was dropped. `email` is not a parameter on 1.0.1.

On 1.0.1, `unvote` puts `user_id` in the query string. The server reads the DELETE body, so that call returns 400. Tag 1.2.0 does the same. 2.1.0 sends `user_id` in the JSON body.

`events.track` takes `name` and an optional `properties` map. It returns `TrackEventResponse`. There is no `eventName` or `userId` parameter; the configured id is copied onto the event. `PredefinedEventNames` is `feedback_list`, `feedback_detail`, and `submit_feedback`. Any other name is a custom event.

`comments.list` also takes optional `page` and `perPage`.

## Metrics

`FeedbackKit.shared.metrics` is new in 2.1.0. It is not in Maven 1.0.1 or tag 1.2.0. There is no metrics composable. The four reads are `summary`, `timeseries`, `breakdown`, and `events`. The API key selects the project. The family is capped at 60 requests per 60 seconds. The SDK does not retry. A 429 is `FeedbackKitError.RateLimitError`.

```kotlin
import com.swiftlydeveloped.feedbackkit.api.MetricBreakdownOptions
import com.swiftlydeveloped.feedbackkit.api.MetricEventsOptions
import com.swiftlydeveloped.feedbackkit.api.MetricSummaryOptions
import com.swiftlydeveloped.feedbackkit.api.MetricTimeseriesOptions
import com.swiftlydeveloped.feedbackkit.models.MetricDimension
import com.swiftlydeveloped.feedbackkit.models.MetricEventScope
import com.swiftlydeveloped.feedbackkit.models.MetricGranularity
import com.swiftlydeveloped.feedbackkit.models.MetricMeasure

val series = FeedbackKit.shared.metrics.timeseries(
    MetricTimeseriesOptions(
        measures = listOf(MetricMeasure.EVENT_COUNT, MetricMeasure.FEEDBACK_COUNT),
        granularity = MetricGranularity.DAY,
        days = 7
    )
)
val zone = series.meta.timezone
for (point in series.series) {
    val count = point.measure(MetricMeasure.EVENT_COUNT) ?: 0.0
}

val summary = FeedbackKit.shared.metrics.summary(
    MetricSummaryOptions(measures = listOf(MetricMeasure.EVENT_COUNT), days = 400)
)
if (summary.meta.eventScope == MetricEventScope.PREDEFINED_ONLY) {
    // Below Pro, only the three predefined names were counted. Custom events are still stored.
}
if (summary.meta.retentionClamped) {
    val allowance = summary.meta.retentionWindowDays
}

val byEvent = FeedbackKit.shared.metrics.breakdown(
    MetricBreakdownOptions(
        measures = listOf(MetricMeasure.EVENT_COUNT, MetricMeasure.UNIQUE_EVENT_USERS),
        dimension = MetricDimension.EVENT_NAME,
        days = 30
    )
)
```

Empty timeseries buckets are present and zero. `series.series.isEmpty()` does not mean "no data". `measure` returns `Double?` because an overflow breakdown row omits `unique_event_users`.

Reading metrics is allowed on every plan. A wider window comes back clamped (`meta.retentionClamped`, `meta.retentionWindowDays`: Free 30, Pro 90, Team 365). Below Pro, `meta.eventScope` is `PREDEFINED_ONLY`. Those are HTTP 200s.

Three requests have no narrower answer and throw `PaymentRequiredError` with `code == "METRICS_CUSTOM_EVENTS_REQUIRE_PRO"`: a breakdown whose dimension is `EVENT_NAME`, a custom `eventName` filter, and the raw event feed.

The event feed's `meta` is pagination. The window block is `metrics`, not `meta`.

```kotlin
val feed = FeedbackKit.shared.metrics.events(
    MetricEventsOptions(
        measures = listOf(MetricMeasure.EVENT_COUNT),
        dateFrom = lastSyncedAt,
        dateTo = "2026-10-12T00:00:00Z",
        page = 1,
        perPage = 100
    )
)
val lastPage = feed.meta.totalPages
```

`SDK_USER_COUNT`, `MRR_SUM`, `MRR_AVERAGE`, and `USERS_WITH_MRR` are in the enum and return 403 `METRICS_MEASURE_REQUIRES_BEARER` on this SDK, which only sends an API key.

## Errors and plan limits

```kotlin
import com.swiftlydeveloped.feedbackkit.errors.FeedbackKitError
import com.swiftlydeveloped.feedbackkit.errors.isRecoverable
import com.swiftlydeveloped.feedbackkit.errors.userMessage

try {
    FeedbackKit.shared.feedback.get("id")
} catch (e: FeedbackKitError) {
    when (e) {
        is FeedbackKitError.AuthenticationError -> { }
        is FeedbackKitError.PaymentRequiredError -> {
            if (e.code == "METRICS_CUSTOM_EVENTS_REQUIRE_PRO") {
                // custom metrics read, not the feedback cap
            }
        }
        is FeedbackKitError.ForbiddenError -> { }
        is FeedbackKitError.NotFoundError -> { }
        is FeedbackKitError.ConflictError -> { }
        is FeedbackKitError.ValidationError -> { }
        is FeedbackKitError.NetworkError -> { }
        is FeedbackKitError.ServerError -> { }
        is FeedbackKitError.RateLimitError -> { }
        is FeedbackKitError.UnknownError -> { }
    }
    val text = e.userMessage
    if (e.isRecoverable) {
        // network, server, rate limit
    }
}
```

Branch on `e.code` when you need the server token. Middleware errors (401, a suspended project, 404, 429, 500) often have no wire code, and `e.code` is then this SDK's class token.

The Free plan's 10-feedback cap is not a 402. Create returns the new item. The server marks it hidden and leaves it out of later lists, so the board does not grow. `Feedback.isHiddenOverLimit` is on the model for a payload that includes it. List responses from the server omit those rows.

An archived project still lists. Create, vote, and comment return 403.

`completed` and `rejected` cannot be voted. The button stays disabled. A direct `votes.vote` call is rejected by the server.

Submitting creates the item and casts the author's vote, so a new row shows one vote.
