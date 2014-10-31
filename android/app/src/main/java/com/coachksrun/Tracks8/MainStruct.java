package com.coachksrun.Tracks8;

/**
 * Created by kevin on 10/30/14.
 */
public class MainStruct
{
    private String m_mixId = null;
    private String m_playToken = null;
    private AsyncResponse m_mainThread = null;

    public MainStruct(String mixId, String playToken, AsyncResponse mainThread)
    {
        m_mixId = mixId;
        m_playToken = playToken;
        m_mainThread = mainThread;
    }

    public void setPlayToken(String playToken)
    {
        m_playToken = playToken;
    }

    public void setMainThread(AsyncResponse mainThread)
    {
        m_mainThread = mainThread;
    }

    public void setMixId(String mixId)
    {
        m_mixId = mixId;

    }

    public String getPlayToken()
    {
        return m_playToken;
    }

    public AsyncResponse getMainThread()
    {
        return m_mainThread;
    }

    public String getMixId()
    {
        return m_mixId;

    }
}
