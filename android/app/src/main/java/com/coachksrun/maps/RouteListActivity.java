package com.coachksrun.maps;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import com.coachksrun.R;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class RouteListActivity extends ListActivity {
    private static final String DOWNLOAD_BROADCAST = "com.coachksrun.maps.download";
    private ArrayList<Route> routes = new ArrayList<Route>();
    private ArrayList<String> timestamps = new ArrayList<String>();
    private ArrayAdapter<String> adapter;

    //TODO: need the Facebook ID
    private int id;

    /**
     *  JSON structure
     *  Overall Object
     *      Array of routes
     *          Route object
     *              id, seconds, minutes, hours, distance, timestamp, array
     *                  array of objects
     *                      object has latitude, longitude (both floats)
     */

    private class Route {
        public int id;
        public int seconds;
        public int minutes;
        public int hours;
        public float distance;
        public ArrayList<LatLng> coords;

        public Route(int i, int s, int m, int h, float d, ArrayList<LatLng> c) {
            id = i;
            seconds = s;
            minutes = m;
            hours = h;
            distance = d;
            coords = c;
        }
    }

    private BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getExtras();
            String result = b.getString("routes");

            try {
                JSONObject routesObject = new JSONObject(result);
                JSONArray routesArray = routesObject.getJSONArray("routes");
                for(int j = 0; j < routesArray.length(); j++) {
                    ArrayList<LatLng> latLngs = new ArrayList<LatLng>();
                    JSONObject route = routesArray.getJSONObject(j);
                    JSONArray coordList = route.getJSONArray("coordinates");
                    for(int k = 0; k < coordList.length(); k++) {
                        JSONObject coordinates = coordList.getJSONObject(k);
                        float latitude = Float.parseFloat(coordinates.getString("latitude"));
                        float longitude = Float.parseFloat(coordinates.getString("longitude"));
                        LatLng latLng = new LatLng(latitude, longitude);
                        latLngs.add(latLng);
                    }

                    routes.add(new Route(
                            route.getInt("id"),
                            route.getInt("seconds"),
                            route.getInt("minutes"),
                            route.getInt("hours"),
                            Float.parseFloat(route.getString("distance")),
                            latLngs
                    ));

                    timestamps.add(route.getString("timestamp"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            adapter.notifyDataSetChanged();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_list);

        IntentFilter filter = new IntentFilter(DOWNLOAD_BROADCAST);
        getApplicationContext().registerReceiver(myReceiver, filter);

        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, timestamps);

        RouteDownloadTask routeDownloadTask = new RouteDownloadTask(id, getApplicationContext());
        routeDownloadTask.execute();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_route_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
