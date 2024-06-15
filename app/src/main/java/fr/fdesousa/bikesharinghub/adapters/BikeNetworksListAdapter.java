/*
 * Copyright (c) 2014-2015 Bruno Parmentier.
 * Copyright (c) 2021-2022,2024 François FERREIRA DE SOUSA.
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

package fr.fdesousa.bikesharinghub.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import fr.fdesousa.bikesharinghub.db.NetworksDataSource;
import fr.fdesousa.bikesharinghub.models.BikeNetworkInfo;

import fr.fdesousa.bikesharinghub.R;

/**
 * Define a list of bike networks.
 */
public class BikeNetworksListAdapter extends ArrayAdapter<BikeNetworkInfo> {

    private ArrayList<String> savedNetworksList;
    private ArrayList<String> cannotFetchNetworksList;

    public BikeNetworksListAdapter(Context context, int resource, int textViewResourceId,
        ArrayList<BikeNetworkInfo> networks, ArrayList<String> cannotFetch) {
        super(context, resource, textViewResourceId, networks);

        NetworksDataSource networksDataSource = new NetworksDataSource(context);
        savedNetworksList = networksDataSource.getNetworksId();
        cannotFetchNetworksList = cannotFetch;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.bike_network_item, parent, false);
        }

        BikeNetworkInfo network = getItem(position);

        if (network != null) {
            TextView network_title = (TextView) v.findViewById(R.id.network_title);

            network_title.setText(network.getLocationName() + "\n" + network.getName());
            if (savedNetworksList.contains(network.getId())) {
                ((ListView)parent).setItemChecked(position, true);
            }
            if (cannotFetchNetworksList != null &&
                cannotFetchNetworksList.contains(network.getId())) {
                // put unreachable networks in greyscale
                network_title.setTextColor(Color.GRAY);
                network_title.setTypeface(null, Typeface.ITALIC);
            } else {
                network_title.setTextColor(Color.BLACK);
                network_title.setTypeface(null, Typeface.NORMAL);
            }
        }

        return v;
    }
}
