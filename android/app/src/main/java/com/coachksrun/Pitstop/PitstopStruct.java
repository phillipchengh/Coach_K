package com.coachksrun.Pitstop;

public class PitstopStruct
{
    private String m_name;
    private double m_latitude;
    private double m_longitude;

    public PitstopStruct(String name, double latitude, double longitude)
    {
        m_name = name;
        m_latitude = latitude;
        m_longitude = longitude;
    }

    public String getName()
    {
        return m_name;
    }

    public double getLatitude()
    {
        return m_latitude;
    }

    public double getLongitude()
    {
        return m_longitude;
    }
} 
