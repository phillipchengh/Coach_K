package com.coachksrun;
import com.coachksrun.Tracks8.MusicService;
import com.coachksrun.Tracks8.utility;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.media.MediaPlayer;
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

import java.util.HashMap;
import java.util.Map;

import java.net.URL;
import java.net.HttpURLConnection;


public class Tracks8Activity extends Activity
{
    public String g_play_token = null;
    public String g_mix_id = null;
    private LocalBroadcastManager g_broadcast_manager = null;
    private MusicService m_MusicService = null;
    private boolean m_bound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        g_broadcast_manager = LocalBroadcastManager.getInstance(getApplicationContext());
        IntentFilter got_mixid_playtoken_intent_filter = new IntentFilter();
	got_mixid_playtoken_intent_filter.addAction(utility.MIX_ID_PLAY_TOKEN_ACTION);
        g_broadcast_manager.registerReceiver(
	     new GotMixIdAndPlayToken_BroadcastReceiver(), got_mixid_playtoken_intent_filter);
        IntentFilter got_trackname_intent_filter = new IntentFilter();
        got_trackname_intent_filter.addAction(utility.TRACK_NAME_ACTION);
        g_broadcast_manager.registerReceiver(
                new GotTrackName_BroadcastReceiver(),got_trackname_intent_filter);
        setContentView(R.layout.activity_tracks8);

        // Lets user choose genre, choose playlist (get play_token and mix_id), and start streaming.
        SetupMusicService();
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
		    
		    // 1b) Get play token.
		    System.out.println("Executing GetPlayToken task....");
		    (new GetPlayToken()).execute();

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

                    Intent got_mix_id_intent = new Intent();
                    got_mix_id_intent.setAction(utility.MIX_ID_PLAY_TOKEN_ACTION);
                    got_mix_id_intent.putExtra("mix_id", g_mix_id);
                    g_broadcast_manager.sendBroadcast(got_mix_id_intent);

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


    private class GetPlayToken extends AsyncTask<Void, Void, Void> {

        protected Void doInBackground(Void... params) {
            try {
                URL url = new URL("http://8tracks.com/sets/new.json?api_key=" + utility.DEV_KEY + "&api_version=3");
                String user_agent = System.getProperty("http.agent");

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("User-Agent", user_agent);
                urlConnection.setDoInput(true);

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());

                JSONObject reader = new JSONObject(utility.readStream(in));
                g_play_token = reader.getString("play_token");
                urlConnection.disconnect();
            } catch (Exception e) {
                System.err.println("FAILURE - Could not send HTTP GET Request: " + e.getMessage());
                e.printStackTrace();
            }

            System.out.println("PLAY TOKEN gotten: " + g_play_token);

            Intent got_play_token_intent = new Intent();
            got_play_token_intent.setAction(utility.MIX_ID_PLAY_TOKEN_ACTION);
            got_play_token_intent.putExtra("play_token", g_play_token);
            g_broadcast_manager.sendBroadcast(got_play_token_intent);
            return null;
        }
    }

    private class GotMixIdAndPlayToken_BroadcastReceiver extends BroadcastReceiver
    {
        private String play_token = null;
        private String mix_id = null;

        public void onReceive(Context context, Intent intent)
        {
            String tmp_play_token = intent.getStringExtra("play_token");
            String tmp_mix_id = intent.getStringExtra("mix_id");
            play_token = (null != tmp_play_token) ? tmp_play_token : play_token;
            mix_id = (null != tmp_mix_id) ? tmp_mix_id : mix_id;

            if (null == play_token || null == mix_id)
            {
                return;
            }

            // Finally have both play token and mix id, so can start music service.
            StartMusicService(play_token, mix_id);
        }
    }

    /**
     * Plays chosen track. On track completion should fetch next track if possible.
     *
     * TODO: Allow PLAY/PAUSE.
     *
     */
    public void StartMusicService(String play_token, String mix_id)
    {
        g_play_token = play_token;
        g_mix_id = mix_id;
        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra("PLAY_TOKEN", play_token);
        intent.putExtra("MIX_ID", mix_id);
        intent.setAction(utility.ACTION_PLAY);

        try
        {
            this.startService(intent);
            ServiceConnection serviceConn = new ServiceConnection() {

                public void onServiceConnected(ComponentName className, IBinder binder)
                {
                    MusicService.LocalBinder serviceBinder = (MusicService.LocalBinder) binder;
                    m_MusicService = serviceBinder.getService();
                    m_bound = true;
                }

                public void onServiceDisconnected(ComponentName className) {
                    m_MusicService = null;
                    m_bound = false;
                }
            };

            bindService(intent, serviceConn,Context.BIND_AUTO_CREATE);
        }
        catch(Exception e)
        {
            System.err.println("Exception in processFinish(): "+e.getMessage());
        }
    }

    /**
     * Respond to Pause button click event.
     */
    public void pauseClicked(View view)
    {
        if( m_bound )
        {
            m_MusicService.pauseTrack();
        }
    }

    /**
     * Respond to Skip button click event.
     */
    public void skipClicked(View view)
    {
        if( m_bound )
	{
	    m_MusicService.skipTrack();
	}
    }

    /**
     * Respond to Stop button click event.
     */
    public void stopClicked(View view)
    {
        if( m_bound )
        {
	        finish();
        }
    }

    /**
     * Go back to main menu after cleaning up music service.
     *
     * Called by things like finish()
     */
    public void onDestroy() 
    {
        while( false == m_bound )
        {
        }

	    if (null != m_MusicService)
	    {
            m_MusicService.releaseMediaPlayer();
	        m_MusicService.stopService(new Intent(this, MusicService.class));
	        m_MusicService = null;
        }
	    super.onDestroy();
    }

    private class GotTrackName_BroadcastReceiver extends BroadcastReceiver
    {
        public void onReceive(Context context, Intent intent)
        {
	    String track_name = intent.getStringExtra("track_name");
	    System.out.println("Got track name:" + track_name);
	    TextView textview = (TextView) findViewById(R.id.track_name);
	    textview.setText(track_name);
	    textview.setBackgroundColor(Color.BLACK);
	    textview.setTextColor(Color.WHITE);
	    textview.setVisibility(View.VISIBLE);
	}
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