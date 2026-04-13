package com.example.tmbdmadproject;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecommendationsFragment extends Fragment {

    private static final String API_KEY  = "801da655375c1e627e8d97311ff30488";
    private static final String BASE_URL = "https://api.themoviedb.org/3/";
    private static final String DB_URL   =
            "https://tmbd-mad-project-default-rtdb.asia-southeast1.firebasedatabase.app/";

    ImageView btnBack;
    TextView tvSubtitle, tvTopGenreLabel;
    RecyclerView rvRecommendations;
    LinearLayout llEmpty;
    int currentPage = 1;
    int totalPages  = 1;
    boolean isLoading = false;
    String currentGenreId = "";
    List<Integer> currentWatchedIds = new ArrayList<>();
    List<Movie> movieList = new ArrayList<>();
    MovieAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_recommendations, container, false);

        btnBack          = view.findViewById(R.id.btnRecommendBack);
        tvSubtitle       = view.findViewById(R.id.tvRecommendSubtitle);
        tvTopGenreLabel  = view.findViewById(R.id.tvTopGenreLabel);
        rvRecommendations = view.findViewById(R.id.rvRecommendations);
        llEmpty          = view.findViewById(R.id.llEmpty);

        rvRecommendations.setLayoutManager(new GridLayoutManager(getContext(), 3));
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
        rvRecommendations.setAdapter(adapter);

        btnBack.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        rvRecommendations.setLayoutManager(layoutManager);

        rvRecommendations.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);

                if (dy <= 0) return; // only trigger scrolling down

                int visibleCount  = layoutManager.getChildCount();
                int totalCount    = layoutManager.getItemCount();
                int firstVisible  = layoutManager.findFirstVisibleItemPosition();

                boolean nearBottom = (firstVisible + visibleCount) >= (totalCount - 6);

                if (nearBottom && !isLoading && currentPage < totalPages
                        && !currentGenreId.isEmpty()) {
                    currentPage++;
                    fetchMoviesByGenre(currentGenreId, currentWatchedIds);
                }
            }
        });

        loadRecommendations();

        return view;
    }

    private void loadRecommendations() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase.getInstance(DB_URL)
                .getReference("watchHistory")
                .child(uid)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (!snapshot.exists()) {
                        showEmpty("Watch some movies first to get recommendations!");
                        return;
                    }

                    // collect watched movie IDs + find top genre
                    List<Integer> watchedIds = new ArrayList<>();
                    Map<String, Integer> genreCount = new HashMap<>();

                    for (DataSnapshot child : snapshot.getChildren()) {
                        Boolean watched = child.child("watched").getValue(Boolean.class);
                        if (watched == null || !watched) continue;

                        Integer movieId = child.child("movieId").getValue(Integer.class);
                        if (movieId != null) watchedIds.add(movieId);
                    }

                    if (watchedIds.isEmpty()) {
                        showEmpty("Watch some movies first to get recommendations!");
                        return;
                    }

                    tvSubtitle.setText("Based on your " + watchedIds.size() + " watched movies");

                    // step 1: fetch genres from watched movies
                    fetchGenresFromWatched(watchedIds, genreCount);
                })
                .addOnFailureListener(e ->
                        showEmpty("Failed to load recommendations")
                );
    }

    private void fetchGenresFromWatched(List<Integer> movieIds,
                                        Map<String, Integer> genreCount) {
        RequestQueue queue = Volley.newRequestQueue(getContext());
        final int[] fetched = {0};
        int total = Math.min(movieIds.size(), 10); // limit to 10 for speed

        for (int i = 0; i < total; i++) {
            int movieId = movieIds.get(i);
            String url = BASE_URL + "movie/" + movieId + "?api_key=" + API_KEY;

            JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                    response -> {
                        try {
                            JSONArray genres = response.getJSONArray("genres");
                            for (int j = 0; j < genres.length(); j++) {
                                JSONObject genre = genres.getJSONObject(j);
                                String name = genre.getString("name");
                                int genreId = genre.getInt("id");
                                String key  = genreId + ":" + name;
                                genreCount.put(key, genreCount.getOrDefault(key, 0) + 1);
                            }
                        } catch (JSONException e) {
                            Log.e("RECOMMEND", e.toString());
                        }

                        fetched[0]++;
                        if (fetched[0] == total) {
                            // find top genre
                            if (genreCount.isEmpty()) {
                                showEmpty("Not enough data to recommend movies.");
                                return;
                            }

                            String topKey = Collections.max(
                                    genreCount.entrySet(),
                                    Map.Entry.comparingByValue()
                            ).getKey();

                            String[] parts   = topKey.split(":");
                            String topGenreId   = parts[0];
                            String topGenreName = parts.length > 1 ? parts[1] : "your top genre";

                            tvTopGenreLabel.setText(
                                    "Top genre: " + topGenreName + " — showing similar picks"
                            );

                            fetchMoviesByGenre(topGenreId, movieIds);
                        }
                    },
                    error -> {
                        fetched[0]++;
                        if (fetched[0] == total && !genreCount.isEmpty()) {
                            String topKey = Collections.max(
                                    genreCount.entrySet(),
                                    Map.Entry.comparingByValue()
                            ).getKey();
                            String[] parts     = topKey.split(":");
                            String topGenreId   = parts[0];
                            String topGenreName = parts.length > 1 ? parts[1] : "your top genre";

                            tvTopGenreLabel.setText(
                                    "Top genre: " + topGenreName + " — showing similar picks"
                            );

                            fetchMoviesByGenre(topGenreId, movieIds);
                        }
                    }
            );
            queue.add(req);
        }
    }

    private void fetchMoviesByGenre(String genreId, List<Integer> watchedIds) {
        if (isLoading) return;
        isLoading = true;

        // save for pagination reuse
        currentGenreId   = genreId;
        currentWatchedIds = watchedIds;

        String url = BASE_URL + "discover/movie"
                + "?api_key=" + API_KEY
                + "&with_genres=" + genreId
                + "&sort_by=vote_average.desc"
                + "&vote_count.gte=500"
                + "&page=" + currentPage; // ← uses currentPage

        RequestQueue queue = Volley.newRequestQueue(getContext());

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        totalPages = response.getInt("total_pages"); // ← save total pages
                        JSONArray results = response.getJSONArray("results");

                        if (currentPage == 1) movieList.clear();

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject movie = results.getJSONObject(i);

                            if (movie.isNull("poster_path")) continue;

                            int id        = movie.getInt("id");
                            String title  = movie.getString("title");
                            String poster = movie.getString("poster_path");
                            double rating = movie.getDouble("vote_average");

                            if (watchedIds.contains(id)) continue;

                            movieList.add(new Movie(id, title, poster, rating));
                        }

                        isLoading = false;

                        if (movieList.isEmpty() && currentPage == 1) {
                            showEmpty("No new recommendations found. Try watching more movies!");
                        } else {
                            rvRecommendations.setVisibility(View.VISIBLE);
                            llEmpty.setVisibility(View.GONE);
                            adapter.notifyDataSetChanged();
                        }

                    } catch (JSONException e) {
                        Log.e("RECOMMEND", e.toString());
                        isLoading = false;
                        if (currentPage == 1) showEmpty("Failed to load recommendations");
                    }
                },
                error -> {
                    Log.e("RECOMMEND", error.toString());
                    isLoading = false;
                    if (currentPage == 1) showEmpty("Failed to load recommendations");
                }
        );

        queue.add(req);
    }

    private void showEmpty(String message) {
        rvRecommendations.setVisibility(View.GONE);
        llEmpty.setVisibility(View.VISIBLE);
        TextView tvEmpty = llEmpty.findViewById(R.id.tvEmpty);
        tvEmpty.setTypeface(ResourcesCompat.getFont(getContext(), R.font.poppins_medium));
        if (tvEmpty != null) tvEmpty.setText(message);
    }
}