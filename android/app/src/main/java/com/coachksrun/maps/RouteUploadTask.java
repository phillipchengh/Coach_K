package com.coachksrun.maps;

import android.os.AsyncTask;

import com.google.android.gms.maps.model.LatLng;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class RouteUploadTask extends AsyncTask<Void, Void, Void> {
    /**
     *  Schema for the routes table:
     *  +---------------+-------+-------+
     *  | FIELD         | TYPE  | KEY   |
     *  +---------------+-------+-------+
     *  | id            | INT   | NTNUL | (Not null)
     *  | seconds       | INT   | NTNUL |
     *  | minutes       | INT   | NTNUL |
     *  | hours         | INT   | NTNUL |
     *  | distance      | FLOAT | NTNUL | (Or string, doesnt matter)
     *  | timestamp     | TEXT  | NTNUL |
     *  | coordinates   | TEXT  | NTNUL |
     *  +---------------+-------+-------+
     */

    // TODO: replace with actual URL
    private static final String UPLOAD_URL = "http://localhost";

    int m_id, m_seconds, m_minutes, m_hours;
    float m_distance;
    ArrayList<LatLng> m_coordinates;

    public RouteUploadTask(int id, int seconds, int minutes, int hours,
                           float distance, ArrayList<LatLng> coords) {
        m_id = id;
        m_seconds = seconds;
        m_minutes = minutes;
        m_hours = hours;
        m_distance = distance;
        m_coordinates = coords;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        DefaultHttpClient client = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(UPLOAD_URL);

        DateTime now = new DateTime();

        try {
            // Build the JSONArray for coordinates
            JSONArray jsonCoordinates = new JSONArray();
            for (LatLng latLng : m_coordinates) {
                JSONObject coordinates = new JSONObject();
                coordinates.put("latitude", latLng.latitude);
                coordinates.put("longitude", latLng.longitude);
                jsonCoordinates.put(coordinates);
            }

            // Send the data
            ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
            postParameters.add(new BasicNameValuePair("id", Integer.toString(m_id)));
            postParameters.add(new BasicNameValuePair("seconds", Integer.toString(m_seconds)));
            postParameters.add(new BasicNameValuePair("minutes", Integer.toString(m_minutes)));
            postParameters.add(new BasicNameValuePair("hours", Integer.toString(m_hours)));
            postParameters.add(new BasicNameValuePair("distance", Float.toString(m_distance)));
            postParameters.add(new BasicNameValuePair("timestamp", Long.toString(now.getMillis())));
            postParameters.add(new BasicNameValuePair("coordinates", jsonCoordinates.toString()));
            httpPost.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));
            client.execute(httpPost);
        } catch (JSONException e) {
            return null;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
