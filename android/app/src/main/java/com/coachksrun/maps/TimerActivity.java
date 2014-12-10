package com.coachksrun.maps;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.coachksrun.MenuActivity;
import com.coachksrun.R;
import com.coachksrun.Tracks8.MusicPlayer;

public class TimerActivity extends Activity {

    public static class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new TimePickerDialog(getActivity(), this, 0, 0, true);
        }

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            long millis = hourOfDay * 3600000L + minute * 60000L;
            ((TimerActivity) getActivity()).setTime(millis);
        }
    }

    private static final long COUNTDOWN_INTERVAL = 1000;

    private TextView mClock;
    private Button mStartButton;
    private Button mSetButton;
    private CountDownTimer mCountDownTimer;
    private Boolean mStarted = false;

    private long mTime = 0L;

    private MusicPlayer mMusicPlayer = new MusicPlayer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);

        mClock = (TextView) findViewById(R.id.timer_clock);
        setClock();
        mStartButton = (Button) findViewById(R.id.timer_start_button);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTime == 0L)
                    return;

                if (mStarted) {
                    mCountDownTimer.cancel();
                    mStarted = false;
                    updateStartButton();
                } else {
                    mCountDownTimer = new CountDownTimer(mTime, COUNTDOWN_INTERVAL) {

                        @Override
                        public void onTick(long millisUntilFinished) {
                            mTime = millisUntilFinished;
                            setClock();
                        }

                        @Override
                        public void onFinish() {
                            // TODO: Make this play a sound or something
                            mTime = 0L;
                            setClock();
                            mStarted = false;
                            updateStartButton();
                            Toast.makeText(TimerActivity.this, "Done!", Toast.LENGTH_LONG).show();
                        }
                    }.start();
                    mStarted = true;
                    updateStartButton();
                }
            }
        });
        mSetButton = (Button) findViewById(R.id.timer_set_button);
        mSetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment dialog = new TimePickerFragment();
                dialog.show(getFragmentManager(), "timePicker");
                setClock();
            }
        });

        Button mainMenuButton = (Button) findViewById(R.id.main_menu_button);
        mainMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMusicPlayer.stopClicked(v);
                Intent i = new Intent(TimerActivity.this, MenuActivity.class);
                startActivity(i);
            }
        });

        mMusicPlayer.SetupMusicService(this);
        mMusicPlayer.setupMusicPlayerBroadcasts(
                LocalBroadcastManager.getInstance(getApplicationContext()));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_timer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        mMusicPlayer.cleanUp();

        super.onDestroy();
    }

    private void setClock() {
        long hours = mTime / 3600000L;
        long mins = (mTime - hours * 3600000L) / 60000L;
        long seconds = (mTime - hours * 3600000L - mins * 60000L) / 1000L;

        String text = String.format("%02d:%02d:%02d", hours, mins, seconds);
        mClock.setText(text);
    }

    public void setTime(long millis) {
        mTime = millis;
        setClock();
    }

    private void updateStartButton() {
        if (mStarted) {
            mStartButton.setText("Stop");
        } else {
            mStartButton.setText("Start");
        }
    }

    public void musicPauseClicked(View view)
    {
        mMusicPlayer.pauseClicked(view);
    }

    public void musicSkipClicked(View view)
    {
        mMusicPlayer.skipClicked(view);
    }

    public void musicStopClicked(View view)
    {
        mMusicPlayer.stopClicked(view);
    }
}
