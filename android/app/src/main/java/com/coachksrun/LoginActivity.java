package com.coachksrun;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.coachksrun.Tracks8.utility;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class LoginActivity extends Activity {

    private GraphUser graph_user;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Session.openActiveSession(this, true, new Session.StatusCallback() {

            @Override
            public void call(Session session, SessionState state, Exception exception) {
                if (session.isOpened()) {
                    Request.newMeRequest(session, new Request.GraphUserCallback() {

                        @Override
                        public void onCompleted(GraphUser user, Response response) {
                            if (user != null) {
                                graph_user = user;
                                (new CheckUser()).execute();
                            }
                        }
                    }).executeAsync();
                }
            }
        });
    }


    private class CheckUser extends AsyncTask<Void, Void, JSONObject> {

        protected JSONObject doInBackground(Void... params) {
            JSONObject student = null;
            try {
                String url_string = "https://coach-k-server.herokuapp.com/student?user_id=" + graph_user.getId();
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
                Intent intent;
                if (status.equals("unregistered")) {
                    intent = new Intent(LoginActivity.this, PreferencesActivity.class);
                    intent.putExtra("fb_user_id", graph_user.getId());
                    intent.putExtra("status", "unregistered");
                    startActivity(intent);
                } else {
                    intent = new Intent(LoginActivity.this, PreferencesActivity.class);
                    intent.putExtra("fb_user_id", graph_user.getId());
                    intent.putExtra("status", "registered");
                    startActivity(intent);
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
        getMenuInflater().inflate(R.menu.login, menu);
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
