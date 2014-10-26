package com.coachksrun;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.InputStream;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import java.io.BufferedReader;
import java.net.URL;
import java.net.HttpURLConnection;
import org.json.JSONObject;
import java.io.IOException;

public class Tracks8Activity extends Activity
{
    public static String DEV_KEY = "afe6602f9dfd8d5552dfa555feda9fab0a0a3643";
    public static String URL_HIP_HOP = "http://8tracks.com/mix_sets/tags:hip_hop:popular.json";
    public static int CAP_NUM_MIXES = 3;
    public static String TEST_MIX_URL = "http://8tracks.com/mixes/14.json?api_key="+DEV_KEY;
    public static String username = "coach_k";
    public static String password = "coach_k";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracks8);
        GetPlaylists();

        try
        {
            String[] params = {TEST_MIX_URL, username, password};
            (new TalkTo8Tracks()).execute(params);
        }
        catch(Exception e)
        {
             System.err.println("FAILURE: "+e.getMessage());
        }
    }

    private void GetPlaylists() {
        // TODO(kristen): Let user choose genre. E.g. mix_%s_url % genre_tag?
        URL url;
        try {
            url = new URL(URL_HIP_HOP);
        }
        catch (Exception e) {
            System.err.println("Couldn't form url object: " + e.getMessage());
            return;
        }
        URL[] urls = {url};
        (new SetupPlaylists()).execute(urls);
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

    private HttpURLConnection SetUp8tracks(HttpURLConnection urlConnection) {
        String user_agent = System.getProperty("http.agent");
        try {
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("User-Agent", user_agent);
            urlConnection.setRequestProperty("X-Api-Version", "2"); // No need user auth.
            urlConnection.setRequestProperty("X-Api-Key", DEV_KEY);
            urlConnection.setDoInput(true);
            return urlConnection;
        }
        catch (Exception e) {
            System.out.println("Couldn't set up url connection: " + e.getMessage());
        }
        return null;
    }

    private class SetupPlaylists extends AsyncTask<URL, Void, JSONObject> {
        // NOTE: 8tracks calls playlists 'mixes'.

        protected JSONObject doInBackground(URL... params) {
            JSONObject json = null;
            try
            {
                URL url = params[0];
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection = SetUp8tracks(urlConnection);
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                String response = readStream(in);

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
                for (int i = 0; i < number_of_mixes && i < CAP_NUM_MIXES; i++) {
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
                }
            };
            list_view.setOnItemClickListener(mixClickedHandler);
        }


    }

    private String readStream(InputStream is) throws IOException
    {

        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(is),1000);
        for (String line = r.readLine(); line != null; line =r.readLine()){
            sb.append(line);
        }
        is.close();
        return sb.toString();
    }

    private class TalkTo8Tracks extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... params) {
            try {
                /*
                 * Testing HttpURLConnection
                 *
                 *
                URL url = new URL(params[0]); // Assumes the URL params is an array of one url
                String user_agent = System.getProperty("http.agent");

                */

                /*
                 * Logging into 8 Track
                 *
                 *
                URL url = new URL("https://8tracks.com/sessions.json?login="+params[1]+"&password="+params[2]+"&api_version=3");
                String user_agent = System.getProperty("http.agent");

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("User-Agent", user_agent);
                urlConnection.setDoInput(true);

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());

                JSONObject reader = new JSONObject(readStream(in));
                JSONObject user = reader.getJSONObject("user");
                String user_token = user.getString("user_token");
                urlConnection.disconnect();
                */

                /*
                 * Obtaining a Play Token
                 *
                 */
                URL url = new URL("http://8tracks.com/sets/new.json?api_key=" + DEV_KEY + "&api_version=3");
                String user_agent = System.getProperty("http.agent");

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("User-Agent", user_agent);
                urlConnection.setDoInput(true);

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());

                JSONObject reader = new JSONObject(readStream(in));
                String play_token = reader.getString("play_token");
                System.out.println("PLAY_TOKEN: " + play_token);
                urlConnection.disconnect();
            } catch (Exception e) {
                System.err.println("FAILURE - Could not send HTTP GET Request: " + e.getMessage());
                e.printStackTrace();
                System.err.println("FAILURE: " + e.getMessage());
                //e.printStackTrace();
            }
            return null;
        }
    }

}


