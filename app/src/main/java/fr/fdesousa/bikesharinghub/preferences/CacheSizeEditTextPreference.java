/*
 * Copyright (c) 2022 François FERREIRA DE SOUSA.
 *
 * This file is part of BikeSharingHub.
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
package fr.fdesousa.bikesharinghub.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.util.AttributeSet;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.View;
import android.widget.EditText;

import fr.fdesousa.bikesharinghub.R;

public class CacheSizeEditTextPreference extends EditTextPreference {

    private static final String PREF_KEY_MAP_CACHE_MAX_SIZE = "pref_map_tiles_cache_max_size";
    private static final String PREF_KEY_MAP_CACHE_TRIM_SIZE = "pref_map_tiles_cache_trim_size";
    private Context mContext;
    private SharedPreferences mPrefs = null;

    public CacheSizeEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mPrefs = getPreferenceManager().getDefaultSharedPreferences(mContext);
    }

    public CacheSizeEditTextPreference(Context context) {
        super(context);
        mContext = context;
        mPrefs = getPreferenceManager().getDefaultSharedPreferences(mContext);
    }

    @Override
    protected void onAddEditTextToDialogView(View dialogView, EditText editText) {
        if (this.getKey().equals(PREF_KEY_MAP_CACHE_MAX_SIZE)) {
            int minimalValue = Integer.parseInt(mPrefs.getString("pref_map_tiles_cache_trim_size", "20")) + 1;
            editText.setText("");
            editText.setFilters(new InputFilter[]{new InputFilterMinMax(1, 600)});
            editText.setHint(String.format(mContext.getString(
                    R.string.pref_map_tiles_cache_size_hint,
                    minimalValue, 600)));
        } else if (this.getKey().equals(PREF_KEY_MAP_CACHE_TRIM_SIZE)) {
            int maximalValue = Integer.parseInt(mPrefs.getString("pref_map_tiles_cache_max_size", "100")) - 1;
            if (maximalValue > 500) {
                maximalValue = 500;
            }
            editText.setText("");
            editText.setFilters(new InputFilter[]{new InputFilterMinMax(0, maximalValue)});
            editText.setHint(String.format(mContext.getString(
                    R.string.pref_map_tiles_cache_size_hint,
                    0, maximalValue)));
        }
        super.onAddEditTextToDialogView(dialogView, editText);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if(positiveResult) {
            EditText edit = getEditText();
            if (getKey().equals(PREF_KEY_MAP_CACHE_MAX_SIZE) &&
                Integer.parseInt(edit.getText().toString()) <= Integer.parseInt
                (mPrefs.getString("pref_map_tiles_cache_trim_size", "20"))) {
                return;
            } else if (getKey().equals(PREF_KEY_MAP_CACHE_TRIM_SIZE) &&
                    Integer.parseInt(edit.getText().toString()) >= Integer.parseInt
                    (mPrefs.getString("pref_map_tiles_cache_max_size", "100"))) {
                return;
            }
        }
        super.onDialogClosed(positiveResult);
    }

    private class InputFilterMinMax implements InputFilter {

        private int min, max;

        public InputFilterMinMax(int min, int max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {   
            try {
                int input = Integer.parseInt(dest.toString() + source.toString());
                if (isInRange(min, max, input))
                    return null;
            } catch (NumberFormatException nfe) { }     
            return "";
        }

        private boolean isInRange(int a, int b, int c) {
            return b > a ? c >= a && c <= b : c >= b && c <= a;
        }
    }

}
