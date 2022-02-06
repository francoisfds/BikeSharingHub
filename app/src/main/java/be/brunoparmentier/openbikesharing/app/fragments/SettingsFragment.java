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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);

        setupInstallOpenBikeSharing();
        setupVersionEntry();
    }

    private void setupInstallOpenBikeSharing() {
        //Market link
        final Preference openBikeSharingPref = findPreference("pref_open_bike_sharing");
        Intent marketIntent = new Intent(Intent.ACTION_VIEW);
        marketIntent.setData(Uri.parse("market://details?id=be.brunoparmentier.openbikesharing.app"));

        PackageManager packageManager = getActivity().getPackageManager();
        List<ResolveInfo> marketActivities =
                packageManager.queryIntentActivities(marketIntent, 0);
        boolean isMarketIntentSafe = marketActivities.size() > 0;

        if (isMarketIntentSafe) {
            openBikeSharingPref.setIntent(marketIntent);
        }
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

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        updatePreference(PREF_KEY_API_URL);
        updatePreference(PREF_KEY_CHOOSE_NETWORK);
    }

    @Override
    public void onPause() {
        super.onPause();

        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
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
        } else if (key.equals(PREF_KEY_CHOOSE_NETWORK)) {
            Preference preference = findPreference(key);
            NetworksDataSource networksDataSource = new NetworksDataSource(getContext());
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
        }
    }
}
