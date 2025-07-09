/*
 * Copyright (c) 2025 François FERREIRA DE SOUSA.
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

package fr.fdesousa.bikesharinghub.tasks;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;

import fr.fdesousa.bikesharinghub.R;
import fr.fdesousa.bikesharinghub.db.NetworksDataSource;
import fr.fdesousa.bikesharinghub.db.StationsDataSource;
import fr.fdesousa.bikesharinghub.models.BikeNetwork;
import fr.fdesousa.bikesharinghub.models.DownloadResult;
import fr.fdesousa.bikesharinghub.models.Station;
import fr.fdesousa.bikesharinghub.parsers.BikeNetworkParser;
import fr.fdesousa.bikesharinghub.widgets.StationsListAppWidgetProvider;

public class JSONDownloadRunnable implements Runnable {

    private static final String TAG = JSONDownloadRunnable.class.getSimpleName();
    private static final String PREF_KEY_API_URL = "pref_api_url";
    public static final String PREF_KEY_STRIP_ID_STATION = "pref_strip_id_station";
    public static final String PREF_KEY_DB_LAST_UPDATE = "db_last_update";
    String mError = null;
    Context mContext;
    DownloadResult mDownloadResult;

    public JSONDownloadRunnable(Context context, DownloadResult callback) {
        mContext = context;
        mDownloadResult = callback;
    }

    @Override
    public void run() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        ArrayList<String> networksId = (new NetworksDataSource(mContext)).getNetworksId();
        ArrayList<String> networksUrlList = new ArrayList<String>();
        String default_apiUrl = mContext.getResources().getString(R.string.pref_default_api_url);
        for (String id : networksId) {
            String stationUrl = sharedPref.getString(PREF_KEY_API_URL, default_apiUrl)
                    + "networks/" + id;
            networksUrlList.add(stationUrl);
        }
        String[] mNetworksUrl = networksUrlList.toArray(new String[networksUrlList.size()]);

        if (mNetworksUrl.length == 0 || mNetworksUrl[0].isEmpty()) {
            mError = "No URL to fetch";
            mDownloadResult.onDownloadResultCallback(mError);
            return;
        }
        JSONArray networksArray = new JSONArray();
        for (int i=0; i<mNetworksUrl.length; i++) {
            try {
                StringBuilder response = new StringBuilder();
                URL url = new URL(mNetworksUrl[i]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String strLine;
                    while ((strLine = input.readLine()) != null) {
                        response.append(strLine);
                    }
                    input.close();
                }
                mDownloadResult.onDownloadResultCallback((int) Math.round(100.0 * (i+1) / mNetworksUrl.length)-1);
                networksArray.put(new JSONObject(response.toString()));
            } catch (InterruptedIOException e) {
                return; //End up silently
            } catch (Exception e) {
                Log.e(TAG, mNetworksUrl[i] + ": " + e.getClass().getSimpleName() + " (" + e.getMessage() + ")");
            }
        }
        if(networksArray.length() == 0) {
            mError = "Unable to fetch any response";
            mDownloadResult.onDownloadResultCallback(mError);
            return;
        }
        String result = networksArray.toString();
        ArrayList<Station> stations = null;
        try {
            JSONArray rawNetworks = new JSONArray(result);

            for (int i = 0; i < rawNetworks.length(); i++) {
                JSONObject rawNetwork = rawNetworks.getJSONObject(i);
                try{
                    boolean stripId = sharedPref.getBoolean(PREF_KEY_STRIP_ID_STATION, false);
                    BikeNetworkParser bikeNetworkParser = new BikeNetworkParser(rawNetwork.toString(), stripId);

                    BikeNetwork bikeNetwork = bikeNetworkParser.getNetwork();
                    if(stations == null) {
                        stations = bikeNetwork.getStations();
                    } else {
                        stations.addAll(bikeNetwork.getStations());
                    }
                } catch (ParseException e) {
                    mError = "Error retreiving data of network " + (i+1) + ": " + e.getMessage();
                }
            }
            if(stations != null) {
                Collections.sort(stations);
                StationsDataSource stationHelper = new StationsDataSource(mContext);
                stationHelper.storeStations(stations);
                sharedPref.edit()
                        .putLong(PREF_KEY_DB_LAST_UPDATE, System.currentTimeMillis())
                        .apply();
            }
        } catch (JSONException e) {
            mError = String.valueOf(R.string.json_error);
        }

        Intent refreshWidgetIntent = new Intent(mContext,
                StationsListAppWidgetProvider.class);
        refreshWidgetIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        refreshWidgetIntent.putExtra(StationsListAppWidgetProvider.EXTRA_REFRESH_LIST_ONLY, true);
        mContext.sendBroadcast(refreshWidgetIntent);
        if(mError != null) {
            mDownloadResult.onDownloadResultCallback(mError);
        } else {
            mDownloadResult.onDownloadResultCallback(100);
        }
    }
}
