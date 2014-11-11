package com.coachksrun.Tracks8;

import com.coachksrun.R;
import com.coachksrun.Tracks8.MusicService;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by kristen on 11/11/14.
 */
public class MusicPlayer {

    private String m_play_token = null;
    private String m_mix_id = null;
    private MusicService m_MusicService = null;
    private boolean m_bound = false;

    private Activity m_callerActivity;

    private ServiceConnection m_serviceConn = null;

    private LocalBroadcastManager m_callerBroadcastManager = null;

    private ServiceToActivity_BroadcastReceiver m_srv_to_act_broadcast_receiver = null;
    private GotMixIdAndPlayToken_BroadcastReceiver m_playtoken_mixid_broadcast_receiver = null;

    public void SetupMusicService(Activity callerActivity)
    {
        m_callerActivity = callerActivity;
        m_callerBroadcastManager = LocalBroadcastManager.getInstance(m_callerActivity.getApplicationContext());

        (new GetPlayToken()).execute();
    }

    /**
     * Plays chosen track. On track completion should fetch next track if possible.
     *
     * TODO: Allow PLAY/PAUSE.
     *
     */
    public void StartMusicService(String play_token, String mix_id)
    {
        m_play_token = play_token;
        m_mix_id = mix_id;
        Intent intent = new Intent(m_callerActivity, MusicService.class);
        intent.putExtra("PLAY_TOKEN", play_token);
        intent.putExtra("MIX_ID", mix_id);
        intent.setAction(utility.ACTION_PLAY);

        try
        {
            m_callerActivity.startService(intent);
            m_serviceConn = new ServiceConnection() {

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

            m_callerActivity.bindService(intent, m_serviceConn, Context.BIND_AUTO_CREATE);
        }
        catch(Exception e) {
            System.err.println("Exception in processFinish(): " + e.getMessage());
        }

        /*
        ContentValues values = new ContentValues();
        values.put(PlaylistDbHelper.COLUMN_NAME_GENRE, g_genre);
        values.put(PlaylistDbHelper.COLUMN_NAME_MIXID, g_mix_id);

        long newRowId = m_db.insert(PlaylistDbHelper.TABLE_NAME, null, values);

        Cursor c = m_db.query(PlaylistDbHelper.TABLE_NAME, null, null, null, null, null, null, null);

        if( c.moveToFirst() )
        {
            System.out.println("SQLiteDB - Genre: "+c.getString(c.getColumnIndexOrThrow(PlaylistDbHelper.COLUMN_NAME_GENRE))+", MixID: "+c.getInt(c.getColumnIndexOrThrow(PlaylistDbHelper.COLUMN_NAME_MIXID)));
        }
        else
        {
            System.out.println("SQLiteDB is empty");
        }

        while( c.moveToNext() )
        {
            System.out.println("SQLiteDB - Genre: "+c.getString(c.getColumnIndexOrThrow(PlaylistDbHelper.COLUMN_NAME_GENRE))+", MixID: "+c.getInt(c.getColumnIndexOrThrow(PlaylistDbHelper.COLUMN_NAME_MIXID)));
        }
        */
    }

    /**
     * Respond to Pause button click event.
     */
    public void pauseClicked(View view)
    {
        m_MusicService.pauseTrack();
    }

    /**
     * Respond to Skip button click event.
     */
    public void skipClicked(View view)
    {
        m_MusicService.skipTrack();
    }

    /**
     * Respond to Stop button click event.
     */
    public void stopClicked(View view)
    {
        m_callerActivity.finish();
    }

    private class ServiceToActivity_BroadcastReceiver extends BroadcastReceiver
    {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            System.out.println(">>>> " + action);
            if (utility.TRACK_NAME_ACTION == action)
            {
                String track_name = intent.getStringExtra("track_name");
                System.out.println("Got track name:" + track_name);
                TextView textview = (TextView) m_callerActivity.findViewById(R.id.track_name);
                textview.setText(track_name);
                textview.setBackgroundColor(Color.BLACK);
                textview.setTextColor(Color.WHITE);
                textview.setVisibility(View.VISIBLE);
            }
            else if (utility.STOP_SERVICE_ACTION == action)
            {
                m_callerActivity.finish();
            }
            else if (utility.REPORT_ACTION == action)
            {
                try
                {
                    String reporting_url = intent.getStringExtra("report_url");
                    System.out.println("Reporting url: " + reporting_url);
                    URL url = new URL(reporting_url);
                    (new ReportTo8Tracks()).execute(url);
                }
                catch (Exception e)
                {
                    System.err.println("Malformed reporting url");
                }
            }
        }
    }

    private class ReportTo8Tracks extends AsyncTask<URL, Void, Void> {

        protected Void doInBackground(URL... params) {
            try
            {
                URL url = params[0];
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection = utility.SetUp8tracks(urlConnection);
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                String response = utility.readStream(in);
                urlConnection.disconnect();
            }
            catch(Exception e)
            {
                System.err.println("Error reporting to 8tracks: "+e.getMessage());
            }
            return null;
        }
    }

    /**
     * Go back to main menu after cleaning up music service.
     *
     * Called by things like finish()
     */
    public void cleanUp()
    {
        if (null != m_MusicService)
        {
            m_MusicService.releaseMediaPlayer();
            m_MusicService.stopService(new Intent(m_callerActivity, MusicService.class));
            m_MusicService = null;
            m_callerActivity.unbindService(m_serviceConn);
        }

        TextView tview = (TextView) m_callerActivity.findViewById(R.id.track_name);
        tview.setVisibility(View.GONE);

        m_callerBroadcastManager.unregisterReceiver(m_srv_to_act_broadcast_receiver);
        m_callerBroadcastManager.unregisterReceiver(m_playtoken_mixid_broadcast_receiver);
    }

    public void setupMusicPlayerBroadcasts(LocalBroadcastManager broadcastManager)
    {

        IntentFilter got_mixid_playtoken_intent_filter = new IntentFilter();
        got_mixid_playtoken_intent_filter.addAction(utility.MIX_ID_PLAY_TOKEN_ACTION);
        broadcastManager.registerReceiver(
                new GotMixIdAndPlayToken_BroadcastReceiver(), got_mixid_playtoken_intent_filter);

        IntentFilter srv_to_act_filter = new IntentFilter();
        srv_to_act_filter.addAction(utility.TRACK_NAME_ACTION);
        srv_to_act_filter.addAction(utility.REPORT_ACTION);
        srv_to_act_filter.addAction(utility.STOP_SERVICE_ACTION);
        m_srv_to_act_broadcast_receiver = new ServiceToActivity_BroadcastReceiver();
        broadcastManager.registerReceiver(m_srv_to_act_broadcast_receiver, srv_to_act_filter);
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
                m_play_token = reader.getString("play_token");
                urlConnection.disconnect();
            } catch (Exception e) {
                System.err.println("FAILURE - Could not send HTTP GET Request: " + e.getMessage());
                e.printStackTrace();
            }

            System.out.println("PLAY TOKEN gotten: " + m_play_token);



            Intent got_play_token_intent = new Intent();
            got_play_token_intent.setAction(utility.MIX_ID_PLAY_TOKEN_ACTION);
            got_play_token_intent.putExtra("play_token", m_play_token);
            m_callerBroadcastManager.sendBroadcast(got_play_token_intent);
            return null;
        }
    }

    private class GotMixIdAndPlayToken_BroadcastReceiver extends BroadcastReceiver
    {
        private String play_token = null;
        private String mix_id = "5130631";

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


}