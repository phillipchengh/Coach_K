package com.coachksrun;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.coachksrun.maps.MapModeSelect;
import com.coachksrun.maps.MapsActivity;


public class MyActivity extends ListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        String[] items = { "login", "8tracks", "maps", "yelp" };
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
                i = new Intent(MyActivity.this, LoginActivity.class);
                startActivity(i);
                break;
            case 1:
                i = new Intent(MyActivity.this, Tracks8Activity.class);
                startActivity(i);
                break;
            case 2:
                i = new Intent(MyActivity.this, MapModeSelect.class);
                startActivity(i);
                break;
            case 3:
                i = new Intent(MyActivity.this, YelpActivity.class);
                startActivity(i);
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
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
