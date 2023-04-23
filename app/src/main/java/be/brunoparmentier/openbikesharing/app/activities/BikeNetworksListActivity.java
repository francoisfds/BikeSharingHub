/*
 * Copyright (c) 2014-2015 Bruno Parmentier.
 * Copyright (c) 2021-2023 François FERREIRA DE SOUSA.
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

package be.brunoparmentier.openbikesharing.app.activities;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import be.brunoparmentier.openbikesharing.app.R;
import be.brunoparmentier.openbikesharing.app.adapters.BikeNetworksListAdapter;
import be.brunoparmentier.openbikesharing.app.db.NetworksDataSource;
import be.brunoparmentier.openbikesharing.app.models.BikeNetworkInfo;
import be.brunoparmentier.openbikesharing.app.parsers.BikeNetworksListParser;

public class BikeNetworksListActivity extends Activity {
    private static final String TAG = BikeNetworksListActivity.class.getSimpleName();

    private static final String DEFAULT_API_URL = "https://api.citybik.es/v2/";
    private static final String PREF_KEY_API_URL = "pref_api_url";

    private Comparator<BikeNetworkInfo> mLocationComparator = new Comparator<BikeNetworkInfo>() {
        public int compare(BikeNetworkInfo network1, BikeNetworkInfo network2) {
            return String.CASE_INSENSITIVE_ORDER.compare(
                        network1.getLocationName(), network2.getLocationName());
        }
    };

    private ListView listView;
    private HashMap<String, BikeNetworkInfo> BikeNetworksHashMap;
    private ArrayList<String> savedNetworksList;
    private ArrayList<String> cannotFetchNetworksList;
    private BikeNetworksListAdapter bikeNetworksListAdapter;
    private NetworksDataSource networksDataSource;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bike_networks_list);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        BikeNetworksHashMap = new HashMap<String, BikeNetworkInfo>();
        cannotFetchNetworksList = new ArrayList<String>();
        networksDataSource = new NetworksDataSource(this);
        savedNetworksList = networksDataSource.getNetworksId();

        listView = (ListView) findViewById(R.id.networksListView);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if(listView.getCheckedItemCount() == 0) {
                    // Do not allow to uncheck all items, so now force this one
                    listView.setItemChecked(position, true);
                }
                ArrayList<BikeNetworkInfo> networksToKeepList = new ArrayList<>();
                for (BikeNetworkInfo network : BikeNetworksHashMap.values()) {
                    if (savedNetworksList.contains(network.getId())) {
                        networksToKeepList.add(network);
                    }
                }
                BikeNetworkInfo selectedNetwork = (BikeNetworkInfo) listView.getItemAtPosition(position);
                if(listView.isItemChecked(position)) {
                    networksToKeepList.add(selectedNetwork);
                    Toast.makeText(BikeNetworksListActivity.this,
                            selectedNetwork.getName()
                                    + " ("
                                    + selectedNetwork.getLocation().getCity()
                                    + ") " + getString(R.string.network_selected),
                            Toast.LENGTH_SHORT).show();
                } else {
                    networksToKeepList.remove(selectedNetwork);
                }

                if (getParent() == null) {
                    setResult(Activity.RESULT_OK);
                } else {
                    getParent().setResult(Activity.RESULT_OK);
                }
                networksDataSource.storeNetworks(networksToKeepList);
                finish();
            }
        });

        String apiUrl = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString(PREF_KEY_API_URL, DEFAULT_API_URL) + "networks";
        new JSONDownloadTask().execute(apiUrl);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bike_networks_list, menu);

        SearchManager manager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(manager.getSearchableInfo(getComponentName()));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                RefreshAdapter(s);
                return true;
            }
        });

        return true;
    }

    private String normalize(String str) {
        return Normalizer.normalize(str, Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+|\\s|'|-|_", "").toLowerCase();
    }

    private void RefreshAdapter(String textCondition) {
        if(BikeNetworksHashMap == null) {
            BikeNetworksHashMap = new HashMap<String, BikeNetworkInfo>();
        }
        ArrayList<BikeNetworkInfo> filteredBikeNetworks = new ArrayList<>();
        int networksToKeepNb = 0;
        for (BikeNetworkInfo network : BikeNetworksHashMap.values()) {
            if (textCondition == null || normalize(network.getLocationName()).contains(normalize(textCondition))
                    || normalize(network.getName()).contains(normalize(textCondition))) {
                filteredBikeNetworks.add(network);
                if (savedNetworksList.contains(network.getId())) {
                    Collections.swap(filteredBikeNetworks, filteredBikeNetworks.indexOf(network), networksToKeepNb);
                    networksToKeepNb++;
                }
            }
        }

        Collections.sort(filteredBikeNetworks.subList(0, networksToKeepNb), mLocationComparator);
        Collections.sort(filteredBikeNetworks.subList(networksToKeepNb, filteredBikeNetworks.size()), mLocationComparator);
        bikeNetworksListAdapter = new BikeNetworksListAdapter(BikeNetworksListActivity.this,
                R.layout.bike_network_item,
                R.id.network_title,
                filteredBikeNetworks,
                cannotFetchNetworksList);
        listView.setAdapter(bikeNetworksListAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class JSONDownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            try {
                StringBuilder response = new StringBuilder();

                URL url = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String strLine;
                    while ((strLine = input.readLine()) != null) {
                        response.append(strLine);
                    }
                    input.close();
                }
                return response.toString();
            } catch (IOException e) {
                return getString(R.string.connection_error);
            }
        }

        @Override
        protected void onPostExecute(final String result) {
            try {
                /* parse result */
                BikeNetworksListParser bikeNetworksListParser = new BikeNetworksListParser(result);
                ArrayList<BikeNetworkInfo> bikeNetworks = bikeNetworksListParser.getNetworks();
                for(int i = 0; i < bikeNetworks.size(); i++) {
                    BikeNetworksHashMap.put(bikeNetworks.get(i).getId(), bikeNetworks.get(i));
                }
            } catch (ParseException e) {
                Log.e(TAG, e.getMessage());
                Toast.makeText(BikeNetworksListActivity.this,
                        R.string.json_error, Toast.LENGTH_LONG).show();
                BikeNetworksHashMap = new HashMap<String, BikeNetworkInfo>();
            }
             /* Take into accounts saved networks: if they are missing
              * in the fetch data, add them at the queue.
              */
            for (String network : savedNetworksList) {
                if (!BikeNetworksHashMap.containsKey(network)) {
                    cannotFetchNetworksList.add(network);
                    BikeNetworksHashMap.put(network, networksDataSource.getNetworkInfoFromId(network));
                }
            }
            RefreshAdapter(null);
        }
    }
}
