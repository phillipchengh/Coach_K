package com.coachksrun.maps;

/**
 * Adapted from AndroidHive
 * http://www.androidhive.info/2012/07/android-gps-location-manager-tutorial/
 */

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.coachksrun.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Create a new GPS tracker to get the current coordinates
        GPSTracker gps = new GPSTracker(this);
        if(gps.canGetLocation()){
            // Final variables for access in nested function
            final double currentLatitude = gps.getLatitude();
            final double currentLongitude = gps.getLongitude();
            LatLng currentLocation = new LatLng(currentLatitude, currentLongitude);

            GoogleMap map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                    .getMap();

            if (map!=null){
                Marker currentMarker = map.addMarker(new MarkerOptions()
                        .position(currentLocation)
                        .title("Current Location"));

                map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                    @Override
                    public void onMapLongClick(LatLng latLng) {
                        //map.addMarker(new MarkerOptions().position(latLng));
                        String uri = "http://maps.google.com/maps?saddr=" +
                                currentLatitude + "," + currentLongitude +
                                "&daddr=" + latLng.latitude + "," + latLng.longitude;
                        Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri));
                        intent.setClassName("com.google.android.apps.maps",
                                "com.google.android.maps.MapsActivity");
                        startActivity(intent);
                    }
                });

                // Move the camera instantly to hamburg with a zoom of 15.
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15));

                // Zoom in, animating the camera.
                map.animateCamera(CameraUpdateFactory.zoomTo(18), 2000, null);
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.maps, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
