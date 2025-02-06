package com.secretspot.textguess.ui.cloudvision

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CloudVisionViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is Cloud Vision"
    }
    val text: LiveData<String> = _text
}