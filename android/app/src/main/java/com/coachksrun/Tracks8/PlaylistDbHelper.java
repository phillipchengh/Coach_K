package com.coachksrun.Tracks8;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by kevin on 11/8/14.
 */
public class PlaylistDbHelper extends SQLiteOpenHelper {

    public static final String TABLE_NAME = "Favorite_Playlists";
    public static final String COLUMN_NAME_MIXID = "MixId";

    public static final String SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS "+TABLE_NAME+"("+COLUMN_NAME_MIXID+" STRING, PRIMARY KEY("+COLUMN_NAME_MIXID+"));";
    public static final String SQL_DELETE_TABLE = "DROP TABLE IF EXISTS "+TABLE_NAME+";";

    public static final int DATABASE_VERSION = 1;
    public static final String SCHEMA = "Music";

    public PlaylistDbHelper(Context context) {
        super(context, SCHEMA, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL(SQL_CREATE_TABLE);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        db.execSQL(SQL_DELETE_TABLE);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        onUpgrade(db, oldVersion, newVersion);
    }
}
