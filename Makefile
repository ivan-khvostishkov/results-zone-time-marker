TimeMarker-release.apk: android/TimeMarker/build/apk/TimeMarker-release-unsigned.apk
	cp android/TimeMarker/build/apk/TimeMarker-release-unsigned.apk android/TimeMarker/build/apk/TimeMarker-release-unaligned.apk
	~/jdk1.7.0_15/bin/jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore ./TimeMarker-release.keystore android/TimeMarker/build/apk/TimeMarker-release-unaligned.apk timemarker-release
	~/android-sdk-linux/tools/zipalign -v 4 android/TimeMarker/build/apk/TimeMarker-release-unaligned.apk android/TimeMarker/build/apk/TimeMarker-release.apk
	cp android/TimeMarker/build/apk/TimeMarker-release.apk TimeMarker-release.apk
