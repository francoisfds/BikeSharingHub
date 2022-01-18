# BikeSharingHub

BikeSharingHub is an Android application that displays the availability of shared bikes in your city.

It uses the [CityBikes API](https://api.citybik.es/v2/) that provides data for more than 400 cities in around 40 countries and displays this data in a list or on an [OpenStreetMap](https://www.openstreetmap.org) layer thanks to the [osmdroid](https://github.com/osmdroid/osmdroid) library (multiple layers are available).

It is a fork of the [OpenBikeSharing](https://github.com/bparmentier/OpenBikeSharing) application, which adds few improvements for recent Android versions.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/fr.fdesousa.bikesharinghub/)

## Contribute

Bug reports and feature requests can be reported in [Issues](https://github.com/francoisfds/BikeSharingHub/issues).

## Build

If you use Android Studio, you can import the project directly from GitHub.

Otherwise you can build it from the command line with
[Gradle](https://developer.android.com/sdk/installing/studio-build.html).
Clone the repo and type:

    ./gradlew build

(You may need to `chmod +x` the `gradlew` script)

The Gradle script will take care of downloading the necessary libraries and will generate the APK's
in `app/build/outputs/apk`.


## Translations

[![Translation status](https://hosted.weblate.org/widgets/bikesharinghub/-/open-graph.png)](https://hosted.weblate.org/engage/bikesharinghub/)

Help translate the app to your language from [our Hosted Weblate page](https://hosted.weblate.org/projects/bikesharinghub/).

## LICENSE

    BikeSharingHub. Shared bikes availability in your city.
    Copyright (C) 2020-2021 François FERREIRA DE SOUSA
    
    BikeSharingHub incorporates a modified version of OpenBikeSharing
    Copyright (C) 2014-2015  Bruno Parmentier

    BikeSharingHub is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.
    
    BikeSharingHub is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    
    You should have received a copy of the GNU General Public License
    along with BikeSharingHub.  If not, see <http://www.gnu.org/licenses/>.
