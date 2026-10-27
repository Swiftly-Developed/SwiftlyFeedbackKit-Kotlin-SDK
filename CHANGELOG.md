# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.2.0] - 2026-10-27

All FeedbackKit SDKs (Swift, JS, React Native, Kotlin, Flutter) now share one version number. The Vapor SDK keeps its own versioning.

### Changed

- **The Maven publication now includes `-sources.jar` and `-javadoc.jar`**, which Maven Central requires, via `singleVariant("release") { withSourcesJar(); withJavadocJar() }`.
- The contact addresses in the repository docs are now getfeedbackkit.com aliases: security@ in `SECURITY.md`, support@ in `SUPPORT.md`, hello@ in `CODE_OF_CONDUCT.md` and developers@ in `CONTRIBUTING.md`.
- The README licence section now reads MIT, matching `LICENSE`.
- The developer email in the published Maven POM (`POM_DEVELOPER_EMAIL`) is now developers@getfeedbackkit.com.

## [2.1.0] - 2026-10-14

The Showcase feedback list from [FEEDBACK01](../docs/specs/FEEDBACK01.md), translated to Material 3. The version follows the Swift SDK release that ships the same design. Every API change is additive, but the default look of `FeedbackList` changes.

### Added

- **`FeedbackListStyle`**: `SHOWCASE` (default) and `CLASSIC`. `FeedbackList` and `FeedbackListScreen` take a `listStyle` parameter.
- **Showcase cards.** An `ElevatedCard` per item with a status eyebrow (a glyph plus the status name), the title, the full description (never truncated), and a footer with the category, the comment count, and a vote button. The vote button is tonal when you have not voted and filled when you have. Completed and rejected items show a plain count, not a button. Each status has its own glyph shape, so status does not rely on colour. Improvement uses the Material "tune" icon.
- **`FeedbackListScreen`**: a top app bar with an optional back arrow and a sort and filter button, a search field, the list, and an add button. It is edge-to-edge aware.
- **Sort and filter sheet.** A `ModalBottomSheet` of filter chips: Sort by (Votes, Newest, Oldest, Comments), Status and Category. The sort is saved in SharedPreferences (`feedbackkit_list` / `feedbackkit_list_sort`). Filters and search are kept for the session only and survive a reload. While a filter is active the list shows a "Filtered by … Clear" row, and a dedicated empty state when nothing matches.
- **Search.** `FeedbackListSearchField` matches title and description, ignoring case and accents.
- `FeedbackSortFilterButton` and `FeedbackListSearchField` for hosts with their own app bar.
- `FeedbackListState`: `searchQuery`, `sortOption`, `selectedStatus`, `selectedCategory`, `displayedItems`, `isFiltered`, `isSearching`, `updateSearchQuery`, `selectSort`, `selectStatus`, `selectCategory`, and `clearSelectedFilters`. `FeedbackSortOption` enum. `FeedbackCategory.pluralDisplayNameRes`.
- `FeedbackList(contentPadding = …)` so the last card clears a host's bars and FAB.
- Showcase skeleton, empty, error, no-results and no-match states.
- New strings in all 11 locales, including `feedbackkit_vote_count` and `feedbackkit_comment_count` plurals.

### Changed

- **`FeedbackList` renders the Showcase style by default.** Pass `listStyle = FeedbackListStyle.CLASSIC` to keep the 1.x cards. The old `FeedbackList` signatures remain as hidden overloads, so code compiled against 1.x still links.
- Showcase takes its surfaces, text and vote colours from the host `MaterialTheme`, so dynamic colour and dark theme follow the app. Status colours still come from `FeedbackKitTheme.statusColors` and are darkened or lightened as needed to reach 4.5:1 against the card.
- Compose BOM raised from `2024.01.00` to `2024.06.00` (Material3 1.2.1).

### Accessibility

- TalkBack reads each card as one element with a "View details" action and a Vote or Remove vote custom action. The vote button reports its count, a Voted / Not voted state and its action. Closed items read "N votes, Voting is closed" and are not a control. Filter chips are radio buttons in single-choice groups.
- 48dp touch targets on every control. Text is `sp` and is tested at font scales 0.85 to 2.0. The card footer stacks when the facts and the vote button no longer fit on one line.
- Right-to-left layouts mirror, with locale digits in counts.

