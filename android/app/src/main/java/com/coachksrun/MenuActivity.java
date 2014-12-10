package com.coachksrun;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.coachksrun.Tracks8.utility;
import com.coachksrun.maps.MapModeSelect;
import com.coachksrun.maps.RouteListActivity;
import com.coachksrun.maps.RouteSelection;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class MenuActivity extends Activity {

    private String fb_user_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        fb_user_id = getIntent().getStringExtra("fb_user_id");
        (new CheckUser()).execute();
    }

    private void updateStatusTextView(String msg) {
        TextView status_textview = (TextView) findViewById(R.id.status_textview);
        status_textview.setText(msg);
    }

    private void initListeners() {
        Button map_button = (Button) findViewById(R.id.map_button);
        map_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MenuActivity.this, RouteSelection.class);
                startActivity(intent);
            }
        });

        Button history_button = (Button) findViewById(R.id.history_button);
        history_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MenuActivity.this, RouteListActivity.class);
                startActivity(intent);
            }
        });

        Button preferences_button = (Button) findViewById(R.id.preferences_button);
        preferences_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MenuActivity.this, PreferencesActivity.class);
                intent.putExtra("fb_user_id", fb_user_id);
                intent.putExtra("status", "returning");
                startActivity(intent);
            }
        });
    }

    private class CheckUser extends AsyncTask<Void, Void, JSONObject> {

        protected JSONObject doInBackground(Void... params) {
            JSONObject student = null;
            try {
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
                System.err.println("User response was null");
                return;
            }
            try {
                String status = student.getString("status");
                if (status.equals("unregistered")) {
                    (new CreateUser()).execute();
                } else if (status.equals("error")) {
                    updateStatusTextView("Error checking user.");
                } else if (status.equals("success")) {
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

    private class CreateUser extends AsyncTask<Void, Void, JSONObject> {

        protected JSONObject doInBackground(Void... params) {
            JSONObject result = null;
            try {
                String url_parameters = "user_id=" + fb_user_id + "&genre=Pop&overall_pace=5.0&total_distance=0";
                String url_string = "https://coach-k-server.herokuapp.com/student";
                URL url = new URL(url_string);

                HttpURLConnection url_connection = (HttpURLConnection) url.openConnection();
                url_connection.setRequestMethod("POST");
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
                System.err.println("User response was null");
                return;
            }
            try {
                String status = student.getString("status");
                if (status.equals("error")) {
                    updateStatusTextView("Error checking user.");
                } else if (status.equals("success")) {
                    initListeners();
                    Intent intent = new Intent(MenuActivity.this, PreferencesActivity.class);
                    intent.putExtra("fb_user_id", fb_user_id);
                    intent.putExtra("status", "new");
                    startActivity(intent);
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
        getMenuInflater().inflate(R.menu.menu, menu);
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
