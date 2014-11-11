package com.coachksrun.maps;

//http://developer.android.com/training/location/retrieve-current.html

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.coachksrun.R;
import com.directions.route.Route;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class MapsActivity extends Activity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener, RoutingListener {
    public final static String EXTRA_LAT_LNG = "com.coachksrun.maps.lat_lng";
    public final static String EXTRA_POLYLINE = "com.coachksrun.maps.polyline";

    private final static int UPDATE_INTERVAL = 1000;
    private final static int FASTEST_INTERVAL = 100;

    private LocationClient mLocationClient;
    private LocationRequest mLocationRequest;
    private Location previousLocation;

    private GoogleMap map;
    private Marker currentMarker;
    private MapStatsFragment mMapStats;
    private ArrayList<LatLng> latLngArray = new ArrayList<LatLng>();
    private ArrayList<LatLng> mRouteLatLngArray;
    private ArrayList<PolylineOptions> polylineOptionsArray = new ArrayList<PolylineOptions>();
    private ArrayList<PolylineOptions> mRoutePolylineOptionsArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mLocationClient = new LocationClient(this, this, this);
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(
                LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        // Set the fastest update interval to 1 second
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        mMapStats = (MapStatsFragment) getFragmentManager().findFragmentById(R.id.map_stats);

        // Get any routing information from RouteSelection activity
        Intent intent = getIntent();
        ArrayList<LatLng> ll = (ArrayList<LatLng>) intent.getSerializableExtra(EXTRA_LAT_LNG);
        if (ll != null)
            mRouteLatLngArray = ll;
        ArrayList<PolylineOptions> p = (ArrayList<PolylineOptions>) intent.getSerializableExtra(EXTRA_POLYLINE);
        if (p != null)
            mRoutePolylineOptionsArray = p;
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

    @Override
    protected void onStart() {
        super.onStart();
        // Connect the client.
        mLocationClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mLocationClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationClient.requestLocationUpdates(mLocationRequest, this);

        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        Location currentLocation = mLocationClient.getLastLocation();
        LatLng latLng = new LatLng(currentLocation.getLatitude(),
                currentLocation.getLongitude());

        previousLocation = currentLocation;

        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                .getMap();
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                LatLng tempLL = new LatLng(previousLocation.getLatitude(), previousLocation.getLongitude());

                map.addMarker(new MarkerOptions()
                        .position(latLng));
                Routing routing = new Routing(Routing.TravelMode.WALKING);
                routing.registerListener(MapsActivity.this);
                routing.execute(tempLL, latLng);

                latLngArray.add(latLng);
            }
        });

        currentMarker = map.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Current Location"));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20));

        // Draw route from previously selected route
        for (int i = 0; i < mRoutePolylineOptionsArray.size(); i++) {
            map.addPolyline(mRoutePolylineOptionsArray.get(i));
        }
    }

    @Override
    public void onDisconnected() {
        Toast.makeText(this, "Disconnected. Please re-connect.",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "Connection failed, please restart.",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(final Location location) {
        LatLng latLng = new LatLng(location.getLatitude(),
                location.getLongitude());

        final float speed = getSpeed(previousLocation, location);

        currentMarker.setPosition(latLng);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String status = "Current Coordinates: " + location.getLatitude() + ", " +
                        location.getLongitude() + "\n" +
                        "Current Speed: " + speed + " mph";
                mMapStats.setSpeed(status);

                final double currentLatitude = location.getLatitude();
                final double currentLongitude = location.getLongitude();
                LatLng currentLocation = new LatLng(currentLatitude, currentLongitude);
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 18));

                if (map != null && currentMarker != null)
                    currentMarker.setPosition(currentLocation);
            }
        });
    }

    private float getSpeed(Location previousLocation, Location currentLocation) {
        float meters = previousLocation.distanceTo(currentLocation);
        float miles = meters * (float) 0.00062137;
        return miles * 60 * 12;
    }

    @Override
    public void onRoutingFailure() {
        Toast.makeText(this, "Could not route, try again.",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRoutingStart() {
        Toast.makeText(this, "Computing route...",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRoutingSuccess(PolylineOptions mPolyOptions, Route route) {
        Toast.makeText(this, "Routing successful.",
                Toast.LENGTH_SHORT).show();
        PolylineOptions polyoptions = new PolylineOptions();
        polyoptions.color(Color.BLUE);
        polyoptions.width(10);
        polyoptions.addAll(mPolyOptions.getPoints());
        map.addPolyline(polyoptions);

        polylineOptionsArray.add(mPolyOptions);
    }
}
