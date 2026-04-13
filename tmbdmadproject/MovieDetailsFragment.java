package com.example.tmbdmadproject;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MovieDetailsFragment extends Fragment {
    androidx.appcompat.widget.AppCompatButton btnWatchlist;
    boolean isInWatchlist = false;
    ImageView star1, star2, star3, star4, star5;
    androidx.appcompat.widget.AppCompatButton btnWatched, btnSubmitReview;
    android.widget.EditText etReview;
    LinearLayout llReviews;
    int userRating = 0;
    boolean isWatched = false;

    private static final String API_KEY = "801da655375c1e627e8d97311ff30488";
    private static final String BASE_URL = "https://api.themoviedb.org/3/";

    LinearLayout llCast, llCrew;
    ImageView imgPoster, imgBackdrop, imgTrailerThumb, btnBack;
    TextView tvTitle, tvRating, tvReleaseDate, tvRuntime,
            tvOverview, tvStatus, tvLanguage, tvBudget, tvRevenue;
    LinearLayout llGenres;
    FrameLayout btnWatchTrailer;

    int movieId;
    String trailerKey = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_movie_details, container, false);

        imgPoster        = view.findViewById(R.id.imgPoster);
        imgBackdrop      = view.findViewById(R.id.imgBackdrop);
        imgTrailerThumb  = view.findViewById(R.id.imgTrailerThumb);
        btnBack          = view.findViewById(R.id.btnBack);
        tvTitle          = view.findViewById(R.id.tvTitle);
        tvRating         = view.findViewById(R.id.tvRating);
        tvReleaseDate    = view.findViewById(R.id.tvReleaseDate);
        tvRuntime        = view.findViewById(R.id.tvRuntime);
        tvOverview       = view.findViewById(R.id.tvOverview);
        tvStatus         = view.findViewById(R.id.tvStatus);
        tvLanguage       = view.findViewById(R.id.tvLanguage);
        tvBudget         = view.findViewById(R.id.tvBudget);
        tvRevenue        = view.findViewById(R.id.tvRevenue);
        llGenres         = view.findViewById(R.id.llGenres);
        btnWatchTrailer  = view.findViewById(R.id.btnWatchTrailer);
        llCast           = view.findViewById(R.id.llCast);
        llCrew           = view.findViewById(R.id.llCrew);
        star1            = view.findViewById(R.id.star1);
        star2            = view.findViewById(R.id.star2);
        star3            = view.findViewById(R.id.star3);
        star4            = view.findViewById(R.id.star4);
        star5            = view.findViewById(R.id.star5);
        btnWatched       = view.findViewById(R.id.btnWatched);
        btnSubmitReview  = view.findViewById(R.id.btnSubmitReview);
        etReview         = view.findViewById(R.id.etReview);
        llReviews        = view.findViewById(R.id.llReviews);


        if (getArguments() != null) {
            String title  = getArguments().getString("title");
            String poster = getArguments().getString("poster");
            double rating = getArguments().getDouble("rating");
            movieId       = getArguments().getInt("movie_id");

            tvTitle.setText(title);
            tvRating.setText(String.format("%.1f", rating));

            // load poster
            Picasso.get()
                    .load("https://image.tmdb.org/t/p/w500" + poster)
                    .into(imgPoster);

            // load backdrop (higher res for the top banner)
            Picasso.get()
                    .load("https://image.tmdb.org/t/p/w780" + poster)
                    .into(imgBackdrop);

            // also set trailer thumbnail to poster while we wait for real trailer
            Picasso.get()
                    .load("https://image.tmdb.org/t/p/w780" + poster)
                    .into(imgTrailerThumb);
        }

        // back button
        btnBack.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        // trailer button
        btnWatchTrailer.setOnClickListener(v -> {
            if (trailerKey != null) {
                String youtubeUrl = "https://www.youtube.com/watch?v=" + trailerKey;
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl)));
            } else {
                Toast.makeText(getContext(), "No trailer available", Toast.LENGTH_SHORT).show();
            }
        });

        loadMovieDetails();
        loadMovieTrailer();
        loadMovieCredits();
        setupStarRating();
        setupWatchedButton();
        setupReviewSubmit();
        loadReviews();
        btnWatchlist = view.findViewById(R.id.btnWatchlist);
        setupWatchlistButton();
        setupAddToListButton(view);
        return view;
    }

    private void setupWatchlistButton() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference ref = FirebaseDatabase
                .getInstance("https://tmbd-mad-project-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("watchlist")
                .child(uid)
                .child(String.valueOf(movieId));

        // check if already in watchlist
        ref.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                isInWatchlist = true;
                btnWatchlist.setText("✓ In Watchlist");
                btnWatchlist.setBackgroundResource(R.drawable.button_style);
            }
        });

        btnWatchlist.setOnClickListener(v -> {
            if (isWatched) {
                Toast.makeText(getContext(),
                        "You already watched this movie!",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            isInWatchlist = !isInWatchlist;

            if (isInWatchlist) {
                btnWatchlist.setText("✓ In Watchlist");
                btnWatchlist.setBackgroundResource(R.drawable.button_style);

                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("movieId",    movieId);
                data.put("title",      tvTitle.getText().toString());
                data.put("posterPath", getArguments().getString("poster"));
                data.put("rating",     getArguments().getDouble("rating"));
                data.put("timestamp",  System.currentTimeMillis());

                ref.setValue(data)
                        .addOnSuccessListener(unused ->
                                Toast.makeText(getContext(),
                                        "Added to watchlist!", Toast.LENGTH_SHORT).show()
                        );
            } else {
                btnWatchlist.setText("+ Add to Watchlist");
                btnWatchlist.setBackgroundResource(R.drawable.bg_card_rounded);

                ref.removeValue()
                        .addOnSuccessListener(unused ->
                                Toast.makeText(getContext(),
                                        "Removed from watchlist", Toast.LENGTH_SHORT).show()
                        );
            }
        });
    }

    private void loadMovieDetails() {
        String url = BASE_URL + "movie/" + movieId + "?api_key=" + API_KEY;
        RequestQueue queue = Volley.newRequestQueue(getContext());

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        tvOverview.setText(response.getString("overview"));

                        String rawDate = response.getString("release_date");

                        try {
                            java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
                            java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMMM dd, yyyy");

                            Date date = inputFormat.parse(rawDate);
                            String formattedDate = outputFormat.format(date);

                            tvReleaseDate.setText(formattedDate);

                        } catch (Exception e) {
                            e.printStackTrace();
                            tvReleaseDate.setText(rawDate); // fallback
                        }

                        tvRuntime.setText(response.getInt("runtime") + " min");
                        tvStatus.setText(response.getString("status"));

                        String langCode = response.getString("original_language");
                        Locale locale = new Locale(langCode);
                        String fullLanguage = locale.getDisplayLanguage(Locale.ENGLISH);
                        tvLanguage.setText(fullLanguage);

                        long budget  = response.getLong("budget");
                        long revenue = response.getLong("revenue");
                        tvBudget.setText(budget > 0
                                ? "$" + String.format("%,d", budget) : "N/A");
                        tvRevenue.setText(revenue > 0
                                ? "$" + String.format("%,d", revenue) : "N/A");

                        // backdrop image
                        if (!response.isNull("backdrop_path")) {
                            String backdrop = response.getString("backdrop_path");
                            Picasso.get()
                                    .load("https://image.tmdb.org/t/p/w780" + backdrop)
                                    .into(imgBackdrop);
                            Picasso.get()
                                    .load("https://image.tmdb.org/t/p/w780" + backdrop)
                                    .into(imgTrailerThumb);
                        }

                        // genres chips
                        JSONArray genres = response.getJSONArray("genres");
                        llGenres.removeAllViews();
                        for (int i = 0; i < genres.length(); i++) {
                            JSONObject genre = genres.getJSONObject(i);
                            TextView chip = new TextView(getContext());
                            chip.setText(genre.getString("name"));
                            chip.setTextColor(getResources().getColor(R.color.black));
                            chip.setBackgroundResource(R.drawable.button_style);
                            chip.setPadding(24, 8, 24, 8);
                            chip.setTextSize(12);
                            chip.setTypeface(ResourcesCompat.getFont(getContext(), R.font.poppins_medium));
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            lp.setMargins(0, 0, 10, 0);
                            chip.setLayoutParams(lp);
                            llGenres.addView(chip);
                        }

                    } catch (JSONException e) {
                        Log.e("MOVIE_DETAILS", e.toString());
                    }
                },
                error -> Log.e("MOVIE_DETAILS", error.toString())
        );

        queue.add(request);
    }

    private void loadMovieTrailer() {
        String url = BASE_URL + "movie/" + movieId + "/videos?api_key=" + API_KEY;
        RequestQueue queue = Volley.newRequestQueue(getContext());

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject video = results.getJSONObject(i);
                            String type = video.getString("type");
                            String site = video.getString("site");

                            // find first YouTube trailer
                            if (type.equals("Trailer") && site.equals("YouTube")) {
                                trailerKey = video.getString("key");

                                // load YouTube thumbnail
                                String thumbUrl = "https://img.youtube.com/vi/"
                                        + trailerKey + "/hqdefault.jpg";
                                Picasso.get().load(thumbUrl).into(imgTrailerThumb);
                                break;
                            }
                        }

                    } catch (JSONException e) {
                        Log.e("MOVIE_TRAILER", e.toString());
                    }
                },
                error -> Log.e("MOVIE_TRAILER", error.toString())
        );

        queue.add(request);
    }

    private void loadMovieCredits() {
        String url = BASE_URL + "movie/" + movieId + "/credits?api_key=" + API_KEY;
        RequestQueue queue = Volley.newRequestQueue(getContext());

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        // ── CAST ──────────────────────────────────────
                        JSONArray cast = response.getJSONArray("cast");
                        llCast.removeAllViews();

                        // show max 15 cast members
                        int castLimit = Math.min(cast.length(), 15);

                        for (int i = 0; i < castLimit; i++) {
                            JSONObject actor = cast.getJSONObject(i);

                            String name      = actor.getString("name");
                            String character = actor.getString("character");
                            String profile   = actor.isNull("profile_path")
                                    ? null : actor.getString("profile_path");

                            // inflate cast item
                            View castView = LayoutInflater.from(getContext())
                                    .inflate(R.layout.item_cast, llCast, false);

                            ImageView imgPhoto   = castView.findViewById(R.id.imgCastPhoto);
                            TextView tvName      = castView.findViewById(R.id.tvCastName);
                            TextView tvCharacter = castView.findViewById(R.id.tvCastCharacter);

                            tvName.setText(name);
                            tvCharacter.setText(character);

                            if (profile != null) {
                                Picasso.get()
                                        .load("https://image.tmdb.org/t/p/w185" + profile)
                                        .into(imgPhoto);
                            } else {
                                imgPhoto.setImageResource(R.drawable.ic_launcher_foreground);
                            }

                            llCast.addView(castView);
                        }

                        // ── CREW ──────────────────────────────────────
                        JSONArray crew = response.getJSONArray("crew");
                        llCrew.removeAllViews();

                        // only show key crew: Director, Producer, Writer, Screenplay, Music
                        String[] importantJobs = {
                                "Director", "Producer", "Screenplay",
                                "Writer", "Original Music Composer"
                        };

                        for (int i = 0; i < crew.length(); i++) {
                            JSONObject member = crew.getJSONObject(i);
                            String job  = member.getString("job");
                            String name = member.getString("name");

                            boolean isImportant = false;
                            for (String j : importantJobs) {
                                if (j.equals(job)) {
                                    isImportant = true;
                                    break;
                                }
                            }

                            if (!isImportant) continue;

                            // build crew row dynamically
                            LinearLayout row = new LinearLayout(getContext());
                            row.setOrientation(LinearLayout.HORIZONTAL);
                            row.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            ));

                            LinearLayout.LayoutParams leftParams =
                                    new LinearLayout.LayoutParams(0,
                                            LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                            leftParams.setMargins(0, 0, 0, 12);

                            LinearLayout.LayoutParams rightParams =
                                    new LinearLayout.LayoutParams(0,
                                            LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                            rightParams.setMargins(0, 0, 0, 12);

                            TextView tvJob = new TextView(getContext());
                            tvJob.setText(job);
                            tvJob.setTextColor(getResources().getColor(R.color.text_secondary));
                            tvJob.setTextSize(13);
                            tvJob.setLayoutParams(leftParams);
                            tvJob.setTypeface(ResourcesCompat.getFont(getContext(), R.font.poppins_medium));
                            TextView tvName = new TextView(getContext());
                            tvName.setText(name);
                            tvName.setTextColor(getResources().getColor(R.color.text_primary));
                            tvName.setTextSize(13);
                            tvName.setTypeface(null, android.graphics.Typeface.BOLD);
                            tvName.setLayoutParams(rightParams);
                            tvName.setTypeface(ResourcesCompat.getFont(getContext(), R.font.poppins_medium));
                            row.addView(tvJob);
                            row.addView(tvName);
                            llCrew.addView(row);
                        }

                    } catch (JSONException e) {
                        Log.e("MOVIE_CREDITS", e.toString());
                    }
                },
                error -> Log.e("MOVIE_CREDITS", error.toString())
        );

        queue.add(request);
    }
    private void setupStarRating() {
        ImageView[] stars = {star1, star2, star3, star4, star5};

        for (ImageView star : stars) {
            star.setOnClickListener(v -> {
                int clickedRating = Integer.parseInt(v.getTag().toString());

                if (clickedRating == userRating) {
                    userRating = 0; // remove rating
                } else {
                    userRating = clickedRating;
                }

                updateStars(stars, userRating);
                saveRatingToFirebase(userRating);

                if (userRating > 0 && !isWatched) {
                    markAsWatched();
                }
            });
        }

        // load existing rating if any
        String uid = com.google.firebase.auth.FirebaseAuth
                .getInstance().getCurrentUser().getUid();

        com.google.firebase.database.FirebaseDatabase
                .getInstance("https://tmbd-mad-project-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("watchHistory")
                .child(uid)
                .child(String.valueOf(movieId))
                .child("rating")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Integer value = snapshot.getValue(Integer.class);
                        userRating = (value != null) ? value : 0;
                        updateStars(stars, userRating);
                    }
                });
    }

    private void updateStars(ImageView[] stars, int rating) {
        for (int i = 0; i < stars.length; i++) {
            if (i < rating) {
                stars[i].setImageResource(R.drawable.ic_star_full); // filled
            } else {
                stars[i].setImageResource(R.drawable.ic_star_outline); // empty
            }
        }
    }

    private void setupAddToListButton(View view) {
        androidx.appcompat.widget.AppCompatButton btnAddToList =
                view.findViewById(R.id.btnAddToList);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        btnAddToList.setOnClickListener(v -> {
            // fetch user's lists
            FirebaseDatabase.getInstance("https://tmbd-mad-project-default-rtdb.asia-southeast1.firebasedatabase.app/")
                    .getReference("lists")
                    .orderByChild("createdBy")
                    .equalTo(uid)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (!snapshot.exists()) {
                            Toast.makeText(getContext(),
                                    "You have no lists yet! Create one in Profile → My Lists",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        // build list of names for dialog
                        List<String> listNames = new ArrayList<>();
                        List<String> listIds   = new ArrayList<>();

                        for (DataSnapshot child : snapshot.getChildren()) {
                            listIds.add(child.getKey());
                            listNames.add(child.child("name").getValue(String.class));
                        }

                        String[] namesArray = listNames.toArray(new String[0]);

                        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                                .setTitle("Add to which list?")
                                .setItems(namesArray, (dialog, which) -> {
                                    String selectedListId = listIds.get(which);

                                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                                    data.put("movieId",    movieId);
                                    data.put("title",      tvTitle.getText().toString());
                                    data.put("posterPath", getArguments().getString("poster"));
                                    data.put("rating",     getArguments().getDouble("rating"));
                                    data.put("timestamp",  System.currentTimeMillis());

                                    FirebaseDatabase.getInstance("https://tmbd-mad-project-default-rtdb.asia-southeast1.firebasedatabase.app/")
                                            .getReference("lists")
                                            .child(selectedListId)
                                            .child("movies")
                                            .child(String.valueOf(movieId))
                                            .setValue(data)
                                            .addOnSuccessListener(unused ->
                                                    Toast.makeText(getContext(),
                                                            "Added to \"" + namesArray[which] + "\"!",
                                                            Toast.LENGTH_SHORT).show()
                                            );
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    });
        });
    }

    private void saveRatingToFirebase(int rating) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference ref = FirebaseDatabase
                .getInstance("https://tmbd-mad-project-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("watchHistory")
                .child(uid)
                .child(String.valueOf(movieId))
                .child("rating");

        if (rating == 0) {
            // 🔥 REMOVE rating
            ref.removeValue();
            Toast.makeText(getContext(), "Rating removed!", Toast.LENGTH_SHORT).show();
        } else {
            ref.setValue(rating);
            Toast.makeText(getContext(), "Rating saved!", Toast.LENGTH_SHORT).show();
        }
    }

    // in markAsWatched()
    private void markAsWatched() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference ref = FirebaseDatabase
                .getInstance("https://tmbd-mad-project-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("watchHistory")
                .child(uid)
                .child(String.valueOf(movieId));

        isWatched = true;
        btnWatched.setText("✓ Watched");
        btnWatched.setBackgroundResource(R.drawable.button_style);

        ref.child("watched").setValue(true);
        ref.child("movieId").setValue(movieId);
        ref.child("title").setValue(tvTitle.getText().toString());
        ref.child("timestamp").setValue(System.currentTimeMillis());
        ref.child("posterPath").setValue(getArguments().getString("poster")); // ← ADD THIS

        Toast.makeText(getContext(), "Marked as watched!", Toast.LENGTH_SHORT).show();
    }

    private void setupWatchedButton() {
        String uid = com.google.firebase.auth.FirebaseAuth
                .getInstance().getCurrentUser().getUid();

        com.google.firebase.database.DatabaseReference ref =
                com.google.firebase.database.FirebaseDatabase
                        .getInstance("https://tmbd-mad-project-default-rtdb.asia-southeast1.firebasedatabase.app/")
                        .getReference("watchHistory")
                        .child(uid)
                        .child(String.valueOf(movieId));

        // check if already watched
        ref.child("watched").get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                isWatched = true;
                btnWatched.setText("✓ Watched");
                btnWatched.setBackgroundResource(R.drawable.button_style);
            }
        });

        btnWatched.setOnClickListener(v -> {
            isWatched = !isWatched;

            if (isWatched) {
                btnWatched.setText("✓ Watched");
                btnWatched.setBackgroundResource(R.drawable.button_style);

                ref.child("watched").setValue(true);
                ref.child("movieId").setValue(movieId);
                ref.child("title").setValue(tvTitle.getText().toString());
                ref.child("timestamp").setValue(System.currentTimeMillis());
                ref.child("posterPath").setValue(getArguments().getString("poster"));

                FirebaseDatabase
                        .getInstance("https://tmbd-mad-project-default-rtdb.asia-southeast1.firebasedatabase.app/")
                        .getReference("watchlist")
                        .child(uid)
                        .child(String.valueOf(movieId))
                        .removeValue();

                // ← update watchlist button UI too
                isInWatchlist = false;
                btnWatchlist.setText("+ Add to Watchlist");
                btnWatchlist.setBackgroundResource(R.drawable.bg_card_rounded);

                Toast.makeText(getContext(), "Added to watch history!", Toast.LENGTH_SHORT).show();
            } else {
                btnWatched.setText("Mark as Watched");
                btnWatched.setBackgroundResource(R.drawable.bg_card_rounded);

                ref.child("watched").setValue(false);
                Toast.makeText(getContext(), "Removed from watch history", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupReviewSubmit() {
        btnSubmitReview.setOnClickListener(v -> {
            String reviewText = etReview.getText().toString().trim();

            if (reviewText.isEmpty()) {
                Toast.makeText(getContext(), "Please write a review first", Toast.LENGTH_SHORT).show();
                return;
            }

            if (userRating == 0) {
                Toast.makeText(getContext(), "Please give a star rating first", Toast.LENGTH_SHORT).show();
                return;
            }

            String uid  = com.google.firebase.auth.FirebaseAuth
                    .getInstance().getCurrentUser().getUid();
            String email = com.google.firebase.auth.FirebaseAuth
                    .getInstance().getCurrentUser().getEmail();

            com.google.firebase.database.DatabaseReference reviewRef =
                    com.google.firebase.database.FirebaseDatabase
                            .getInstance("https://tmbd-mad-project-default-rtdb.asia-southeast1.firebasedatabase.app/")
                            .getReference("reviews")
                            .child(String.valueOf(movieId))
                            .push(); // unique key per review

            java.util.Map<String, Object> reviewData = new java.util.HashMap<>();
            reviewData.put("uid",       uid);
            reviewData.put("email",     email);
            reviewData.put("review",    reviewText);
            reviewData.put("rating",    userRating);
            reviewData.put("timestamp", System.currentTimeMillis());

            reviewRef.setValue(reviewData)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(getContext(), "Review submitted!", Toast.LENGTH_SHORT).show();
                        etReview.setText("");
                        loadReviews(); // refresh reviews
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        });
    }

    private void loadReviews() {
        com.google.firebase.database.FirebaseDatabase
                .getInstance("https://tmbd-mad-project-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("reviews")
                .child(String.valueOf(movieId))
                .get()
                .addOnSuccessListener(snapshot -> {
                    llReviews.removeAllViews();

                    if (!snapshot.exists()) {
                        TextView empty = new TextView(getContext());
                        empty.setText("No reviews yet. Be the first!");
                        empty.setTypeface(ResourcesCompat.getFont(getContext(), R.font.poppins_medium));
                        empty.setTextColor(getResources().getColor(R.color.text_secondary));
                        empty.setTextSize(13);
                        llReviews.addView(empty);
                        return;
                    }

                    for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                        String reviewId = child.getKey();
                        String email     = child.child("email").getValue(String.class);
                        String review    = child.child("review").getValue(String.class);
                        Integer rating   = child.child("rating").getValue(Integer.class);
                        Long timestamp   = child.child("timestamp").getValue(Long.class);

                        View reviewView = LayoutInflater.from(getContext())
                                .inflate(R.layout.item_review, llReviews, false);

                        ImageView btnDelete = reviewView.findViewById(R.id.btnDeleteReview);
                        TextView tvInitial = reviewView.findViewById(R.id.tvReviewInitial);
                        TextView tvUser    = reviewView.findViewById(R.id.tvReviewUser);
                        TextView tvDate    = reviewView.findViewById(R.id.tvReviewDate);
                        TextView tvRating  = reviewView.findViewById(R.id.tvReviewRating);
                        TextView tvText    = reviewView.findViewById(R.id.tvReviewText);

                        // show first letter of email as avatar
                        String initial = email != null
                                ? String.valueOf(email.charAt(0)).toUpperCase() : "U";
                        tvInitial.setText(initial);
                        tvInitial.setTypeface(ResourcesCompat.getFont(getContext(), R.font.poppins_medium));
                        tvUser.setText(email != null ? email.split("@")[0] : "User");
                        tvUser.setTypeface(ResourcesCompat.getFont(getContext(), R.font.poppins_medium));
                        tvText.setText(review);
                        tvText.setTypeface(ResourcesCompat.getFont(getContext(), R.font.poppins_medium));
                        tvRating.setText("★ " + (rating != null ? rating*2 : 0));

                        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        String reviewUid  = child.child("uid").getValue(String.class);

                        if (currentUid.equals(reviewUid)) {
                            btnDelete.setVisibility(View.VISIBLE);

                            btnDelete.setOnClickListener(v -> {
                                androidx.appcompat.app.AlertDialog dialog =
                                        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                                                .setTitle("Delete Review")
                                                .setIcon(R.drawable.ic_delete)
                                                .setMessage("Are you sure you want to delete this review?")
                                                .setPositiveButton("Delete", (dialogInterface, which) -> {

                                                    FirebaseDatabase.getInstance("https://tmbd-mad-project-default-rtdb.asia-southeast1.firebasedatabase.app/")
                                                            .getReference("reviews")
                                                            .child(String.valueOf(movieId))
                                                            .child(reviewId)
                                                            .removeValue()
                                                            .addOnSuccessListener(unused -> {
                                                                Toast.makeText(getContext(), "Review deleted!", Toast.LENGTH_SHORT).show();
                                                                loadReviews();
                                                            });

                                                })
                                                .setNegativeButton("Cancel", (dialogInterface, which) -> dialogInterface.dismiss())
                                                .create();

                                dialog.show();

                                // optional: make delete button red
                                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                                        .setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                            });
                        } else {
                            btnDelete.setVisibility(View.GONE);
                        }

                        // format timestamp
                        if (timestamp != null) {
                            java.text.SimpleDateFormat sdf =
                                    new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.ENGLISH);
                            tvDate.setText(sdf.format(new java.util.Date(timestamp)));
                            tvDate.setTypeface(ResourcesCompat.getFont(getContext(), R.font.poppins_medium));
                        }

                        llReviews.addView(reviewView);
                    }
                });
    }
}