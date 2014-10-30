package com.coachksrun.Tracks8;

import android.content.Intent;

/**
 * Created by kevin on 10/30/14.
 */
public class ServiceStruct
{
    private MusicService m_serviceThread = null;
    private Intent m_intent = null;

    public ServiceStruct(MusicService serviceThread , Intent intent)
    {

        m_serviceThread = serviceThread;
        m_intent = intent;
    }

    public void setServiceThread(MusicService serviceThread)
    {
        m_serviceThread = serviceThread;
    }

    public void setIntent(Intent intent)
    {
        m_intent = intent;
    }

    public MusicService getServiceThread()
    {
        return m_serviceThread;
    }

    public Intent getIntent()
    {
        return m_intent;
    }
}
