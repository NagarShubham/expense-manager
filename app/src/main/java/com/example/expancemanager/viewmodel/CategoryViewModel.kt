package com.example.expancemanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expancemanager.data.Category
import com.example.expancemanager.data.CategoryRepository
import com.example.expancemanager.data.CategoryRepository.CategoryResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository
) : ViewModel() {
    val categories: StateFlow<List<Category>> =
        categoryRepository
            .getCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun addCategory(
        name: String,
        emoji: String
    ): CategoryResult =
        withContext(Dispatchers.IO) {
            categoryRepository.addCategory(name, emoji)
        }

    suspend fun updateCategory(
        oldName: String,
        newName: String,
        emoji: String
    ): CategoryResult =
        withContext(Dispatchers.IO) {
            categoryRepository.updateCategory(oldName, newName, emoji)
        }

    suspend fun deleteCategory(name: String): CategoryResult =
        withContext(Dispatchers.IO) {
            categoryRepository.deleteCategory(name)
        }

    /** Moves the category at [fromIndex] one position up or down and persists the new order. */
    fun moveCategory(
        fromIndex: Int,
        toIndex: Int
    ) {
        val current = categories.value
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val reordered = current.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        viewModelScope.launch(Dispatchers.IO) {
            categoryRepository.reorder(reordered.map { it.name })
        }
    }
}
