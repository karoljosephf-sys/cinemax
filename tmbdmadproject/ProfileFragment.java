package com.example.tmbdmadproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

public class ProfileFragment extends Fragment {

    private static final String DB_URL =
            "https://tmbd-mad-project-default-rtdb.asia-southeast1.firebasedatabase.app/";

    TextView tvAvatarInitial, tvProfileName, tvProfileEmail,
            tvProfileWatched, tvProfileReviews, tvProfileRating,
            tvLogout, tvEditProfile, tvWatchlist, tvMyLists;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvAvatarInitial  = view.findViewById(R.id.tvAvatarInitial);
        tvProfileName    = view.findViewById(R.id.tvProfileName);
        tvProfileEmail   = view.findViewById(R.id.tvProfileEmail);
        tvProfileWatched = view.findViewById(R.id.tvProfileWatched);
        tvProfileReviews = view.findViewById(R.id.tvProfileReviews);
        tvProfileRating  = view.findViewById(R.id.tvProfileRating);
        tvLogout         = view.findViewById(R.id.tvLogout);
        tvEditProfile    = view.findViewById(R.id.tvEditProfile);
        tvWatchlist      = view.findViewById(R.id.tvWatchlist);
        tvMyLists        = view.findViewById(R.id.tvMyLists);
        loadProfile();
        loadStats();
        setupButtons(view);

        return view;
    }

    private void loadProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        // get name from realtime database
        FirebaseDatabase.getInstance(DB_URL)
                .getReference("users")
                .child(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String name = snapshot.child("name").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);

                    if (name != null && !name.isEmpty()) {
                        tvProfileName.setText(name);
                        tvAvatarInitial.setText(
                                String.valueOf(name.charAt(0)).toUpperCase()
                        );
                    } else {
                        tvProfileName.setText("User");
                        tvAvatarInitial.setText("U");
                    }

                    if (email != null) {
                        tvProfileEmail.setText(email);
                    } else {
                        tvProfileEmail.setText(user.getEmail());
                        tvAvatarInitial.setText(
                                String.valueOf(user.getEmail().charAt(0)).toUpperCase()
                        );
                    }
                });
    }

    private void loadStats() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // watched count + avg rating
        FirebaseDatabase.getInstance(DB_URL)
                .getReference("watchHistory")
                .child(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    int watchedCount = 0;
                    int totalRating  = 0;
                    int ratedCount   = 0;

                    for (DataSnapshot child : snapshot.getChildren()) {
                        Boolean watched = child.child("watched").getValue(Boolean.class);
                        if (watched == null || !watched) continue;

                        watchedCount++;

                        Integer rating = child.child("rating").getValue(Integer.class);
                        if (rating != null && rating > 0) {
                            totalRating += rating;
                            ratedCount++;
                        }
                    }

                    tvProfileWatched.setText(String.valueOf(watchedCount));

                    if (ratedCount > 0) {
                        double avg = (double) totalRating / ratedCount;
                        tvProfileRating.setText(String.format("%.1f", avg));
                    } else {
                        tvProfileRating.setText("—");
                    }
                });

        // review count
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
                    tvProfileReviews.setText(String.valueOf(count));
                });
    }

    private void setupButtons(View view) {
        // logout
        tvLogout.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(getContext())
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Logout", (dialog, which) -> {
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(getActivity(), MainActivity.class));
                        getActivity().finish();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        // edit profile
        tvEditProfile.setOnClickListener(v -> showEditProfileDialog());

        // recommendations menu item
        view.findViewById(R.id.tvRecommendations).setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new RecommendationsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        tvWatchlist.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new WatchListFragment())
                    .addToBackStack(null)
                    .commit();
        });

        tvMyLists.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new MyListsFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void showEditProfileDialog() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        android.widget.EditText input = new android.widget.EditText(getContext());
        input.setHint("Enter new name");
        input.setText(tvProfileName.getText().toString());
        input.setPadding(40, 20, 40, 20);

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Edit Name")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = input.getText().toString().trim();

                    if (newName.isEmpty()) {
                        Toast.makeText(getContext(),
                                "Name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseDatabase.getInstance(DB_URL)
                            .getReference("users")
                            .child(uid)
                            .child("name")
                            .setValue(newName)
                            .addOnSuccessListener(unused -> {
                                tvProfileName.setText(newName);
                                tvAvatarInitial.setText(
                                        String.valueOf(newName.charAt(0)).toUpperCase()
                                );
                                Toast.makeText(getContext(),
                                        "Name updated!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(),
                                            "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}