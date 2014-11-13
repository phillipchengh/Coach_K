package com.coachksrun;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.coachksrun.Tracks8.utility;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;


public class PreferencesActivity extends Activity {

    private String fb_user_id;
    private String status;
    private Spinner genre_spinner;
    private ArrayAdapter<CharSequence> genre_adapter;
    private SeekBar pace_seekbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);
        (new GetUser()).execute();
        fb_user_id = getIntent().getStringExtra("fb_user_id");
        status = getIntent().getStringExtra("status");
        TextView user_textview = (TextView) findViewById(R.id.user_textview);
        user_textview.setText("id: " + fb_user_id + " status: " + status);
        genre_spinner = (Spinner) findViewById(R.id.genre_spinner);
        genre_adapter = ArrayAdapter.createFromResource(this, R.array.genres, android.R.layout.simple_spinner_item);
        genre_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genre_spinner.setAdapter(genre_adapter);
        pace_seekbar = (SeekBar) findViewById(R.id.pace_seekbar);

    }

    // Assume that pace is 0.5-10.0
    private int paceToProgress(float pace) {
        float progress = ((pace - 0.5f) / 9.5f) * 100f;
        return Math.round(progress);
    }

    // Assume that progress is 0-100
    private float progressToPace(int progress) {
        float pace = ((((float) progress) / 100f) * 9.5f) + 0.5f;
        return pace;
    }

    private void updateStatusTextView(String msg) {
        TextView status_textview = (TextView) findViewById(R.id.status_textview);
        status_textview.setText(msg);
    }

    private void updatePaceTextView(float pace) {
        TextView pace_textview = (TextView) findViewById(R.id.pace_textview);
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(2);
        pace_textview.setText("Pace: " + df.format(pace));
    }

    private void initListeners() {
        genre_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        pace_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress;

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                progress = i;
                updatePaceTextView(progressToPace(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        Button save_button = (Button) findViewById(R.id.save_button);
        save_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                updateStatusTextView("Saving user preferences...");
                (new PutUser()).execute();
            }
        });
    }

    private void updatePreferences(JSONArray data) {
        try {
            JSONObject user = data.getJSONObject(0);
            String genre = user.getString("genre");
            String pace_string = user.getString("overall_pace");
            float pace = Float.parseFloat(pace_string);
            ArrayAdapter genre_adapter = (ArrayAdapter) genre_spinner.getAdapter();
            genre_spinner.setSelection(genre_adapter.getPosition(genre));
            pace_seekbar.setProgress(paceToProgress(pace));
            updatePaceTextView(pace);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private class GetUser extends AsyncTask<Void, Void, JSONObject> {

        protected JSONObject doInBackground(Void... params) {
            JSONObject student = null;
            try {
                updateStatusTextView("Getting user preferences...");
                String url_string = "https://coach-k-server.herokuapp.com/student?user_id=" + fb_user_id;
                URL url = new URL(url_string);

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());

                student = new JSONObject(utility.readStream(in));
                urlConnection.disconnect();
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
            return student;
        }

        protected void onPostExecute(JSONObject student) {
            if (student == null) {
                updateStatusTextView("Unable to get server response.");
                return;
            }
            try {
                String status = student.getString("status");
                if (status.equals("error")) {
                    updateStatusTextView("Error getting user.");
                } else if (status.equals("success")) {
                    updateStatusTextView("");
                    JSONArray data = student.getJSONArray("data");
                    updatePreferences(data);
                    initListeners();
                } else {
                    updateStatusTextView("An unknown error occurred.");
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private class PutUser extends AsyncTask<Void, Void, JSONObject> {

        protected JSONObject doInBackground(Void... params) {
            JSONObject result = null;
            try {
                updateStatusTextView("Saving user preferences...");
                String url_parameters = "user_id=" + fb_user_id
                        + "&genre=" + genre_spinner.getSelectedItem()
                        + "&overall_pace=" + progressToPace(pace_seekbar.getProgress())
                        + "&total_distance=0";
                String url_string = "https://coach-k-server.herokuapp.com/student";
                URL url = new URL(url_string);

                HttpURLConnection url_connection = (HttpURLConnection) url.openConnection();
                url_connection.setRequestMethod("PUT");
                url_connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                url_connection.setRequestProperty("charset", "utf-8");
                url_connection.setRequestProperty("Content-Length", Integer.toString(url_parameters.getBytes().length));

                DataOutputStream wr = new DataOutputStream(url_connection.getOutputStream());
                wr.writeBytes(url_parameters);
                wr.flush();
                wr.close();

                InputStream in = new BufferedInputStream(url_connection.getInputStream());

                result = new JSONObject(utility.readStream(in));
                url_connection.disconnect();
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
            return result;
        }

        protected void onPostExecute(JSONObject student) {
            if (student == null) {
                updateStatusTextView("Server error occurred.");
                return;
            }
            try {
                String status = student.getString("status");
                if (status.equals("error")) {
                    updateStatusTextView("Error putting user.");
                } else if (status.equals("success")) {
                    updateStatusTextView("Saved.");
                    JSONArray data = student.getJSONArray("data");
                    updatePreferences(data);
                    initListeners();
                } else {
                    updateStatusTextView("An unknown error occurred.");
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.user, menu);
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
