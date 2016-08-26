package me.carleslc.stormy;

import android.location.Address;
import android.support.annotation.NonNull;

import java.util.List;

@FunctionalInterface
public interface LocalityCallback {
    void onLocalityResponse(@NonNull List<Address> addresses);
}
