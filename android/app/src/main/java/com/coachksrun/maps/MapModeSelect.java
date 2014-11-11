package com.coachksrun.maps;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.coachksrun.R;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class MapModeSelect extends ListActivity {

    private static final int REQUEST_ROUTE = 0;
    private static final int REQUEST_ACTIVE = 1;

    private ArrayList<LatLng> latLngArray = new ArrayList<LatLng>();
    private ArrayList<PolylineOptions> polylineOptionsArray = new ArrayList<PolylineOptions>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_mode_select);

        String[] items = { "Active", "Route" };
        setListAdapter(new ArrayAdapter<String>(
                getApplicationContext(),
                android.R.layout.simple_list_item_1,
                items));
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position,
                                long id) {
        Intent i;
        switch(position) {
            case 0:
                i = new Intent(MapModeSelect.this, MapsActivity.class);
                i.putExtra(MapsActivity.EXTRA_LAT_LNG, latLngArray);
                i.putExtra(MapsActivity.EXTRA_POLYLINE, polylineOptionsArray);
                startActivityForResult(i, REQUEST_ACTIVE);
                break;
            case 1:
                i = new Intent(MapModeSelect.this, RouteSelection.class);
                startActivityForResult(i, REQUEST_ROUTE);
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.map_mode_select, menu);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case REQUEST_ROUTE:
                if (data != null) {
                    latLngArray = (ArrayList<LatLng>) data.getSerializableExtra(RouteSelection.EXTRA_LAT_LNG);
                    polylineOptionsArray = (ArrayList<PolylineOptions>) data.getSerializableExtra(RouteSelection.EXTRA_POLYLINE);
                }
                break;
            default:
                Log.e("MapModeSelect", "Switch statement went to default!\n");
                break;
        }
    }
}
