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
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.cachemanager.CacheManager;
import org.osmdroid.tileprovider.modules.SqlTileWriter;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.TileSourcePolicyException;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.LocationUtils;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.gestures.OneFingerZoomOverlay;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;

import fr.fdesousa.bikesharinghub.db.NetworksDataSource;
import fr.fdesousa.bikesharinghub.db.StationsDataSource;
import fr.fdesousa.bikesharinghub.models.BikeNetworkLocation;
import fr.fdesousa.bikesharinghub.models.Station;
import fr.fdesousa.bikesharinghub.models.StationStatus;
import fr.fdesousa.bikesharinghub.R;
import fr.fdesousa.bikesharinghub.tilesource.CustomTileSource;

public class MapActivity extends Activity implements MapEventsReceiver, ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = "MapActivity";
    private static final String MAP_CURRENT_ZOOM_KEY = "map-current-zoom";
    private static final String MAP_CENTER_LAT_KEY = "map-center-lat";
    private static final String MAP_CENTER_LON_KEY = "map-center-lon";

    private static final String PREF_KEY_MAP_LAYER = "pref_map_layer";
    private static final String PREF_KEY_MAP_CACHE_MAX_SIZE = "pref_map_tiles_cache_max_size";
    private static final String PREF_KEY_MAP_CACHE_TRIM_SIZE = "pref_map_tiles_cache_trim_size";
    private static final String PREF_KEY_DB_LAST_UPDATE = "db_last_update";
    private static final String KEY_STATION = "station";
    private static final String MAP_LAYER_MAPNIK = "mapnik";
    private static final String MAP_LAYER_CYCLEMAP = "cyclemap";
    private static final String MAP_LAYER_OSMPUBLICTRANSPORT = "osmpublictransport";

    private static final String[] REQUEST_LOC_LIST = {
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    };
    private static final int REQUEST_LOC_PERMISSION_CODE = 1;

    private MapView map;
    private IMapController mapController;
    private MyLocationNewOverlay myLocationOverlay;
    private StationMarkerInfoWindow stationMarkerInfoWindow;
    private NetworksDataSource networksDataSource;
    private StationsDataSource stationsDataSource;
    private ScrollView stationDetailsView;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(MAP_CURRENT_ZOOM_KEY, map.getZoomLevel());
        outState.putDouble(MAP_CENTER_LAT_KEY, map.getMapCenter().getLatitude());
        outState.putDouble(MAP_CENTER_LON_KEY, map.getMapCenter().getLongitude());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        setDBLastUpdateText(settings);

        stationsDataSource = new StationsDataSource(this);
        networksDataSource = new NetworksDataSource(this);
        ArrayList<Station> stations = stationsDataSource.getStations();

        final Context context = getApplicationContext();
        long systemCacheMaxBytes = 1024 * 1024 * Long.valueOf(settings.getString(PREF_KEY_MAP_CACHE_MAX_SIZE, "100"));
        long systemCacheTrimBytes = 1024 * 1024 * Long.valueOf(settings.getString(PREF_KEY_MAP_CACHE_TRIM_SIZE, "100"));
        Configuration.getInstance().setTileFileSystemCacheMaxBytes(systemCacheMaxBytes);
        Configuration.getInstance().setTileFileSystemCacheTrimBytes(systemCacheTrimBytes);
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));

        map = (MapView) findViewById(R.id.mapView);
        stationMarkerInfoWindow = new StationMarkerInfoWindow(R.layout.bonuspack_bubble, map);

        stationDetailsView = findViewById(R.id.scrollView);

        /* handling map events */
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(this, this);
        map.getOverlays().add(0, mapEventsOverlay);

        /* markers list */
        RadiusMarkerClusterer stationsMarkers = new RadiusMarkerClusterer(this);
        Bitmap clusterIcon = getBitmapFromVectorDrawable(this, R.drawable.marker_cluster);
        map.getOverlays().add(stationsMarkers);
        stationsMarkers.setIcon(clusterIcon);
        stationsMarkers.setRadius(100);

        for (final Station station : stations) {
            stationsMarkers.add(createStationMarker(station));
        }
        map.invalidate();

        map.getOverlays().add(new CopyrightOverlay(context));
        map.getOverlays().add(new OneFingerZoomOverlay());
        map.setTilesScaledToDpi(true);
        map.setMultiTouchControls(true);
        map.setMinZoomLevel(Double.valueOf(3));

        /* map tile source */
        String mapLayer = settings.getString(PREF_KEY_MAP_LAYER, "");
        switch (mapLayer) {
            case MAP_LAYER_MAPNIK:
                map.setTileSource(TileSourceFactory.MAPNIK);
                break;
            case MAP_LAYER_CYCLEMAP:
                map.setTileSource(CustomTileSource.CYCLOSM);
                break;
            case MAP_LAYER_OSMPUBLICTRANSPORT:
                map.setTileSource(CustomTileSource.OPNVKARTE);
                break;
            default:
                map.setTileSource(CustomTileSource.getDefaultTileSource());
                break;
        }

        GpsMyLocationProvider imlp = new GpsMyLocationProvider(this.getBaseContext());
        imlp.setLocationUpdateMinDistance(1000);
        imlp.setLocationUpdateMinTime(60000);
        map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        myLocationOverlay = new MyLocationNewOverlay(imlp, this.map);
        myLocationOverlay.enableMyLocation();
        map.getOverlays().add(this.myLocationOverlay);

        mapController = map.getController();
        if (savedInstanceState != null) {
            mapController.setZoom(savedInstanceState.getInt(MAP_CURRENT_ZOOM_KEY));
            mapController.setCenter(new GeoPoint(savedInstanceState.getDouble(MAP_CENTER_LAT_KEY),
                    savedInstanceState.getDouble(MAP_CENTER_LON_KEY)));
        } else {
            Location userLocation = null;
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {
                LocationManager locationManager =
                        (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
                userLocation = LocationUtils.getLastKnownLocation(locationManager);
            }
            if (userLocation != null) {
                mapController.setZoom(16);
                mapController.animateTo(new GeoPoint(userLocation));
            } else if (networksDataSource.getNetworkInfoList().size() != 0) {
                //Arbitrary use the first location of the list
                BikeNetworkLocation currentNetworkLocation = networksDataSource.getNetworkInfoList().get(0).getLocation();
                double bikeNetworkLatitude = currentNetworkLocation.getLatitude();
                double bikeNetworkLongitude = currentNetworkLocation.getLongitude();
                mapController.setZoom(13);
                mapController.setCenter(new GeoPoint(bikeNetworkLatitude, bikeNetworkLongitude));
            }
        }

        try {
            CacheManager mCacheManager = new CacheManager(map);
            long cacheUsed = mCacheManager.currentCacheUsage();

            // If map cache is too big, launch cleaning in another thread because it may take a lot of time
            if(cacheUsed > Configuration.getInstance().getTileFileSystemCacheMaxBytes()) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        MapActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MapActivity.this,
                                    getString(R.string.map_cache_cleaning_started),
                                    Toast.LENGTH_LONG).show();
                            }
                        });
                        SqlTileWriter sqlTileWriter = new SqlTileWriter();
                        sqlTileWriter.runCleanupOperation();
                        sqlTileWriter.onDetach();
                        MapActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MapActivity.this,
                                    getString(R.string.map_cache_cleaning_done),
                                    Toast.LENGTH_SHORT).show();
                            }
                        });
                        Log.d(TAG, "Map cache has been cleaned");
                    }
                }).start();
            }
        } catch (TileSourcePolicyException e) {
            Log.e(TAG, "Enable to access cache manager, map cache could not be cleaned.");
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        myLocationOverlay.disableMyLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        myLocationOverlay.enableMyLocation();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_my_location:
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
                    mooveToLocation();
                } else {
                    ActivityCompat.requestPermissions(this,
                            REQUEST_LOC_LIST, REQUEST_LOC_PERMISSION_CODE);
                }
                return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean singleTapConfirmedHelper(GeoPoint geoPoint) {
        InfoWindow.closeAllInfoWindowsOn(map);
        return true;
    }

    @Override
    public boolean longPressHelper(GeoPoint geoPoint) {
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOC_PERMISSION_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (grantResults[1] == PackageManager.PERMISSION_DENIED) {
                        Toast.makeText(this, getString(R.string.location_coarse_only), Toast.LENGTH_SHORT).show();
                    }
                    mooveToLocation();
                } else if(!ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    Toast.makeText(this, getString(R.string.location_not_granted), Toast.LENGTH_LONG).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void mooveToLocation() {
        try {
            LocationManager locationManager =
                    (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            GeoPoint userLocation = new GeoPoint(LocationUtils.getLastKnownLocation(locationManager));
            mapController.animateTo(userLocation);
        } catch (NullPointerException ex) {
            Toast.makeText(this, getString(R.string.location_not_found), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Location not found");
        }
    }

    private Marker createStationMarker(Station station) {
        GeoPoint stationLocation = new GeoPoint((int) (station.getLatitude() * 1000000),
                (int) (station.getLongitude() * 1000000));
        Marker marker = new Marker(map);
        marker.setRelatedObject(station);
        marker.setInfoWindow(stationMarkerInfoWindow);
        marker.setPosition(stationLocation);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        marker.setTitle(station.getName());
        marker.setSnippet(String.valueOf(station.getFreeBikes())); // free bikes
        if (station.getEmptySlots() != -1) {
            marker.setSubDescription(String.valueOf(station.getEmptySlots())); // empty slots
        }

        /* Marker icon */
        int emptySlots = station.getEmptySlots();
        int freeBikes = station.getFreeBikes();

        Context mContext = this.getApplicationContext();
        Drawable drawable = ContextCompat.getDrawable(mContext, R.drawable.ic_station_marker);
        float freeBikesRatio = (float) freeBikes / (float) (freeBikes + emptySlots);
        float mTextAnchorU = Marker.ANCHOR_CENTER, mTextAnchorV = Marker.ANCHOR_CENTER;

        Bitmap finalIcon = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(finalIcon);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        Paint firstPaint = new Paint();

        if(freeBikes == 0 || station.getStatus() == StationStatus.CLOSED) {
            firstPaint.setColor(Color.WHITE);
            canvas.drawCircle(mTextAnchorU * finalIcon.getWidth(), mTextAnchorV * finalIcon.getHeight(), finalIcon.getHeight()/2f, firstPaint);
        } else {
            float sweepAngle;
            if(freeBikesRatio < 1.0) {
                sweepAngle = 360f * freeBikesRatio;
            } else {
                sweepAngle = 362f; //need to overwrite 1 degre taken for each edge
            }
            RectF oval = new RectF(0, 0, finalIcon.getWidth(), finalIcon.getHeight());
            RectF ovalInt = new RectF(0.1f * finalIcon.getWidth(), 0.1f * finalIcon.getHeight(), finalIcon.getWidth() * 0.9f, finalIcon.getHeight() * 0.9f);
            firstPaint.setColor(getResources().getColor(R.color.bike_red));
            //fill in the gauge
            canvas.drawArc(oval, -88, sweepAngle - 3, true, firstPaint);
            firstPaint.setColor(Color.BLACK);
            //inner contour of the gauge
            canvas.drawArc(ovalInt, -88, sweepAngle - 3, true, firstPaint);
            if (emptySlots > 0) {
                //edges of jauge
                canvas.drawArc(oval, -91, 2, true, firstPaint);
                canvas.drawArc(oval, -90 + sweepAngle - 2, 3, true, firstPaint);
                //fill what's left of the gauge
                firstPaint.setColor(Color.WHITE);
                canvas.drawArc(oval, -90 + sweepAngle + 1, 360 - sweepAngle - 2, true, firstPaint);
            }
            //inside circle
            firstPaint.setColor(Color.WHITE);
            canvas.drawCircle(mTextAnchorU * finalIcon.getWidth(), mTextAnchorV * finalIcon.getHeight(), finalIcon.getHeight() / 2.65f, firstPaint);
        }

        if ((emptySlots == 0 && freeBikes == 0) || station.getStatus() == StationStatus.CLOSED) {
            firstPaint.setColor(Color.BLACK);
            firstPaint.setStrokeWidth(4);
            canvas.drawLine(0.15f*finalIcon.getWidth(), 0.15f*finalIcon.getHeight(), 0.85f*finalIcon.getWidth(), 0.85f*finalIcon.getHeight(), firstPaint);
            canvas.drawLine(0.15f*finalIcon.getWidth(), 0.85f*finalIcon.getHeight(), 0.85f*finalIcon.getWidth(), 0.15f*finalIcon.getHeight(), firstPaint);
        } else {
            Paint mTextPaint = new Paint();
            mTextPaint.setColor(Color.BLACK);
            mTextPaint.setTextSize(15 * getResources().getDisplayMetrics().density);
            mTextPaint.setFakeBoldText(true);
            mTextPaint.setTextAlign(Paint.Align.CENTER);
            mTextPaint.setAntiAlias(true);
            String text = String.valueOf(freeBikes);
            int textHeight = (int) (mTextPaint.descent() + mTextPaint.ascent());
            canvas.drawText(text,
                    mTextAnchorU * finalIcon.getWidth(),
                    mTextAnchorV * finalIcon.getHeight() - textHeight / 2,
                    mTextPaint);
        }
        drawable.draw(canvas);
        marker.setIcon(new BitmapDrawable(getResources(), finalIcon));
        return marker;
    }

    private class StationMarkerInfoWindow extends MarkerInfoWindow {

        public StationMarkerInfoWindow(int layoutResId, final MapView mapView) {
            super(layoutResId, mapView);
        }

        @Override
        public void onClose() {
            stationDetailsView.setVisibility(View.GONE);
            super.onClose();
        }

        @Override
        public void onOpen(Object item) {
            Marker marker = (Marker) item;
            final Station markerStation = (Station) marker.getRelatedObject();
            super.onOpen(item);
            closeAllInfoWindowsOn(map);

            LinearLayout layout = (LinearLayout) getView().findViewById(R.id.map_bubble_layout);
            if (markerStation.getEmptySlots() == -1) {
                ImageView emptySlotsLogo = (ImageView) getView().findViewById(R.id.bubble_emptyslots_logo);
                emptySlotsLogo.setVisibility(View.GONE);
            }

            ImageView regularBikesLogo = (ImageView) getView().findViewById(R.id.bubble_freebikes_logo);
            TextView regularBikesValue = (TextView) getView().findViewById(R.id.bubble_description);
            ImageView eBikesLogo = (ImageView) getView().findViewById(R.id.bubble_ebikes_logo);
            TextView eBikesValue = (TextView) getView().findViewById(R.id.bubble_ebikes_value);

            int bikes = markerStation.getFreeBikes();
            if (markerStation.getEBikes() != null && regularBikesLogo != null
                    && eBikesLogo != null && regularBikesValue != null && eBikesValue != null) {
                int ebikes = markerStation.getEBikes();
                regularBikesValue.setText(String.valueOf(bikes - ebikes));
                regularBikesLogo.setImageResource(R.drawable.ic_regular_bike);
                eBikesLogo.setVisibility(View.VISIBLE);
                eBikesValue.setVisibility(View.VISIBLE);
                eBikesValue.setText(String.valueOf(ebikes));
            } else {
                regularBikesLogo.setImageResource(R.drawable.ic_bike);
                eBikesLogo.setVisibility(View.GONE);
                eBikesValue.setVisibility(View.GONE);
            }

            layout.setClickable(true);
            layout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(MapActivity.this, StationActivity.class);
                    intent.putExtra(KEY_STATION, markerStation);
                    startActivity(intent);
                }
            });

            //StationsDetails :
            TextView stationName = (TextView) findViewById(R.id.stationName);
            TextView stationEmptySlots = (TextView) findViewById(R.id.stationEmptySlots);
            TextView stationFreeBikes = (TextView) findViewById(R.id.stationFreeBikes);
            Integer freeBikes = markerStation.getFreeBikes();
            Integer emptySlots = markerStation.getEmptySlots();

            stationName.setText(markerStation.getName());
            setLastUpdateText(markerStation.getLastUpdate());
            stationFreeBikes.setText(String.valueOf(freeBikes));
            if (emptySlots == -1) {
                ImageView stationEmptySlotsLogo = (ImageView) findViewById(R.id.stationEmptySlotsLogo);
                stationEmptySlots.setVisibility(View.GONE);
                stationEmptySlotsLogo.setVisibility(View.GONE);
            } else {
                stationEmptySlots.setText(String.valueOf(emptySlots));
            }

            TextView stationNetwork = (TextView) findViewById(R.id.stationNetwork);
            String networkName = networksDataSource.getNetworkInfoFromId(markerStation.getNetworkId()).getName();
            stationNetwork.setText(networkName);

            TextView stationAddress = (TextView) findViewById(R.id.stationAddress);
            if (markerStation.getAddress() != null) {
                stationAddress.setText(markerStation.getAddress());
                stationAddress.setVisibility(View.VISIBLE);
            } else {
                stationAddress.setVisibility(View.GONE);
            }

            /* extra info on station */
            Boolean isBankingStation = markerStation.isBanking();
            Boolean isBonusStation = markerStation.isBonus();
            StationStatus stationStatus = markerStation.getStatus();
            Integer stationEBikes = markerStation.getEBikes();

            ImageView stationBanking = (ImageView) findViewById(R.id.stationBanking);
            if (isBankingStation != null) {
                stationBanking.setVisibility(View.VISIBLE);
                if (isBankingStation) {
                    stationBanking.setImageDrawable(getResources().getDrawable(R.drawable.ic_banking_on));
                    stationBanking.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Toast.makeText(getApplicationContext(),
                                    getString(R.string.cards_accepted),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    stationBanking.setImageDrawable(getResources().getDrawable(R.drawable.ic_banking_off));
                }
            } else {
                stationBanking.setVisibility(View.GONE);
            }

            ImageView stationBonus = (ImageView) findViewById(R.id.stationBonus);
            if (isBonusStation != null) {
                stationBonus.setVisibility(View.VISIBLE);
                if (isBonusStation) {
                    stationBonus.setImageDrawable(getResources().getDrawable(R.drawable.ic_bonus_on));
                    stationBonus.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Toast.makeText(getApplicationContext(),
                                    getString(R.string.is_bonus_station),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    stationBonus.setImageDrawable(getResources().getDrawable(R.drawable.ic_bonus_off));
                }
            } else {
                stationBonus.setVisibility(View.GONE);
            }
            if ((emptySlots == 0 && freeBikes == 0) || (stationStatus != null && stationStatus == StationStatus.CLOSED)) {
                stationName.setPaintFlags(stationName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                stationName.setPaintFlags(stationName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }

            ImageView eBikesImage = (ImageView) findViewById(R.id.stationEBikesLogo);
            ImageView regularBikesImage = (ImageView) findViewById(R.id.stationFreeBikesLogo);
            TextView stationEBikesValue = (TextView) findViewById(R.id.stationEBikesValue);
            if (stationEBikes != null) {
                regularBikesImage.setImageResource(R.drawable.ic_regular_bike);
                eBikesImage.setVisibility(View.VISIBLE);
                stationEBikesValue.setVisibility(View.VISIBLE);
                stationEBikesValue.setText(String.valueOf(stationEBikes));
                stationFreeBikes.setText(String.valueOf(freeBikes - stationEBikes));   //display regular bikes only
            } else {
                regularBikesImage.setImageResource(R.drawable.ic_bike);
                eBikesImage.setVisibility(View.GONE);
                stationEBikesValue.setVisibility(View.GONE);
            }
            stationDetailsView.setVisibility(View.VISIBLE);
        }
    }

    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private void setLastUpdateText(String rawLastUpdateISO8601) {
        long timeDifferenceInSeconds;
        TextView stationLastUpdate = (TextView) findViewById(R.id.stationLastUpdate);
        SimpleDateFormat timestampFormatISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        timestampFormatISO8601.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            long lastUpdate = timestampFormatISO8601.parse(rawLastUpdateISO8601).getTime();
            long currentDateTime = System.currentTimeMillis();
            timeDifferenceInSeconds = (currentDateTime - lastUpdate) / 1000;

            if (timeDifferenceInSeconds < 60) {
                stationLastUpdate.setText(getString(R.string.updated_just_now));
            } else if (timeDifferenceInSeconds >= 60 && timeDifferenceInSeconds < 3600) {
                int minutes = (int) timeDifferenceInSeconds / 60;
                stationLastUpdate.setText(getResources().getQuantityString(R.plurals.updated_minutes_ago,
                        minutes, minutes));
            } else if (timeDifferenceInSeconds >= 3600 && timeDifferenceInSeconds < 86400) {
                int hours = (int) timeDifferenceInSeconds / 3600;
                stationLastUpdate.setText(getResources().getQuantityString(R.plurals.updated_hours_ago,
                        hours, hours));
            } else if (timeDifferenceInSeconds >= 86400) {
                int days = (int) timeDifferenceInSeconds / 86400;
                stationLastUpdate.setText(getResources().getQuantityString(R.plurals.updated_days_ago,
                        days, days));
            }

            stationLastUpdate.setTypeface(null, Typeface.ITALIC);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void setDBLastUpdateText(SharedPreferences settings) {
        TextView lastUpdate = (TextView) findViewById(R.id.mapDbLastUpdate);
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

}
