package me.carleslc.stormy;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class CurrentWeather {

    private String mIcon, mSummary, mTimeZone;
    private long mTime;
    private double mTemperature, mHumidity, mPrecipChance;

    public String getIcon() {
        return mIcon;
    }

    public void setIcon(String icon) {
        mIcon = icon;
    }

    public String getSummary() {
        return mSummary;
    }

    public void setSummary(String summary) {
        mSummary = summary;
    }

    public int getIconId() {
        int iconId;
        switch (mIcon) {
            case "clear-night": iconId = R.drawable.clear_night; break;
            case "rain": iconId = R.drawable.rain; break;
            case "snow": iconId = R.drawable.snow; break;
            case "sleet": iconId = R.drawable.sleet; break;
            case "wind": iconId = R.drawable.wind; break;
            case "fog": iconId = R.drawable.fog; break;
            case "cloudy": iconId = R.drawable.cloudy; break;
            case "partly-cloudy-day": iconId = R.drawable.partly_cloudy; break;
            case "partly-cloudy-night": iconId = R.drawable.cloudy_night; break;
            default: iconId = R.drawable.clear_day; // clear-day
        }
        return iconId;
    }

    public long getTime() {
        return mTime;
    }

    public String getFormattedTime() {
        DateFormat formatter = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
        formatter.setTimeZone(TimeZone.getTimeZone(getTimeZone()));
        return formatter.format(new Date(getTime()*1000));
    }

    public void setTime(long time) {
        mTime = time;
    }

    public int getTemperature() {
        return (int)Math.round(mTemperature);
    }

    public void setTemperature(double temperature) {
        mTemperature = temperature;
    }

    public double getHumidity() {
        return mHumidity;
    }

    public void setHumidity(double humidity) {
        mHumidity = humidity;
    }

    public int getPrecipChance() {
        return (int)Math.round(100*mPrecipChance);
    }

    public void setPrecipChance(double precipChance) {
        mPrecipChance = precipChance;
    }

    public String getTimeZone() {
        return mTimeZone;
    }

    public void setTimeZone(String timeZone) {
        mTimeZone = timeZone;
    }
}
