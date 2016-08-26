package me.carleslc.stormy;

import android.location.Location;

@FunctionalInterface
public interface UpdateLocationListener {
    void onUpdateLocation(Location location);
}
