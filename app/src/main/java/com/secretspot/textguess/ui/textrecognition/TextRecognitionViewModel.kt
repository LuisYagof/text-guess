package com.secretspot.textguess.ui.textrecognition

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TextRecognitionViewModel : ViewModel() {

    private val _recognizedText = MutableLiveData<String>()
    val recognizedText: LiveData<String> = _recognizedText

    private val _isProcessing = MutableLiveData<Boolean>()
    val isProcessing: LiveData<Boolean> = _isProcessing

    fun updateRecognizedText(text: String) {
        _recognizedText.value = text
    }

    fun setProcessing(processing: Boolean) {
        _isProcessing.value = processing
    }
}