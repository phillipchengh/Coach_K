package com.coachksrun.Pitstop;

import com.coachksrun.Pitstop.OAuth1Provider;

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

    private final String YELP_SEARCH_LIMIT = "5";
    private final int BUSINESS_LIMIT = Integer.parseInt(YELP_SEARCH_LIMIT);
    private final String YELP_SEARCH_TERM = "juice";
    private final String YELP_SEARCH_PATH = "/v2/search";
    private final String YELP_API_HOST = "api.yelp.com";

    private OAuthService m_oAuthService = null;
    private Token m_oAuthToken = null;

    private void SetupYelper()
    {
	ServiceBuilder builder = new ServiceBuilder();
	builder.provider(OAuth1Provider.class);
	builder.apiKey(YELP_KEY);
	builder.apiSecret(YELP_SECRET);
	m_oAuthService = builder.build();
        m_oAuthToken = new Token(YELP_TOKEN, YELP_TOKEN_SECRET);
    }

    private void GetPitstops(String location)
    {
	String[] location_arg = {location};
	(new AskYelpTask()).execute(location_arg);
    }

    private class AskYelpTask extends AsyncTask<String, Void, Void>
    {
	protected Void doInBackground(String... locations)
	{
	    String location = locations[0];

	    // Build OAuth request.
	    OAuthRequest request = new OAuthRequest(
		 Verb.GET, "http://" + YELP_API_HOST + YELP_SEARCH_PATH);
	    request.addQuerystringParameter("term", YELP_SEARCH_TERM);
	    request.addQuerystringParameter("location", location);
	    request.addQuerystringParameter("limit", YELP_SEARCH_LIMIT);
	    
	    // Query Yelp for pitstops.
	    m_oAuthService.signRequest(m_oAuthToken, request);
	    
	    Response response = request.send();
	    String responseBody = response.getBody();

	    try
	    {
		JSONObject json = new JSONObject(responseBody);
		
		if (null == json)
		    return null;

		JSONArray businesses = json.getJSONArray("businesses");
		int numBusinesses = json.getInt("total");
		if (numBusinesses > BUSINESS_LIMIT)
		{
		    numBusinesses = BUSINESS_LIMIT;
		}
		String[] businessNames = new String[numBusinesses];
		for (int i = 0; i < numBusinesses; i++)
		{
		    JSONObject business = businesses.getJSONObject(i);
		    businessNames[i] = business.getString("name");
		}
		System.out.println("First FIVE BUSINESSES: ");
		for (String name: businessNames)
		{
		    System.out.println(name);
		}
	    }
	    catch (JSONException e)
	    {
		System.err.println("Malformed YElp JSON response: " + responseBody);
	    }
	    return null;
	}
    }

    public void DisplayPitstopsOnMap()
    {
	SetupYelper();
	GetPitstops("San francisco");
    }

}