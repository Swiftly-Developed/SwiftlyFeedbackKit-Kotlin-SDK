package com.swiftlydeveloped.feedbackkit.state

import com.swiftlydeveloped.feedbackkit.models.FeedbackCategory
import com.swiftlydeveloped.feedbackkit.models.FeedbackSortOption
import com.swiftlydeveloped.feedbackkit.models.FeedbackStatus
import com.swiftlydeveloped.feedbackkit.storage.FeedbackSortStore
import com.swiftlydeveloped.feedbackkit.ui.showcase.ShowcaseFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedbackListSelectionTest {

    private class MemorySortStore(var stored: FeedbackSortOption = FeedbackSortOption.DEFAULT) : FeedbackSortStore {
        val writes = mutableListOf<FeedbackSortOption>()
        override fun read() = stored
        override fun write(option: FeedbackSortOption) {
            writes += option
            stored = option
        }
    }

    private fun TestScope.loadedState(): FeedbackListState =
        FeedbackListState(scope = this, loadItems = { ShowcaseFixtures }).also { it.load() }

    @Test
    fun `attaching a sort store hydrates the saved sort`() = runTest {
        val state = loadedState()
        state.sortStore = MemorySortStore(FeedbackSortOption.COMMENTS)
        assertEquals(FeedbackSortOption.COMMENTS, state.sortOption)
    }

    @Test
    fun `selecting a sort persists it, filters are never persisted`() = runTest {
        val store = MemorySortStore()
        val state = loadedState()
        state.sortStore = store

        state.selectSort(FeedbackSortOption.NEWEST)
        state.selectStatus(FeedbackStatus.APPROVED)
        state.selectCategory(FeedbackCategory.BUG_REPORT)
        state.updateSearchQuery("crash")

        assertEquals(listOf(FeedbackSortOption.NEWEST), store.writes)
    }

    @Test
    fun `isFiltered counts status and category only`() = runTest {
        val state = loadedState()
        advanceUntilIdle()
        assertFalse(state.isFiltered)

        state.updateSearchQuery("dark")
        state.selectSort(FeedbackSortOption.OLDEST)
        assertFalse(state.isFiltered)
        assertTrue(state.isSearching)

        state.selectCategory(FeedbackCategory.IMPROVEMENT)
        assertTrue(state.isFiltered)
    }

    @Test
    fun `clearing selected filters keeps the sort and the search text`() = runTest {
        val state = loadedState()
        advanceUntilIdle()
        state.selectSort(FeedbackSortOption.COMMENTS)
        state.updateSearchQuery("mode")
        state.selectStatus(FeedbackStatus.IN_PROGRESS)
        state.selectCategory(FeedbackCategory.FEATURE_REQUEST)

        state.clearSelectedFilters()

        assertFalse(state.isFiltered)
        assertEquals(FeedbackSortOption.COMMENTS, state.sortOption)
        assertEquals("mode", state.searchQuery)
        assertEquals(listOf("1", "5"), state.displayedItems.map { it.id })
    }

    @Test
    fun `displayed items follow the selection and survive a reload`() = runTest {
        val state = loadedState()
        advanceUntilIdle()
        state.selectStatus(FeedbackStatus.APPROVED)
        assertEquals(listOf("2"), state.displayedItems.map { it.id })

        state.refresh()
        advanceUntilIdle()

        assertEquals(FeedbackStatus.APPROVED, state.selectedStatus)
        assertEquals(listOf("2"), state.displayedItems.map { it.id })
        assertEquals(ShowcaseFixtures.size, state.feedbackItems.size)
    }
}
