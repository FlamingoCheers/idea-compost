package com.ideacompost.app.ui.composts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ideacompost.app.data.db.dao.CompostDao
import com.ideacompost.app.data.db.entity.CompostEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CompostsViewModel @Inject constructor(
    compostDao: CompostDao
) : ViewModel() {
    val composts: StateFlow<List<CompostEntity>> = compostDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
