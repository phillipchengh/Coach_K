package com.coachksrun.maps;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.coachksrun.MenuActivity;
import com.coachksrun.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class RunSummaryActivity extends Activity {
    public final static String EXTRA_LAT_LNG = "com.coachksrun.maps.lat_lng";
    public final static String EXTRA_POLYLINE = "com.coachksrun.maps.polyline";
    public final static String EXTRA_DISTANCE = "com.coachksrun.maps.distance";
    public final static String EXTRA_SECONDS = "com.coachksrun.maps.seconds";
    public final static String EXTRA_MINUTES = "com.coachksrun.maps.minutes";
    public final static String EXTRA_HOURS = "com.coachksrun.maps.hours";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run_summary);

        GoogleMap map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                .getMap();

        Intent intent = getIntent();
        ArrayList<LatLng> latLngs = (ArrayList<LatLng>) intent.getSerializableExtra(EXTRA_LAT_LNG);
        ArrayList<PolylineOptions> polylineOptions =
                (ArrayList<PolylineOptions>) intent.getSerializableExtra(EXTRA_POLYLINE);

        for(PolylineOptions polylineOption : polylineOptions)
            map.addPolyline(polylineOption);

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngs.get(0), 18));

        float distance = intent.getFloatExtra(EXTRA_DISTANCE, 0);
        ((TextView) findViewById(R.id.summary_total_distance)).setText(
                "Total Distance: " + String.valueOf(distance));


        String timeString = "Time Elapsed: ";
        int hours = intent.getIntExtra(EXTRA_HOURS, -1);
        if(hours > 0) timeString += Integer.toString(hours) + ":";

        int minutes = intent.getIntExtra(EXTRA_MINUTES, -1);
        minutes %= 60;
        if(minutes < 10) timeString += "0";
        timeString += Integer.toString(minutes) + ":";

        int seconds = intent.getIntExtra(EXTRA_SECONDS, -1);
        seconds %= 60;
        if(seconds < 10) timeString += "0";
        timeString += Integer.toString(seconds);

        ((TextView) findViewById(R.id.summary_total_time)).setText(timeString);

        int totalSeconds = hours*3600+minutes*60+seconds;
        ((TextView) findViewById(R.id.summary_avg_pace)).setText("Average Pace: " +
                String.valueOf(Math.abs(distance/totalSeconds * 3600)));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_run_summary, menu);
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

    public void returnMainMenu(View view) {
        Intent i = new Intent(RunSummaryActivity.this, MenuActivity.class);
        startActivity(i);
    }
}
