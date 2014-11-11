package com.coachksrun.maps;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.coachksrun.R;

public class MapStatsFragment extends Fragment {

    private TextView mTextView;

    public static MapStatsFragment newInstance() {
        MapStatsFragment fragment = new MapStatsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public MapStatsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        TextView textView = (TextView) inflater.inflate(R.layout.fragment_map_stats, container);
        mTextView = textView;
        return textView;
    }

    public void setSpeed(String speed) {
        mTextView.setText("" + speed);
    }

}
