package com.coachksrun.Tracks8;
import com.coachksrun.Tracks8.utility;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.media.AudioManager;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class MusicService extends Service implements MediaPlayer.OnPreparedListener
{
    String m_playToken;
    String m_mixID;

    MusicService_BroadcastReceiver broadcast_manager;

    private class MusicService_BroadcastReceiver extends BroadcastReceiver
    {

        public void onReceive(Context context, Intent intent)
        {

            utility.mediaPlayer.stop();

        }
    }

    public int onStartCommand(Intent intent, int flags, int startId)
    {
        IntentFilter broadcast_mgr_intent_filter = new IntentFilter();
        broadcast_mgr_intent_filter.addAction(utility.MIX_ID_PLAY_TOKEN_ACTION);
        this.registerReceiver(new MusicService_BroadcastReceiver(), broadcast_mgr_intent_filter);

        m_playToken = intent.getStringExtra("PLAY_TOKEN");
        m_mixID = intent.getStringExtra("MIX_ID");

        ServiceStruct[] params = {(new ServiceStruct(this, intent))};
        (new PlayStream()).execute(params);

        return 0;
    }

    public void playStream(Intent intent, String uri_string)
    {
        if (intent.getAction().equals(utility.ACTION_PLAY)) {
            try
            {
                if ( null == utility.mediaPlayer)
                {
                    utility.mediaPlayer = new MediaPlayer();
                    utility.mediaPlayer.setOnCompletionListener(new EndOfTrackListener(this, intent));
                    utility.mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                }
                else
                {
                    utility.mediaPlayer.reset();
                }

                //Uri myUri = Uri.parse(uri_string);
                utility.mediaPlayer.setOnPreparedListener(this);
                utility.mediaPlayer.setDataSource(uri_string);
                utility.mediaPlayer.prepareAsync(); // prepare async to not block main thread
            }
            catch(Exception e)
            {
                System.err.println("Exception creating music service: "+e.getMessage());
            }
        }
    }

    public IBinder onBind(Intent intent)
    {
        return null;

    }

    public void onPrepared(MediaPlayer player)
    {
        player.start();
    }

    public class EndOfTrackListener implements MediaPlayer.OnCompletionListener
    {
        private MusicService m_delegate = null;
        private Intent m_intent = null;

        public EndOfTrackListener(MusicService service_thread, Intent intent)
        {
            m_delegate = service_thread;
            m_intent = intent;
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
            ServiceStruct[] params = {(new ServiceStruct(m_delegate, m_intent))};
            (new PlayStream()).execute(params);
        }
    }


    private class PlayStream extends AsyncTask<ServiceStruct, Void, JSONObject> {
        public MusicService delegate = null;
        public Intent intent = null;

        protected JSONObject doInBackground(ServiceStruct... params) {
            delegate = params[0].getServiceThread();
            intent = params[0].getIntent();

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
                JSONObject set = json.getJSONObject("set");
                JSONObject track = set.getJSONObject("track");
                streamUrl = track.getString("url");

                System.out.println("STREAM URL: " + streamUrl);
            } catch (Exception e) {
                System.err.println("Malformed STREAM json: " + json.toString());
                return;
            }

            delegate.playStream(intent, streamUrl);
        }
    }
}
