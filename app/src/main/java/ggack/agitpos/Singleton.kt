
object Singleton {
    var account : String? = null

    //이름이 중복되는지 확인
    fun Map<String, Map<String, Any>>.isDuplicated(key : String, name : String) : Boolean{
        if(this.contains(name))
            return true
        for (pair in this.toList()) {
            if (pair.first != key && pair.second["extraName"] == name)
                return true
        }
        return false
    }
}