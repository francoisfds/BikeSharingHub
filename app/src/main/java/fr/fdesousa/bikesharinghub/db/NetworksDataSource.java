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

package fr.fdesousa.bikesharinghub.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;

import fr.fdesousa.bikesharinghub.models.BikeNetworkInfo;
import fr.fdesousa.bikesharinghub.models.BikeNetworkLocation;

public class NetworksDataSource {
    private DatabaseHelper dbHelper;

    private static final String QUERY_NETWORK_INFO_LIST = "SELECT *"
                + " FROM " + DatabaseHelper.NETWORKS_TABLE_NAME;
    private static final String QUERY_NETWORK_INFO_BY_ID = "SELECT *"
                + " FROM " + DatabaseHelper.NETWORKS_TABLE_NAME
                + " WHERE " + DatabaseHelper.NETWORKS_COLUMN_ID + " = ";
    private static final String QUERY_NETWORK_ID_LIST = "SELECT "
                + DatabaseHelper.NETWORKS_COLUMN_ID
                + " FROM " + DatabaseHelper.NETWORKS_TABLE_NAME;
    private static final String QUERY_COLOR_BY_ID_LIST = "SELECT "
                + DatabaseHelper.NETWORKS_COLUMN_ID + ", " + DatabaseHelper.NETWORKS_COLUMN_COLOR
                + " FROM " + DatabaseHelper.NETWORKS_TABLE_NAME;

    public NetworksDataSource(Context context) {
        dbHelper = DatabaseHelper.getInstance(context);
    }

    public void storeNetworks(ArrayList<BikeNetworkInfo> bikeNetworks) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            clearNetworks();
            for (BikeNetworkInfo bikeNetwork : bikeNetworks) {
                ContentValues values = new ContentValues();
                BikeNetworkLocation location = bikeNetwork.getLocation();
                values.put(DatabaseHelper.NETWORKS_COLUMN_ID, bikeNetwork.getId());
                values.put(DatabaseHelper.NETWORKS_COLUMN_NAME, bikeNetwork.getName());
                values.put(DatabaseHelper.NETWORKS_COLUMN_COMPANY, bikeNetwork.getCompany());
                values.put(DatabaseHelper.NETWORKS_COLUMN_LATITUDE,
                        Double.doubleToRawLongBits(location.getLatitude()));
                values.put(DatabaseHelper.NETWORKS_COLUMN_LONGITUDE,
                        Double.doubleToRawLongBits(location.getLongitude()));
                values.put(DatabaseHelper.NETWORKS_COLUMN_CITY, location.getCity());
                values.put(DatabaseHelper.NETWORKS_COLUMN_COUNTRY, location.getCountry());
                if (bikeNetwork.getColor() != null) {
                    values.put(DatabaseHelper.NETWORKS_COLUMN_COLOR, bikeNetwork.getColor());
                }
                db.insert(DatabaseHelper.NETWORKS_TABLE_NAME, null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void clearNetworks() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.NETWORKS_TABLE_NAME, null, null);
    }

    public ArrayList<String> getNetworksId() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<String> networksId = new ArrayList<String>();
        Cursor cursor = db.rawQuery(QUERY_NETWORK_ID_LIST, null);

        try {
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    networksId.add(cursor.getString(0));
                    cursor.moveToNext();
                }
            }
            return networksId;
        } finally {
            cursor.close();
        }
    }

    public BikeNetworkInfo getNetworkInfoFromId(String networkId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(QUERY_NETWORK_INFO_BY_ID + "'" + networkId + "'", null);

        try {
            if (cursor.moveToFirst()) {
                BikeNetworkLocation foundLocation = new BikeNetworkLocation(
                    Double.longBitsToDouble(cursor.getLong(3)), Double.longBitsToDouble(cursor.getLong(4)),
                    cursor.getString(5), cursor.getString(6));
                BikeNetworkInfo foudNetworkInfo = new BikeNetworkInfo(
                    cursor.getString(0), cursor.getString(1),
                    cursor.getString(2), foundLocation);
                return foudNetworkInfo;
            } else {
                return null;
            }
        } finally {
            cursor.close();
        }
    }

    public ArrayList<BikeNetworkInfo> getNetworkInfoList() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ArrayList<BikeNetworkInfo> networkInfoList = new ArrayList<>();
        Cursor cursor = db.rawQuery(QUERY_NETWORK_INFO_LIST, null);

        try {
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    BikeNetworkLocation currentLocation = new BikeNetworkLocation(
                        Double.longBitsToDouble(cursor.getLong(3)), Double.longBitsToDouble(cursor.getLong(4)),
                        cursor.getString(5), cursor.getString(6));
                    BikeNetworkInfo currentNetwork = new BikeNetworkInfo(
                        cursor.getString(0), cursor.getString(1),
                        cursor.getString(2), currentLocation);
                    networkInfoList.add(currentNetwork);
                    cursor.moveToNext();
                }
            }
            return networkInfoList;
        } finally {
            cursor.close();
        }
    }

    public HashMap getNetworksColor() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        HashMap<String, String> colorMap = new HashMap<String, String>();
        Cursor cursor = db.rawQuery(QUERY_COLOR_BY_ID_LIST, null);

        try {
            if (cursor.moveToFirst()) {
                while (!cursor.isAfterLast()) {
                    colorMap.put(cursor.getString(0), cursor.getString(1));
                    cursor.moveToNext();
                }
            }
            return colorMap;
        } finally {
            cursor.close();
        }
    }

}
