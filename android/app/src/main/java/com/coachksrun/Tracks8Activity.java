package com.coachksrun;

import android.app.Activity;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.coachksrun.Tracks8.PlaylistDbHelper;
import com.coachksrun.Tracks8.utility;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


public class Tracks8Activity extends Activity
{
    private String m_mixId = null;

    private PlaylistDbHelper m_dbHelper = null;
    private SQLiteDatabase m_db = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tracks8);

        // Lets user choose genre, get playlist (mix_id), and start streaming.
	ChooseGenreAndGetMixId();
        setupSQLiteDB();
    }

    public void setupSQLiteDB()
    {
        m_dbHelper = new PlaylistDbHelper(this);
        m_db = m_dbHelper.getWritableDatabase();
    }

    /**
     * Displays genres.
     * Once user picks genre, fetch first mix id.
     * Store in db so that DuringRun activity can access it.
     */
    private void ChooseGenreAndGetMixId() {
        final Map<String, String> genre_name_to_tag = new HashMap<String, String>();
        genre_name_to_tag.put("Hip hop", "hip_hop");
        genre_name_to_tag.put("Electronic", "electronic");
        genre_name_to_tag.put("Workout", "workout");
        genre_name_to_tag.put("Rock", "rock");
        final String[] genre_tags = genre_name_to_tag.keySet().toArray(new String[0]);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getApplicationContext(), android.R.layout.simple_list_item_1, genre_tags);
        final ListView list_view = (ListView) findViewById(
                R.id.tracks8_playlists_selection_list);
        list_view.setAdapter(adapter);
        // Gets chosen genre tag.
        AdapterView.OnItemClickListener genreClickedHandler = new AdapterView.OnItemClickListener() {
            public void onItemClick(
                    AdapterView parent, View v, int position, long id) {
                // 1a) Get mix id.
                // User chose this; stream this mix!
                String chosen_tag = genre_name_to_tag.get(
                        genre_tags[position]);
                System.out.println("Chosen genre tag: " + chosen_tag);
                String genre_url = String.format(
                        utility.URL_GENRE, chosen_tag);

                URL url;
                try
                {
                    url = new URL(genre_url);
                }
                catch (Exception e)
                {
                    System.err.println(
                            "Couldn't form url object: " + e.getMessage());
                    return;
                }
                URL[] urls = {url};
                System.out.println(
                        "Executing GetSelectedPlaylistMixId task....");
                (new GetSelectedPlaylistMixId()).execute(urls);

                list_view.setAdapter(null);
                TextView tview = (TextView) findViewById(R.id.track_name);
                tview.setVisibility(View.VISIBLE);
                tview.setText(utility.LOADING_TEXT);
            }
        };
        list_view.setOnItemClickListener(genreClickedHandler);
    }
    private class GetSelectedPlaylistMixId extends AsyncTask<URL, Void, JSONObject> {
        // NOTE: 8tracks calls playlists 'mixes'.
        protected JSONObject doInBackground(URL... params) {
            JSONObject json = null;
            try
            {
                URL url = params[0];
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection = utility.SetUp8tracks(urlConnection);
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                String response = utility.readStream(in);
                json = new JSONObject(response);
                urlConnection.disconnect();
            }
            catch(Exception e)
            {
                System.err.println("FAILURE: "+e.getMessage());
                //e.printStackTrace();
            }
            return json;
        }
        // Handles get-playlists response.
        protected void onPostExecute(JSONObject mixes_json) {
            if (mixes_json == null) {
                System.err.println("Couldn't get any mixes");
                return;
            }

            try {
                JSONArray mixes = mixes_json.getJSONArray("mixes");
		JSONObject first_mix = mixes.getJSONObject(0);
		m_mixId = first_mix.getString("id");

		// Rewrite mix id saved in db.
		m_db.delete(PlaylistDbHelper.TABLE_NAME, null, null);

		ContentValues values = new ContentValues();
		values.put(PlaylistDbHelper.COLUMN_NAME_MIXID, m_mixId);
		m_db.insert(PlaylistDbHelper.TABLE_NAME, null, values);

		finish();

            } catch (Exception e) {
                System.err.println("Malformed mixes json: " + mixes_json.toString());
                return;
            }
        }
    }

   @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.tracks8, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
