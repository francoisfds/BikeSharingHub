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
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;

import java.lang.Class;

public class StationsListViewModelFactory implements ViewModelProvider.Factory {

    private Application mApplictaion;

    public StationsListViewModelFactory(Application applictaion) {
        mApplictaion = applictaion;
    }

    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        return (T) new StationsListViewModel(mApplictaion);
    }
}
