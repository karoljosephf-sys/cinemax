package com.example.tmbdmadproject;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

public class ListDetailFragment extends Fragment {

    private static final String DB_URL =
            "https://tmbd-mad-project-default-rtdb.asia-southeast1.firebasedatabase.app/";

    LinearLayout llListMovies;
    TextView tvListTitle, tvListDescription, tvListMeta;
    ImageView btnBack;

    String listId;
    String listName;
    boolean isOwner;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_list_detail, container, false);

        llListMovies     = view.findViewById(R.id.llListMovies);
        tvListTitle      = view.findViewById(R.id.tvListTitle);
        tvListDescription = view.findViewById(R.id.tvListDescription);
        tvListMeta       = view.findViewById(R.id.tvListMeta);
        btnBack          = view.findViewById(R.id.btnListDetailBack);

        if (getArguments() != null) {
            listId   = getArguments().getString("list_id");
            listName = getArguments().getString("list_name");
            isOwner  = getArguments().getBoolean("is_owner", false);
            tvListTitle.setText(listName);
        }

        btnBack.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        loadListDetails();
        loadListMovies();

        return view;
    }

    private void loadListDetails() {
        FirebaseDatabase.getInstance(DB_URL)
                .getReference("lists")
                .child(listId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String description  = snapshot.child("description").getValue(String.class);
                    String creatorName  = snapshot.child("creatorName").getValue(String.class);
                    long movieCount     = snapshot.child("movies").getChildrenCount();

                    tvListDescription.setText(
                            description != null && !description.isEmpty()
                                    ? description : "No description");
                    tvListMeta.setText("By " + (creatorName != null ? creatorName : "User")
                            + " • " + movieCount + " movies");
                });
    }

    private void loadListMovies() {
        FirebaseDatabase.getInstance(DB_URL)
                .getReference("lists")
                .child(listId)
                .child("movies")
                .get()
                .addOnSuccessListener(snapshot -> {
                    llListMovies.removeAllViews();

                    if (!snapshot.exists()) {
                        showEmpty();
                        return;
                    }

                    List<DataSnapshot> items = new ArrayList<>();
                    for (DataSnapshot child : snapshot.getChildren()) items.add(child);

                    for (DataSnapshot child : items) {
                        Integer movieId   = child.child("movieId").getValue(Integer.class);
                        String title      = child.child("title").getValue(String.class);
                        String posterPath = child.child("posterPath").getValue(String.class);
                        Double rating     = child.child("rating").getValue(Double.class);

                        if (movieId == null) continue;

                        View itemView = LayoutInflater.from(getContext())
                                .inflate(R.layout.item_watchlist, llListMovies, false);

                        ImageView imgPoster = itemView.findViewById(R.id.imgWatchlistPoster);
                        TextView tvTitle    = itemView.findViewById(R.id.tvWatchlistTitle);
                        TextView tvRating   = itemView.findViewById(R.id.tvWatchlistRating);
                        ImageView btnRemove = itemView.findViewById(R.id.btnWatchlistRemove);

                        tvTitle.setText(title != null ? title : "Unknown");
                        tvRating.setText(rating != null
                                ? String.format("⭐ %.1f", rating) : "⭐ —");

                        if (posterPath != null) {
                            Picasso.get()
                                    .load("https://image.tmdb.org/t/p/w500" + posterPath)
                                    .placeholder(R.drawable.bg_card_rounded)
                                    .into(imgPoster);
                        }

                        int finalMovieId   = movieId;
                        String finalPoster = posterPath;
                        double finalRating = rating != null ? rating : 0;

                        // click → movie details
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

                        // remove from list (owner only)
                        if (isOwner) {
                            btnRemove.setVisibility(View.VISIBLE);
                            btnRemove.setOnClickListener(v -> {
                                FirebaseDatabase.getInstance(DB_URL)
                                        .getReference("lists")
                                        .child(listId)
                                        .child("movies")
                                        .child(String.valueOf(finalMovieId))
                                        .removeValue()
                                        .addOnSuccessListener(unused -> {
                                            Toast.makeText(getContext(),
                                                    "Removed from list", Toast.LENGTH_SHORT).show();
                                            loadListMovies();
                                            loadListDetails();
                                        });
                            });
                        } else {
                            btnRemove.setVisibility(View.GONE);
                        }

                        llListMovies.addView(itemView);
                    }
                });
    }

    private void showEmpty() {
        TextView empty = new TextView(getContext());
        empty.setText("No movies in this list yet.\nAdd movies from their details page!");
        empty.setTextColor(getResources().getColor(R.color.text_secondary));
        empty.setTextSize(14);
        empty.setGravity(android.view.Gravity.CENTER);
        empty.setPadding(0, 60, 0, 0);
        llListMovies.addView(empty);
    }
}