package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.db.AppDatabase
import com.example.db.VaultMessage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VaultViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "secure-vault-db"
    ).build()

    val messages: StateFlow<List<VaultMessage>> = db.messageDao().getAllMessages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveMessage(coverText: String, secretPayload: String, isSent: Boolean) {
        viewModelScope.launch {
            db.messageDao().insertMessage(
                VaultMessage(
                    coverText = coverText,
                    secretPayload = secretPayload,
                    isSent = isSent
                )
            )
        }
    }

    fun shredAll() {
        viewModelScope.launch {
            db.messageDao().clearAll()
        }
    }
}
