package com.jeeva.locationtracker.util

import android.content.Context

object Prefs {
    private const val FILE_NAME = "family_locator_prefs"
    private const val KEY_ROLE = "role"
    private const val KEY_FAMILY_CODE = "family_code"

    enum class Role { PARENT, VIEWER }

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun getRole(context: Context): Role? =
        prefs(context).getString(KEY_ROLE, null)?.let { Role.valueOf(it) }

    fun getFamilyCode(context: Context): String? =
        prefs(context).getString(KEY_FAMILY_CODE, null)

    fun save(context: Context, role: Role, familyCode: String) {
        prefs(context).edit()
            .putString(KEY_ROLE, role.name)
            .putString(KEY_FAMILY_CODE, familyCode)
            .apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
