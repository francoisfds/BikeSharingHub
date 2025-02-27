/*
 * Copyright (c) 2014-2015 Bruno Parmentier.
 * Copyright (c) 2020-2025 François FERREIRA DE SOUSA.
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
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
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
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import fr.fdesousa.bikesharinghub.db.NetworksDataSource;
import fr.fdesousa.bikesharinghub.db.StationsDataSource;
import fr.fdesousa.bikesharinghub.models.BikeNetworkLocation;
import fr.fdesousa.bikesharinghub.models.DownloadResult;
import fr.fdesousa.bikesharinghub.models.Station;
import fr.fdesousa.bikesharinghub.models.StationStatus;
import fr.fdesousa.bikesharinghub.R;
import fr.fdesousa.bikesharinghub.tasks.JSONDownloadRunnable;
import fr.fdesousa.bikesharinghub.tilesource.CustomTileSource;
import fr.fdesousa.bikesharinghub.widgets.StationsListAppWidgetProvider;

public class MapActivity extends Activity implements MapEventsReceiver, ActivityCompat.OnRequestPermissionsResultCallback, DownloadResult {
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
    private NetworksDataSource networksDataSource;
    private StationsDataSource stationsDataSource;
    private ScrollView stationDetailsView;
    private Drawable iconSelected;
    private Drawable previousDrawable = null;
    private Marker selectedMarker = null;
    private boolean isDetailViewOpened = false;
    private MenuItem favoriteMenuItem;
    private Handler mHandler = new Handler();
    private SharedPreferences settings;
    private RadiusMarkerClusterer stationsMarkers;
    private ExecutorService mExecutorService;
    private Future mDownloadFuture;

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

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        setDBLastUpdateText();

        stationsDataSource = new StationsDataSource(this);
        networksDataSource = new NetworksDataSource(this);
        ArrayList<Station> stations = stationsDataSource.getStations();

        final Context context = getApplicationContext();
        long systemCacheMaxBytes = 1024 * 1024 * Long.valueOf(settings.getString(PREF_KEY_MAP_CACHE_MAX_SIZE, "100"));
        long systemCacheTrimBytes = 1024 * 1024 * Long.valueOf(settings.getString(PREF_KEY_MAP_CACHE_TRIM_SIZE, "100"));
        Configuration.getInstance().setTileFileSystemCacheMaxBytes(systemCacheMaxBytes);
        Configuration.getInstance().setTileFileSystemCacheTrimBytes(systemCacheTrimBytes);
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));

        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_station_marker);
        Bitmap finalIcon = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(finalIcon);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        Paint firstPaint = new Paint();
        firstPaint.setColor(getResources().getColor(R.color.bike_red));
        canvas.drawCircle(Marker.ANCHOR_CENTER * finalIcon.getWidth(), Marker.ANCHOR_CENTER * finalIcon.getHeight(), finalIcon.getHeight() / 2f, firstPaint);
        drawable.draw(canvas);
        iconSelected = new BitmapDrawable(getResources(), finalIcon);

        map = (MapView) findViewById(R.id.mapView);

        stationDetailsView = findViewById(R.id.scrollView);

        /* handling map events */
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(this, this);
        map.getOverlays().add(0, mapEventsOverlay);

        /* markers list */
        stationsMarkers = new RadiusMarkerClusterer(this);
        Bitmap clusterIcon = getBitmapFromVectorDrawable(this, R.drawable.marker_cluster);
        map.getOverlays().add(stationsMarkers);
        stationsMarkers.setIcon(clusterIcon);
        stationsMarkers.setRadius(100);

        boolean hasExtra = getIntent().hasExtra(KEY_STATION);
        String stationExtraId = "";
        if(hasExtra) {
            Station stationExtra = (Station) getIntent().getSerializableExtra(KEY_STATION);
            stationExtraId = stationExtra.getId();
        }

        for (final Station station : stations) {
            if(hasExtra && station.getId().equals(stationExtraId)) {
                selectedMarker = createStationMarker(station);  //Keep ref of this marker
                stationsMarkers.add(selectedMarker);
            } else {
                stationsMarkers.add(createStationMarker(station));
            }
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
        } else if (hasExtra) {
            mapController.setZoom(16f);
            previousDrawable = selectedMarker.getIcon();
            selectedMarker.setIcon(iconSelected);
            setStationDetails((Station) selectedMarker.getRelatedObject());
            stationDetailsView.setVisibility(View.VISIBLE);
            isDetailViewOpened = true;
            mapController.animateTo(selectedMarker.getPosition());
            invalidateOptionsMenu();
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

    private boolean triggerActionDirection() {
        if (selectedMarker == null) {
            return false;
        }
        GeoPoint sPoint = selectedMarker.getPosition();
        Uri sLocationUri = Uri.parse("geo:" + sPoint.getLatitude() + "," + sPoint.getLongitude());
        Intent intent = new Intent(Intent.ACTION_VIEW, sLocationUri);
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> activities = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            activities = packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0));
        } else {
            activities = packageManager.queryIntentActivities(intent, 0);
        }
        if (activities != null && activities.size() > 0) {
            startActivity(intent);
        } else {
            Toast.makeText(this, getString(R.string.no_nav_application), Toast.LENGTH_LONG).show();
        }
        return true;
    }

    private boolean isFavorite() {
        if(selectedMarker == null) return false;
        Station selectedStation = (Station) selectedMarker.getRelatedObject();
        return stationsDataSource.isFavoriteStation(selectedStation.getId());
    }

    private void setFavorite(boolean favorite) {
        Station selectedStation = (Station) selectedMarker.getRelatedObject();
        if (favorite) {
            stationsDataSource.addFavoriteStation(selectedStation.getId());
            favoriteMenuItem.setIcon(R.drawable.ic_menu_favorite);
            Toast.makeText(MapActivity.this,
                    getString(R.string.station_added_to_favorites), Toast.LENGTH_SHORT).show();
        } else {
            stationsDataSource.removeFavoriteStation(selectedStation.getId());
            favoriteMenuItem.setIcon(R.drawable.ic_menu_favorite_outline);
            Toast.makeText(MapActivity.this,
                    getString(R.string.stations_removed_from_favorites), Toast.LENGTH_SHORT).show();
        }

        /* Refresh widget with new favorite */
        Intent refreshWidgetIntent = new Intent(getApplicationContext(),
                StationsListAppWidgetProvider.class);
        refreshWidgetIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        refreshWidgetIntent.putExtra(StationsListAppWidgetProvider.EXTRA_REFRESH_LIST_ONLY, true);
        sendBroadcast(refreshWidgetIntent);
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
            case R.id.action_refresh_map:
                executeDownloadTask();
                return true;
            case R.id.action_directions:
                return triggerActionDirection();
            case R.id.action_favorite:
                setFavorite(!isFavorite());
                return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        favoriteMenuItem = menu.findItem(R.id.action_favorite);
        MenuItem directionsMenuItem = menu.findItem(R.id.action_directions);
        if(isDetailViewOpened) {
            favoriteMenuItem.setEnabled(true).setVisible(true);
            directionsMenuItem.setEnabled(true).setVisible(true);
            if (isFavorite()) {
                favoriteMenuItem.setIcon(R.drawable.ic_menu_favorite);
            } else {
                favoriteMenuItem.setIcon(R.drawable.ic_menu_favorite_outline);
            }
        } else {
            favoriteMenuItem.setEnabled(false).setVisible(false);
            directionsMenuItem.setEnabled(false).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean singleTapConfirmedHelper(GeoPoint geoPoint) {
        InfoWindow.closeAllInfoWindowsOn(map);
        if (isDetailViewOpened) {
            stationDetailsView.setVisibility(View.GONE);
            isDetailViewOpened = false;
            mHandler.removeCallbacksAndMessages(null);
            if (previousDrawable != null && selectedMarker != null) {
                selectedMarker.setIcon(previousDrawable);
                selectedMarker = null;
            }
            invalidateOptionsMenu();
        }
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
        marker.setPosition(stationLocation);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {

            @Override
            public boolean onMarkerClick(Marker marker, MapView mapView) {
                if(isDetailViewOpened) {
                    if(marker.getPosition().equals(selectedMarker.getPosition())) {
                        //Need to hide details, proceed to singleTapConfirmedHelper
                        return false;
                    } else if (previousDrawable != null) {
                        selectedMarker.setIcon(previousDrawable);
                    }
                }
                selectedMarker = marker;
                previousDrawable = selectedMarker.getIcon();
                selectedMarker.setIcon(iconSelected);
                setStationDetails((Station) selectedMarker.getRelatedObject());
                stationDetailsView.setVisibility(View.VISIBLE);
                isDetailViewOpened = true;
                mapController.animateTo(selectedMarker.getPosition());
                invalidateOptionsMenu();
                return true;
            }
        });

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

    private void setStationDetails(Station markerStation) {
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

    private void executeDownloadTask() {
        if(mDownloadFuture != null ) {
            mDownloadFuture.cancel(true);
        }
        Runnable jsonRunnable = new JSONDownloadRunnable(getApplicationContext(), this);
        mExecutorService = Executors.newSingleThreadExecutor();
        mDownloadFuture = mExecutorService.submit(jsonRunnable);
    }

    @Override
    public void onDownloadResultCallback(String error) {
        mExecutorService.shutdown();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //setRefreshActionButtonState(false);
                //refreshLayout.setRefreshing(false);
                if (error != null) {
                    Log.e(TAG, "Download returned with an error: " + error);
                    /* TODO Display the error in the toast */
                    Toast.makeText(getApplicationContext(),
                            getApplicationContext().getResources().getString(R.string.connection_error),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                ArrayList<Station> stations = stationsDataSource.getStations();
                ArrayList<Marker> markerContent = stationsMarkers.getItems();
                markerContent.clear();
                for (final Station station : stations) {
                    markerContent.add(createStationMarker(station));
                }
                stationsMarkers.invalidate();
                map.invalidate();

                setDBLastUpdateText();
            }
        });

    }

    private class LastUpdateRunnable implements Runnable {

        private final String rawLastUpdateISO8601;
        private SimpleDateFormat timestampFormatISO8601;
        private TextView stationLastUpdate;

        public LastUpdateRunnable(String lastUpdateText, final Handler handler) {
            rawLastUpdateISO8601 = lastUpdateText;
            timestampFormatISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            timestampFormatISO8601.setTimeZone(TimeZone.getTimeZone("UTC"));
            stationLastUpdate = (TextView) findViewById(R.id.stationLastUpdate);
            stationLastUpdate.setTypeface(null, Typeface.ITALIC);
        }

        public void run() {
            try {
                long timeDifferenceInSeconds;
                long lastUpdate = timestampFormatISO8601.parse(rawLastUpdateISO8601).getTime();
                long currentDateTime = System.currentTimeMillis();
                timeDifferenceInSeconds = (currentDateTime - lastUpdate) / 1000;
                Log.d("FFDS", "il y a " + timeDifferenceInSeconds);

                if (timeDifferenceInSeconds < 60) {
                    stationLastUpdate.setText(getString(R.string.updated_just_now));
                    mHandler.postDelayed(this, 1000);
                } else if (timeDifferenceInSeconds >= 60 && timeDifferenceInSeconds < 3600) {
                    int minutes = (int) timeDifferenceInSeconds / 60;
                    stationLastUpdate.setText(getResources().getQuantityString(R.plurals.updated_minutes_ago,
                            minutes, minutes));
                    mHandler.postDelayed(this, 1000);
                } else if (timeDifferenceInSeconds >= 3600 && timeDifferenceInSeconds < 86400) {
                    int hours = (int) timeDifferenceInSeconds / 3600;
                    stationLastUpdate.setText(getResources().getQuantityString(R.plurals.updated_hours_ago,
                            hours, hours));
                    mHandler.postDelayed(this, 60000);
                } else if (timeDifferenceInSeconds >= 86400) {
                    int days = (int) timeDifferenceInSeconds / 86400;
                    stationLastUpdate.setText(getResources().getQuantityString(R.plurals.updated_days_ago,
                            days, days));
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    private void setLastUpdateText(String rawLastUpdateISO8601) {
        TextView stationLastUpdate = (TextView) findViewById(R.id.stationLastUpdate);
        stationLastUpdate.setTypeface(null, Typeface.ITALIC);

        final Handler handler = new Handler();
        final Runnable runnable = new LastUpdateRunnable(rawLastUpdateISO8601, handler);
        handler.post(runnable);
    }

    private void setDBLastUpdateText() {
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
