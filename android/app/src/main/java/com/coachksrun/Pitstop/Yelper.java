package com.coachksrun.Pitstop;

import com.coachksrun.Pitstop.OAuth1Provider;
import com.coachksrun.Pitstop.PitstopStruct;
import com.coachksrun.maps.MapsActivity;

import android.app.Activity;
import android.os.AsyncTask;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;


public class Yelper
{
    private final String YELP_KEY = "2MGYz7clZgaYz693e7PnKA";
    private final String YELP_SECRET = "s0wN1BZUq4zfQtp3QArHnm_MsHY";
    private final String YELP_TOKEN = "208_YKa1H_GNCFsPIOREdKdrPFsH5L4c";
    private final String YELP_TOKEN_SECRET = "Tq9xG1kA05aGWRuvKq9Pboejx_Q";

    private static Yelper m_instance = null;

    private final String YELP_SEARCH_LIMIT = "5";
    private final int BUSINESS_LIMIT = Integer.parseInt(YELP_SEARCH_LIMIT);
    private final String YELP_SEARCH_TERM = "juice";
    private final String YELP_SEARCH_PATH = "/v2/search";
    private final String YELP_API_HOST = "api.yelp.com";

    private OAuthService m_oAuthService = null;
    private Token m_oAuthToken = null;

    private Yelper()
    {
        ServiceBuilder builder = new ServiceBuilder();
        builder.provider(OAuth1Provider.class);
        builder.apiKey(YELP_KEY);
        builder.apiSecret(YELP_SECRET);
        m_oAuthService = builder.build();
        m_oAuthToken = new Token(YELP_TOKEN, YELP_TOKEN_SECRET);
    }

    public static Yelper getInstance()
    {
        if( null == m_instance )
        {
            m_instance = new Yelper();
        }

        return m_instance;
    }

    public void getPitstops(double latitude, double longitude)
    {
        Double[] location_arg = {new Double(latitude), new Double(longitude)};
        (new AskYelpTask()).execute(location_arg);
    }

    private class AskYelpTask extends AsyncTask<Double, Void, String>
    {
        private double m_latitude;
        private double m_longitude;

        protected String doInBackground(Double... locations)
        {
            m_latitude = locations[0].doubleValue();
            m_longitude = locations[1].doubleValue();

            // Build OAuth request.
            OAuthRequest request = new OAuthRequest(Verb.GET, "http://" + YELP_API_HOST + YELP_SEARCH_PATH);
            request.addQuerystringParameter("term", YELP_SEARCH_TERM);
            request.addQuerystringParameter("ll", m_latitude + "," + m_longitude);
            request.addQuerystringParameter("limit", YELP_SEARCH_LIMIT);

            // Query Yelp for pitstops.
            m_oAuthService.signRequest(m_oAuthToken, request);

            Response response = request.send();
            String responseBody = response.getBody();

            return responseBody;
        }

        protected void onPostExecute(String responseBody) 
        {
            try
            {
                JSONObject json = new JSONObject(responseBody);

                if (null == json)
                {
                    return;
                }

                JSONArray businesses = json.getJSONArray("businesses");
                int numBusinesses = json.getInt("total");

                if (numBusinesses > BUSINESS_LIMIT)
                {
                    numBusinesses = BUSINESS_LIMIT;
                }

                PitstopStruct[] pitstopArr = new PitstopStruct[numBusinesses];
                
                for (int i = 0; i < numBusinesses; i++)
                {
                    JSONObject business = businesses.getJSONObject(i);
                    JSONObject location = business.getJSONObject("location");
                    JSONObject coordinates = location.getJSONObject("coordinate");
                    pitstopArr[i] = new PitstopStruct(business.getString("name"), coordinates.getDouble("latitude"), coordinates.getDouble("longitude"));
                }
                
                MapsActivity.displayPitstops(pitstopArr, numBusinesses);
            }
            catch (JSONException e)
            {
                System.err.println("Malformed Yelp JSON response: " + responseBody);
            }
        }
    }
}
