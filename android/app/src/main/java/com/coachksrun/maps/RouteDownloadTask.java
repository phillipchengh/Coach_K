package com.coachksrun.maps;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class RouteDownloadTask extends AsyncTask<Void, Void, String> {
    // TODO: replace with actual URL
    private static final String DOWNLOAD_URL = "http://localhost";
    private static final String DOWNLOAD_BROADCAST = "com.coachksrun.maps.download";
    private int m_id;
    private Context m_context;

    public RouteDownloadTask(int id, Context context) {
        m_context = context;
        m_id = id;
    }

    @Override
    protected String doInBackground(Void... params) {
        DefaultHttpClient client = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(DOWNLOAD_URL);
        InputStream inputStream = null;
        String result = "";

        // Set the parameters for the POST request
        try {
            ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
            postParameters.add(new BasicNameValuePair("id", Integer.toString(m_id)));
            httpPost.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // Execute the HTTP request
        try {
            HttpResponse response = client.execute(httpPost);
            HttpEntity entity = response.getEntity();

            inputStream = entity.getContent();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, "UTF-8"), 8);
            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            result = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                if(inputStream != null) inputStream.close();
            } catch (Exception squish) {
                squish.printStackTrace();
            }
        }

        return result;
    }

    @Override
    protected void onPostExecute(final String result) {
        Intent intent = new Intent();
        intent.putExtra("routes", result);
        intent.setAction(DOWNLOAD_BROADCAST);
        m_context.sendBroadcast(intent);
    }
}