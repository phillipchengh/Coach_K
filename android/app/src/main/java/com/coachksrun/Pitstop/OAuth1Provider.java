package com.coachksrun.Pitstop;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.Token;

/**
 * Generic service provider for OAuth 1.0.
 */
public class OAuth1Provider extends DefaultApi10a 
{

    @Override
    public String getAccessTokenEndpoint() 
    {
	return null;
    }

    @Override
    public String getAuthorizationUrl(Token arg0) 
    {
	return null;
    }

    @Override
    public String getRequestTokenEndpoint() 
    {
	return null;
    }
}