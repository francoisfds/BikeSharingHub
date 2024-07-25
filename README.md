# BikeSharingHub

BikeSharingHub is an Android application that displays the availability of shared bikes in your city.

It uses the [CityBikes API](https://api.citybik.es/v2/) that provides data for more than 400 cities in around 40 countries and displays this data in a list or on an [OpenStreetMap](https://www.openstreetmap.org) layer thanks to the [osmdroid](https://github.com/osmdroid/osmdroid) library (multiple layers are available).

It is a fork of the [OpenBikeSharing](https://github.com/bparmentier/OpenBikeSharing) application, which adds few improvements for recent Android versions.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/fr.fdesousa.bikesharinghub/)
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
    alt="Get it on Google Play"
    height="80">](https://play.google.com/store/apps/details?id=fr.fdesousa.bikesharinghub)
[<img src="https://user-images.githubusercontent.com/15369785/212381735-4f53e18c-39f2-4435-a9b8-2577e9cada77.png"
    alt="Get it on Github"
    height="80">](https://github.com/francoisfds/BikeSharingHub/releases)

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

## Technical notice for Android 7 and lower

BikeSharingHub is fully compatible with recent Android versions down to Android 4.
HTTPS connection requires the device to trust an Authority that is embedded on Android since the version 7.1.1 only. Thus, on devices runnig Android 7 and lower, the application uses by default an API URL with HTTP only. Users who which to use HTTPS can change the URL in the application, however it will become necessary to install manualy the certificate authority provided below on the security settings of their device. Be aware that installing a certifcate will affects the whole device and not only this application, so make sure to proceed with the proper file.

* Official link to the certificate to be installed : https://letsencrypt.org/certs/isrgrootx1.pem
* SHA-256 of file : 22b557a27055b33606b6559f37703928d3e4ad79f110b407d04986e1843543d1

## LICENSE

    BikeSharingHub. Shared bikes availability in your city.
    Copyright (C) 2020-2024 François FERREIRA DE SOUSA
    
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
