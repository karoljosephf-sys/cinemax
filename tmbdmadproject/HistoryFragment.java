package com.example.tmbdmadproject;

import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryFragment extends Fragment {

    private static final String DB_URL =
            "https://tmbd-mad-project-default-rtdb.asia-southeast1.firebasedatabase.app/";

    RecyclerView rvHistory;
    TextView tvHistoryCount;
    HistoryAdapter adapter;
    List<HistoryMovie> historyList = new ArrayList<>();
    private boolean isLoading = false;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_history, container, false);

        rvHistory      = view.findViewById(R.id.rvHistory);
        tvHistoryCount = view.findViewById(R.id.tvHistoryCount);

        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new HistoryAdapter(getContext(), historyList);
        rvHistory.setAdapter(adapter);

//        loadHistory();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // clear everything first so stale data doesn't show
        if (isLoading) return;
        historyList.clear();
        adapter.notifyDataSetChanged();
        loadHistory(); // fresh load every time fragment becomes visible
    }

    private void loadHistory() {
        if (isLoading) return;
        isLoading = true;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase.getInstance(DB_URL)
                .getReference("watchHistory")
                .child(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    historyList.clear();

                    if (!snapshot.exists()) {
                        int count = 0;
                        String text = count + " movies watched";

                        android.text.SpannableString spannable = new android.text.SpannableString(text);

                        // COLOR
                        spannable.setSpan(
                                new android.text.style.ForegroundColorSpan(
                                        getResources().getColor(R.color.bg_button)
                                ),
                                0,
                                String.valueOf(count).length(),
                                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        );

                        // BOLD
                        spannable.setSpan(
                                new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                                0,
                                String.valueOf(count).length(),
                                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        );

                        tvHistoryCount.setText(spannable);
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    for (DataSnapshot child : snapshot.getChildren()) {
                        Boolean watched = child.child("watched").getValue(Boolean.class);
                        if (watched == null || !watched) continue;

                        Integer movieIdVal = child.child("movieId").getValue(Integer.class);
                        String title       = child.child("title").getValue(String.class);
                        String posterPath  = child.child("posterPath").getValue(String.class);
                        Integer rating     = child.child("rating").getValue(Integer.class);
                        Long timestamp     = child.child("timestamp").getValue(Long.class);

                        int movieId      = movieIdVal != null ? movieIdVal : 0;
                        int finalRating  = rating     != null ? rating     : 0;
                        long finalTs     = timestamp  != null ? timestamp  : 0;
                        String finalTitle = title     != null ? title      : "Unknown";
                        String currentUid = uid;

                        // if posterPath is missing — fetch from TMDb then save it back
                        if (posterPath == null || posterPath.isEmpty()) {
                            fetchAndSavePoster(movieId, finalTitle, finalRating,
                                    finalTs, currentUid);
                        } else {
                            // posterPath exists — load review and add directly
                            final String finalPoster = posterPath;
                            fetchReviewAndAdd(movieId, finalTitle, finalPoster,
                                    finalRating, finalTs, currentUid);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("HISTORY", "Failed to load: " + e.getMessage())
                );
    }

    private void fetchAndSavePoster(int movieId, String title, int rating,
                                    long timestamp, String uid) {
        String url = "https://api.themoviedb.org/3/movie/" + movieId
                + "?api_key=801da655375c1e627e8d97311ff30488";

        com.android.volley.RequestQueue queue =
                com.android.volley.toolbox.Volley.newRequestQueue(getContext());

        com.android.volley.toolbox.JsonObjectRequest req =
                new com.android.volley.toolbox.JsonObjectRequest(
                        com.android.volley.Request.Method.GET, url, null,
                        response -> {
                            try {
                                String poster = response.isNull("poster_path")
                                        ? null : response.getString("poster_path");

                                // save posterPath back to Firebase so next time it's cached
                                if (poster != null) {
                                    FirebaseDatabase.getInstance(DB_URL)
                                            .getReference("watchHistory")
                                            .child(uid)
                                            .child(String.valueOf(movieId))
                                            .child("posterPath")
                                            .setValue(poster);
                                }

                                fetchReviewAndAdd(movieId, title, poster, rating, timestamp, uid);

                            } catch (Exception e) {
                                Log.e("HISTORY", "Poster parse error: " + e.getMessage());
                            }
                        },
                        error -> Log.e("HISTORY", "TMDb fetch failed: " + error.toString())
                );

        queue.add(req);
    }

    private void fetchReviewAndAdd(int movieId, String title, String posterPath,
                                   int rating, long timestamp, String uid) {
        FirebaseDatabase.getInstance(DB_URL)
                .getReference("reviews")
                .child(String.valueOf(movieId))
                .get()
                .addOnSuccessListener(reviewSnap -> {

                    String latestReview = "";
                    for (DataSnapshot r : reviewSnap.getChildren()) {
                        String reviewUid = r.child("uid").getValue(String.class);
                        if (uid.equals(reviewUid)) {
                            String rv = r.child("review").getValue(String.class);
                            if (rv != null) latestReview = rv;
                        }
                    }

                    // add raw movie — no headers yet
                    historyList.add(new HistoryMovie(
                            movieId, title, posterPath,
                            rating, latestReview, timestamp
                    ));

                    // injectHeaders handles everything — sorting, headers, count, notify
                    injectHeaders();
                })
                .addOnFailureListener(e ->
                        Log.e("HISTORY", "Review fetch failed: " + e.getMessage())
                );
    }

    private void injectHeaders() {
        if (historyList.isEmpty()) return;

        // remove any existing headers first before rebuilding
        List<HistoryMovie> moviesOnly = new ArrayList<>();
        for (HistoryMovie item : historyList) {
            if (!item.isHeader) moviesOnly.add(item);
        }

        // sort movies only by timestamp newest first
        moviesOnly.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));

        // now inject headers
        List<HistoryMovie> withHeaders = new ArrayList<>();
        String lastLabel = "";

        for (HistoryMovie movie : moviesOnly) {
            String label = getDateLabel(movie.timestamp);

            if (!label.equals(lastLabel)) {
                withHeaders.add(new HistoryMovie(label));
                lastLabel = label;
            }

            withHeaders.add(movie);
        }

        historyList.clear();
        historyList.addAll(withHeaders);
        adapter.notifyDataSetChanged();
        int count = moviesOnly.size();
        String text = count + " movies watched";

        android.text.SpannableString spannable = new android.text.SpannableString(text);

        // COLOR
        spannable.setSpan(
                new android.text.style.ForegroundColorSpan(
                        getResources().getColor(R.color.bg_button)
                ),
                0,
                String.valueOf(count).length(),
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        // BOLD
        spannable.setSpan(
                new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                0,
                String.valueOf(count).length(),
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        tvHistoryCount.setText(spannable);
    }

    private int countMovies() {
        int count = 0;
        for (HistoryMovie item : historyList) {
            if (!item.isHeader) count++;
        }
        return count;
    }

    private String getDateLabel(long timestamp) {
        Calendar movieCal = Calendar.getInstance();
        movieCal.setTimeInMillis(timestamp);

        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        // compare year + day
        boolean isToday = movieCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && movieCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);

        boolean isYesterday = movieCal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR)
                && movieCal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR);

        if (isToday)     return "Today";
        if (isYesterday) return "Yesterday";

        // otherwise show the actual date
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH);
        return sdf.format(new Date(timestamp));
    }
}