/*
 * Copyright (c) 2014-2015 Bruno Parmentier.
 * Copyright (c) 2022 François FERREIRA DE SOUSA.
 *
 * This file is part of BikeSharingHub.
 * BikeSharingHub incorporates a modified version of OpenBikeSharing
 *
 * BikeSharingHub is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BikeSharingHub is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BikeSharingHub.  If not, see <http://www.gnu.org/licenses/>.
 */

package be.brunoparmentier.openbikesharing.app.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;

import java.util.List;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.cachemanager.CacheManager;
import org.osmdroid.tileprovider.tilesource.TileSourcePolicyException;
import org.osmdroid.views.MapView;

import be.brunoparmentier.openbikesharing.app.BuildConfig;
import be.brunoparmentier.openbikesharing.app.R;
import be.brunoparmentier.openbikesharing.app.db.NetworksDataSource;
import be.brunoparmentier.openbikesharing.app.models.BikeNetworkInfo;

/**
 * Settings fragment
 */
public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "SettingsFragment";
    private static final String PREF_KEY_CHOOSE_NETWORK = "choose_network";
    private static final String PREF_KEY_API_URL = "pref_api_url";
    private static final String PREF_KEY_MAP_CACHE_MAX_SIZE = "pref_map_tiles_cache_max_size";
    private static final String PREF_KEY_MAP_CACHE_TRIM_SIZE = "pref_map_tiles_cache_trim_size";
    private Context mContext = null;
    private SharedPreferences mPrefs = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);

        mPrefs = getPreferenceScreen().getSharedPreferences();
        setupVersionEntry();
    }

    @Override
    public void onAttach (Activity activity) {
        super.onAttach (activity);
        mContext = (Context) activity;
    }

    /* Setup version entry */
    private void setupVersionEntry() {
        String versionName;
        final Preference versionPref = findPreference("pref_version");
        try {
            versionName = getActivity().getPackageManager()
                    .getPackageInfo(getActivity().getPackageName(), 0).versionName;
            if (BuildConfig.DEBUG) {
                String buildTime = DateFormat.format("yyyyMMddHHmmdd", new java.util.Date(BuildConfig.BUILD_TIMESTAMP)).toString();
                versionName += "-debug-" + buildTime;
            }
            versionPref.setSummary(versionName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    public void onResume(){
        super.onResume();

        mPrefs.registerOnSharedPreferenceChangeListener(this);
        updatePreference(PREF_KEY_API_URL);
        updatePreference(PREF_KEY_CHOOSE_NETWORK);
        updatePreference(PREF_KEY_MAP_CACHE_MAX_SIZE);
        updatePreference(PREF_KEY_MAP_CACHE_TRIM_SIZE);
    }

    @Override
    public void onPause() {
        super.onPause();

        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updatePreference(key);
    }

    private void updatePreference(String key){
        if (key.equals(PREF_KEY_API_URL)){
            Preference preference = findPreference(key);
            //if (preference instanceof EditTextPreference){
                EditTextPreference editTextPreference =  (EditTextPreference) preference;
                editTextPreference.setSummary(editTextPreference.getText());
            //}
        } else if (key.equals(PREF_KEY_CHOOSE_NETWORK) && mContext != null) {
            Preference preference = findPreference(key);
            NetworksDataSource networksDataSource = new NetworksDataSource(mContext);
            List<String> idList = networksDataSource.getNetworksId();
            switch(idList.size()) {
                case 0:
                    preference.setSummary(getString(R.string.pref_title_bike_networks_list_summary_none));
                    break;
                case 1:
                    BikeNetworkInfo networkInfo = networksDataSource.getNetworkInfoFromId(idList.get(0));
                    String networkName = networkInfo.getName();
                    String networkCity = networkInfo.getLocation().getCity();
                    preference.setSummary(networkName + " (" + networkCity + ")");
                    break;
                default:
                    preference.setSummary(getResources().getString(
                            R.string.pref_title_bike_networks_list_summary_multiple_selection,
                            idList.size()));
                    break;
                }
        } else if (key.equals(PREF_KEY_MAP_CACHE_MAX_SIZE)){
            Preference preference = findPreference(key);
            EditTextPreference editTextPreference =  (EditTextPreference) preference;
            try {
                CacheManager mCacheManager = new CacheManager(new MapView(mContext));
                long tileCacheUsed = mCacheManager.currentCacheUsage();
                float approx = Float.valueOf(tileCacheUsed) / 1048576;
                editTextPreference.setSummary(String.format(getString(
                    R.string.pref_map_tiles_cache_max_size_summary,
                    editTextPreference.getText(), String.format("%.2f", approx))));
            } catch (TileSourcePolicyException e) {
                // No cache usage to display (should not happen)
                editTextPreference.setSummary(String.format(getString(
                    R.string.pref_map_tiles_cache_trim_size_summary,
                    editTextPreference.getText())));
                e.printStackTrace();
            }
        } else if (key.equals(PREF_KEY_MAP_CACHE_TRIM_SIZE)){
            Preference preference = findPreference(key);
            EditTextPreference editTextPreference =  (EditTextPreference) preference;
            editTextPreference.setSummary(String.format(getString(
                    R.string.pref_map_tiles_cache_trim_size_summary,
                    editTextPreference.getText())));
        }
    }
}
