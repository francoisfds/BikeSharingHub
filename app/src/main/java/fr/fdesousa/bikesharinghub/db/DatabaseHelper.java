/*
 * Copyright (c) 2015 Bruno Parmentier.
 * Copyright (c) 2020, 2022 François FERREIRA DE SOUSA.
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

package fr.fdesousa.bikesharinghub.db;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static DatabaseHelper instance;
    private Context mContext;

    private static final String DB_NAME = "openbikesharing.sqlite";
    private static final int DB_VERSION = 3;

    public static final String STATIONS_TABLE_NAME = "stations";
    public static final String STATIONS_COLUMN_ID = "id";
    public static final String STATIONS_COLUMN_NAME = "name";
    public static final String STATIONS_COLUMN_LAST_UPDATE = "last_update";
    public static final String STATIONS_COLUMN_LATITUDE = "latitude";
    public static final String STATIONS_COLUMN_LONGITUDE = "longitude";
    public static final String STATIONS_COLUMN_FREE_BIKES = "free_bikes";
    public static final String STATIONS_COLUMN_EMPTY_SLOTS = "empty_slots";
    public static final String STATIONS_COLUMN_ADDRESS = "address";
    public static final String STATIONS_COLUMN_BANKING = "banking";
    public static final String STATIONS_COLUMN_BONUS = "bonus";
    public static final String STATIONS_COLUMN_STATUS = "status";
    public static final String STATIONS_COLUMN_EBIKES = "ebikes";
    public static final String STATIONS_COLUMN_NETWORK = "network_id";

    public static final String FAV_STATIONS_TABLE_NAME = "fav_stations";
    public static final String FAV_STATIONS_COLUMN_ID = "id";

    public static final String NETWORKS_TABLE_NAME = "networks";
    public static final String NETWORKS_COLUMN_ID = "id";
    public static final String NETWORKS_COLUMN_NAME = "name";
    public static final String NETWORKS_COLUMN_COMPANY = "compagny";
    public static final String NETWORKS_COLUMN_LATITUDE = "latitude";
    public static final String NETWORKS_COLUMN_LONGITUDE = "longitude";
    public static final String NETWORKS_COLUMN_CITY = "city";
    public static final String NETWORKS_COLUMN_COUNTRY = "country";
    public static final String NETWORKS_COLUMN_COLOR = "color";

    private static final String PREF_KEY_NETWORK_ID = "network-id";

    public static DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE " + STATIONS_TABLE_NAME + "("
                + STATIONS_COLUMN_ID + " TEXT PRIMARY KEY,"
                + STATIONS_COLUMN_NAME + " TEXT NOT NULL,"
                + STATIONS_COLUMN_LAST_UPDATE + " TEXT NOT NULL,"
                + STATIONS_COLUMN_LATITUDE + " NUMERIC NOT NULL,"
                + STATIONS_COLUMN_LONGITUDE + " NUMERIC NOT NULL,"
                + STATIONS_COLUMN_FREE_BIKES + " INTEGER NOT NULL,"
                + STATIONS_COLUMN_EMPTY_SLOTS + " INTEGER NOT NULL,"
                + STATIONS_COLUMN_ADDRESS + " TEXT,"
                + STATIONS_COLUMN_BANKING + " INTEGER,"
                + STATIONS_COLUMN_BONUS + " INTEGER,"
                + STATIONS_COLUMN_STATUS + " TEXT,"
                + STATIONS_COLUMN_EBIKES + " INTEGER, \""
                + STATIONS_COLUMN_NETWORK + "\" TEXT)"
        );
        db.execSQL("CREATE TABLE " + FAV_STATIONS_TABLE_NAME + "("
                + FAV_STATIONS_COLUMN_ID + " TEXT PRIMARY KEY)"
        );
        db.execSQL("CREATE TABLE " + NETWORKS_TABLE_NAME + "("
                + NETWORKS_COLUMN_ID + " TEXT PRIMARY KEY,"
                + NETWORKS_COLUMN_NAME + " TEXT NOT NULL,"
                + NETWORKS_COLUMN_COMPANY + " TEXT NOT NULL,"
                + NETWORKS_COLUMN_LATITUDE + " NUMERIC NOT NULL,"
                + NETWORKS_COLUMN_LONGITUDE + " NUMERIC NOT NULL,"
                + NETWORKS_COLUMN_CITY + " TEXT NOT NULL,"
                + NETWORKS_COLUMN_COUNTRY + " TEXT NOT NULL,"
                + NETWORKS_COLUMN_COLOR + " TEXT)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        if (oldVersion < 2) {
             db.execSQL("ALTER TABLE " + STATIONS_TABLE_NAME + " ADD COLUMN ebikes INTEGER;");
        }
        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE " + NETWORKS_TABLE_NAME + "("
                    + NETWORKS_COLUMN_ID + " TEXT PRIMARY KEY,"
                    + NETWORKS_COLUMN_NAME + " TEXT NOT NULL,"
                    + NETWORKS_COLUMN_COMPANY + " TEXT NOT NULL,"
                    + NETWORKS_COLUMN_LATITUDE + " NUMERIC NOT NULL,"
                    + NETWORKS_COLUMN_LONGITUDE + " NUMERIC NOT NULL,"
                    + NETWORKS_COLUMN_CITY + " TEXT NOT NULL,"
                    + NETWORKS_COLUMN_COUNTRY + " TEXT NOT NULL,"
                    + NETWORKS_COLUMN_COLOR + " TEXT)"
            );
            db.execSQL("ALTER TABLE " + STATIONS_TABLE_NAME + " ADD COLUMN \""
                    + STATIONS_COLUMN_NETWORK + "\" TEXT;");
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
            if(settings.contains(PREF_KEY_NETWORK_ID)) {
                String id = settings.getString(PREF_KEY_NETWORK_ID, "");

                //Transfer current network-id in the 'stations' table
                db.execSQL("UPDATE " + STATIONS_TABLE_NAME + " SET '"
                    + STATIONS_COLUMN_NETWORK + "' = '" + id + "'");
            }
        }
    }

}
