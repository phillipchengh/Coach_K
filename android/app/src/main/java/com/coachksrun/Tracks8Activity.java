package com.coachksrun;
import com.coachksrun.Tracks8.MusicService;
import com.coachksrun.Tracks8.PlaylistDbHelper;
import com.coachksrun.Tracks8.utility;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;

import java.nio.DoubleBuffer;
import java.util.HashMap;
import java.util.Map;

import java.net.URL;
import java.net.HttpURLConnection;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;


public class Tracks8Activity extends Activity
{
    public String g_play_token = null;
    public String g_mix_id = null;
    public String g_genre = null;
    private LocalBroadcastManager g_broadcast_manager = null;
    private MusicService m_MusicService = null;
    private ServiceConnection m_serviceConn = null;
    private boolean m_bound = false;

    private PlaylistDbHelper m_dbHelper = null;
    private SQLiteDatabase m_db = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tracks8);

        // Lets user choose genre, choose playlist (get play_token and mix_id), and start streaming.
        SetupMusicService();
        //setupSQLiteDB();
    }

    public void setupSQLiteDB()
    {
        m_dbHelper = new PlaylistDbHelper(this);
        m_db = m_dbHelper.getWritableDatabase();
    }


    /**
     * Displays genres. Kicks off two asynchronous tasks:
     * 1a) Display popular playlists, let user pick, and get that playlist's mix_id.
     * 1b) Get play token id.
     */
    private void SetupMusicService() {
        // Let user choose genre.
        final Map<String, String> genre_name_to_tag = new HashMap<String, String>();
        genre_name_to_tag.put("Hip hop", "hip_hop");
        genre_name_to_tag.put("Electronic", "electronic");
        genre_name_to_tag.put("Workout", "workout");
        genre_name_to_tag.put("Rock", "rock");
        final String[] genre_tags = genre_name_to_tag.keySet().toArray(new String[0]);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getApplicationContext(), android.R.layout.simple_list_item_1, genre_tags);
        final ListView list_view = (ListView) findViewById(
                R.id.tracks8_playlists_selection_list);
        list_view.setAdapter(adapter);
        // Gets chosen genre tag.
        AdapterView.OnItemClickListener genreClickedHandler = new AdapterView.OnItemClickListener() {
            public void onItemClick(
                    AdapterView parent, View v, int position, long id) {
                // 1a) Get mix id.
                // User chose this; stream this mix!
                String chosen_tag = genre_name_to_tag.get(
                        genre_tags[position]);
                System.out.println("Chosen genre tag: " + chosen_tag);
                g_genre = chosen_tag;
                String genre_url = String.format(
                        utility.URL_GENRE, chosen_tag);

                URL url;
                try
                {
                    url = new URL(genre_url);
                }
                catch (Exception e)
                {
                    System.err.println(
                            "Couldn't form url object: " + e.getMessage());
                    return;
                }
                URL[] urls = {url};
                System.out.println(
                        "Executing GetSelectedPlaylistMixId task....");
                (new GetSelectedPlaylistMixId()).execute(urls);

                list_view.setAdapter(null);
                TextView tview = (TextView) findViewById(R.id.track_name);
                tview.setVisibility(View.VISIBLE);
                tview.setText(utility.LOADING_TEXT);
            }
        };
        list_view.setOnItemClickListener(genreClickedHandler);
    }
    private class GetSelectedPlaylistMixId extends AsyncTask<URL, Void, JSONObject> {
        // NOTE: 8tracks calls playlists 'mixes'.
        protected JSONObject doInBackground(URL... params) {
            JSONObject json = null;
            try
            {
                URL url = params[0];
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection = utility.SetUp8tracks(urlConnection);
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                String response = utility.readStream(in);
                json = new JSONObject(response);
                urlConnection.disconnect();
            }
            catch(Exception e)
            {
                System.err.println("FAILURE: "+e.getMessage());
                //e.printStackTrace();
            }
            return json;
        }
        // Handles get-playlists response.
        protected void onPostExecute(JSONObject mixes_json) {
            if (mixes_json == null) {
                System.err.println("Couldn't get any mixes");
                return;
            }
            // Get Ids and Names of mixes.
            final Map<String, String> mix_id_names;
            try {
                JSONArray mixes = mixes_json.getJSONArray("mixes");
                int number_of_mixes = mixes.length();
                mix_id_names = new HashMap<String, String>();
                for (int i = 0; i < number_of_mixes && i < utility.CAP_NUM_MIXES; i++) {
                    JSONObject mix = mixes.getJSONObject(i);
                    mix_id_names.put(mix.getString("name"), mix.getString("id"));
                }
            } catch (Exception e) {
                System.err.println("Malformed mixes json: " + mixes_json.toString());
                return;
            }
            System.out.println(mix_id_names);
            // Displays possible mix names to user.
            final String[] mix_names = mix_id_names.keySet().toArray(new String[0]);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    getApplicationContext(), android.R.layout.simple_list_item_1, mix_names);
            final ListView list_view = (ListView) findViewById(R.id.tracks8_playlists_selection_list);
            list_view.setAdapter(adapter);
            TextView tview = (TextView) findViewById(R.id.track_name);
            tview.setVisibility(View.INVISIBLE);

            // Gets chosen mix with id for streaming.
            AdapterView.OnItemClickListener mixClickedHandler = new AdapterView
                    .OnItemClickListener() {
                public void onItemClick(AdapterView parent, View v, int position, long id) {
                    // User chose this; stream this mix!
                    g_mix_id = mix_id_names.get(mix_names[position]);
                    System.out.println("Chosen id: " + g_mix_id);
                    /*
                    Intent got_mix_id_intent = new Intent();
                    got_mix_id_intent.setAction(utility.MIX_ID_PLAY_TOKEN_ACTION);
                    got_mix_id_intent.putExtra("mix_id", g_mix_id);
                    //g_broadcast_manager.sendBroadcast(got_mix_id_intent);
                    */

                    // SAVE MIX_ID TO DB


                    System.out.println("MIX ID gotten: " + g_mix_id);
                    list_view.setAdapter(null);
                    // Show pause-next-stop buttons.
                    LinearLayout llview = (LinearLayout) findViewById(R.id.track_control_buttons);
                    llview.setVisibility(View.VISIBLE);
                }
            };
            list_view.setOnItemClickListener(mixClickedHandler);
        }
    }


    /**
     * Go back to main menu after cleaning up music service.
     *
     * Called by things like finish()
     */
    public void onDestroy() 
    {
        if (null != m_MusicService)
        {
            m_MusicService.releaseMediaPlayer();
            m_MusicService.stopService(new Intent(this, MusicService.class));
            m_MusicService = null;
            unbindService(m_serviceConn);
        }
        TextView tview = (TextView) findViewById(R.id.track_name);
        tview.setVisibility(View.GONE);


        super.onDestroy();
    }



   @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.tracks8, menu);
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
