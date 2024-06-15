/*
 * Copyright (c) 2015 Bruno Parmentier.
 * Copyright (c) 2022-2024 François FERREIRA DE SOUSA.
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

package fr.fdesousa.bikesharinghub.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fr.fdesousa.bikesharinghub.db.StationsDataSource;
import fr.fdesousa.bikesharinghub.db.NetworksDataSource;
import fr.fdesousa.bikesharinghub.models.BikeNetwork;
import fr.fdesousa.bikesharinghub.models.Station;
import fr.fdesousa.bikesharinghub.parsers.BikeNetworkParser;

import fr.fdesousa.bikesharinghub.R;
import fr.fdesousa.bikesharinghub.activities.StationsListActivity;

/**
 * Implementation of App Widget functionality.
 */
public class StationsListAppWidgetProvider extends AppWidgetProvider {
    private static final String TAG = StationsListAppWidgetProvider.class.getSimpleName();

    private static final String DEFAULT_API_URL = "https://api.citybik.es/v2/";
    private static final String PREF_KEY_API_URL = "pref_api_url";
    private static final String PREF_KEY_DB_LAST_UPDATE = "db_last_update";
    private static final String PREF_KEY_STRIP_ID_STATION = "pref_strip_id_station";

    public static final String EXTRA_ITEM = "be.brunoparmentier.openbikesharing.app.widget.EXTRA_ITEM";
    public static final String EXTRA_REFRESH_LIST_ONLY =
            "be.brunoparmentier.openbikesharing.app.widget.EXTRA_REFRESH_LIST_ONLY";

    private ArrayList<Station> stations;
    private Context mContext;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate");
        mContext = context;

        // update each of the widgets with the remote adapter
        for (int appWidgetId : appWidgetIds) {
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.app_widget);

            rv.setViewVisibility(R.id.widgetRefreshButton, View.VISIBLE);

            // The empty view is displayed when the collection has no items. It should be a sibling
            // of the collection view.
            rv.setEmptyView(R.id.widgetStationsList, R.id.widgetEmptyView);

