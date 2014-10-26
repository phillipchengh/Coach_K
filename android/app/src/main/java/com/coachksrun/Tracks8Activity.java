package com.coachksrun;

import android.app.Activity;
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

            String user_agent = System.getProperty("http.agent");
            /*
            URL url = new URL(TEST_MIX_URL);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", user_agent);
            System.out.println("Connection: "+conn.toString());
            int responseCode = conn.getResponseCode();

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            StringBuffer response = new StringBuffer();

            String inputLine = in.readLine();

            while( null != inputLine )
            {
                response.append(inputLine);
                inputLine = in.readLine();
            }

            in.close();

            //System.out.println(response.toString());

            TextView textView = new TextView(this);
            textView.setTextSize(40);
            textView.setText(response.toString());
            setContentView(textView);
            */

            URL url = new URL("http://www.android.com/");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("User-Agent", user_agent);
            urlConnection.setDoInput(true);
            int responseCode = urlConnection.getResponseCode();
            System.out.println("Response Code: "+responseCode);

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            System.out.println(readStream(in));
            urlConnection.disconnect();
        }
        catch(Exception e)
        {
            System.err.println("FAILURE - Could not send HTTP GET Request: "+e.getMessage());
            e.printStackTrace();
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
