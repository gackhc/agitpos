package ggack.agitpos.ui.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ggack.agitpos.data.model.EventModel

open class BaseViewModel : ViewModel() {
    val liveEvent = MutableLiveData<EventModel?>(null)
}