### Not included

- Pull-to-refresh. `refresh()` and `isRefreshing` are unchanged, and the list shows a progress indicator while refreshing. The gesture is a `UI04` follow-up.

### Testing

- Roborazzi screenshot tests under Robolectric (`./gradlew :feedbackkit:verifyRoborazziDebug`). Reference images are in `feedbackkit/src/test/screenshots/`.
- Unit and TalkBack semantics tests for filtering, sorting, sort persistence, the optimistic vote, status colour contrast, string resources, and touch targets.

### Documentation

- Added [docs/getting-started.md](docs/getting-started.md). It matches the 2.1.0 API and states what Maven Central 1.0.1 does not include.
- Corrected the anonymous-id notes. A blank `userId` clears SharedPreferences only. `configure()` does not mint a new id while an instance already exists. `logout()` clears both stores. The next process start generates `local_<uuid>` when both stores are empty.
- Noted that `users.getCurrentUser()` calls `GET /users/{id}`, which the server does not register.
- Noted that `VoteButton` does not pass `email` or `notifyStatusChange`, and that `FeedbackList` has no pull gesture. The 1.0.0 notes below called the list a pull-to-refresh view. The composable exposes `refresh()` and `isRefreshing` for a gesture the host supplies.
- Website docs, the Android blog guide, and the Kotlin rows of the Play Data safety notes now use the same vote, register, and event signatures as this tree.
- Noted that Maven 1.0.1 `unvote` puts `user_id` in the query string, which the server does not read, so that call returns 400. 2.1.0 sends it in the DELETE body.

Also first shipped in 2.1.0 (drafted earlier under the untagged 1.2.0 and 1.2.1 headings):

> ⚠️ **Precondition for the metrics API — read before upgrading.** The metrics endpoints are
> **not yet deployed to every environment**.
> Measured 2026-08-25: the development API serves them; the **testflight and production APIs
> answer `404`** to `/api/v1/metrics/summary` while their `/health` answers `200`, so the routes
> are genuinely absent rather than the service being down. Until that deploy lands, a metrics call
> against production fails. Every other change listed here is unaffected.

#### Added

- **Metrics API** — `FeedbackKit.shared.metrics` with four read operations: `summary`,
  `timeseries`, `breakdown` and `events`. All four use the API key, which identifies the
  project, so there is no project parameter to pass.
- Metrics models: `MetricSummary`, `MetricSeries`, `MetricSeriesPoint`, `MetricBreakdown`,
  `MetricBreakdownItem`, `MetricEventList`, `MetricEventRow`, `MetricsMeta` and
  `MetricsPaginationMeta`, plus the four closed vocabularies `MetricMeasure`,
  `MetricDimension`, `MetricGranularity` and `MetricEventScope`.
- Optional-returning measure accessors — `summary.measure(MetricMeasure.EVENT_COUNT)` returns
  `Double?`, because a breakdown's overflow row legitimately omits `unique_event_users`.
- `PredefinedEventNames` — the three event names FeedbackKit itself defines
  (`feedback_list`, `feedback_detail`, `submit_feedback`), previously spelled by hand at
  every call site. The match is exact and case-sensitive, and it decides what a Free-tier
  metrics read can count.
- `FeedbackKitError.RateLimitError` for **429**, with `isRecoverable == true`. The metrics
  family is capped at 60 requests per 60 seconds; the SDK deliberately does not retry.
- `FeedbackKitError.fromStatusCode` accepts optional `code` and `field` arguments. Existing
  call sites compile unchanged.

#### Changed

- Documented metrics windows are Free 30 days, Pro 90 days and Team 365 days. The client still reads `retentionWindowDays` from the response and does not hardcode them.

#### Fixed

- **Anonymous users now have a persistent id.** `configure()` with no `userId`
  generates `local_<uuid>` (a lowercase UUID v4, the same shape Flutter stores
  and the same `local_` prefix Swift uses) and writes it to SharedPreferences
  in private mode under `feedbackkit_user_id`. The next `configure()` reuses
  that value. An id passed to `configure`, or assigned to `userId`, replaces
  it and is what gets stored. A blank assignment clears it and does not
  generate a replacement until the next `configure()` after the store is
  empty. `feedback.list`, `feedback.create`, `votes.vote`, `votes.unvote`,
  and `comments.create` send that id. SharedPreferences is the source of
  truth after the first `configure()`.
