package com.ideacompost.app.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** 个人资料：昵称与头像 emoji（非敏感，普通 prefs 即可）。 */
@Singleton
class ProfileStore @Inject constructor(@ApplicationContext ctx: Context) {
    private val p = ctx.getSharedPreferences("profile", Context.MODE_PRIVATE)

    var nickname: String
        get() = p.getString(KEY_NICK, "园丁") ?: "园丁"
        set(v) = p.edit().putString(KEY_NICK, v).apply()

    var avatarEmoji: String
        get() = p.getString(KEY_AVATAR, "🌱") ?: "🌱"
        set(v) = p.edit().putString(KEY_AVATAR, v).apply()

    private companion object {
        const val KEY_NICK = "nickname"
        const val KEY_AVATAR = "avatar_emoji"
    }
}
