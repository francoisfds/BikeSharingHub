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
package fr.fdesousa.bikesharinghub.models;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;

import java.util.ArrayList;

import be.brunoparmentier.openbikesharing.app.db.StationsDataSource;
import be.brunoparmentier.openbikesharing.app.models.Station;

public class StationsListViewModel extends AndroidViewModel {

    private ArrayList<Station> stations;
    private StationsDataSource stationsDataSource;

    public StationsListViewModel(Application application) {
        super(application);
        stationsDataSource = new StationsDataSource(application);
    }

    public ArrayList<Station> getStations() {
        if(stations == null) {
            stations = stationsDataSource.getStations();
        }
        return stations;
   }

    public ArrayList<Station> getFavoriteStations() {
        if(stations == null) {
            stations = stationsDataSource.getFavoriteStations();
        }
        return stations;
   }

}
