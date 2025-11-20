package com.example.storageapp

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.storageapp.StorageHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
class NotesViewModel(app: Application) : AndroidViewModel(app) {
    enum class Target { INTERNAL, EXTERNAL_SAF }
    private val _fileName = MutableStateFlow("")
    val fileName: StateFlow<String> = _fileName
    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content
    private val _target = MutableStateFlow(Target.INTERNAL)
    val target: StateFlow<Target> = _target
    // Menyimpan Uri terakhir berkas SAF yang dibuka/dibuat
    private val _lastExternalUri = MutableStateFlow<Uri?>(null)
    val lastExternalUri: StateFlow<Uri?> = _lastExternalUri
    fun setFileName(v: String) { _fileName.value = v }
    fun setContent(v: String) { _content.value = v }

    fun setTarget(t: Target) {_target.value = t}
    fun setExternalUri(u: Uri?) { _lastExternalUri.value = u }

    fun saveInternal() {
        val name = fileName.value.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            StorageHelper.writeInternal(getApplication(), name, content.value)
        }
    }
    fun openInternal() {
        val name = fileName.value.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            val txt = StorageHelper.readInternal(getApplication(), name)
            txt?.let { _content.value = it }
        }
    }
    fun deleteInternal() {
        val name = fileName.value.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            StorageHelper.deleteInternal(getApplication(), name)
        }
    }
    fun saveExternal(contentResolver: ContentResolver) {
        val uri = lastExternalUri.value ?: return
        viewModelScope.launch {
            StorageHelper.writeExternalSAF(contentResolver, uri, content.value)
        }
    }
    fun openExternal(contentResolver: ContentResolver) {
        val uri = lastExternalUri.value ?: return
        viewModelScope.launch {
            val txt = StorageHelper.readExternalSAF(contentResolver, uri)
            txt?.let { _content.value = it }
        }
    }
    fun deleteExternal(contentResolver: ContentResolver) {
        val uri = lastExternalUri.value ?: return
        viewModelScope.launch {
            StorageHelper.deleteExternalSAF(contentResolver, uri)
            _lastExternalUri.value = null
        }
    }
}