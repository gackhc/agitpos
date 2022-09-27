package ggack.agitpos.listener

interface ListListener {
    fun onClick(key : String, item : Any?) {}
    fun onADD(key : String, item : Any?) {}
    fun onLongClick(key : String, item : Any?) {}
    fun onMember(key : String, item : Any?, b : Boolean) {}
    fun onRemove(key : String, item : Any?) {}
    fun onChange(key : String, item : Any?) {}
    fun onRecord(key : String, item : Any?) {}
    fun onChange(position : Int) {}
}