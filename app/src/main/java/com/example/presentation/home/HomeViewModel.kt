package com.example.presentation.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PdfMasterApplication
import com.example.data.local.HistoryEntity
import com.example.domain.model.PdfTool
import com.example.domain.model.PdfToolsList
import com.example.domain.model.ToolCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val selectedCategory: ToolCategory? = null,
    val filteredTools: List<PdfTool> = PdfToolsList.allTools,
    val recentFiles: List<HistoryEntity> = emptyList()
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PdfMasterApplication
    private val historyRepo = app.historyRepository

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<ToolCategory?>(null)
    private val _isSearchActive = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> = combine(
        _searchQuery,
        _selectedCategory,
        _isSearchActive,
        historyRepo.recentFiles
    ) { query, category, isSearch, recents ->
        val tools = PdfToolsList.allTools.filter { tool ->
            val matchesCategory = category == null || tool.category == category
            val matchesQuery = query.isBlank() || tool.id.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }

        val filteredRecents = if (query.isBlank()) {
            recents.take(5)
        } else {
            recents.filter { it.outputFileName.contains(query, ignoreCase = true) }
        }

        HomeUiState(
            searchQuery = query,
            isSearchActive = isSearch,
            selectedCategory = category,
            filteredTools = tools,
            recentFiles = filteredRecents
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearch() {
        _isSearchActive.update { !it }
        if (!_isSearchActive.value) {
            _searchQuery.value = ""
        }
    }

    fun onCategorySelected(category: ToolCategory?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    fun toggleFavorite(item: HistoryEntity) {
        viewModelScope.launch {
            historyRepo.toggleFavorite(item.id, item.isFavorite)
        }
    }

    fun deleteRecentFile(id: Long) {
        viewModelScope.launch {
            historyRepo.delete(id, deleteFile = true)
        }
    }
}
