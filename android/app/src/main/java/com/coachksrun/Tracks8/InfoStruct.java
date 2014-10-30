package com.coachksrun.Tracks8;

/**
 * Created by kevin on 10/27/14.
 */
public class InfoStruct
{
    private String m_mixId;
    private AsyncResponse m_mainThread;

    public InfoStruct(String mixId, AsyncResponse mainThread)
    {
        m_mixId = mixId;
        m_mainThread = mainThread;
    }

    public void setMixId(String mixId)
    {
        m_mixId = mixId;
    }

    public void setMainThread(AsyncResponse mainThread)
    {
        m_mainThread = mainThread;
    }

    public String getMixId()
    {
        return m_mixId;
    }

    public AsyncResponse getMainThread()
    {
        return m_mainThread;
    }
}
