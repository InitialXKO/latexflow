package com.growsnova.latexflow.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

/**
 * 历史记录持久化存储 (基于 SharedPreferences)
 * TODO: 未来可迁移至 Room 数据库以支持更复杂的操作
 */
class HistoryRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("latex_history", Context.MODE_PRIVATE)
    private val KEY_HISTORY = "history_list"
    private val MAX_HISTORY = 20

    /**
     * 添加新的识别记录
     */
    fun addRecord(latex: String) {
        val current = getHistory().toMutableList()
        current.remove(latex) // 去重
        current.add(0, latex)
        
        val saveList = if (current.size > MAX_HISTORY) current.take(MAX_HISTORY) else current
        prefs.edit().putString(KEY_HISTORY, JSONArray(saveList).toString()).apply()
    }

    /**
     * 获取历史记录列表
     */
    fun getHistory(): List<String> {
        val json = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        val array = JSONArray(json)
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        return list
    }

    /**
     * 清空历史
     */
    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }
}
