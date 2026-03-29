/*
 * Copyright (c) 2014-2015 Bruno Parmentier.
 * Copyright (c) 2023,2026 François FERREIRA DE SOUSA.
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

package fr.fdesousa.bikesharinghub.activities;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import androidx.core.app.NavUtils;
import androidx.fragment.app.Fragment;

import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.MenuItem;

import fr.fdesousa.bikesharinghub.R;
import fr.fdesousa.bikesharinghub.fragments.SettingsAboutFragment;
import fr.fdesousa.bikesharinghub.fragments.SettingsAdvancedFragment;
import fr.fdesousa.bikesharinghub.fragments.SettingsFragment;
import fr.fdesousa.bikesharinghub.models.Station;

/**
 * Settings activity
 */
public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        PreferenceFragment targetFragment = new SettingsFragment();
        String EXTRA_SUBSCREEN_NAME = "extra_subscreen_name";
        if(getIntent().hasExtra(EXTRA_SUBSCREEN_NAME)) {
            String subscreenExtra = getIntent().getStringExtra(EXTRA_SUBSCREEN_NAME);
            if(subscreenExtra.equals("advanced")) {
                targetFragment = new SettingsAdvancedFragment();
                setTitle(getResources().getString(R.string.pref_title_advanced));
            } else if (subscreenExtra.equals("about")) {
                targetFragment = new SettingsAboutFragment();
                setTitle(getResources().getString(R.string.pref_title_about));
            }
        }

        getFragmentManager().beginTransaction().replace(android.R.id.content,
                targetFragment).commit();
    }
}
