package com.coachksrun.Tracks8;

import android.os.AsyncTask;
import java.io.BufferedInputStream;
import java.io.OutputStreamWriter;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class preferences
{
    private class insertTask extends AsyncTask<String, Void, Void>    
    {
        protected Void doInBackground(String... params) 
        {
            try
            {
                String userId = params[0];
                String genre = params[1];

                URL url = new URL("http://Coach-k-server.herokuapp.com/student");
                String user_agent = System.getProperty("http.agent");

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("User-Agent", user_agent);
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);

                String urlParameters = "user_id="+userId+"&genre="+genre+"&total_distance=0&overall_pace=0";
                OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
                writer.write(urlParameters);
                writer.flush();

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                String response = utility.readStream(in);
                urlConnection.disconnect();
               
                writer.close();
                System.out.println("Insert response: "+response);
            }
            catch(Exception e)
            {
                System.err.println("Exception inserting new preference into PostGres: "+e.getMessage());
            }
            
            return null;
        }
    };

    private class queryTask extends AsyncTask<String, Void, Void>
    {
        protected Void doInBackground(String... params)
        {
            try
            {
                String userId = params[0];

                URL url = new URL("http://Coach-k-server.herokuapp.com/student?user_id="+userId);
                String user_agent = System.getProperty("http.agent");

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("User-Agent", user_agent);
                urlConnection.setDoInput(true);
                
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                String response = utility.readStream(in);
                urlConnection.disconnect();

                System.out.println("Query response: "+response);
            }
            catch(Exception e)
            {
                System.err.println("Exception querying preferences from PostGres: "+e.getMessage());
            }

            return null;
        }
    }

    public void insertPerferences(String userId, String genre)
    {
        String[] params = {userId, genre};
        (new insertTask()).execute(params);
    }

    public void queryPerferences(String userId)
    {
        String[] params = { userId.replace(" ","%20") };
        (new queryTask()).execute(params);
    }
}
