package com.fintrack.android.ui.categories

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.android.data.model.*
import com.fintrack.android.data.repository.FinTrackRepository
import com.fintrack.android.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CategoriesScreenData(val categories: List<Category>, val tags: List<String>)

class CategoriesViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = FinTrackRepository(application)

    private val _state = MutableStateFlow<UiState<CategoriesScreenData>>(UiState.Loading)
    val state: StateFlow<UiState<CategoriesScreenData>> = _state.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    init { load() }

    fun load() {
        val cachedCategories = repo.peekCache<List<Category>>(FinTrackRepository.KEY_CATEGORIES, object : com.google.gson.reflect.TypeToken<List<Category>>() {}.type)
        val cachedTags = repo.peekCache<List<String>>(FinTrackRepository.KEY_TAGS, object : com.google.gson.reflect.TypeToken<List<String>>() {}.type)
        if (cachedCategories != null || cachedTags != null) {
            _state.value = UiState.Success(CategoriesScreenData(cachedCategories ?: emptyList(), cachedTags ?: emptyList()))
        } else if (_state.value !is UiState.Success) {
            _state.value = UiState.Loading
        }
        viewModelScope.launch {
            val catResult = repo.getCategories()
            val tagResult = repo.getTags()
            if (catResult.isFailure || tagResult.isFailure) {
                if (_state.value !is UiState.Success) {
                    _state.value = UiState.Error((catResult.exceptionOrNull() ?: tagResult.exceptionOrNull())?.message ?: "Failed to load")
                }
                return@launch
            }
            _state.value = UiState.Success(CategoriesScreenData(catResult.getOrDefault(emptyList()), tagResult.getOrDefault(emptyList())))
        }
    }

    fun saveCategory(id: Int?, name: String, type: String, icon: String, color: String) {
        viewModelScope.launch {
            val body = CategoryRequest(name, type, icon, color)
            val result = if (id == null) repo.createCategory(body) else repo.updateCategory(id, body)
            result.fold(onSuccess = { load() }, onFailure = { _actionError.value = it.message ?: "Failed to save category" })
        }
    }

    fun deleteCategory(id: Int) {
        viewModelScope.launch {
            repo.deleteCategory(id).fold(onSuccess = { load() }, onFailure = { _actionError.value = it.message ?: "Failed to delete category" })
        }
    }

    fun createDefaultCategories() {
        viewModelScope.launch {
            repo.createDefaultCategories().fold(onSuccess = { load() }, onFailure = { _actionError.value = it.message ?: "Failed to create defaults" })
        }
    }

    fun addTag(tag: String) {
        val current = (state.value as? UiState.Success)?.data?.tags ?: return
        val normalized = tag.trim().lowercase()
        if (normalized.isBlank() || current.contains(normalized)) return
        viewModelScope.launch {
            repo.saveTags(SaveTagsRequest(current + normalized)).fold(
                onSuccess = { load() },
                onFailure = { _actionError.value = it.message ?: "Failed to add tag" }
            )
        }
    }

    fun deleteTag(tag: String) {
        val current = (state.value as? UiState.Success)?.data?.tags ?: return
        viewModelScope.launch {
            repo.saveTags(SaveTagsRequest(current.filter { it != tag })).fold(
                onSuccess = { load() },
                onFailure = { _actionError.value = it.message ?: "Failed to delete tag" }
            )
        }
    }

    // Renaming cascades server-side to every transaction & recurring rule
    // that used the old tag name — see SettingsController::renameTag() in
    // the Nextcloud app.
    fun renameTag(oldName: String, newName: String) {
        viewModelScope.launch {
            repo.renameTag(RenameTagRequest(oldName, newName)).fold(
                onSuccess = { load() },
                onFailure = { _actionError.value = it.message ?: "Failed to rename tag" }
            )
        }
    }

    fun clearActionError() { _actionError.value = null }
}
