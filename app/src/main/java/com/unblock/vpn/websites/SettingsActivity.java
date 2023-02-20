package com.unblock.vpn.websites;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;


import com.unblock.vpn.websites.util.LocaleHelper;
import com.unblock.vpn.websites.util.SettingsManager;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private SettingsManager settingsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        settingsManager = settingsManagerListener;
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
        TextView toolbarTittle = findViewById(R.id.toolbar_title);
        toolbarTittle.setText(R.string.settings_text);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, SettingsFragment.setListener(settingsManager, new SettingsFragment()))
                    .commit();
        }
    }

    SettingsManager settingsManagerListener = new SettingsManager() {
        @Override
        public void onLanguageChange(String locale) {
            Log.d("SettingsManager", locale);
            LocaleHelper.setLocale(SettingsActivity.this, locale);
            recreate();
        }

        @Override
        public void onThemeChange(String theme) {
            Log.d("SettingsManager", theme);
            changeUiMode(theme);
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        LocaleHelper.setLocale(SettingsActivity.this, preferences.getString("language", "en"));
    }

    private void setLocale(String x){
        Locale locale = new Locale(x);
        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        DisplayMetrics displayMetrics = resources.getDisplayMetrics();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
            configuration.setLocale(locale);
        } else{
            configuration.locale=locale;
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N){
            getApplicationContext().createConfigurationContext(configuration);
        } else {
            resources.updateConfiguration(configuration,displayMetrics);
        }
        recreate();
    }

    public void changeUiMode(String theme){
        if(theme.equals("Dark mode")){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }else if(theme.equals("Auto")){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }else if(theme.equals("Light mode")){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home :
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public static class SettingsFragment extends PreferenceFragmentCompat{
        private static SettingsManager settingsManagerx;

        public static Fragment setListener(SettingsManager settingsManager, Fragment fragment){
            settingsManagerx = settingsManager;
            return fragment;
        }

        @Override
        public void onAttach(@NonNull Context context) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            String len = preferences.getString("language", "en");
            LocaleHelper.setLocale(context, len);
            super.onAttach(context);
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            Preference theme = findPreference("theme");
            theme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    settingsManagerx.onThemeChange(newValue.toString());
                    return true;
                }
            });

//            Preference language = findPreference("language");
//            language.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//                @Override
//                public boolean onPreferenceChange(Preference preference, Object newValue) {
//                    settingsManagerx.onLanguageChange(newValue.toString());
//                    return true;
//                }
//            });
        }



    }
}