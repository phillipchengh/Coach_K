package com.coachksrun;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.net.URL;
import java.net.HttpURLConnection;
import org.json.JSONObject;
import java.io.IOException;

public class Tracks8Activity extends Activity
{
    public static String DEV_KEY = "afe6602f9dfd8d5552dfa555feda9fab0a0a3643";
    public static String TEST_MIX_URL = "http://8tracks.com/mixes/14.json?api_key="+DEV_KEY;
    public static String username = "coach_k";
    public static String password = "coach_k";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracks8);
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
            try
            {
                /*
                 * Testing HttpURLConnection
                 *
                 *
                URL url = new URL(params[0]); // Assumes the URL params is an array of one url
                String user_agent = System.getProperty("http.agent");

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("User-Agent", user_agent);
                urlConnection.setDoInput(true);

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                System.out.println(readStream(in));
                urlConnection.disconnect();
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
                URL url = new URL("http://8tracks.com/sets/new.json?api_key="+DEV_KEY+"&api_version=3");
                String user_agent = System.getProperty("http.agent");

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("User-Agent", user_agent);
                urlConnection.setDoInput(true);

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());

                JSONObject reader = new JSONObject(readStream(in));
                String play_token = reader.getString("play_token");
                System.out.println("PLAY_TOKEN: "+play_token);
                urlConnection.disconnect();
            }
            catch(Exception e)
            {
                System.err.println("FAILURE: "+e.getMessage());
                //e.printStackTrace();
            }
            return null;
        }
    }
}
