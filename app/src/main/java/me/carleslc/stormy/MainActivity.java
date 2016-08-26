package me.carleslc.stormy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static Activity sInstance;

    private CurrentWeather mCurrentWeather;
    private LocationService mLocationService;

    @BindView(R.id.locationLabel) TextView mLocationLabel;
    @BindView(R.id.timeLabel) TextView mTimeLabel;
    @BindView(R.id.temperatureLabel) TextView mTemperatureLabel;
    @BindView(R.id.humidityValue) TextView mHumidityValue;
    @BindView(R.id.precipValue) TextView mPrecipValue;
    @BindView(R.id.summaryLabel) TextView mSummaryLabel;
    @BindView(R.id.iconImageView) ImageView mIconImageView;
    @BindView(R.id.refreshImageView) ImageView mRefreshImageView;
    @BindView(R.id.progressBar) ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sInstance = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mLocationService = new LocationService(this);

        mProgressBar.setVisibility(View.INVISIBLE);

        mLocationService.setOnUpdateLocationListener((location) -> {
                toggleRefresh();
                getForecast(location.getLatitude(), location.getLongitude());
        });
        mLocationService.setAutoRefresh(false);
        mRefreshImageView.setOnClickListener((view) -> mLocationService.updateLocation());
    }

    private void getForecast(double latitude, double longitude) {
        String apiKey = getString(R.string.forecast_api_key);

        String options = "units=auto&lang=" + Locale.getDefault().getLanguage();

        String url = "https://api.forecast.io/forecast/" + apiKey +
                "/" + latitude + "," + longitude + "/?" + options;

        if (isNetworkAvailable()) {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> toggleRefresh());
                    Log.e(TAG, "Exception caught: ", e);
                    alertUserAboutError();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        final String jsonData = response.body().string();
                        Log.v(TAG, jsonData);
                        if (response.isSuccessful()) {
                            mCurrentWeather = getCurrentDetails(jsonData);
                            updateDisplay();
                        }
                        else {
                            toggleRefresh();
                            alertUserAboutError();
                        }
                    } catch (IOException | JSONException e) {
                        Log.e(TAG, "Exception caught: ", e);
                        toggleRefresh();
                        alertUserAboutError();
                    }
                }
            });
        }
        else {
            alertUserAboutError(getString(R.string.network_unavailable_message));
            toggleRefresh();
        }
    }

    private void toggleRefresh() {
        mProgressBar.setVisibility(mProgressBar.getVisibility() == View.VISIBLE ?
                View.INVISIBLE : View.VISIBLE);
        mRefreshImageView.setVisibility(mRefreshImageView.getVisibility() == View.VISIBLE ?
                View.INVISIBLE : View.VISIBLE);
    }

    private void updateDisplay() {
        runOnUiThread(() -> {
            mTemperatureLabel.setText(mCurrentWeather.getTemperature() + "");
            mTimeLabel.setText(String.format(getString(R.string.time_label_info),
                    mCurrentWeather.getFormattedTime()));
            mHumidityValue.setText(mCurrentWeather.getHumidity() + "");
            mPrecipValue.setText(mCurrentWeather.getPrecipChance() + "%");
            mSummaryLabel.setText(mCurrentWeather.getSummary());
            mIconImageView.setImageDrawable(ContextCompat.getDrawable(this, mCurrentWeather.getIconId()));

            mLocationService.getLocality((addresses) -> {
                if (!addresses.isEmpty()) {
                    String address = LocationService.getRegionalAddress(addresses.get(0));
                    if (!address.isEmpty()) mLocationLabel.setText(address);
                }
                toggleRefresh();
            });
        });
    }

    private CurrentWeather getCurrentDetails(String jsonData) throws JSONException {
        JSONObject forecast = new JSONObject(jsonData);
        JSONObject currently = forecast.getJSONObject("currently");

        CurrentWeather currentWeather = new CurrentWeather();
        currentWeather.setTimeZone(forecast.getString("timezone"));
        currentWeather.setHumidity(currently.getDouble("humidity"));
        currentWeather.setTime(currently.getLong("time"));
        currentWeather.setIcon(currently.getString("icon"));
        currentWeather.setPrecipChance(currently.getDouble("precipProbability"));
        currentWeather.setSummary(currently.getString("summary"));
        currentWeather.setTemperature(currently.getDouble("temperature"));

        Log.i(TAG, currentWeather.getFormattedTime() + " / " + currentWeather.getSummary());

        return currentWeather;
    }

    public static boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager)
                sInstance.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = manager.getActiveNetworkInfo();

        // Network present and connected?
        return networkInfo != null && networkInfo.isConnected();
    }

    private void alertUserAboutError() {
        new AlertDialogFragment().show(getFragmentManager(), getString(R.string.alert_tag));
    }

    private void alertUserAboutError(String message) {
        Bundle args = new Bundle();
        args.putString(getString(R.string.error_message_key), message);
        AlertDialogFragment alert = new AlertDialogFragment();
        alert.setArguments(args);
        alert.show(getFragmentManager(), getString(R.string.alert_tag));
    }

    @Override
    // Called after onCreate
    protected void onResume() {
        super.onResume();
        mLocationService.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!mLocationService.isRequestingPermissions()) mLocationService.disconnect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == LocationService.MY_PERMISSION_ACCESS_LOCATION &&
                // If request is cancelled, the result arrays are empty
                (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            mLocationService.checkForPermissions();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case LocationService.CONNECTION_FAILURE_RESOLUTION_REQUEST: {
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");
                        mLocationService.connect(); // reconnect
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.w(TAG, "Location services connection failed.");
                        break;
                }
                break;
            }
            case LocationService.REQUEST_CHECK_SETTINGS: {
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");
                        mLocationService.updateLocation();
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        mSummaryLabel.setText(R.string.location_disabled);
                        break;
                }
                mLocationService.setRequestingPermissions(false);
            }
        }
    }
}
