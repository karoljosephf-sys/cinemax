package com.example.tmbdmadproject;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsFragment extends Fragment {

    private static final String API_KEY = "801da655375c1e627e8d97311ff30488";
    private static final String BASE_URL = "https://api.themoviedb.org/3/";
    private static final String DB_URL =
            "https://tmbd-mad-project-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private boolean isLoading = false;
    TextView tvStatsSubtitle, tvWatchedCount, tvAvgRating,
            tvReviewCount, tvTopGenre, tvTopMood,
            tvHighestRated, tvLastWatched;
    LinearLayout llRatingBreakdown, llRecentActivity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        tvStatsSubtitle   = view.findViewById(R.id.tvStatsSubtitle);
        tvWatchedCount    = view.findViewById(R.id.tvWatchedCount);
        tvAvgRating       = view.findViewById(R.id.tvAvgRating);
        tvReviewCount     = view.findViewById(R.id.tvReviewCount);
        tvTopGenre        = view.findViewById(R.id.tvTopGenre);
        tvTopMood         = view.findViewById(R.id.tvTopMood);
        tvHighestRated    = view.findViewById(R.id.tvHighestRated);
        tvLastWatched     = view.findViewById(R.id.tvLastWatched);
        llRatingBreakdown = view.findViewById(R.id.llRatingBreakdown);
        llRecentActivity  = view.findViewById(R.id.llRecentActivity);

        loadStats();

        return view;
    }
    @Override
    public void onResume() {
        super.onResume();
        if (!isLoading) loadStats();
    }
    private void loadStats() {
        if (isLoading) return;
        isLoading = true;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase.getInstance(DB_URL)
                .getReference("watchHistory")
                .child(uid)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if (!snapshot.exists()) {
                        tvStatsSubtitle.setText("No movies watched yet!");
                        return;
                    }

                    // ── collect data ───────────────────────────
                    List<DataSnapshot> watchedMovies = new ArrayList<>();
                    int totalRating  = 0;
                    int ratedCount   = 0;
                    int[] starCounts = {0, 0, 0, 0, 0}; // index 0=1star ... 4=5star

                    String highestRatedTitle = "—";
                    int highestRating = 0;
                    String lastWatchedTitle = "—";
                    long lastTimestamp = 0;

                    // for genre tracking
                    List<Integer> movieIdsToFetch = new ArrayList<>();

                    for (DataSnapshot child : snapshot.getChildren()) {
                        Boolean watched = child.child("watched").getValue(Boolean.class);
                        if (watched == null || !watched) continue;

                        watchedMovies.add(child);

                        String title     = child.child("title").getValue(String.class);
                        Integer rating   = child.child("rating").getValue(Integer.class);
                        Long timestamp   = child.child("timestamp").getValue(Long.class);
                        Integer movieId  = child.child("movieId").getValue(Integer.class);

                        if (movieId != null) movieIdsToFetch.add(movieId);

                        // rating stats
                        if (rating != null && rating > 0) {
                            totalRating += rating;
                            ratedCount++;
                            starCounts[rating - 1]++;

                            if (rating > highestRating && title != null) {
                                highestRating = rating;
                                highestRatedTitle = title;
                            }
                        }

                        // last watched
                        if (timestamp != null && timestamp > lastTimestamp) {
                            lastTimestamp = timestamp;
                            lastWatchedTitle = title != null ? title : "—";
                        }
                    }

                    // ── update basic stats ─────────────────────
                    int watchedCount = watchedMovies.size();
                    tvWatchedCount.setText(String.valueOf(watchedCount));
                    tvStatsSubtitle.setText(watchedCount + " movies in your history");
                    tvHighestRated.setText(highestRatedTitle);
                    tvLastWatched.setText(lastWatchedTitle);

                    if (ratedCount > 0) {
                        double avg = (double) totalRating / ratedCount;
                        tvAvgRating.setText(String.format("%.1f", avg));
                    } else {
                        tvAvgRating.setText("—");
                    }

                    // ── rating breakdown bars ──────────────────
                    buildRatingBreakdown(starCounts, ratedCount);

                    // ── recent activity ────────────────────────
                    buildRecentActivity(watchedMovies);

                    // ── fetch genres for top genre ─────────────
                    fetchGenresForMovies(movieIdsToFetch);

                    // ── load review count ──────────────────────
                    loadReviewCount(uid);

                    // ── load top mood ──────────────────────────
                    loadTopMood(uid);
                });
    }

    private void buildRatingBreakdown(int[] starCounts, int total) {
        llRatingBreakdown.removeAllViews();

        for (int i = 4; i >= 0; i--) {
            int count = starCounts[i];
            int star  = i + 1;

            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rowParams.setMargins(0, 0, 0, 8);
            row.setLayoutParams(rowParams);

            // star label
            TextView tvStar = new TextView(getContext());
            tvStar.setText(star + "★");
            tvStar.setTextColor(getResources().getColor(R.color.text_secondary));
            tvStar.setTypeface(ResourcesCompat.getFont(getContext(), R.font.poppins_medium));
            tvStar.setTextSize(12);
            LinearLayout.LayoutParams starParams =
                    new LinearLayout.LayoutParams(60, LinearLayout.LayoutParams.WRAP_CONTENT);
            tvStar.setLayoutParams(starParams);

            // progress bar background
            LinearLayout barBg = new LinearLayout(getContext());
            barBg.setBackgroundColor(getResources().getColor(R.color.bg_card));
            LinearLayout.LayoutParams bgParams =
                    new LinearLayout.LayoutParams(0, 12, 1f);
            bgParams.setMargins(8, 0, 8, 0);
            barBg.setLayoutParams(bgParams);

            // filled bar
            View barFill = new View(getContext());
            barFill.setBackgroundColor(getResources().getColor(R.color.bg_button));
            float fillPercent = total > 0 ? (float) count / total : 0f;
            LinearLayout.LayoutParams fillParams =
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, fillPercent);
            barFill.setLayoutParams(fillParams);
            barBg.addView(barFill);

            // remaining bar space
            View barEmpty = new View(getContext());
            LinearLayout.LayoutParams emptyParams =
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT,
                            1f - fillPercent);
            barEmpty.setLayoutParams(emptyParams);
            barBg.addView(barEmpty);

            // count label
            TextView tvCount = new TextView(getContext());
            tvCount.setText(String.valueOf(count));
            tvCount.setTextColor(getResources().getColor(R.color.text_secondary));
            tvCount.setTextSize(12);
            tvCount.setTypeface(ResourcesCompat.getFont(getContext(), R.font.poppins_medium));
            LinearLayout.LayoutParams countParams =
                    new LinearLayout.LayoutParams(60, LinearLayout.LayoutParams.WRAP_CONTENT);
            tvCount.setLayoutParams(countParams);
            tvCount.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

            row.addView(tvStar);
            row.addView(barBg);
            row.addView(tvCount);
            llRatingBreakdown.addView(row);
        }
    }

    private void buildRecentActivity(List<DataSnapshot> movies) {
        llRecentActivity.removeAllViews();

        // sort by timestamp descending
        List<DataSnapshot> sorted = new ArrayList<>(movies);
        sorted.sort((a, b) -> {
            Long ta = a.child("timestamp").getValue(Long.class);
            Long tb = b.child("timestamp").getValue(Long.class);
            if (ta == null) ta = 0L;
            if (tb == null) tb = 0L;
            return Long.compare(tb, ta);
        });

        // show max 5 recent
        int limit = Math.min(sorted.size(), 5);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);

        for (int i = 0; i < limit; i++) {
            DataSnapshot child = sorted.get(i);
            String title     = child.child("title").getValue(String.class);
            Integer rating   = child.child("rating").getValue(Integer.class);
            Long timestamp   = child.child("timestamp").getValue(Long.class);

            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.bg_chip);
            row.setPadding(24, 20, 24, 20);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rowParams.setMargins(0, 0, 0, 30);
            row.setLayoutParams(rowParams);

            // title
            TextView tvTitle = new TextView(getContext());
            tvTitle.setText(title != null ? title : "Unknown");
            tvTitle.setTextColor(getResources().getColor(R.color.text_primary));
            tvTitle.setTextSize(13);
            tvTitle.setMaxLines(1);
            tvTitle.setTypeface(ResourcesCompat.getFont(getContext(), R.font.poppins_medium));
            tvTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams titleParams =
                    new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvTitle.setLayoutParams(titleParams);

            // right side — rating + date
            LinearLayout rightCol = new LinearLayout(getContext());
            rightCol.setOrientation(LinearLayout.VERTICAL);
            rightCol.setGravity(android.view.Gravity.END);

            TextView tvRating = new TextView(getContext());
            tvRating.setText(rating != null && rating > 0
                    ? "★ " + rating + "/5" : "Not rated");
            tvRating.setTextColor(getResources().getColor(R.color.bg_button));
            tvRating.setTextSize(12);
            tvRating.setTypeface(ResourcesCompat.getFont(getContext(), R.font.poppins_medium));
            TextView tvDate = new TextView(getContext());
            String dateStr = timestamp != null
                    ? sdf.format(new Date(timestamp)) : "—";
            tvDate.setText(dateStr);
            tvDate.setTextColor(getResources().getColor(R.color.text_hint));
            tvDate.setTextSize(11);
            tvDate.setTypeface(ResourcesCompat.getFont(getContext(), R.font.poppins_medium));
            rightCol.addView(tvRating);
            rightCol.addView(tvDate);

            row.addView(tvTitle);
            row.addView(rightCol);
            llRecentActivity.addView(row);
        }
    }

    private void fetchGenresForMovies(List<Integer> movieIds) {
        if (movieIds.isEmpty()) {
            tvTopGenre.setText("—");
            return;
        }

        Map<String, Integer> genreCount = new HashMap<>();
        final int[] fetched = {0};
        int total = movieIds.size();

        RequestQueue queue = Volley.newRequestQueue(getContext());

        for (int movieId : movieIds) {
            String url = BASE_URL + "movie/" + movieId + "?api_key=" + API_KEY;

            JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                    response -> {
                        try {
                            JSONArray genres = response.getJSONArray("genres");
                            for (int i = 0; i < genres.length(); i++) {
                                JSONObject genre = genres.getJSONObject(i);
                                String name = genre.getString("name");
                                genreCount.put(name, genreCount.getOrDefault(name, 0) + 1);
                            }
                        } catch (Exception e) {
                            // skip
                        }

                        fetched[0]++;
                        if (fetched[0] == total) {
                            // find top genre
                            String topGenre = "—";
                            int maxCount = 0;
                            for (Map.Entry<String, Integer> entry : genreCount.entrySet()) {
                                if (entry.getValue() > maxCount) {
                                    maxCount = entry.getValue();
                                    topGenre = entry.getKey();
                                }
                            }
                            tvTopGenre.setText(topGenre);
                        }
                    },
                    error -> {
                        fetched[0]++;
                        if (fetched[0] == total && !genreCount.isEmpty()) {
                            String topGenre = Collections.max(
                                    genreCount.entrySet(),
                                    Map.Entry.comparingByValue()
                            ).getKey();
                            tvTopGenre.setText(topGenre);
                        }
                    }
            );

            queue.add(req);
        }
    }

    private void loadReviewCount(String uid) {
        FirebaseDatabase.getInstance(DB_URL)
                .getReference("reviews")
                .get()
                .addOnSuccessListener(snapshot -> {
                    int count = 0;
                    for (DataSnapshot movieReviews : snapshot.getChildren()) {
                        for (DataSnapshot review : movieReviews.getChildren()) {
                            String reviewUid = review.child("uid").getValue(String.class);
                            if (uid.equals(reviewUid)) count++;
                        }
                    }
                    tvReviewCount.setText(String.valueOf(count));
                });
    }

    private void loadTopMood(String uid) {
        FirebaseDatabase.getInstance(DB_URL)
                .getReference("moodHistory")
                .child(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        tvTopMood.setText("—");
                        return;
                    }

                    Map<String, Integer> moodCount = new HashMap<>();
                    for (DataSnapshot entry : snapshot.getChildren()) {
                        String mood = entry.child("mood").getValue(String.class);
                        if (mood != null) {
                            moodCount.put(mood, moodCount.getOrDefault(mood, 0) + 1);
                        }
                    }

                    if (moodCount.isEmpty()) {
                        tvTopMood.setText("—");
                        return;
                    }

                    String topMood = Collections.max(
                            moodCount.entrySet(),
                            Map.Entry.comparingByValue()
                    ).getKey();

                    tvTopMood.setText(topMood);
                });
    }
}