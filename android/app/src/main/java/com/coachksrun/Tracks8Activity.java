package com.coachksrun;
import com.coachksrun.Tracks8.AsyncResponse;
import com.coachksrun.Tracks8.MainStruct;
import com.coachksrun.Tracks8.utility;
import com.coachksrun.Tracks8.MusicService;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.InputStream;

import java.util.HashMap;
import java.util.Map;

import java.io.BufferedReader;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.IOException;

public class Tracks8Activity extends Activity implements AsyncResponse
{
    public String g_play_token;
    public String g_mix_id;
    public MediaPlayer g_media_player = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracks8);
        GetPlaylists();
    }

    /**
     * Plays chosen track. On track completion should fetch next track if possible.
     *
     * TODO: Allow PLAY/PAUSE.
     *
     * @param stream_params include the play_token and Mix Id
     */
    public void processFinish(MainStruct stream_params)
    {
        System.out.println("Running processFinish()");

        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra("PLAY_TOKEN", stream_params.getPlayToken());
        intent.putExtra("MIX_ID", stream_params.getMixId());
        intent.setAction(utility.ACTION_PLAY);

        try
        {
            this.startService(intent);
        }
        catch(Exception e)
        {
            System.err.println("Exception in processFinish(): "+e.getMessage());
        }
    }

    private class SetupPlaylists extends AsyncTask<URL, Void, JSONObject> {
        // NOTE: 8tracks calls playlists 'mixes'.

        private AsyncResponse delegate = null;

        public void setDelegate(AsyncResponse mainThread)
        {
            delegate = mainThread;
        }

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
            // TODO(kristen): Change text color.
            final String[] mix_names = mix_id_names.keySet().toArray(new String[0]);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                    getApplicationContext(), android.R.layout.simple_list_item_1, mix_names);
            ListView list_view = (ListView) findViewById(R.id.tracks8_playlists_selection_list);
            list_view.setAdapter(adapter);

            // Gets chosen mix with id for streaming.
            AdapterView.OnItemClickListener mixClickedHandler = new AdapterView
                    .OnItemClickListener() {
                public void onItemClick(AdapterView parent, View v, int position, long id) {
                    // User chose this; stream this mix!
                    String chosen_mix_id = mix_id_names.get(mix_names[position]);
                    System.out.println("Chosen id: " + chosen_mix_id);

                    g_mix_id = chosen_mix_id;

                    System.out.println("SetupPlaylists: " + delegate.toString());

                    MainStruct[] params = {(new MainStruct(chosen_mix_id, null, delegate))};
                    (new TalkTo8Tracks()).execute(params);
                }
            };
            list_view.setOnItemClickListener(mixClickedHandler);
        }
    }

    private void GetPlaylists() {
        // TODO(kristen): Let user choose genre. E.g. mix_%s_url % genre_tag?
        URL url;
        try {
            url = new URL(utility.URL_HIP_HOP);
        }
        catch (Exception e) {
            System.err.println("Couldn't form url object: " + e.getMessage());
            return;
        }
        URL[] urls = {url};
        SetupPlaylists setupPlaylistsTask = new SetupPlaylists();
        setupPlaylistsTask.setDelegate(this);
        setupPlaylistsTask.execute(urls);
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

    private class TalkTo8Tracks extends AsyncTask<MainStruct, Void, MainStruct> {
        public AsyncResponse delegate = null;

        protected MainStruct doInBackground(MainStruct... params) {

            delegate = params[0].getMainThread();
            String play_token = "";

            System.out.println("TalkTo8Tracks: " + delegate.toString());

            try {
                /*
                 * Obtaining a Play Token
                 *
                 */
                URL url = new URL("http://8tracks.com/sets/new.json?api_key=" + utility.DEV_KEY + "&api_version=3");
                String user_agent = System.getProperty("http.agent");

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("User-Agent", user_agent);
                urlConnection.setDoInput(true);

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());

                JSONObject reader = new JSONObject(utility.readStream(in));
                play_token = reader.getString("play_token");
                g_play_token = play_token;
                //System.out.println("PLAY_TOKEN: " + play_token);
                //ret_url = "http://8tracks.com/sets/"+play_token+"/play.json?mix_id="+params[0].getMixId();
                //ret_url = "http://8tracks.com/sets/"+play_token+"/play.json?mix_id="+params[0].getMixId();
                urlConnection.disconnect();
            } catch (Exception e) {
                System.err.println("FAILURE - Could not send HTTP GET Request: " + e.getMessage());
                e.printStackTrace();
                System.err.println("FAILURE: " + e.getMessage());
                //e.printStackTrace();
            }

            MainStruct get_stream_params = new MainStruct(params[0].getMixId(), play_token, delegate);
            //(new PlayStream()).execute(get_stream_params);

            return get_stream_params;
        }

        protected void onPostExecute(MainStruct get_stream_params)
        {
            delegate.processFinish(get_stream_params);
        }
    }
}


