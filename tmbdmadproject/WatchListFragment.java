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
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class WatchListFragment extends Fragment {

    private static final String DB_URL =
            "https://tmbd-mad-project-default-rtdb.asia-southeast1.firebasedatabase.app/";

    LinearLayout llWatchlist;
    TextView tvWatchlistCount;
    ImageView btnBack;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_watch_list, container, false);

        llWatchlist      = view.findViewById(R.id.llWatchlist);
        tvWatchlistCount = view.findViewById(R.id.tvWatchlistCount);
        btnBack          = view.findViewById(R.id.btnWatchlistBack);

        btnBack.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        loadWatchlist();

        return view;
    }

    private void loadWatchlist() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase.getInstance(DB_URL)
                .getReference("watchlist")
                .child(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    llWatchlist.removeAllViews();

                    if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                        tvWatchlistCount.setText("0 movies");
                        showEmpty();
                        return;
                    }

                    long count = snapshot.getChildrenCount();
                    tvWatchlistCount.setText(count + " movies");

                    // sort by timestamp newest first
                    List<DataSnapshot> items = new ArrayList<>();
                    for (DataSnapshot child : snapshot.getChildren()) items.add(child);
                    items.sort((a, b) -> {
                        Long ta = a.child("timestamp").getValue(Long.class);
                        Long tb = b.child("timestamp").getValue(Long.class);
                        if (ta == null) ta = 0L;
                        if (tb == null) tb = 0L;
                        return Long.compare(tb, ta);
                    });

                    for (DataSnapshot child : items) {
                        Integer movieId   = child.child("movieId").getValue(Integer.class);
                        String title      = child.child("title").getValue(String.class);
                        String posterPath = child.child("posterPath").getValue(String.class);
                        Double rating     = child.child("rating").getValue(Double.class);

                        if (movieId == null) continue;

                        View itemView = LayoutInflater.from(getContext())
                                .inflate(R.layout.item_watchlist, llWatchlist, false);

                        ImageView imgPoster   = itemView.findViewById(R.id.imgWatchlistPoster);
                        TextView tvTitle      = itemView.findViewById(R.id.tvWatchlistTitle);
                        TextView tvRating     = itemView.findViewById(R.id.tvWatchlistRating);
                        ImageView btnRemove   = itemView.findViewById(R.id.btnWatchlistRemove);

                        tvTitle.setText(title != null ? title : "Unknown");
                        tvRating.setText(rating != null
                                ? String.format("⭐ %.1f", rating) : "⭐ —");

                        if (posterPath != null) {
                            Picasso.get()
                                    .load("https://image.tmdb.org/t/p/w500" + posterPath)
                                    .placeholder(R.drawable.bg_card_rounded)
                                    .into(imgPoster);
                        }

                        // click → open movie details
                        int finalMovieId    = movieId;
                        String finalPoster  = posterPath;
                        double finalRating  = rating != null ? rating : 0;

                        itemView.setOnClickListener(v -> {
                            Bundle args = new Bundle();
                            args.putInt("movie_id",  finalMovieId);
                            args.putString("title",  title);
                            args.putString("poster", finalPoster);
                            args.putDouble("rating", finalRating);

                            MovieDetailsFragment fragment = new MovieDetailsFragment();
                            fragment.setArguments(args);

                            ((AppCompatActivity) getContext())
                                    .getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.fragmentContainer, fragment)
                                    .addToBackStack(null)
                                    .commit();
                        });

                        // remove button
                        btnRemove.setOnClickListener(v -> {
                            new androidx.appcompat.app.AlertDialog.Builder(getContext())
                                    .setTitle("Remove from Watchlist")
                                    .setMessage("Remove \"" + title + "\" from your watchlist?")
                                    .setPositiveButton("Remove", (dialog, which) -> {
                                        FirebaseDatabase.getInstance(DB_URL)
                                                .getReference("watchlist")
                                                .child(uid)
                                                .child(String.valueOf(finalMovieId))
                                                .removeValue()
                                                .addOnSuccessListener(unused -> loadWatchlist());
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        });

                        llWatchlist.addView(itemView);
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("WATCHLIST", e.getMessage())
                );
    }

    private void showEmpty() {
        TextView empty = new TextView(getContext());
        empty.setText("Your watchlist is empty.\nAdd movies from their details page!");
        empty.setTextColor(getResources().getColor(R.color.text_secondary));
        empty.setTextSize(14);
        empty.setGravity(android.view.Gravity.CENTER);
        empty.setPadding(0, 60, 0, 0);
        llWatchlist.addView(empty);
    }
}