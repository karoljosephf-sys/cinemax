package com.example.tmbdmadproject;


import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String API_KEY  = "801da655375c1e627e8d97311ff30488";
    private static final String BASE_URL = "https://api.themoviedb.org/3/";

    RecyclerView rvTrending, rvRecommended, rvPopular, rvNowPlaying, rvUpcoming;
    List<Movie> trendingList    = new ArrayList<>();
    List<Movie> recommendedList = new ArrayList<>();
    List<Movie> popularList = new ArrayList<>();
    List<Movie> nowPlayingList = new ArrayList<>();
    List<Movie> upcomingList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        rvTrending    = view.findViewById(R.id.rvTrending);
        rvRecommended = view.findViewById(R.id.rvRecommended);
        rvPopular = view.findViewById(R.id.rvPopular);
        rvNowPlaying = view.findViewById(R.id.rvNowPlaying);
//        rvUpcoming = view.findViewById(R.id.rvUpcoming);

        rvTrending.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvRecommended.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvPopular.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvNowPlaying.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
//        rvUpcoming.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        loadMovies("trending/movie/week", trendingList, rvTrending);
        loadMovies("movie/top_rated", recommendedList, rvRecommended);
        loadMovies("movie/popular", popularList, rvPopular);
        loadMovies("movie/now_playing", nowPlayingList, rvNowPlaying);
//        loadMovies("movie/upcoming", upcomingList, rvUpcoming);
        setupMoodChips(view);

        return view;
    }
    private void loadMovies(String endpoint, List<Movie> list, RecyclerView recyclerView) {
        String url = BASE_URL + endpoint + "?api_key=" + API_KEY;

        if(endpoint.equals("movie/upcoming")){
           url = BASE_URL + "movie/upcoming?api_key=" + API_KEY + "&region=PH";
        }

        RequestQueue queue = Volley.newRequestQueue(getContext());

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");

                        list.clear(); // important

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject movie = results.getJSONObject(i);

                            int id = movie.getInt("id");
                            String title = movie.getString("title");
                            String posterPath = movie.isNull("poster_path")
                                    ? null
                                    : movie.getString("poster_path");
                            double rating = movie.optDouble("vote_average", 0.0);

                            list.add(new Movie(id, title, posterPath, rating));
                        }

                        recyclerView.setAdapter(
                                new MovieAdapter(getContext(), list, R.layout.item_movie_home, movie -> openMovieDetails(movie))
                        );

                    } catch (JSONException e) {
                        Log.e("HOME_LOAD", e.toString());
                    }
                },
                error -> Log.e("HOME_LOAD", error.toString())
        );

        queue.add(request);
    }
    private void setupMoodChips(View view) {
        TextView moodHappy    = view.findViewById(R.id.moodHappy);
        TextView moodSad      = view.findViewById(R.id.moodSad);
        TextView moodExcited  = view.findViewById(R.id.moodExcited);
        TextView moodChill    = view.findViewById(R.id.moodChill);
        TextView moodScared   = view.findViewById(R.id.moodScared);
        TextView moodRomantic = view.findViewById(R.id.moodRomantic);

        moodHappy.setOnClickListener(v ->
                openMoodResult("Happy", "😄", Constants.MOOD_HAPPY));
        moodSad.setOnClickListener(v ->
                openMoodResult("Sad", "😢", Constants.MOOD_SAD));
        moodExcited.setOnClickListener(v ->
                openMoodResult("Excited", "🤩", Constants.MOOD_EXCITED));
        moodChill.setOnClickListener(v ->
                openMoodResult("Chill", "😌", Constants.MOOD_CHILL));
        moodScared.setOnClickListener(v ->
                openMoodResult("Scared", "😱", Constants.MOOD_SCARED));
        moodRomantic.setOnClickListener(v ->
                openMoodResult("Romantic", "💕", Constants.MOOD_ROMANTIC));
    }

    private void openMoodResult(String mood, String emoji, String genres) {
        Bundle args = new Bundle();
        args.putString("mood",   mood);
        args.putString("emoji",  emoji);
        args.putString("genres", genres);

        MoodResultFragment fragment = new MoodResultFragment();
        fragment.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
    }
    private void openMovieDetails(Movie movie) {

        Bundle bundle = new Bundle();
        bundle.putInt("movie_id", movie.id);
        bundle.putString("title", movie.title);
        bundle.putString("poster", movie.posterPath);
        bundle.putDouble("rating", movie.rating);

        MovieDetailsFragment fragment = new MovieDetailsFragment();
        fragment.setArguments(bundle);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null) // 👈 so back button works
                .commit();
    }
    private void loadTrending() {
        String url = BASE_URL + "trending/movie/week?api_key=" + API_KEY;
        RequestQueue queue = Volley.newRequestQueue(getContext());

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject movie = results.getJSONObject(i);

                            int id            = movie.getInt("id");
                            String title      = movie.getString("title");
                            String posterPath = movie.isNull("poster_path")
                                    ? null
                                    : movie.getString("poster_path");
                            double rating = movie.isNull("vote_average")
                                    ? 0.0
                                    : movie.optDouble("vote_average", 0.0);

                            trendingList.add(new Movie(id, title, posterPath, rating));
                        }

                        rvTrending.setAdapter(new MovieAdapter(getContext(), trendingList, R.layout.item_movie_home, movie -> openMovieDetails(movie)));

                    } catch (JSONException e) {
                        Log.e("HOME_TRENDING", e.toString());
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("HOME_TRENDING", error.toString());
                    Toast.makeText(getContext(), "Failed to load trending", Toast.LENGTH_SHORT).show();
                }
        );

        queue.add(request);
    }

    private void loadRecommended() {
        String url = BASE_URL + "movie/top_rated?api_key=" + API_KEY;
        RequestQueue queue = Volley.newRequestQueue(getContext());

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject movie = results.getJSONObject(i);

                            int id            = movie.getInt("id");
                            String title      = movie.getString("title");
                            String posterPath = movie.isNull("poster_path")
                                    ? null
                                    : movie.getString("poster_path");
                            double rating = movie.isNull("vote_average")
                                    ? 0.0
                                    : movie.optDouble("vote_average", 0.0);

                            recommendedList.add(new Movie(id, title, posterPath, rating));
                        }

                        rvRecommended.setAdapter(new MovieAdapter(getContext(), recommendedList, R.layout.item_movie_home, movie -> openMovieDetails(movie)));

                    } catch (JSONException e) {
                        Log.e("HOME_RECOMMENDED", e.toString());
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("HOME_RECOMMENDED", error.toString());
                    Toast.makeText(getContext(), "Failed to load recommended", Toast.LENGTH_SHORT).show();
                }
        );

        queue.add(request);
    }
}