/*
 * Copyright (c) 2014-2015 Bruno Parmentier.
 * Copyright (c) 2020,2022 François FERREIRA DE SOUSA.
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

package fr.fdesousa.bikesharinghub.parsers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;

import fr.fdesousa.bikesharinghub.models.BikeNetwork;
import fr.fdesousa.bikesharinghub.models.BikeNetworkLocation;
import fr.fdesousa.bikesharinghub.models.Station;
import fr.fdesousa.bikesharinghub.models.StationStatus;

/**
 * Parse information on a bike network.
 */
public class BikeNetworkParser {
    private BikeNetwork bikeNetwork;

    public BikeNetworkParser(String toParse, boolean stripIdFromStationName) throws ParseException {
        ArrayList<Station> stations = new ArrayList<>();

        try {
            JSONObject jsonObject = new JSONObject(toParse);
            JSONObject rawNetwork = jsonObject.getJSONObject("network");

            /* network name & id */
            String networkId = rawNetwork.getString("id");
            String networkName = rawNetwork.getString("name");
            String networkCompany = rawNetwork.getString("company");

            /* network location */
            BikeNetworkLocation networkLocation;
            {
                JSONObject rawLocation = rawNetwork.getJSONObject("location");

                double latitude = rawLocation.getDouble("latitude");
                double longitude = rawLocation.getDouble("longitude");
                String city = rawLocation.getString("city");
                String country = rawLocation.getString("country");

                networkLocation = new BikeNetworkLocation(latitude, longitude, city, country);
            }

            /* stations list */
            {
                JSONArray rawStations = rawNetwork.getJSONArray("stations");

                for (int i = 0; i < rawStations.length(); i++) {
                    JSONObject rawStation = rawStations.getJSONObject(i);

                    String id = rawStation.getString("id");
                    String name = rawStation.getString("name");
                    if (stripIdFromStationName) name = name.replaceAll("^[0-9 ]*- *", "");
                    String lastUpdate = rawStation.getString("timestamp");
                    double latitude = rawStation.getDouble("latitude");
                    double longitude = rawStation.getDouble("longitude");
                    int freeBikes = rawStation.getInt("free_bikes");
                    int emptySlots;
                    if (!rawStation.isNull("empty_slots")) {
                        emptySlots = rawStation.getInt("empty_slots");
                    } else {
                        emptySlots = -1;
                    }

                    Station station = new Station(id, name, lastUpdate, latitude, longitude,
                            freeBikes, emptySlots, networkId);

                    /* extra info */
                    if (rawStation.has("extra")) {
                        JSONObject rawExtra = rawStation.getJSONObject("extra");

                        /* address */
                        if (rawExtra.has("address")) {
                            station.setAddress(rawExtra.getString("address"));
                        } else if (rawExtra.has("description")) {
                            station.setAddress(rawExtra.getString("description"));
                        }

                        /* banking */
                        if (rawExtra.has("banking")) { // JCDecaux
                            station.setBanking(rawExtra.getBoolean("banking"));
                        } else if (rawExtra.has("payment-terminal")) {
                            station.setBanking(rawExtra.getBoolean("payment-terminal"));
                        } else if (rawExtra.has("ticket")) { // dublinbikes, citycycle
                            station.setBanking(rawExtra.getBoolean("ticket"));
                        }

                        /* bonus */
                        if (rawExtra.has("bonus")) {
                            station.setBonus(rawExtra.getBoolean("bonus"));
                        }

                        /* status */
                        if (rawExtra.has("status")) {
                            JSONObject rawStatus = rawExtra.optJSONObject("status");
                            if (rawStatus != null) {
                                if (rawStatus.has("online")) {
                                    if (!rawStatus.getBoolean("online")) {
                                        station.setStatus(StationStatus.CLOSED);
                                    } else {
                                        station.setStatus(StationStatus.OPEN);
                                    }
                                }
                            } else{
                                String status = rawExtra.getString("status");
                                status = status.toUpperCase();
                                if (status.equals("CLOSED")
                                        || status.equals("CLS")
                                        || status.equals("1")
                                        || status.equals("OFFLINE")
                                        || status.equals("RÉFORMÉ")
                                        || status.equals("PLANNED")
                                        || status.equals("UNDER CONSTRUCTION")
                                        || status.equals("EN MAINTENANCE")
                                        || status.equals("IN_MAINTENANCE")
                                        || status.equals("MAINTENANCE")
                                        || status.equals("MAINTENANCE/IMPLEMENTATION")
                                ) {
                                    station.setStatus(StationStatus.CLOSED);
                                } else {
                                    station.setStatus(StationStatus.OPEN);
                                }
                            }
                        } else if (rawExtra.has("statusValue")) {
                            if (rawExtra.getString("statusValue").equals("Not In Service")) { // Bike Share
                                station.setStatus(StationStatus.CLOSED);
                            } else {
                                station.setStatus(StationStatus.OPEN);
                            }
                        } else if (rawExtra.has("locked")) {
                            if (rawExtra.getBoolean("locked")) { // bixi
                                station.setStatus(StationStatus.CLOSED);
                            } else {
                                station.setStatus(StationStatus.OPEN);
                            }
                        } else if (rawExtra.has("open")) {
                            if (!rawExtra.getBoolean("open")) { // dublinbikes, citycycle
                                station.setStatus(StationStatus.CLOSED);
                            } else {
                                station.setStatus(StationStatus.OPEN);
                            }
                        } else if (rawExtra.has("online")) {
                            if (!rawExtra.getBoolean("online")) {
                                station.setStatus(StationStatus.CLOSED);
                            } else {
                                station.setStatus(StationStatus.OPEN);
                            }
                        } else if (rawExtra.has("installed")) {
                            if (!rawExtra.getBoolean("installed")) {
                                station.setStatus(StationStatus.CLOSED);
                            } else {
                                station.setStatus(StationStatus.OPEN);
                            }
                        } else if (rawExtra.has("renting") && rawExtra.has("returning") ) {
                            Object renting = rawExtra.get("renting");
                            Object returning = rawExtra.get("returning");
                            if (renting instanceof Integer && returning instanceof Integer &&
                                    (Integer) renting == 0 && (Integer) returning == 0) {
                                station.setStatus(StationStatus.CLOSED);
                            } else if (renting instanceof Boolean && returning instanceof Boolean &&
                                    !((Boolean) renting) && !((Boolean) returning)) {
                                station.setStatus(StationStatus.CLOSED);
                            } else {
                                station.setStatus(StationStatus.OPEN);
                            }
                        }

                        /* electric bikes */
                        if (rawExtra.has("ebikes")) {
                            station.setEBikes(rawExtra.getInt("ebikes"));
                        }
                    }
                    stations.add(station);
                }
            }

            bikeNetwork = new BikeNetwork(networkId, networkName, networkCompany, networkLocation, stations);
        } catch (JSONException e) {
            throw new ParseException(e.getMessage(), 0);
        }
    }

    public BikeNetwork getNetwork() {
        return bikeNetwork;
    }

}
