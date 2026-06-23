package com.imi.smartedge.sidebar.panel

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LocaleHelper {
    private const val SELECTED_LANGUAGE = "Locale.Helper.Selected.Language"

    fun onAttach(context: Context): Context {
        val lang = getPersistedData(context, Locale.getDefault().language)
        return setLocale(context, lang)
    }

    fun setLocale(context: Context, language: String?): Context {
        persist(context, language)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            updateResources(context, language)
        } else {
            updateResourcesLegacy(context, language)
        }
    }

    private fun getPersistedData(context: Context, defaultLanguage: String): String? {
        val preferences = context.getSharedPreferences("panel_prefs", Context.MODE_PRIVATE)
        return preferences.getString(SELECTED_LANGUAGE, defaultLanguage)
    }

    private fun persist(context: Context, language: String?) {
        val preferences = context.getSharedPreferences("panel_prefs", Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putString(SELECTED_LANGUAGE, language)
        editor.apply()
    }

    private fun updateResources(context: Context, language: String?): Context {
        val locale = if (language.isNullOrEmpty()) Locale.getDefault() else Locale(language)
        Locale.setDefault(locale)
        val configuration = context.resources.configuration
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        
        // Also update legacy resources for older components/Skin compatibility
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
        
        return context.createConfigurationContext(configuration)
    }

    private fun updateResourcesLegacy(context: Context, language: String?): Context {
        val locale = if (language.isNullOrEmpty()) Locale.getDefault() else Locale(language)
        Locale.setDefault(locale)
        val resources = context.resources
        val configuration = resources.configuration
        configuration.locale = locale
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLayoutDirection(locale)
        }
        resources.updateConfiguration(configuration, resources.displayMetrics)
        return context
    }
}
