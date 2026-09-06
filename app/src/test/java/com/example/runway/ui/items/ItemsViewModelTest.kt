package com.example.runway.ui.items

import com.example.runway.MainDispatcherRule
import com.example.runway.domain.model.Item
import com.example.runway.fake.FakeItemRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ItemsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `starts in a loading state`() {
        val viewModel = ItemsViewModel(FakeItemRepository())
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `emits items from the repository once collected`() = runTest {
        val repository = FakeItemRepository()
        val viewModel = ItemsViewModel(repository)

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        val expected = listOf(Item(1L, "Wire up Room", "Entity, DAO, database"))
        repository.emit(expected)

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(expected, viewModel.uiState.value.items)

        job.cancel()
    }

    @Test
    fun `a blank title is rejected and never reaches the repository`() = runTest {
        val repository = FakeItemRepository()
        val viewModel = ItemsViewModel(repository)

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        viewModel.onAddItem(title = "   ", note = "ignored")

        assertEquals(0, repository.saveCount)
        assertEquals("A title is required.", viewModel.uiState.value.errorMessage)

        job.cancel()
    }

    @Test
    fun `a valid item is saved and the error is cleared`() = runTest {
        val repository = FakeItemRepository()
        val viewModel = ItemsViewModel(repository)

        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect { }
        }

        viewModel.onAddItem(title = "  Prove the ViewModel  ", note = " StateFlow ")

        assertEquals(1, repository.saveCount)
        assertEquals(null, viewModel.uiState.value.errorMessage)
        assertEquals("Prove the ViewModel", viewModel.uiState.value.items.single().title)
        assertEquals("StateFlow", viewModel.uiState.value.items.single().note)

        job.cancel()
    }
}
