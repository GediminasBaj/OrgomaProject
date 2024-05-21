package com.learningkotlin.orgomaproject;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import com.google.android.gms.maps.model.LatLng;

// Custom LatLng class, because Firebase require empty constructor
// and LatLng doesn't have it, so firebase Throws error and app crash
// Needed to implement Parcelable because of Coordinates sending, used resources:
// https://stackoverflow.com/questions/15133121/pass-arraylist-implements-parcelable-to-activity
// https://stackoverflow.com/questions/45796345/how-to-put-latlong-object-in-parcelable-class
// I didn't write code that needed for Parcelable, android studio did everything for me,
// I just implemented everything that android studio asked for me, to solve errors.
public class CustomLatLng implements Parcelable {
    private double latitude;
    private double longitude;

    public CustomLatLng() {
        // empty constructor required by firebase
    }

    public CustomLatLng(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    protected CustomLatLng(Parcel in) {
        latitude = in.readDouble();
        longitude = in.readDouble();
    }

    public static final Creator<CustomLatLng> CREATOR = new Creator<CustomLatLng>() {
        @Override
        public CustomLatLng createFromParcel(Parcel in) {
            return new CustomLatLng(in);
        }

        @Override
        public CustomLatLng[] newArray(int size) {
            return new CustomLatLng[size];
        }
    };

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    // for conversion to latLng - using LatLng class method
    public LatLng toLatLng() {
        return new LatLng(latitude, longitude);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
    }
}
