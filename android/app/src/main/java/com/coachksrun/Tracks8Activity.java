package com.coachksrun;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.HttpURLConnection;
import java.lang.StringBuffer;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

public class Tracks8Activity extends Activity
{
    public static String DEV_KEY = "afe6602f9dfd8d5552dfa555feda9fab0a0a3643";
    public static String TEST_MIX_URL = "http://8tracks.com/mixes/14.json?api_key="+DEV_KEY;

    private String readStream(InputStream is) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            int i = is.read();
            while (i != -1) {
                bo.write(i);
                i = is.read();
            }
            return bo.toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracks8);
        try
        {
            URL url = new URL(TEST_MIX_URL);
            URL[] urlArr = {url};
            (new TalkTo8Tracks()).execute(urlArr);


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

    private class TalkTo8Tracks extends AsyncTask<URL, Void, Void> {
        protected Void doInBackground(URL... urlArr) {
            try
            {
                URL url = urlArr[0]; // Assumes the URL params is an array of one url
                String user_agent = System.getProperty("http.agent");

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("User-Agent", user_agent);
                urlConnection.setDoInput(true);

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                System.out.println(readStream(in));
                urlConnection.disconnect();
            }
            catch(Exception e)
            {
                System.err.println("FAILURE - Could not send HTTP GET Request: "+e.getMessage());
                e.printStackTrace();
            }
            return null;
        }
    }
}
