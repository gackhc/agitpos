package ggack.agitpos.data.login

import javax.inject.Inject
import javax.inject.Singleton

/** A PIN session is intentionally not restored after process death. */
@Singleton
class UserSession @Inject constructor() {
    var name: String? = null
        private set
    fun signIn(name: String) { this.name = name }
    fun clear() { name = null }
    fun permits(name: String, master: Boolean = false): Boolean =
        name.isNotBlank() && this.name == name && (!master || name == "*master")
}