            // Here we setup the intent which points to the StationsListAppWidgetService which will
            // provide the views for this collection.
            Intent intent = new Intent(context, StationsListAppWidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            // When intents are compared, the extras are ignored, so we need to embed the extras
            // into the data so that the extras will not be ignored.
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            rv.setRemoteAdapter(R.id.widgetStationsList, intent);

            // Click on the refresh button updates the stations
            final Intent refreshIntent = new Intent(context, StationsListAppWidgetProvider.class);
            refreshIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            final PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, appWidgetId,
                    refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            rv.setOnClickPendingIntent(R.id.widgetRefreshButton, refreshPendingIntent);

            // Click on the widget title launches application
            final Intent openAppIntent = new Intent(Intent.ACTION_MAIN);
            openAppIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            openAppIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            openAppIntent.setComponent(new ComponentName(context.getPackageName(),
                    StationsListActivity.class.getCanonicalName()));
            final PendingIntent openAppPendingIntent = PendingIntent.getActivity(context, 0,
                    openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            rv.setOnClickPendingIntent(R.id.widgetTitle, openAppPendingIntent);

            /* Set last checked time from database */
            long dbLastUpdate = PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(PREF_KEY_DB_LAST_UPDATE, -1);

            if (dbLastUpdate == -1) {
                rv.setTextViewText(R.id.WidgetDbLastUpdate,
                    String.format(context.getString(R.string.db_last_update),
                    context.getString(R.string.db_last_update_never)));
            } else {
                rv.setTextViewText(R.id.WidgetDbLastUpdate, String.format(context.getString(R.string.db_last_update),
                    DateUtils.formatSameDayTime(dbLastUpdate, System.currentTimeMillis(),
                            DateFormat.DEFAULT, DateFormat.DEFAULT)));
            }

            appWidgetManager.updateAppWidget(appWidgetId, rv);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        super.onDisabled(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        Log.d(TAG, intent.getAction() + " received");
        mContext = context;

        if (intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
            final AppWidgetManager mgr = AppWidgetManager.getInstance(mContext);
            final ComponentName cn = new ComponentName(mContext, StationsListAppWidgetProvider.class);
            if (intent.getBooleanExtra(EXTRA_REFRESH_LIST_ONLY, false)) {
                /* Update widget list with data from database */
                mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn), R.id.widgetStationsList);

                /* Update all views */
                StationsListAppWidgetProvider.this.onUpdate(mContext, mgr, mgr.getAppWidgetIds(cn));
            } else {
                /* Hide the refresh button to show feedback */
                RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.app_widget);
                rv.setViewVisibility(R.id.widgetRefreshButton, View.GONE);
                int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                mgr.partiallyUpdateAppWidget(widgetId, rv);

                /* Download new data then update widget list */
                NetworksDataSource networksDataSource = new NetworksDataSource(context);
                ArrayList<String> networksId = networksDataSource.getNetworksId();
                ArrayList<String> networksUrlList = new ArrayList<String>();
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                for (String id : networksId) {
                    String stationUrl = settings.getString(PREF_KEY_API_URL, DEFAULT_API_URL)
                                + "networks/" + id;
                    networksUrlList.add(stationUrl);
                }
                String[] networksUrl = networksUrlList.toArray(new String[networksUrlList.size()]);

                new JSONDownloadTask().execute(networksUrl);
            }
        }
    }

    private class JSONDownloadTask extends AsyncTask<String, Void, String> {

        Exception error;

        @Override
        protected String doInBackground(String... urls) {
            if (urls.length == 0 || urls[0].isEmpty()) {
                error = new Exception("No URL to fetch");
                return null;
            }
            JSONArray networksArray = new JSONArray();
            for (int i=0; i<urls.length; i++) {
                try {
                    StringBuilder response = new StringBuilder();
                    URL url = new URL(urls[i]);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String strLine;
                        while ((strLine = input.readLine()) != null) {
                            response.append(strLine);
                        }
                        input.close();
                    }
                    networksArray.put(new JSONObject(response.toString()));
                } catch (Exception e) {
                    Log.e(TAG, urls[i] + ": " + e.getClass().getSimpleName() + " (" + e.getMessage() + ")");
                }
            }
            if(networksArray.length() == 0) {
                error = new Exception("Unable to fetch any response");
            }
            return networksArray.toString();
        }

        @Override
        protected void onPostExecute(final String result) {
            final AppWidgetManager mgr = AppWidgetManager.getInstance(mContext);
            final ComponentName cn = new ComponentName(mContext, StationsListAppWidgetProvider.class);
            if (error != null) {
                Log.d(TAG, error.getMessage());
            } else {
                /* parse result */
                boolean stripId = PreferenceManager.getDefaultSharedPreferences(mContext)
                    .getBoolean(PREF_KEY_STRIP_ID_STATION, false);
                stations = null;
                try {
                    JSONArray rawNetworks = new JSONArray(result);

                    for (int i = 0; i < rawNetworks.length(); i++) {
                        JSONObject rawNetwork = rawNetworks.getJSONObject(i);
                        try{
                            BikeNetworkParser bikeNetworkParser = new BikeNetworkParser(rawNetwork.toString(), stripId);

                            BikeNetwork bikeNetwork = bikeNetworkParser.getNetwork();
                            if(stations == null) {
                                stations = bikeNetwork.getStations();
                            } else {
                                stations.addAll(bikeNetwork.getStations());
                            }
                        } catch (ParseException e) {
                            Log.e(TAG, "Error retreiving data of network " + (i+1) + " : " + e.getMessage());
                        }
                    }
                } catch (JSONException e) {
                        Log.e(TAG, e.getMessage());
                }
                if(stations != null) {
                    Collections.sort(stations);
                    StationsDataSource stationsDataSource = new StationsDataSource(mContext);
                    stationsDataSource.storeStations(stations);

                    PreferenceManager.getDefaultSharedPreferences(mContext).edit()
                            .putLong(PREF_KEY_DB_LAST_UPDATE, System.currentTimeMillis())
                            .apply();

                    mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn), R.id.widgetStationsList);
                }
            }
            /* Update all views anyway */
            StationsListAppWidgetProvider.this.onUpdate(mContext, mgr, mgr.getAppWidgetIds(cn));
        }
    }
}

