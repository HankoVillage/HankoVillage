package com.konit.hankovillage.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.konit.hankovillage.ui.counter.ProjectData

object GuestProjectStorage {
    private const val PREF_NAME = "guest_counter_prefs"
    private const val KEY_PROJECTS = "guest_projects"
    private val gson = Gson()

    fun loadProjects(context: Context): List<ProjectData> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PROJECTS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ProjectData>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveProjects(context: Context, projects: List<ProjectData>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(projects)
        prefs.edit().putString(KEY_PROJECTS, json).apply()
    }

    fun hasGuestProjects(context: Context): Boolean {
        return loadProjects(context).isNotEmpty()
    }

    fun clearProjects(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_PROJECTS).apply()
    }
}