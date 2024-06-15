/*
 * Copyright (c) 2014-2015 Bruno Parmentier.
 * Copyright (c) 2020,2022,2024 François FERREIRA DE SOUSA.
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
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import fr.fdesousa.bikesharinghub.db.NetworksDataSource;
import fr.fdesousa.bikesharinghub.models.Station;
import fr.fdesousa.bikesharinghub.models.StationStatus;

import fr.fdesousa.bikesharinghub.R;

/**
 * Define a list of stations with their title, number of bikes and empty slots.
 */
public class StationsListAdapter extends ArrayAdapter<Station> {

    private HashMap<String, String> colorMap;

    public StationsListAdapter(Context context, int resource, ArrayList<Station> stations) {
        super(context, resource, stations);
        NetworksDataSource networksDataSource = new NetworksDataSource(context);
        colorMap = networksDataSource.getNetworksColor();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.station_list_item, parent, false);
        }

        final Station station = getItem(position);

        if (station != null) {
            TextView stationNameTitle = (TextView) v.findViewById(R.id.stationNameTitle);
            TextView freeBikesValue = (TextView) v.findViewById(R.id.freeBikesValue);
            ImageView regularBikesLogo = (ImageView) v.findViewById(R.id.freeBikesLogo);
            TextView freeEBikesValue = (TextView) v.findViewById(R.id.freeEBikesValue);
            ImageView eBikesLogo = (ImageView) v.findViewById(R.id.freeEBikesLogo);
            TextView emptySlotsValue = (TextView) v.findViewById(R.id.emptySlotsValue);
            StationStatus stationStatus = station.getStatus();

            if (stationNameTitle != null) {
                stationNameTitle.setText(station.getName());

                if (stationStatus == StationStatus.CLOSED) {
                    stationNameTitle.setPaintFlags(stationNameTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    stationNameTitle.setPaintFlags(stationNameTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                }
            }

            if (freeBikesValue != null) {
                int bikes = station.getFreeBikes();
                if(station.getEBikes() != null && regularBikesLogo != null
                && eBikesLogo != null && freeEBikesValue != null ) {
                    int ebikes = station.getEBikes();
                    freeBikesValue.setText(String.valueOf(bikes-ebikes));
                    regularBikesLogo.setImageResource(R.drawable.ic_regular_bike);
                    eBikesLogo.setVisibility(View.VISIBLE);
                    freeEBikesValue.setVisibility(View.VISIBLE);
                    freeEBikesValue.setText(String.valueOf(ebikes));
                } else {
                    freeBikesValue.setText(String.valueOf(bikes));
                    regularBikesLogo.setImageResource(R.drawable.ic_bike);
                    eBikesLogo.setVisibility(View.GONE);
                    freeEBikesValue.setVisibility(View.GONE);
                }
            }

            if (emptySlotsValue != null) {
                int emptySlots = station.getEmptySlots();
                ImageView emptySlotsLogo = (ImageView) v.findViewById(R.id.emptySlotsLogo);
                if (emptySlots == -1) {
                    emptySlotsLogo.setVisibility(View.GONE);
                    emptySlotsValue.setVisibility(View.GONE);
                } else {
                    emptySlotsLogo.setVisibility(View.VISIBLE);
                    emptySlotsValue.setVisibility(View.VISIBLE);
                    emptySlotsValue.setText(String.valueOf(emptySlots));
                }
            }

            //Use the color's network as background ; or white if null
            String colorNetwork = colorMap.get(station.getNetworkId());
            if(colorNetwork == null) {
                v.setBackgroundColor(Color.WHITE);
            } else {
                v.setBackgroundColor(Color.parseColor(colorNetwork));
            }
        }

        return v;
    }
}