- **An id an older build saved in DataStore is kept.** When SharedPreferences
  has no id and the host did not pass one, `configure()` reads DataStore
  (`user_id`), copies that id into SharedPreferences, and uses it. A new
  `local_<uuid>` is generated only when both stores are empty. An explicit
  id, or one already in SharedPreferences, still wins, and DataStore is not
  read in those cases. A blank DataStore value is treated as absent.
- `votes.vote` serialized the status-change opt-in as `notifyOnStatusChange`,
  which the server does not bind. The wire key is now `notify_status_change`,
  and `vote` takes an optional `email`. Pass `notifyStatusChange` for the new
  name; `notifyOnStatusChange` still compiles.
- `users.register` sent `email`, `name`, `external_id`, and `metadata`, and
  never `user_id` or `mrr`. The body is now `user_id` plus an optional `mrr`.
  The old arguments remain, deprecated, and are not sent.

Wire-contract bugs present in every published artifact (**1.0.0** and **1.0.1**). 1.0.1's sources still require `VoteResponse.success`, which the server has never sent.

- **Vote and unvote responses decode.** The server body is `{feedback_id, vote_count, has_voted}`. Requiring `success` threw after the vote had already been stored, and the button rolled the count back.
- **Unvote sends `user_id` in the JSON body.** The server reads `CreateVoteDTO` from the DELETE body. A query parameter never reached it, so unvote returned 400.
- **Vote requests send `user_id` and the mailing-list keys in snake_case.** The status-change opt-in is `notify_status_change`, noted above.
- **Tracked events send `event_name` and decode `{id, event_name, user_id, properties, created_at}`.** Requiring `success` / `event_id` threw after the event was stored.
- **Create-feedback sends `user_email`, `subscribe_to_mailing_list`, and `mailing_list_email_types`.** The previous camelCase keys were ignored, so the email and the mailing-list opt-in were dropped.
- **An unknown feedback `status` or `category` drops that row** instead of failing the whole list. A row that is missing a required field still fails the decode. `testflight` was already a known status.
- **`SdkUser` reads `user_id`, `mrr`, `first_seen_at`, and `last_seen_at`.** The register request sends `user_id` and an optional `mrr`, noted above.
- **HTTP 422 keeps its status code** on `UnknownError`. **429** copies a delta-seconds `Retry-After` onto `RateLimitError`. An HTTP-date `Retry-After` is left unset.
- **`FeedbackKitConfig` rejects a blank API key** at construction, matching the configure DSL.
- **Feedback list filters reload when they change.** A newer load no longer records the cancelled request as an error or clears the newer request's loading flag.
- **A feedback card with no click handler is not a button.** Vote failures are reported through `onError` and an accessibility error, instead of reverting with no signal. Comment loading and submit ignore cancellation the same way.
- **Compose error text uses the 11 locale catalogs.** Validation and conflict messages still show the server's own text. `userMessage` remains the English wording for non-Compose callers.

Three defects in error reporting that affected **every** operation, not only the new ones:

- **The server's explanation never reached the app.** A body of the shared middleware form
  (`401`, `403`, `404`, `429`, `500`) was read as a primitive, so the `error` flag won the
  lookup chain and every such failure reported the flag's own rendering instead of the
  server's `reason`. An expired API key reported a four-letter string.
- **A typed error envelope was discarded silently.** Reading it raised inside a swallowing
  `catch`, so the code, the message and the offending field were all thrown away and the
  transport's status text was reported instead. Both shapes are now decoded, branching on
  the JSON type of `error` rather than on the status code — a 403 arrives in both shapes.
- **429 had no arm** and fell into `UnknownError` with a null `statusCode`.

The message text on existing errors therefore changes, from a placeholder to the server's
own words. No behaviour depends on the old strings.

