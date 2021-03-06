package com.coachksrun.Tracks8;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.coachksrun.R;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class MusicPlayer {

    private MusicService m_MusicService = null;
    private ServiceConnection m_serviceConn = null;

    private RemoteControlClient m_remoteControlClient = null;
    private ComponentName m_LockScreen_Receiver = null;

    private Activity m_callerActivity;
    private LocalBroadcastManager m_callerBroadcastManager = null;

    private ServiceToActivity_BroadcastReceiver m_srv_to_act_broadcast_receiver = null;
    private GotMixIdAndPlayToken_BroadcastReceiver m_playtoken_mixid_broadcast_receiver = null;
    private LockScreenReceiver m_lockscreen_broadcast_receiver = null;

    private PlaylistDbHelper m_dbHelper = null;
    private SQLiteDatabase m_db = null;

    private String m_currentTrackName = "Loading...";

    PowerManager.WakeLock partial_wakelock = null;
    private AudioManager myAudioManager = null;
    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener()
	{
	    public void onAudioFocusChange(int focusChange)
	    {
		if (focusChange == AudioManager.AUDIOFOCUS_LOSS)
		{
		    myAudioManager.unregisterMediaButtonEventReceiver(m_LockScreen_Receiver);
		    myAudioManager.abandonAudioFocus(audioFocusChangeListener);
		    partial_wakelock.release();
		}
	    }
	};



    public void SetupMusicService(Activity callerActivity)
    {
        m_callerActivity = callerActivity;
        m_callerBroadcastManager = LocalBroadcastManager.getInstance(m_callerActivity.getApplicationContext());
        myAudioManager = (AudioManager)m_callerActivity.getSystemService(Context.AUDIO_SERVICE);	

        (new GetPlayToken()).execute();

	setupSQLiteDB();
    }

    public void SetupLockscreenControls()
    {
        /**
         * Set up Lock Screen Controls
         */
	ComponentName media_receiver = new ComponentName(m_callerActivity.getPackageName(), MediaEventReceiver.class.getName());

        Intent mediaButtonIntent = new Intent();
	mediaButtonIntent.setAction(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(media_receiver);
        PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(m_callerActivity.getApplicationContext(), 0, mediaButtonIntent, 0);

	myAudioManager.registerMediaButtonEventReceiver(mediaPendingIntent);

        m_remoteControlClient = new RemoteControlClient(mediaPendingIntent);
        m_remoteControlClient.setTransportControlFlags(
                RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE | 
		RemoteControlClient.FLAG_KEY_MEDIA_NEXT);

        m_remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);

        myAudioManager.registerRemoteControlClient(m_remoteControlClient);

	/**
	 * Request audio focus for this app.
	 */
	myAudioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);

	/**
	 * Partial wake lock so that CPU continues to run when screen's off.
	 */
	PowerManager power_manager = (PowerManager) m_callerActivity.getSystemService(
	    Context.POWER_SERVICE);
	partial_wakelock = power_manager.newWakeLock(
             PowerManager.PARTIAL_WAKE_LOCK, "Tracks8_WakeLock");
	partial_wakelock.acquire();


	/**
	 * Edit lockscreen metadata.
	 */
	SetTrackName();
    }

    /**
     * Plays chosen track. On track completion should fetch next track if possible.
     *
     * TODO: Allow PLAY/PAUSE.
     *
     */
    public void StartMusicService(String play_token, String mix_id)
    {
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
                }

                public void onServiceDisconnected(ComponentName className) {
                    m_MusicService = null;
                }
            };

            m_callerActivity.bindService(intent, m_serviceConn, Context.BIND_AUTO_CREATE);
        }
        catch(Exception e) {
            System.err.println("Exception in processFinish(): " + e.getMessage());
        }
        SetupLockscreenControls();

    }

    private void togglePause()
    {
        m_MusicService.pauseTrack();

	if (m_MusicService.paused())
	{
	    m_remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
	}
	else
	{
	    m_remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
	}
    }

    /**
     * Respond to Pause button click event.
     */
    public void pauseClicked(View view)
    {
	togglePause();
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

    private void SetTrackName()
    {
	// Display track name on screen lock.
	m_remoteControlClient.editMetadata(false)
	    .putString(MediaMetadataRetriever.METADATA_KEY_TITLE,
		       m_currentTrackName)
	    .apply();
    }

    private class ServiceToActivity_BroadcastReceiver extends BroadcastReceiver
    {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            System.out.println(">>>> " + action);
            if (utility.TRACK_NAME_ACTION == action)
            {
                m_currentTrackName = intent.getStringExtra("track_name");
                System.out.println("Got track name:" + m_currentTrackName);
                TextView textview = (TextView) m_callerActivity.findViewById(R.id.track_name);
                textview.setText(m_currentTrackName);

		SetTrackName();
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
	m_callerBroadcastManager.unregisterReceiver(m_lockscreen_broadcast_receiver);
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

	IntentFilter lockscreen_filter = new IntentFilter();
	lockscreen_filter.addAction(Intent.ACTION_MEDIA_BUTTON);
	m_lockscreen_broadcast_receiver = new LockScreenReceiver();
	broadcastManager.registerReceiver(m_lockscreen_broadcast_receiver, lockscreen_filter);
    
    }


    private class GetPlayToken extends AsyncTask<Void, Void, Void> {

        protected Void doInBackground(Void... params) {
	    String play_token = null;
            try {
                URL url = new URL("http://8tracks.com/sets/new.json?api_key=" + utility.DEV_KEY + "&api_version=3");
                String user_agent = System.getProperty("http.agent");

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("User-Agent", user_agent);
                urlConnection.setDoInput(true);

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());

                JSONObject reader = new JSONObject(utility.readStream(in));
                play_token = reader.getString("play_token");
                urlConnection.disconnect();
            } catch (Exception e) {
                System.err.println("FAILURE - Could not send HTTP GET Request: " + e.getMessage());
                e.printStackTrace();
            }

            System.out.println("PLAY TOKEN gotten: " + play_token);

            Intent got_play_token_intent = new Intent();
            got_play_token_intent.setAction(utility.MIX_ID_PLAY_TOKEN_ACTION);
            got_play_token_intent.putExtra("play_token", play_token);
            m_callerBroadcastManager.sendBroadcast(got_play_token_intent);
            return null;
        }
    }

    private class GotMixIdAndPlayToken_BroadcastReceiver extends BroadcastReceiver
    {
        private String play_token = null;
        private String mix_id = null;

        public void onReceive(Context context, Intent intent)
        {
            play_token = intent.getStringExtra("play_token");

            if (null == play_token)
            {
                return;
            }

            // Fetch MIX_ID from DB
            if (null == mix_id)
            {
                mix_id = GetMixIdToPlay();
            }

            // Finally have both play token & mix id, so start music service.
            StartMusicService(play_token, mix_id);
        }
    }

    private String GetMixIdToPlay()
    {
        Cursor c = m_db.query(PlaylistDbHelper.TABLE_NAME, null, null, null, null, null, null);

	String mix_id = null;
        if( c.moveToFirst() )
        {
            System.out.println("SQLiteDB - Mix id "+c.getString(c.getColumnIndexOrThrow(PlaylistDbHelper.COLUMN_NAME_MIXID)));
	        mix_id = c.getString(c.getColumnIndexOrThrow(PlaylistDbHelper.COLUMN_NAME_MIXID));
        }
        else
        {
            System.out.println("SQLiteDB is empty");
	    mix_id = "5130631"; // DEFAULT HIP HOP playlist in case of fail.
        }

	c.close();
	return mix_id;
    }

    public void setupSQLiteDB()
    {
        m_dbHelper = new PlaylistDbHelper(m_callerActivity);
        m_db = m_dbHelper.getWritableDatabase();
    }

    public static class MediaEventReceiver extends BroadcastReceiver
    {
        public void onReceive(Context context, Intent intent)
        {
	    LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
	}
    }

    public class LockScreenReceiver extends BroadcastReceiver
    {
        public void onReceive(Context context, Intent intent)
        {
            if( intent.getAction() != Intent.ACTION_MEDIA_BUTTON ||
		null == m_MusicService )
            {
                return;
            }

            KeyEvent key = intent.getParcelableExtra(intent.EXTRA_KEY_EVENT);

	    // Don't react twice (for down and up actions).
	    if (KeyEvent.ACTION_DOWN != key.getAction())
	    {
		return;
	    }

            switch (key.getKeyCode())
            {
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                {
		    togglePause();
                    break;
                }
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                {
		    m_MusicService.skipTrack();
                    break;
                }
                default:
                {
                    return;
                }
            }
        }
    }
}
