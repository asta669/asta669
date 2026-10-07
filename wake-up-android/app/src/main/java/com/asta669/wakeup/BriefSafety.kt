package com.asta669.wakeup

/** Keep user-editable model/key fields out of URL structure and HTTP header syntax. */
internal object BriefSafety {
    fun validCredentials(key: String, model: String): Boolean =
        key.matches(Regex("[A-Za-z0-9_-]{20,200}")) &&
            model.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,99}"))
}
