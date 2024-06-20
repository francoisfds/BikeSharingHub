/*
 * Copyright (c) 2014-2015 Bruno Parmentier.
 * Copyright (c) 2020-2024 François FERREIRA DE SOUSA.
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

import android.Manifest;
import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.MatrixCursor;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.IndexOutOfBoundsException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.osmdroid.util.LocationUtils;

import fr.fdesousa.bikesharinghub.db.StationsDataSource;
import fr.fdesousa.bikesharinghub.db.NetworksDataSource;
import fr.fdesousa.bikesharinghub.models.BikeNetwork;
import fr.fdesousa.bikesharinghub.models.BikeNetworkInfo;
import fr.fdesousa.bikesharinghub.models.BikeNetworkLocation;
import fr.fdesousa.bikesharinghub.models.Station;
import fr.fdesousa.bikesharinghub.parsers.BikeNetworkParser;

import fr.fdesousa.bikesharinghub.R;
import fr.fdesousa.bikesharinghub.adapters.SearchStationAdapter;
import fr.fdesousa.bikesharinghub.fragments.StationsListFragment;
import fr.fdesousa.bikesharinghub.fragments.WelcomeDialogFragment;
import fr.fdesousa.bikesharinghub.widgets.StationsListAppWidgetProvider;

public class StationsListActivity extends FragmentActivity implements ActionBar.TabListener, ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = StationsListActivity.class.getSimpleName();

    private static final String DEFAULT_API_URL = "https://api.citybik.es/v2/";
    private static final String PREF_KEY_API_URL = "pref_api_url";
    private static final String PREF_KEY_NETWORK_ID = "network-id";
    private static final String PREF_KEY_NETWORK_NAME = "network-name";
    private static final String PREF_KEY_NETWORK_CITY = "network-city";
    private static final String PREF_KEY_NETWORK_LATITUDE = "network-latitude";
    private static final String PREF_KEY_NETWORK_LONGITUDE = "network-longitude";
    private static final String PREF_KEY_FAV_STATIONS = "fav-stations";
    private static final String PREF_KEY_STRIP_ID_STATION = "pref_strip_id_station";
    private static final String PREF_KEY_DB_LAST_UPDATE = "db_last_update";
    private static final String PREF_KEY_DEFAULT_TAB = "pref_default_tab";

    private static final String KEY_BIKE_NETWORK = "bikeNetwork";
    private static final String KEY_STATIONS = "stations";
    private static final String KEY_FAV_STATIONS = "favStations";
    private static final String KEY_NEARBY_STATIONS = "nearbyStations";
    private static final String KEY_NETWORK_ID = "network-id";

    private static final String[] REQUEST_LOC_LIST = {
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    };
    private static final int REQUEST_LOC_CODE = 1;

    public static final int PICK_NETWORK_REQUEST = 1;

    private BikeNetwork bikeNetwork;
    private ArrayList<Station> stations;
    private ArrayList<Station> favStations;
    private ArrayList<Station> nearbyStations;
    private StationsDataSource stationsDataSource;
    private NetworksDataSource networksDataSource;

    private JSONDownloadTask jsonDownloadTask;

    private SharedPreferences settings;

    private Menu optionsMenu;
    private ActionBar actionBar;
    private SearchView searchView;
    private ViewPager viewPager;
    private TabsPagerAdapter tabsPagerAdapter;

    private StationsListFragment allStationsFragment;
    private StationsListFragment favoriteStationsFragment;
    private StationsListFragment nearbyStationsFragment;
    private String fragTags[] = {null, null, null};

    private SwipeRefreshLayout refreshLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stations_list);

        refreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        refreshLayout.setColorSchemeResources(R.color.bike_red,R.color.parking_blue_dark);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                executeDownloadTask();
            }
        });
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        viewPager.setOffscreenPageLimit(2);
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
                if (tabsPagerAdapter.getItem(position).equals(nearbyStationsFragment)) {
                    if (ContextCompat.checkSelfPermission(StationsListActivity.this,
                            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(StationsListActivity.this,
                            REQUEST_LOC_LIST, REQUEST_LOC_CODE);
                    } else {
                        setNearbyStations();
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                /* as explained on
                http://stackoverflow.com/questions/25978462/swiperefreshlayout-viewpager-limit-horizontal-scroll-only
                 */
                refreshLayout.setEnabled(state == ViewPager.SCROLL_STATE_IDLE);

            }
        });

        stationsDataSource = new StationsDataSource(this);
        networksDataSource = new NetworksDataSource(this);
        stations = stationsDataSource.getStations();
        favStations = stationsDataSource.getFavoriteStations();
        nearbyStations = new ArrayList<>();

        tabsPagerAdapter = new TabsPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(tabsPagerAdapter);

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        actionBar = getActionBar();
        int defaultTabIndex = Integer.valueOf(settings.getString(PREF_KEY_DEFAULT_TAB, "0"));
        for (int i = 0; i < 3; i++) {
            ActionBar.Tab tab = actionBar.newTab();
            tab.setTabListener(this);
            tab.setText(tabsPagerAdapter.getPageTitle(i));
            actionBar.addTab(tab, (defaultTabIndex == i));
        }
        actionBar.setHomeButtonEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        if(settings.contains(PREF_KEY_NETWORK_ID)) {
            upgradeAppSinceVersion25();
        }
        boolean firstRun = false;
        try {
            firstRun = networksDataSource.getNetworksId().size() == 0;
        } catch (IndexOutOfBoundsException e) {}
        setDBLastUpdateText();

        if (firstRun) {
            FragmentManager fm = getSupportFragmentManager();
            WelcomeDialogFragment.getInstance().show(fm, "fragment_welcome");
        } else {
            executeDownloadTask();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOC_CODE:
                if (grantResults.length == 2 && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    setNearbyStations();
                } else if(!ActivityCompat.shouldShowRequestPermissionRationale(
                        this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    nearbyStationsFragment.setEmptyView(R.string.loc_perm_forbidden);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if ((jsonDownloadTask != null && jsonDownloadTask.getStatus() == AsyncTask.Status.FINISHED)) {
            long dbLastUpdate = settings.getLong(PREF_KEY_DB_LAST_UPDATE, -1);
            long currentTime = System.currentTimeMillis();

            /* Refresh list with latest data from database */
            stations = stationsDataSource.getStations();
            favStations = stationsDataSource.getFavoriteStations();
            tabsPagerAdapter.updateAllStationsListFragment(stations);
            tabsPagerAdapter.updateFavoriteStationsFragment(favStations);
            setDBLastUpdateText();

            /* Update automatically if data is more than 10 min old */
            if ((dbLastUpdate != -1) && ((currentTime - dbLastUpdate) > 600000)) {
                executeDownloadTask();
            }
        }
    }

    private void setDBLastUpdateText() {
        TextView lastUpdate = (TextView) findViewById(R.id.dbLastUpdate);
        long dbLastUpdate = settings.getLong(PREF_KEY_DB_LAST_UPDATE, -1);

        if (dbLastUpdate == -1) {
            lastUpdate.setText(String.format(getString(R.string.db_last_update),
                    getString(R.string.db_last_update_never)));
        } else {
            lastUpdate.setText(String.format(getString(R.string.db_last_update),
                    DateUtils.formatSameDayTime(dbLastUpdate, System.currentTimeMillis(),
                            DateFormat.DEFAULT, DateFormat.DEFAULT)));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.optionsMenu = menu;
        getMenuInflater().inflate(R.menu.stations_list, menu);

        if (jsonDownloadTask != null &&
                (jsonDownloadTask.getStatus() == AsyncTask.Status.PENDING
                || jsonDownloadTask.getStatus() == AsyncTask.Status.RUNNING)) {
            setRefreshActionButtonState(true);

        }

        SearchManager manager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(manager.getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                loadData(s);
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.action_refresh:
                executeDownloadTask();
                return true;
            case R.id.action_map:
                Intent mapIntent = new Intent(this, MapActivity.class);
                startActivity(mapIntent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_NETWORK_REQUEST) {
            Log.d(TAG, "PICK_NETWORK_REQUEST");
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "RESULT_OK");
                executeDownloadTask();
            }
        }
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

    }

    private void loadData(String query) {
        ArrayList<Station> queryStations = new ArrayList<>();
        String[] columns = new String[]{"_id", "text"};
        Object[] temp = new Object[]{0, "default"};

        MatrixCursor cursor = new MatrixCursor(columns);

        if (stations != null) {
            for (int i = 0; i < stations.size(); i++) {
                Station station = stations.get(i);
                if (station.getName().toLowerCase().contains(query.toLowerCase())) {
                    temp[0] = i;
                    temp[1] = station.getName();
                    cursor.addRow(temp);
                    queryStations.add(station);
                }
            }
        }

        searchView.setSuggestionsAdapter(new SearchStationAdapter(this, cursor, queryStations));

    }

    private void setRefreshActionButtonState(final boolean refreshing) {
        if (optionsMenu != null) {
            final MenuItem refreshItem = optionsMenu.findItem(R.id.action_refresh);
            if (refreshItem != null) {
                if (refreshing) {
                    refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
                } else {
                    refreshItem.setActionView(null);
                }
            }
        }
    }
    //put here the code to update the bikes data
    private void executeDownloadTask(){
        ArrayList<String> networksId = networksDataSource.getNetworksId();
        ArrayList<String> networksUrlList = new ArrayList<String>();
        for (String id : networksId) {
            String stationUrl = settings.getString(PREF_KEY_API_URL, DEFAULT_API_URL)
                        + "networks/" + id;
            networksUrlList.add(stationUrl);
        }
        String[] networksUrl = networksUrlList.toArray(new String[networksUrlList.size()]);
        jsonDownloadTask = new JSONDownloadTask();
        jsonDownloadTask.execute(networksUrl);
    }


    private void setNearbyStations() {
        if (stations == null) {
            return;
        }
        final double radius = 0.01;
        nearbyStations = new ArrayList<>();
        LocationManager locationManager =
                (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        final Location userLocation = LocationUtils.getLastKnownLocation(locationManager);
        if (userLocation != null) {
            for (Station station : stations) {
                if ((station.getLatitude() > userLocation.getLatitude() - radius)
                        && (station.getLatitude() < userLocation.getLatitude() + radius)
                        && (station.getLongitude() > userLocation.getLongitude() - radius)
                        && (station.getLongitude() < userLocation.getLongitude() + radius)) {
                    nearbyStations.add(station);
                }
            }
            Collections.sort(nearbyStations, new Comparator<Station>() {

                @Override
                public int compare(Station station1, Station station2) {
                    float[] result1 = new float[3];
                    Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                            station1.getLatitude(), station1.getLongitude(), result1);
                    Float distance1 = result1[0];

                    float[] result2 = new float[3];
                    Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                            station2.getLatitude(), station2.getLongitude(), result2);
                    Float distance2 = result2[0];

                    return distance1.compareTo(distance2);
                }
            });
            tabsPagerAdapter.updateNearbyStationsFragment(nearbyStations);
            int locationMinutes = (int) ((System.currentTimeMillis() - userLocation.getTime())/60000);
            if (nearbyStations.size() != 0 && locationMinutes > 10) {
                Toast.makeText(getApplicationContext(),
                        getApplicationContext().getResources().getString(R.string.location_outdated,
                        locationMinutes),Toast.LENGTH_SHORT).show();
            }
        } else {
            nearbyStationsFragment.setEmptyView(R.string.location_not_found);
            // TODO: listen for location
        }
    }

    private class JSONDownloadTask extends AsyncTask<String, Void, String> {

        Exception error;

        @Override
        protected void onPreExecute() {
            setRefreshActionButtonState(true);
        }

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
            if (error != null) {
                Log.d(TAG, error.getMessage());
                Toast.makeText(getApplicationContext(),
                        getApplicationContext().getResources().getString(R.string.connection_error),
                        Toast.LENGTH_SHORT).show();
            } else {
                /* parse result */
                boolean stripId = settings.getBoolean(PREF_KEY_STRIP_ID_STATION, false);
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
                        Toast.makeText(StationsListActivity.this,
                                R.string.json_error, Toast.LENGTH_LONG).show();
                }
                if(stations != null) {
                    Collections.sort(stations);
                    stationsDataSource.storeStations(stations);
                    favStations = stationsDataSource.getFavoriteStations();

                    settings.edit()
                            .putLong(PREF_KEY_DB_LAST_UPDATE, System.currentTimeMillis())
                            .apply();
                    setDBLastUpdateText();

                    if (nearbyStationsFragment.getUserVisibleHint()) {
                        if (ContextCompat.checkSelfPermission(StationsListActivity.this,
                                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(StationsListActivity.this,
                                REQUEST_LOC_LIST, REQUEST_LOC_CODE);
                        } else {
                            setNearbyStations();
                        }
                    }

                    tabsPagerAdapter.updateAllStationsListFragment(stations);
                    tabsPagerAdapter.updateFavoriteStationsFragment(favStations);
                    tabsPagerAdapter.updateNearbyStationsFragment(nearbyStations);

                    Intent refreshWidgetIntent = new Intent(getApplicationContext(),
                            StationsListAppWidgetProvider.class);
                    refreshWidgetIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                    refreshWidgetIntent.putExtra(StationsListAppWidgetProvider.EXTRA_REFRESH_LIST_ONLY, true);
                    sendBroadcast(refreshWidgetIntent);

                } else {
                    Toast.makeText(StationsListActivity.this,
                            R.string.json_error, Toast.LENGTH_LONG).show();
                }
            }
            setRefreshActionButtonState(false);
            refreshLayout.setRefreshing(false);
        }
    }

    private class TabsPagerAdapter extends FragmentPagerAdapter {
        private static final int NUM_ITEMS = 3;

        public TabsPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);

            allStationsFragment = StationsListFragment.newInstance(StationsListFragment.FRAGMENT_ALL,
                    getResources().getString(R.string.no_stations));
            favoriteStationsFragment = StationsListFragment.newInstance(StationsListFragment.FRAGMENT_FAVORITES,
                    getResources().getString(R.string.no_favorite_stations));
            nearbyStationsFragment = StationsListFragment.newInstance(StationsListFragment.FRAGMENT_NEARBY,
                    getResources().getString(R.string.no_nearby_stations));
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return nearbyStationsFragment;
                case 1:
                    return favoriteStationsFragment;
                case 2:
                    return allStationsFragment;
                default:
                    return null;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.nearby_stations);
                case 1:
                    return getString(R.string.favorite_stations);
                case 2:
                    return getString(R.string.all_stations);
                default:
                    return null;
            }
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position)
        {
            Fragment frag = (Fragment) super.instantiateItem(container, position);
            fragTags[position] = frag.getTag();
            return frag;
        }

        public void updateAllStationsListFragment(ArrayList<Station> stations) {
            if(fragTags[2] != null) {
                StationsListFragment frgt = (StationsListFragment) getSupportFragmentManager().findFragmentByTag(fragTags[2]);
                frgt.updateStationsList(stations);
            }
        }

        public void updateFavoriteStationsFragment(ArrayList<Station> stations) {
            if(fragTags[1] != null) {
                StationsListFragment frgt = (StationsListFragment) getSupportFragmentManager().findFragmentByTag(fragTags[1]);
                frgt.updateStationsList(stations);
            }
        }

        public void updateNearbyStationsFragment(ArrayList<Station> stations) {
            if(fragTags[0] != null) {
                StationsListFragment frgt = (StationsListFragment) getSupportFragmentManager().findFragmentByTag(fragTags[0]);
                frgt.updateStationsList(stations);
            }
        }
    }

    /* On VERSIONCODE 25 network-id was store in shared-pref, migrate data into database. */
    private void upgradeAppSinceVersion25() {

        //Write current network-id and its attributes in the 'networks' table
        String id = settings.getString(PREF_KEY_NETWORK_ID, "");
        String name = settings.getString(PREF_KEY_NETWORK_NAME, "");
        String city = settings.getString(PREF_KEY_NETWORK_CITY, "");
        double latitude = Double.longBitsToDouble(settings.getLong(
                        PREF_KEY_NETWORK_LATITUDE, 0));
        double longitude = Double.longBitsToDouble(settings.getLong(
                        PREF_KEY_NETWORK_LONGITUDE, 0));
        BikeNetworkLocation loc = new BikeNetworkLocation(latitude, longitude, city, "");
        BikeNetworkInfo savedNetwork = new BikeNetworkInfo(id, name, "", loc);
        networksDataSource.storeNetworks(new ArrayList<BikeNetworkInfo>(Arrays.asList(savedNetwork)));

        //Delete obsolete shared preferences
        settings.edit().remove(PREF_KEY_NETWORK_ID).apply();
        settings.edit().remove(PREF_KEY_NETWORK_NAME).apply();
        settings.edit().remove(PREF_KEY_NETWORK_CITY).apply();
        settings.edit().remove(PREF_KEY_NETWORK_LATITUDE).apply();
        settings.edit().remove(PREF_KEY_NETWORK_LONGITUDE).apply();
    }
}
