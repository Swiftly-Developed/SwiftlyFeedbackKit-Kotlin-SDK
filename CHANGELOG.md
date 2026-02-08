# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2024-02-08

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
