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
package fr.fdesousa.bikesharinghub.tilesource;

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourcePolicy;
import org.osmdroid.tileprovider.tilesource.XYTileSource;

public class CustomTileSource {

    public static final OnlineTileSourceBase CYCLOSM = new XYTileSource("CyclOSM",
            0, 19, 256, ".png", new String[]{
            "https://a.tile-cyclosm.openstreetmap.fr/cyclosm/",
            "https://b.tile-cyclosm.openstreetmap.fr/cyclosm/",
            "https://c.tile-cyclosm.openstreetmap.fr/cyclosm/"},
            "© OpenStreetMap contributors",
            new TileSourcePolicy(2,
                    TileSourcePolicy.FLAG_NO_BULK
                            | TileSourcePolicy.FLAG_NO_PREVENTIVE
                            | TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL
                            | TileSourcePolicy.FLAG_USER_AGENT_NORMALIZED
            ));
    // max concurrent thread number is 2 (cf. https://operations.osmfoundation.org/policies/tiles/)

    public static final OnlineTileSourceBase OPNVKARTE = new XYTileSource("Öpnvkarte",
            0, 18, 256, ".png",
            new String[]{"https://tile.memomaps.de/tilegen/"},
            "© OpenStreetMap contributors, tiles by MeMoMaps CC-BY-SA",
            new TileSourcePolicy(2,
                    TileSourcePolicy.FLAG_NO_BULK
                            | TileSourcePolicy.FLAG_NO_PREVENTIVE
                            | TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL
                            | TileSourcePolicy.FLAG_USER_AGENT_NORMALIZED
            ));
    // Free to use under the conditions of the CC-BY-SA and ODbL (cf FAQ section of https://%C3%B6pnvkarte.de)
}
