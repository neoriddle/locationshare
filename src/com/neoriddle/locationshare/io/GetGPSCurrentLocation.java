package com.neoriddle.locationshare.io;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.neoriddle.locationshare.R;
import com.neoriddle.locationshare.utils.AndroidUtils;

public class GetGPSCurrentLocation extends MapActivity {

    private static final int LAST_LOCATION_DETAIL_DIALOG_ID = 0x01;
    private static final int ABOUT_DIALOG_ID = 0x02;

    private MapView mapView;
    private MyLocationOverlay overlay;
    private SharedPreferences activityPreferences;
    private ActivityPreferenceChangeListener preferenceListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.get_gps_current_location);
        activityPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferenceListener = new ActivityPreferenceChangeListener();

        mapView = (MapView) findViewById(R.id.mapView);
        overlay = new MyLocationOverlay(this, mapView);
        mapView.getOverlays().add(overlay);
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityPreferences.registerOnSharedPreferenceChangeListener(preferenceListener);
        overlay.enableMyLocation();

        // Read preferences for compass
        if(activityPreferences.getBoolean("compass_control", false))
            overlay.enableCompass();
        else
            overlay.disableCompass();

        // Read preferences for satellite, traffic and streetview overlay
        mapView.setSatellite(activityPreferences.getBoolean("satellite_overlay", false));
        mapView.setTraffic(activityPreferences.getBoolean("traffic_overlay", false));
        mapView.setStreetView(activityPreferences.getBoolean("streetview_overlay", false));

        // Read preferences for map view controls
        mapView.setBuiltInZoomControls(activityPreferences.getBoolean("builtin_zoom_controls", false));
    }

    @Override
    protected void onPause() {
        super.onPause();
        activityPreferences.unregisterOnSharedPreferenceChangeListener(preferenceListener);

        if(overlay.isMyLocationEnabled())
            overlay.disableMyLocation();
        if(overlay.isCompassEnabled())
            overlay.disableCompass();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.get_gps_current_location, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.refreshMenu:
            Toast.makeText(this, "TODO: Call refresh", Toast.LENGTH_SHORT).show();
            // TODO Implement refresshing location
            return true;
        case R.id.lastLocationDetailMenu:
            if (overlay.getLastFix() == null)
                Toast.makeText(this, R.string.last_location_info_not_available, Toast.LENGTH_SHORT).show();
            else
                showDialog(LAST_LOCATION_DETAIL_DIALOG_ID);
            return true;
        case R.id.sendBySmsMenu:
            Toast.makeText(this, "TODO: Send location by SMS", Toast.LENGTH_SHORT).show();
            // TODO Send location by SMS
            return true;
        case R.id.sendByEmailMenu:
            final Location lastLocation = overlay.getLastFix();
            if (lastLocation == null)
                Toast.makeText(this, R.string.last_location_info_not_available,
                        Toast.LENGTH_SHORT).show();
            else {
                final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                final String[] recipients = {
                    "neoriddle@gmail.com"
                };

                final SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
                final String subject = preferences.getString("default_subject_for_email_alert",
                        getString(R.string.default_subject_for_email_alert));
                final String message = preferences.getString(
                        "default_template_for_email_alert",
                        getString(R.string.default_template_for_email_alert,
                                lastLocation.getLatitude(),
                                lastLocation.getLongitude(),
                                lastLocation.getAccuracy(),
                                lastLocation.getSpeed(),
                                new SimpleDateFormat("yyyyMMdd_HHmmss_ZZZZ").format(new Date(lastLocation.getTime()))));

                emailIntent.setType("plain/text");
                emailIntent.putExtra(Intent.EXTRA_EMAIL, recipients);
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                emailIntent.putExtra(Intent.EXTRA_TEXT, message);

                startActivity(emailIntent);
            }
            return true;
        case R.id.preferencesMenu:
            startActivity(new Intent(this, Preferences.class));
            return true;
        case R.id.aboutMenu:
            showDialog(ABOUT_DIALOG_ID);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        final LayoutInflater factory = LayoutInflater.from(this);

        switch (id) {
        case LAST_LOCATION_DETAIL_DIALOG_ID:
            final View detailView = factory.inflate(R.layout.show_last_location_detail, null);

            return new AlertDialog.Builder(this).
                setTitle(R.string.last_location_detail).
                setView(detailView).
                setPositiveButton(R.string.close, null).
                create();

        case ABOUT_DIALOG_ID :
            final View aboutView = factory.inflate(R.layout.about_dialog, null);

            final TextView versionLabel = (TextView)aboutView.findViewById(R.id.version_label);
            versionLabel.setText(getString(R.string.version_msg, AndroidUtils.getAppVersionName(getApplicationContext())));

            return new AlertDialog.Builder(this).
                setIcon(R.drawable.icon).
                setTitle(R.string.app_name).
                setView(aboutView).
                setPositiveButton(R.string.close, null).
                create();

        default:
            return super.onCreateDialog(id);
        }
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch(id) {
        case LAST_LOCATION_DETAIL_DIALOG_ID:
            final Location lastLocation = overlay.getLastFix();

            final TextView latitudeText = (TextView)dialog.findViewById(R.id.latitude_value);
            latitudeText.setText(Double.toString(lastLocation.getLatitude()));

            final TextView longitudeText = (TextView)dialog.findViewById(R.id.longitude_value);
            longitudeText.setText(Double.toString(lastLocation.getLongitude()));

            final TextView bearingText = (TextView) dialog.findViewById(R.id.bearing_value);
            bearingText.setText(Float.toString(lastLocation.getBearing()));

            final TextView altitudeText = (TextView) dialog.findViewById(R.id.altitude_value);
            altitudeText.setText(Double.toString(lastLocation.getAltitude()));

            final TextView speedText = (TextView) dialog.findViewById(R.id.speed_value);
            speedText.setText(getString(R.string.speed_value, lastLocation.getSpeed()));

            final TextView timeText = (TextView) dialog.findViewById(R.id.time_value);
            final DateFormat df = //DateFormat.getDateInstance(DateFormat.LONG, Locale.US);
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            final Date date = new Date(lastLocation.getTime());
            timeText.setText(df.format(date));

            final TextView providerText = (TextView) dialog.findViewById(R.id.provider_value);
            providerText.setText(lastLocation.getProvider());

            final TextView accuracyText = (TextView) dialog.findViewById(R.id.accuracy_value);
            accuracyText.setText(getString(R.string.accuracy_value, lastLocation.getAccuracy()));

            break;

        default:
            super.onPrepareDialog(id, dialog);
        }
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    private class ActivityPreferenceChangeListener implements SharedPreferences.OnSharedPreferenceChangeListener {
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            // TODO Implement preferences listener
            //Toast.makeText(getParent(), "pref=" + sharedPreferences.toString() + "\nkey=" + key, Toast.LENGTH_SHORT).show();
        }
    }
}