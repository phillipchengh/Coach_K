package com.coachksrun.Tracks8;

import android.media.MediaPlayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;


public class utility {
    public static String DEV_KEY = "afe6602f9dfd8d5552dfa555feda9fab0a0a3643";
    public static String URL_GENRE = "http://8tracks.com/mix_sets/tags:%s:popular.json";
    public static int CAP_NUM_MIXES = 3;

    public static String MIX_ID_PLAY_TOKEN_ACTION = "com.coachksrun.action.GET_MIX_ID_PLAY_TOKEN";
    public static String SKIP_SONG_ACTION = "com.coachksrun.action.SKIP_SONG";

    public static final String ACTION_PLAY = "com.example.action.PLAY";

    public static HttpURLConnection SetUp8tracks(HttpURLConnection urlConnection)
    {
        String user_agent = System.getProperty("http.agent");

        try
        {
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("User-Agent", user_agent);
            urlConnection.setRequestProperty("X-Api-Version", "2"); // No need user auth.
            urlConnection.setRequestProperty("X-Api-Key", DEV_KEY);
            urlConnection.setDoInput(true);
            return urlConnection;
        }
        catch (Exception e)
        {
            System.out.println("Couldn't set up url connection: " + e.getMessage());
        }

        return null;
    }

    public static String readStream(InputStream is) throws IOException
    {

        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(is),1000);
        for (String line = r.readLine(); line != null; line =r.readLine()){
            sb.append(line);
        }
        is.close();
        return sb.toString();
    }

}
