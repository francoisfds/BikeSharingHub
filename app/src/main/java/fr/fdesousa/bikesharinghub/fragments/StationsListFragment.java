/*
 * Copyright (c) 2014-2015 Bruno Parmentier.
 * Copyright (c) 2021-2024 François FERREIRA DE SOUSA.
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

package fr.fdesousa.bikesharinghub.fragments;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import fr.fdesousa.bikesharinghub.models.Station;

import fr.fdesousa.bikesharinghub.R;
import fr.fdesousa.bikesharinghub.activities.StationActivity;
import fr.fdesousa.bikesharinghub.adapters.StationsListAdapter;
import fr.fdesousa.bikesharinghub.models.StationsListViewModel;
import fr.fdesousa.bikesharinghub.models.StationsListViewModelFactory;

public class StationsListFragment extends Fragment implements ViewModelStoreOwner {
    private static final String KEY_STATION = "station";
    private static final String KEY_EMPTY_LIST_TEXT = "empty_list_text_key";
    private static final String KEY_FRAGMENT_ID = "fragment_id_key";
    public static final int FRAGMENT_NEARBY = 1;
    public static final int FRAGMENT_FAVORITES = 2;
    public static final int FRAGMENT_ALL = 3;

    private ArrayList<Station> stations;
    private StationsListAdapter stationsListAdapter;
    private String emptyViewContent;
    private TextView emptyView;

    /* newInstance constructor for creating fragment with arguments */
    public static StationsListFragment newInstance(int fragmentId, String emptyListText) {
        StationsListFragment stationsListFragment = new StationsListFragment();
        Bundle args = new Bundle();
        args.putString(KEY_EMPTY_LIST_TEXT, emptyListText);
        args.putInt(KEY_FRAGMENT_ID, fragmentId);
        stationsListFragment.setArguments(args);
        return stationsListFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            emptyViewContent = savedInstanceState.getString(KEY_EMPTY_LIST_TEXT);
        } else {
            emptyViewContent = getArguments().getString(KEY_EMPTY_LIST_TEXT);
        }
        StationsListViewModelFactory factory = new StationsListViewModelFactory(getActivity().getApplication());
        StationsListViewModel model = new ViewModelProvider(this, factory).get(StationsListViewModel.class);
        switch(getArguments().getInt(KEY_FRAGMENT_ID)) {
            case 1:
                stations = new ArrayList<Station>();
                break;
            case 2:
                stations = model.getFavoriteStations();
                break;
            case 3:
                stations = model.getStations();
                break;
        }
        stationsListAdapter = new StationsListAdapter(getActivity(),
                R.layout.station_list_item, stations);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stations_list, container, false);
        ListView listView = (ListView) view.findViewById(R.id.stationsListView);
        listView.setAdapter(stationsListAdapter);
        emptyView = (TextView) view.findViewById(R.id.emptyList);
        emptyView.setText(emptyViewContent);
        listView.setEmptyView(view.findViewById(R.id.emptyList));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Intent intent = new Intent(getActivity(), StationActivity.class);
                intent.putExtra(KEY_STATION, stations.get(position));
                startActivity(intent);
            }
        });
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                SwipeRefreshLayout refreshLayout = (SwipeRefreshLayout) getActivity().findViewById(R.id.swipe_container);
                if(firstVisibleItem == 0) refreshLayout.setEnabled(true);
                else refreshLayout.setEnabled(false);
            }
        });
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_EMPTY_LIST_TEXT, emptyView.getText().toString());
    }


    public void updateStationsList(ArrayList<Station> stations) {
        if (stationsListAdapter != null) {
            stationsListAdapter.clear();
            stationsListAdapter.addAll(stations);
            stationsListAdapter.notifyDataSetChanged();
        }
    }

    public void setEmptyView(int id) {
        if(emptyView != null) {
            emptyView.setText(id);
        }
   }

}
