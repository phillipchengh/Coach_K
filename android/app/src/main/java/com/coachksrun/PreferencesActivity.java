package com.coachksrun;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.coachksrun.Tracks8.utility;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class PreferencesActivity extends Activity {

    private String fb_user_id;
    private String status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);
        fb_user_id = getIntent().getStringExtra("fb_user_id");
        status = getIntent().getStringExtra("status");
        TextView status_textview = (TextView) findViewById(R.id.status_textview);
        status_textview.setText("id: " + fb_user_id + " status: " + status);
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

    private class CreateUser extends AsyncTask<Void, Void, JSONObject> {

        protected JSONObject doInBackground(Void... params) {
            JSONObject result = null;
            try {
                String url_parameters = "user_id=" + fb_user_id + "genre=indie&distance=0";
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
        }
    }
}
