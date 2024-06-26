/*
 * Copyright (c) 2014-2015 Bruno Parmentier.
 * Copyright (c) 2020,2024 François FERREIRA DE SOUSA.
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;

import fr.fdesousa.bikesharinghub.R;
import fr.fdesousa.bikesharinghub.activities.BikeNetworksListActivity;
import fr.fdesousa.bikesharinghub.activities.StationsListActivity;

public class WelcomeDialogFragment extends DialogFragment {

    public static WelcomeDialogFragment instance = null;

    public static WelcomeDialogFragment getInstance() {
        if(instance == null) {
            instance = new WelcomeDialogFragment();
        }
        return instance;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.welcome_dialog_message);
        builder.setTitle(R.string.welcome_dialog_title);
        builder.setPositiveButton(R.string.welcome_dialog_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(getActivity(), BikeNetworksListActivity.class);
                getActivity().startActivityForResult(intent, StationsListActivity.PICK_NETWORK_REQUEST);
            }
        });
        builder.setNegativeButton(R.string.welcome_dialog_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getActivity().finish();
            }
        });
        return builder.create();
    }

}
