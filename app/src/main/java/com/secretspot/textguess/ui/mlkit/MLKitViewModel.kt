package com.secretspot.textguess.ui.mlkit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MLKitViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is ML Kit"
    }
    val text: LiveData<String> = _text
}