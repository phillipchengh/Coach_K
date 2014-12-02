package com.coachksrun.maps;

//http://developer.android.com/training/location/retrieve-current.html

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.coachksrun.R;
import com.coachksrun.Tracks8.MusicPlayer;
import com.coachksrun.Pitstop.PitstopStruct;
import com.coachksrun.Pitstop.Yelper;
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

import org.joda.time.DateTime;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.Seconds;

import java.util.ArrayList;

public class MapsActivity extends Activity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener, RoutingListener {
    public final static String EXTRA_LAT_LNG = "com.coachksrun.maps.lat_lng";
    public final static String EXTRA_POLYLINE = "com.coachksrun.maps.polyline";
    public final static String EXTRA_DISTANCE = "com.coachksrun.maps.distance";
    public final static String EXTRA_SECONDS = "com.coachksrun.maps.seconds";
    public final static String EXTRA_MINUTES = "com.coachksrun.maps.minutes";
    public final static String EXTRA_HOURS = "com.coachksrun.maps.hours";

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

    private ArrayList<LatLng> mActualLatLngArray = new ArrayList<LatLng>();
    private ArrayList<PolylineOptions> mActualPolylineOptionsArray = new ArrayList<PolylineOptions>();
    private float totalDistance = 0;
    private static final DateTime before = new DateTime();

    private MusicPlayer mMusicPlayer = new MusicPlayer();

    private static ArrayList<MarkerOptions> yelpMarkers;
    private ArrayList<Marker> yelpMapMarkers  = new ArrayList<Marker>();
    private Yelper hola_abbrevio = Yelper.getInstance();

    //TODO: need the Facebook ID
    private int id;

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

        mMusicPlayer.SetupMusicService(this);
        mMusicPlayer.setupMusicPlayerBroadcasts(
                LocalBroadcastManager.getInstance(getApplicationContext()));

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
        if (currentMarker == null) {
            currentMarker = map.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Current Location"));
        }
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20));

        // Draw route from previously selected route
        for(PolylineOptions polylineOptions : mRoutePolylineOptionsArray)
            map.addPolyline(polylineOptions);
    }

    @Override
    public void onDestroy() {
        mMusicPlayer.cleanUp();

        super.onDestroy();
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
        LatLng previousLatLng = new LatLng(previousLocation.getLatitude(),
                previousLocation.getLongitude());
        PolylineOptions polylineOptions = new PolylineOptions()
                .add(previousLatLng, latLng)
                .width(5)
                .color(Color.GREEN);

        mActualLatLngArray.add(latLng);
        mActualPolylineOptionsArray.add(polylineOptions);
        totalDistance = previousLocation.distanceTo(location);

        final float speed = getSpeed(previousLocation, location);

        currentMarker.setPosition(latLng);
        //map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
        map.addPolyline(polylineOptions);


        //Update Yelp markers
        hola_abbrevio.getPitstops(location.getLatitude(), location.getLongitude());
        for(int i = 0; i < yelpMapMarkers.size(); i++) {
            yelpMapMarkers.get(i).remove();
        }
        yelpMapMarkers.clear();
        if(yelpMarkers != null) {
            for (int i = 0; i < yelpMarkers.size(); i++) {
                Marker currentYelpMarker = map.addMarker(yelpMarkers.get(i));
                yelpMapMarkers.add(currentYelpMarker);
            }
        }

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
                //map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 18));

                if (map != null && currentMarker != null)
                    currentMarker.setPosition(currentLocation);
            }
        });

        previousLocation = location;
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

    public void musicPauseClicked(View view)
    {
        mMusicPlayer.pauseClicked(view);
    }

    public void musicSkipClicked(View view)
    {
        mMusicPlayer.skipClicked(view);
    }

    public void musicStopClicked(View view)
    {
        mMusicPlayer.stopClicked(view);
    }

    //Adds route back to start position
    public void routeHomeClick(View view) {
        if(mActualLatLngArray.size() > 0) {
            LatLng latLng = mActualLatLngArray.get(0);
            Routing routing = new Routing(Routing.TravelMode.WALKING);
            routing.registerListener(MapsActivity.this);
            routing.execute(mActualLatLngArray.get(mActualLatLngArray.size() - 1), latLng);
        }

    }

    public void routeFinishedClick(View view) {
        DateTime now = new DateTime();
        Intent data = new Intent(MapsActivity.this, RunSummaryActivity.class);

        int seconds = Seconds.secondsBetween(before, now).getSeconds();
        int minutes = Minutes.minutesBetween(before, now).getMinutes();
        int hours = Hours.hoursBetween(before, now).getHours();

        data.putExtra(EXTRA_LAT_LNG, mActualLatLngArray);
        data.putExtra(EXTRA_POLYLINE, mActualPolylineOptionsArray);
        data.putExtra(EXTRA_DISTANCE, totalDistance);
        data.putExtra(EXTRA_SECONDS, seconds);
        data.putExtra(EXTRA_MINUTES, minutes);
        data.putExtra(EXTRA_HOURS, hours);

        RouteUploadTask routeUploadTask = new RouteUploadTask(id, seconds, minutes, hours,
                totalDistance, mActualLatLngArray);
        routeUploadTask.execute();

        startActivity(data);
    }

    /*
     *  EXAMPLE CODE FOR YELP API
     *
     *  Yelper yelpClient = Yelper.getInstance();
     *  yelpClient.getPitstops(double latitude, double longitude);
     *
     *
     *  TODO: Implement displayPitstops()
     *
     *  After call to getPitstops(), coordinates of nearby juice businesses are
     *  retrieved via an async task, then displayPitstops() is called to show 
     *  businesses as markers on map
     */
    public static void displayPitstops(PitstopStruct[] pitstopArr, int numPitstops)
    {
        if (yelpMarkers == null) {
            yelpMarkers = new ArrayList<MarkerOptions>();
        }
        else {
            yelpMarkers.clear();
        }
        for(int i = 0; i < numPitstops; i++)
        {
            System.out.println(pitstopArr[i].getName()+": latitude: "+pitstopArr[i].getLatitude()+", longitude: "+pitstopArr[i].getLongitude());
            MarkerOptions pitStop = new MarkerOptions();
            LatLng coordinates = new LatLng(pitstopArr[i].getLatitude(), pitstopArr[i].getLongitude());
            pitStop.position(coordinates);
            pitStop.title(pitstopArr[i].getName());
            System.out.println("Added Title:" + pitStop.getTitle());
            yelpMarkers.add(pitStop);
        }


    }
}
