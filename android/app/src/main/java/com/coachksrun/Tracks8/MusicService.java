package com.coachksrun.Tracks8;
import com.coachksrun.R;
import com.coachksrun.Tracks8.utility;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.media.AudioManager;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.LinearLayout;
import android.widget.TextView;

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
    private final IBinder m_Binder = new LocalBinder();

    private LocalBroadcastManager m_broadcast_manager = null;
    MusicService_BroadcastReceiver broadcast_manager;

    public void pauseTrack()
    {
        if (null != m_mediaPlayer) {
            if (m_mediaPlayer.isPlaying())
            {
                m_mediaPlayer.pause();
                isPaused = true;
            }
            else
            {
                m_mediaPlayer.start();
                isPaused = false;
            }
        }
    }

    public void skipTrack()
    {
        if (null != m_mediaPlayer) {
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

    private class MusicService_BroadcastReceiver extends BroadcastReceiver
    {
        public void onReceive(Context context, Intent intent)
        {
            m_mediaPlayer.stop();
        }
    }

    public int onStartCommand(Intent intent, int flags, int startId)
    {
        m_intent = intent;
	m_broadcast_manager = LocalBroadcastManager.getInstance(
				getApplicationContext());

        IntentFilter broadcast_mgr_intent_filter = new IntentFilter();
        broadcast_mgr_intent_filter.addAction(utility.MIX_ID_PLAY_TOKEN_ACTION);
        this.registerReceiver(new MusicService_BroadcastReceiver(), broadcast_mgr_intent_filter);

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
                JSONObject set = json.getJSONObject("set");
                JSONObject track = set.getJSONObject("track");
                streamUrl = track.getString("url");
                System.out.println("STREAM URL: " + streamUrl);

		tellActivityTrackName(track.getString("name"));
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
                    JSONObject set = json.getJSONObject("set");
                    JSONObject track = set.getJSONObject("track");
                    String streamUrl = track.getString("url");

		    tellActivityTrackName(track.getString("name"));

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

    private void tellActivityTrackName(String track_name)
    {
	Intent send_trackname_intent = new Intent();
	send_trackname_intent.setAction(utility.TRACK_NAME_ACTION);
	send_trackname_intent.putExtra("track_name", track_name);
	m_broadcast_manager.sendBroadcast(send_trackname_intent);
    }
}