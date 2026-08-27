package com.ideacompost.app.data.ai

import org.json.JSONArray
import org.json.JSONObject

/** JSON 五级管线的 MVP 实现：剥壳 → 定位首个平衡花括号 → 解析（specs/02 §3）。 */
object JsonExtractor {

    fun extractObject(text: String): JSONObject {
        val raw = locate(text, '{', '}') ?: throw IllegalArgumentException("no json object found")
        return JSONObject(raw)
    }

    fun extractArray(text: String): JSONArray {
        val raw = locate(text, '[', ']') ?: throw IllegalArgumentException("no json array found")
        return JSONArray(raw)
    }

    private fun locate(text: String, open: Char, close: Char): String? {
        val start = text.indexOf(open)
        if (start < 0) return null
        var depth = 0
        var inStr = false
        var esc = false
        for (i in start until text.length) {
            val c = text[i]
            when {
                esc -> esc = false
                c == '\\' && inStr -> esc = true
                c == '"' -> inStr = !inStr
                !inStr && c == open -> depth++
                !inStr && c == close -> {
                    depth--
                    if (depth == 0) return text.substring(start, i + 1)
                }
            }
        }
        return null
    }
}
