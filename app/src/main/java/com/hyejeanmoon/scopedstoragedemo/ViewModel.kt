package com.hyejeanmoon.scopedstoragedemo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class ViewModel(application: Application) : AndroidViewModel(application), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + Job()

    private val _result: MutableLiveData<FileDownloadResult> = MutableLiveData()
    val result: LiveData<FileDownloadResult> = _result


    fun fileDownload(url: String) {
        launch {
            withContext(Dispatchers.IO) {
                _result.postValue(FileDownload().storageFile(url = url, context = getApplication()))
            }
        }

    }
}