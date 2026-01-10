package com.example.lumisound.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lumisound.data.model.Track
import com.example.lumisound.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {
    
    private val _searchResults = MutableStateFlow<List<Track>>(emptyList())
    val searchResults: StateFlow<List<Track>> = _searchResults.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun searchTracks(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _error.value = null
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            musicRepository.searchTracks(query, limit = 20)
                .onSuccess { tracks ->
                    _searchResults.value = tracks
                    _isLoading.value = false
                }
                .onFailure { exception ->
                    _error.value = exception.message ?: "Ошибка при поиске треков"
                    _searchResults.value = emptyList()
                    _isLoading.value = false
                }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}