- **Debug logging no longer prints a full URL or a response body.** With `debug` on, the
  client logs the method, the path, and query *names* with their values replaced. Customer
  event names travel in `event_name` and in response bodies, so neither is written out.
  A cancelled request is rethrown instead of being wrapped as a `FeedbackKitError`.

#### Notes

- Maven Central's latest artifact is still 1.0.1; none of the above is published there.
- The changes in this list add no dependency and do not change the shared JSON configuration.

## [1.2.0] - 2026-10-14

### Fixed

- The library compiles again: the `R` imports pointed at `com.swiftlydeveloped.feedbackkit.R`, but the module's namespace is `com.getfeedbackkit.feedbackkit`.
- Comment lists no longer throw `MissingFieldException`: the server never sends `feedback_id` on comments, so `Comment.feedbackId` is now `String?` (default `null`). Code that assigns it to a non-null `String` needs a null check.
- The admin badge on comments now renders: `Comment.isOfficial` reads the server's `is_admin` key (it read `is_official`, which the server never sends).
- `Feedback.email` is now populated: it reads the server's `user_email` key (it read `email`, which the server never sends).
- `./gradlew` works on a fresh clone: `gradle/wrapper/gradle-wrapper.jar` is now committed instead of git-ignored.

### Added

- Cross-SDK parity tests against server-generated wire corpora, plus comment-model and status-partition test suites.

## [1.1.1] - 2026-04-24

### Changed

- Version alignment with Swift SDK 1.1.1 hotfix (Xcode 26 / Swift 6.2 compatibility). No functional changes to this SDK.

## [1.1.0] - 2026-04-15

### Added

- LICENSE file (MIT)
- CONTRIBUTING.md with contribution guidelines
- SECURITY.md with vulnerability reporting policy
- CODE_OF_CONDUCT.md (Contributor Covenant v2.1)
- SUPPORT.md with support channels

### Changed

- Migrated API URLs to getfeedbackkit.com
- Fixed developer email in gradle.properties
- Updated version references in README installation examples
- Standardized documentation across all FeedbackKit SDKs

## [1.0.1] - 2026-02-09

### Changed

- Updated production URLs
- Bumped version for republishing

## [1.0.0] - 2026-02-08

### Added

- Initial release of SwiftlyFeedbackKit-Kotlin SDK
- **Core SDK**
  - `FeedbackKit` main client with singleton pattern
  - `FeedbackKitConfig` for configuration with builder pattern
  - Environment presets (Production, Staging, Local)
  - DataStore-based persistent storage

- **API Layer**
  - `FeedbackApi` - List, get, create feedback
  - `VotesApi` - Vote and unvote with userId support
  - `CommentsApi` - List and create comments
  - `UsersApi` - User registration
  - `EventsApi` - Event tracking

- **Models**
  - `Feedback` data class with Kotlin Serialization
  - `FeedbackStatus` enum with canVote property
  - `FeedbackCategory` enum
  - `Comment` data class
  - `VoteResponse` data class
  - `SdkUser` data class
  - `TrackedEvent` data class

- **Error Handling**
  - `FeedbackKitError` sealed class hierarchy
  - Specific error types: Authentication, PaymentRequired, Forbidden, NotFound, Conflict, Validation, Network, Server, Unknown
  - HTTP status code mapping
  - User-friendly error messages
  - Recoverable error detection

- **UI Components (Jetpack Compose)**
  - `FeedbackList` - LazyColumn with pull-to-refresh
  - `FeedbackCard` - Card composable with badges and vote button
  - `FeedbackDetailView` - Full detail screen with comments
  - `SubmitFeedbackView` - Form for submitting feedback
  - `StatusBadge` - Status indicator badge
  - `CategoryBadge` - Category indicator badge
  - `VoteButton` - Vote button with optimistic updates

- **State Management**
  - `FeedbackListState` - State holder for feedback list
  - `rememberFeedbackListState` composable
  - `FeedbackKitProvider` with CompositionLocal

- **Theming**
  - `FeedbackKitTheme` data class
  - `StatusColors` for status badge colors
  - `CategoryColors` for category badge colors
  - Light and Dark theme presets
  - Full theme customization support

- **Example App**
  - Complete example Android app
  - Navigation between list, detail, and submit screens
  - System dark mode support
