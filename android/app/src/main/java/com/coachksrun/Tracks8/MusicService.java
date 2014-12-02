package com.coachksrun.Tracks8;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class MusicService extends Service implements MediaPlayer.OnPreparedListener
{
    private String m_playToken = null;
    private String m_mixID = null;
    private Intent m_intent = null;
    private MediaPlayer m_mediaPlayer = null;
    private boolean isPaused = false;
    private boolean skipping = false;
    private final IBinder m_Binder = new LocalBinder();

    private LocalBroadcastManager m_broadcast_manager = null;

    public void pauseTrack()
    {
        if (null != m_mediaPlayer) {
	    System.out.println("everywHere");

            if (m_mediaPlayer.isPlaying())
            {
		System.out.println("Here");
                m_mediaPlayer.pause();
                isPaused = true;
            }
            else
            {
		System.out.println("THere");
                m_mediaPlayer.start();
                isPaused = false;
            }
        }
    }

    public boolean paused()
    {
        return isPaused;
    }

    public void skipTrack()
    {
        if (null != m_mediaPlayer)
        {
            if ( m_mediaPlayer.isPlaying())
            {
                (new SkipTrack_Task()).execute();
            }
            else
            {
                System.err.println("Skip Track Error: Either MediaPlayer is not playing or MusicService has NOT started");
            }
        }
    }


    public void skipTrack_old()
    {
        if (null != m_mediaPlayer)
        {
            if( skipping )
            {
                return;
            }
            else
            {
                skipping = true;
            }

            if ( m_mediaPlayer.isPlaying() || isPaused )
            {
                (new SkipTrack_Task()).execute();
            }
            else
            {
                System.err.println("Skip Track Error: Either MediaPlayer is not playing or MusicService has NOT started");
            }
        }
    }


    public void releaseMediaPlayer()
    {
        if (null != m_mediaPlayer) {
            m_mediaPlayer.release();
            m_mediaPlayer = null;
        }
    }

    @Override
        public void onDestroy()
        {
            m_mixID = null;
            m_playToken = null;
            m_intent = null;
            releaseMediaPlayer();
        }

    public int onStartCommand(Intent intent, int flags, int startId)
    {
        m_intent = intent;
        m_broadcast_manager = LocalBroadcastManager.getInstance(
                getApplicationContext());

        m_playToken = intent.getStringExtra("PLAY_TOKEN");
        m_mixID = intent.getStringExtra("MIX_ID");

        (new PlayStream()).execute(this);

        return 0;
    }

    public void playStream(String uri_string)
    {
        if (m_intent.getAction().equals(utility.ACTION_PLAY)) {
            try
            {
                if ( null == m_mediaPlayer)
                {
                    m_mediaPlayer = new MediaPlayer();
                    m_mediaPlayer.setOnCompletionListener(new EndOfTrackListener(this));
                    m_mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                }
                else
                {
                    m_mediaPlayer.reset();
                }

                //Uri myUri = Uri.parse(uri_string);
                m_mediaPlayer.setOnPreparedListener(this);
                m_mediaPlayer.setDataSource(uri_string);
                m_mediaPlayer.prepareAsync(); // prepare async to not block main thread
            }
            catch(Exception e)
            {
                System.err.println("Exception creating music service: "+e.getMessage());
            }
        }
    }

    public class LocalBinder extends Binder
    {
        public MusicService getService()
        {
            return MusicService.this;
        }
    }

    public IBinder onBind(Intent intent)
    {
        return m_Binder;
    }

    public void onPrepared(MediaPlayer player)
    {
        player.start();
        if( skipping )
        {
            skipping = false;
        }
    }

    public class EndOfTrackListener implements MediaPlayer.OnCompletionListener
    {
        private MusicService m_delegate = null;

        public EndOfTrackListener(MusicService service_thread)
        {
            m_delegate = service_thread;
        }

        /**
         * Current track has finished playing. Ask for next one.
         *
         * TODO: mix might be finished. Want to ask for next mix?
         *
         * @param mp
         */
        public void onCompletion(MediaPlayer mp)
        {
            // Asks for next track (a.k.a. stream).
            // OR TODO: some async task gets tracks ahead of time and pushes onto a queue.
            (new PlayStream()).execute(m_delegate);
        }
    }

    private class PlayStream extends AsyncTask<MusicService, Void, JSONObject> {
        public MusicService delegate = null;

        protected JSONObject doInBackground(MusicService... params) {
            delegate = params[0];

            JSONObject json = null;
            try {
                URL url = new URL(String.format("http://8tracks.com/sets/%s/next.json?mix_id=%s", m_playToken, m_mixID));
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection = utility.SetUp8tracks(urlConnection);
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                String response = utility.readStream(in);

                json = new JSONObject(response);
                urlConnection.disconnect();
            } catch (Exception e) {
                System.err.println("FAILURE: " + e.getMessage());
                //e.printStackTrace();
            }

            System.out.println("Mix Json object from 8tracks: " + json.toString());

            return json;
        }

        protected void onPostExecute(JSONObject json) {
            if (json == null) {
                System.err.println("Couldn't get stream");
                return;
            }

            // Get Ids and Names of mixes.
            String streamUrl = null;
            try {
                streamUrl = handleTrackResponseAndGetStreamUrl(json);
            } catch (Exception e) {
                System.err.println("Malformed STREAM json: " + json.toString());
                return;
            }

            delegate.playStream(streamUrl);
        }

    }

    private class SkipTrack_Task extends AsyncTask<Void, Void, String>
    {
        private String skip_url = "http://8tracks.com/sets/111696185/skip.json?mix_id=14";
        protected String doInBackground(Void... param)
        {
            String response = null;

            if( null == m_playToken || null == m_mixID )
            {
                System.err.println("g_variables not set");
                return null;
            }

            String skip_url="http://8tracks.com/sets/"+m_playToken+"/skip.json?mix_id="+m_mixID;
            try
            {
                URL url = new URL(skip_url);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection = utility.SetUp8tracks(urlConnection);
                urlConnection.setRequestMethod("POST");

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                response = utility.readStream(in);
                System.out.println(response);
                urlConnection.disconnect();
            } catch (Exception e) {
                System.err.println("FAILURE - AsyncTask for skipping track: " + e.getMessage());
                e.printStackTrace();
            }

            return response;
        }

        protected void onPostExecute(String response)
        {
            if( null == response )
            {
                System.out.println("Did not get Skip Track response");
                return;
            }

            System.out.println("Response is :"+response);

            try
            {
                JSONObject json = new JSONObject(response);

                String status = json.getString("status");

                if( status.equals("200 OK") )
                {
                    String streamUrl = handleTrackResponseAndGetStreamUrl(json);
                    playStream(streamUrl);
                }
                else {
                    System.err.println("SKIP REQUEST FAILED: " + status);
                }
            }
            catch(Exception e)
            {
                System.err.println("Exception parsing JSON");
            }
        }
    }


    /**
     * Get track streaming url and tell activity track name.
     * If last track, clean up music service.
     */
    private String handleTrackResponseAndGetStreamUrl(JSONObject json)
    {
        String streamUrl = null;
        try
        {
            JSONObject set = json.getJSONObject("set");
            boolean at_end = set.getBoolean("at_end");
            if (at_end) 
            {
                Intent stop_service_intent = new Intent();
                stop_service_intent.setAction(utility.STOP_SERVICE_ACTION);
                m_broadcast_manager.sendBroadcast(stop_service_intent);
            }
            else
            {
                JSONObject track = set.getJSONObject("track");
                streamUrl = track.getString("url");
                System.out.println("STREAM URL: " + streamUrl);

                tellActivityTrackName(track.getString("name"));

                reportTo8Tracks(track.getString("id"));
            }
        }
        catch(Exception e)
        {
            System.err.println("Exception parsing JSON getting stream url");
        }

        return streamUrl;
    }

    private void tellActivityTrackName(String track_name)
    {
        Intent send_trackname_intent = new Intent();
        send_trackname_intent.setAction(utility.TRACK_NAME_ACTION);
        send_trackname_intent.putExtra("track_name", track_name);
        m_broadcast_manager.sendBroadcast(send_trackname_intent);
    }

    private void reportTo8Tracks(String track_id)
    {
        String url = String.format("http://8tracks.com/sets/%s/report.json?track_id=%s&mix_id=%s", m_playToken, track_id, m_mixID);
        Intent reporting_intent = new Intent();
        reporting_intent.setAction(utility.REPORT_ACTION);
        reporting_intent.putExtra("report_url", url);
        m_broadcast_manager.sendBroadcast(reporting_intent);
    }
}
