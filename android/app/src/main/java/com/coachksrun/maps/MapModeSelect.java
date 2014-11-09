package com.coachksrun.maps;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.coachksrun.LoginActivity;
import com.coachksrun.R;
import com.coachksrun.Tracks8Activity;
import com.coachksrun.YelpActivity;

public class MapModeSelect extends ListActivity {

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
                startActivity(i);
                break;
            case 1:
                i = new Intent(MapModeSelect.this, RouteSelection.class);
                startActivity(i);
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
}
