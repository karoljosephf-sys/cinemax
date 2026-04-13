package com.example.tmbdmadproject;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MoodResultFragment extends Fragment {

    RecyclerView rvMoodResults;
    TextView tvMoodTitle;
    ImageView btnBack;
    List<Movie> movieList = new ArrayList<>();
    MovieAdapter adapter;

    String genreIds;
    int currentPage  = 1;
    int totalPages   = 1;
    boolean isLoading = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_mood_result, container, false);

        rvMoodResults = view.findViewById(R.id.rvMoodResults);
        tvMoodTitle   = view.findViewById(R.id.tvMoodTitle);
        btnBack       = view.findViewById(R.id.btnMoodBack);

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        rvMoodResults.setLayoutManager(layoutManager);

        adapter = new MovieAdapter(getContext(), movieList, R.layout.item_movie_search, movie -> {
            Bundle args = new Bundle();
            args.putInt("movie_id",  movie.id);
            args.putString("title",  movie.title);
            args.putString("poster", movie.posterPath);
            args.putDouble("rating", movie.rating);

            MovieDetailsFragment fragment = new MovieDetailsFragment();
            fragment.setArguments(args);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        rvMoodResults.setAdapter(adapter);

        // ── pagination scroll listener ─────────────────────────────────
        rvMoodResults.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);

                // only trigger when scrolling down
                if (dy <= 0) return;

                int visibleItemCount    = layoutManager.getChildCount();
                int totalItemCount      = layoutManager.getItemCount();
                int firstVisibleItem    = layoutManager.findFirstVisibleItemPosition();

                // when user is 6 items from the bottom → load next page
                boolean nearBottom = (firstVisibleItem + visibleItemCount)
                        >= (totalItemCount - 6);

                if (nearBottom && !isLoading && currentPage < totalPages) {
                    currentPage++;
                    loadMoodMovies();
                }
            }
        });

        btnBack.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        if (getArguments() != null) {
            String mood   = getArguments().getString("mood");
            genreIds      = getArguments().getString("genres");
            String emoji  = getArguments().getString("emoji");

            tvMoodTitle.setText(emoji + " " + mood + " Movies");

            loadMoodMovies(); // load page 1
            logMoodToFirebase(mood);
        }

        return view;
    }

    private void loadMoodMovies() {
        if (isLoading) return;
        isLoading = true;

        String url = "https://api.themoviedb.org/3/discover/movie"
                + "?api_key=" + Constants.TMDB_API_KEY
                + "&with_genres=" + genreIds
                + "&sort_by=popularity.desc"
                + "&page=" + currentPage; // ← page param

        RequestQueue queue = Volley.newRequestQueue(getContext());

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        // save total pages on first load
                        totalPages = response.getInt("total_pages");

                        JSONArray results = response.getJSONArray("results");

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject movie = results.getJSONObject(i);

                            if (movie.isNull("poster_path")) continue;

                            int id            = movie.getInt("id");
                            String title      = movie.getString("title");
                            String posterPath = movie.getString("poster_path");
                            double rating     = movie.getDouble("vote_average");

                            movieList.add(new Movie(id, title, posterPath, rating));
                        }

                        adapter.notifyDataSetChanged();

                    } catch (JSONException e) {
                        Log.e("MOOD_RESULT", e.toString());
                        Toast.makeText(getContext(), "Error loading movies", Toast.LENGTH_SHORT).show();
                    }

                    isLoading = false;
                },
                error -> {
                    Log.e("MOOD_RESULT", error.toString());
                    Toast.makeText(getContext(), "Failed to load movies", Toast.LENGTH_SHORT).show();
                    isLoading = false;
                }
        );

        queue.add(request);
    }

    private void logMoodToFirebase(String mood) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        java.util.Map<String, Object> entry = new java.util.HashMap<>();
        entry.put("mood",      mood);
        entry.put("timestamp", System.currentTimeMillis());

        FirebaseDatabase.getInstance(Constants.DB_URL)
                .getReference("moodHistory")
                .child(uid)
                .push()
                .setValue(entry);
    }
}