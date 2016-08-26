package me.carleslc.stormy;

import android.app.Activity;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LocationService implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    public static final String TAG = LocationService.class.getSimpleName();
    public static final int MY_PERMISSION_ACCESS_LOCATION = 700;
    public static final int REQUEST_CHECK_SETTINGS = 800;
    public static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 900;

    // Refresh intervals, in milliseconds
    private static final int UPDATE_INTERVAL = 60000;
    private static final int FASTEST_UPDATE_INTERVAL = 5000;

    private Activity mContext;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private UpdateLocationListener mUpdateLocationListener;

    private boolean mHasPermissions, mAutoRefresh, mRequestingPermissions = false;

    public LocationService(Activity context) {
        mContext = context;
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        checkForPermissions();
    }

    public void connect() {
        if (!mGoogleApiClient.isConnected()) mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "Location services connected.");
        updateLocation();
    }

    public boolean isAutoRefresh() {
        return mAutoRefresh;
    }

    public void setAutoRefresh(boolean autoRefresh) {
        if (autoRefresh) updateLocation();
        else {
            removeUpdates();
            mLocationRequest = null;
        }
        mAutoRefresh = autoRefresh;
    }

    private LocationRequest getLocationRequest() {
        if (mLocationRequest == null) {
            mLocationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                    .setInterval(UPDATE_INTERVAL)
                    .setFastestInterval(FASTEST_UPDATE_INTERVAL);
        }
        return mLocationRequest;
    }

    public void updateLocation() {
        if (!mGoogleApiClient.isConnected()) {
            Log.w(TAG, "GoogleApiClient is disconnected.");
            mGoogleApiClient.connect();
        }
        else if (hasPermissions()) {
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest
                    .Builder()
                    .addLocationRequest(getLocationRequest())
                    .setAlwaysShow(true);

            LocationServices.SettingsApi
                    .checkLocationSettings(mGoogleApiClient, builder.build())
                    .setResultCallback(result -> {
                        final Status status = result.getStatus();
                        switch (status.getStatusCode()) {
                            case LocationSettingsStatusCodes.SUCCESS:
                                Log.i(TAG, "All location settings are satisfied.");
                                try {
                                    LocationServices.FusedLocationApi.requestLocationUpdates(
                                            mGoogleApiClient, getLocationRequest(), this);
                                } catch (SecurityException e) {
                                    mHasPermissions = false;
                                }
                                break;
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. " +
                                        "Showing the user a dialog to upgrade location settings.");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the result
                                    // in onActivityResult().
                                    status.startResolutionForResult(mContext, REQUEST_CHECK_SETTINGS);
                                    mRequestingPermissions = true;
                                } catch (IntentSender.SendIntentException e) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                Log.i(TAG, "Location settings are inadequate, and cannot be " +
                                        "fixed on runtime.");
                                break;
                        }
                    });
        } else checkForPermissions();
    }

    public void setRequestingPermissions(boolean requestingPermissions) {
        mRequestingPermissions = requestingPermissions;
    }

    public boolean isRequestingPermissions() {
        return mRequestingPermissions;
    }

    public void disconnect() {
        if (mGoogleApiClient.isConnected()) {
            removeUpdates();
            mGoogleApiClient.disconnect();
        }
    }

    private void removeUpdates() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended.");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(mContext, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "Location services connection failed.", e);
            }
        } else {
            Log.w(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    public boolean hasPermissions() {
        return mHasPermissions;
    }

    public void checkForPermissions() {
        // If we don't have permissions we request them on runtime
        if (ContextCompat.checkSelfPermission(mContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            mHasPermissions = false;
            Log.i(TAG, TAG + " without permissions!");
            ActivityCompat.requestPermissions(mContext, new String[]
                            { android.Manifest.permission.ACCESS_FINE_LOCATION },
                    MY_PERMISSION_ACCESS_LOCATION);
        }
        else {
            mHasPermissions = true;
            if (mGoogleApiClient.isConnected()) updateLocation();
        }
    }

    public void setOnUpdateLocationListener(UpdateLocationListener listener) {
        mUpdateLocationListener = listener;
        if (mLastLocation != null) listener.onUpdateLocation(mLastLocation);
    }

    private void handleNewLocation(Location location) {
        mLastLocation = location;
        Log.d(TAG, "Location updated: " + mLastLocation.toString());
        if (mUpdateLocationListener != null) {
            mUpdateLocationListener.onUpdateLocation(mLastLocation);
        }
        if (!isAutoRefresh()) setAutoRefresh(false);
    }

    public void getLocality(LocalityCallback callback) {
        new GeocoderTask(callback).execute();
    }

    public static String getRegionalAddress(@NonNull Address address) {
        String addressText = "";
        String locality = address.getLocality();
        if (locality != null) addressText += locality;
        String subAdminArea = address.getSubAdminArea();
        if (subAdminArea != null) {
            addressText += (addressText.isEmpty() ? "" : ", ") + subAdminArea;
        }
        return addressText;
    }

    private class GeocoderTask extends AsyncTask<String, Void, List<Address>> {

        private LocalityCallback mCallback;

        public GeocoderTask(@NonNull LocalityCallback callback) {
            mCallback = callback;
        }

        @Override
        protected List<Address> doInBackground(String... strings) {
            Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
            List<Address> addresses = new ArrayList<>();
            if (mLastLocation != null) {
                double latitude = mLastLocation.getLatitude();
                double longitude = mLastLocation.getLongitude();
                try {
                    addresses = geocoder.getFromLocation(latitude, longitude, 1);
                } catch (IOException e) {
                    Throwable cause = e.getCause();
                    Log.i(TAG, "Error " + (cause != null ? cause.getClass().getSimpleName()
                            : "(" + e.getClass().getSimpleName() + ":" + e.getMessage() + ")")
                            + " getting locality with Geocoder. " +
                            "Trying with HTTP/GET on Google Maps API.");
                    addresses = geolocateFromGoogleApis(latitude, longitude);
                }
            }
            return addresses;
        }

        private List<Address> geolocateFromGoogleApis(double latitude, double longitude) {
            List<Address> addresses = new ArrayList<>();

            final String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" +
                    latitude + "," + longitude + "&sensor=true";

            if (MainActivity.isNetworkAvailable()) {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(url)
                        .build();

                try {
                    // Synchronous call because we're already on a background thread behind UI
                    Response response = client.newCall(request).execute();
                    final String jsonData = response.body().string();
                    Log.v(TAG, jsonData);
                    if (response.isSuccessful()) {
                        addresses.add(getAddressFromGoogleApis(jsonData));
                    } else {
                        Log.e(TAG, "Unexpected code getting locality with HTTP/GET " +
                                "on Google Maps API: " + response);
                    }
                } catch (IOException | JSONException e) {
                    Log.e(TAG, "Error getting locality with HTTP/GET on Google Maps API: ", e);
                }
            } else {
                Log.e(TAG, "Error getting locality with HTTP/GET on Google Maps API: " +
                        "Network unavailable.");
            }
            return addresses;
        }

        private Address getAddressFromGoogleApis(String jsonData) throws JSONException {
            Address address = new Address(Locale.getDefault());
            JSONObject addressObject = new JSONObject(jsonData);
            JSONObject results = addressObject.getJSONArray("results").getJSONObject(0);
            JSONArray addressComponents = results.getJSONArray("address_components");
            JSONObject location = results.getJSONObject("geometry").getJSONObject("location");
            address.setLatitude(location.getDouble("lat"));
            address.setLongitude(location.getDouble("lng"));
            // Street Address, Street Number
            address.setAddressLine(0, addressComponents.getJSONObject(1).getString("long_name")
                                    + addressComponents.getJSONObject(0).getString("short_name"));
            address.setLocality(addressComponents.getJSONObject(2).getString("long_name"));
            address.setAdminArea(addressComponents.getJSONObject(4).getString("long_name"));
            address.setSubAdminArea(addressComponents.getJSONObject(3).getString("long_name"));
            JSONObject countryObject = addressComponents.getJSONObject(5);
            address.setCountryName(countryObject.getString("long_name"));
            address.setCountryCode(countryObject.getString("short_name"));
            address.setPostalCode(addressComponents.getJSONObject(6).getString("short_name"));

            return address;
        }

        @Override
        protected void onPostExecute(List<Address> addresses){
            mCallback.onLocalityResponse(addresses);
        }
    }

}
