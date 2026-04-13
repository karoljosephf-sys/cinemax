package com.example.tmbdmadproject;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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

import java.util.ArrayList;
import java.util.List;

public class MyListsFragment extends Fragment {

    private static final String DB_URL =
            "https://tmbd-mad-project-default-rtdb.asia-southeast1.firebasedatabase.app/";

    LinearLayout llLists;
    TextView tvListCount;
    ImageView btnBack;
    androidx.appcompat.widget.AppCompatButton btnCreateList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_my_lists, container, false);

        llLists       = view.findViewById(R.id.llLists);
        tvListCount   = view.findViewById(R.id.tvListCount);
        btnBack       = view.findViewById(R.id.btnListsBack);
        btnCreateList = view.findViewById(R.id.btnCreateList);

        btnBack.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        btnCreateList.setOnClickListener(v -> showCreateListDialog());

        loadMyLists();

        return view;
    }

    private void loadMyLists() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase.getInstance(DB_URL)
                .getReference("lists")
                .orderByChild("createdBy")
                .equalTo(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    llLists.removeAllViews();

                    if (!snapshot.exists()) {
                        tvListCount.setText("0 lists");
                        showEmpty();
                        return;
                    }

                    List<DataSnapshot> items = new ArrayList<>();
                    for (DataSnapshot child : snapshot.getChildren()) items.add(child);

                    // sort newest first
                    items.sort((a, b) -> {
                        Long ta = a.child("timestamp").getValue(Long.class);
                        Long tb = b.child("timestamp").getValue(Long.class);
                        if (ta == null) ta = 0L;
                        if (tb == null) tb = 0L;
                        return Long.compare(tb, ta);
                    });

                    tvListCount.setText(items.size() + " lists");

                    for (DataSnapshot child : items) {
                        String listId      = child.getKey();
                        String name        = child.child("name").getValue(String.class);
                        String description = child.child("description").getValue(String.class);
                        long movieCount    = child.child("movies").getChildrenCount();

                        addListCard(listId, name, description, (int) movieCount, true);
                    }
                });
    }

    private void addListCard(String listId, String name, String description,
                             int movieCount, boolean isOwner) {
        View cardView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_user_list, llLists, false);

        TextView tvName        = cardView.findViewById(R.id.tvListName);
        TextView tvDescription = cardView.findViewById(R.id.tvListDescription);
        TextView tvMovieCount  = cardView.findViewById(R.id.tvListMovieCount);
        ImageView btnDelete    = cardView.findViewById(R.id.btnDeleteList);

        tvName.setText(name);
        tvDescription.setText(description != null ? description : "");
        tvMovieCount.setText(movieCount + " movies");

        if (isOwner) {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(getContext())
                        .setTitle("Delete List")
                        .setMessage("Delete \"" + name + "\"? This cannot be undone.")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            FirebaseDatabase.getInstance(DB_URL)
                                    .getReference("lists")
                                    .child(listId)
                                    .removeValue()
                                    .addOnSuccessListener(unused -> {
                                        Toast.makeText(getContext(),
                                                "List deleted!", Toast.LENGTH_SHORT).show();
                                        loadMyLists();
                                    });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        } else {
            btnDelete.setVisibility(View.GONE);
        }

        // open list detail
        cardView.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("list_id",   listId);
            args.putString("list_name", name);
            args.putBoolean("is_owner", isOwner);

            ListDetailFragment fragment = new ListDetailFragment();
            fragment.setArguments(args);

            ((AppCompatActivity) getContext())
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        llLists.addView(cardView);
    }

    private void showCreateListDialog() {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_create_list, null);

        EditText etName        = dialogView.findViewById(R.id.etListName);
        EditText etDescription = dialogView.findViewById(R.id.etListDescription);

        // 🔥 Custom WHITE title
        TextView title = new TextView(getContext());
        title.setText("Create New List");
        title.setTextColor(getResources().getColor(R.color.white));
        title.setTextSize(18);
        title.setPadding(40, 40, 40, 20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        androidx.appcompat.app.AlertDialog dialog =
                new androidx.appcompat.app.AlertDialog.Builder(getContext())
                        .setCustomTitle(title) // ✅ use this instead of setTitle
                        .setView(dialogView)
                        .setPositiveButton("Create", null) // ⚠️ set null (we handle manually)
                        .setNegativeButton("Cancel", null)
                        .create();

        dialog.show();

        // 🔥 Background
        dialog.getWindow().setBackgroundDrawableResource(R.color.bg_primary);

        // 🔥 Button colors
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setTextColor(getResources().getColor(R.color.bg_button));

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(getResources().getColor(R.color.bg_button));

        // 🔥 Handle clicks manually (so validation works)
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String name = etName.getText().toString().trim();
                    String description = etDescription.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(getContext(),
                                "List name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    createList(name, description);
                    dialog.dismiss();
                });

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
                .setOnClickListener(v -> dialog.dismiss());
    }

    private void createList(String name, String description) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // get user's name first
        FirebaseDatabase.getInstance(DB_URL)
                .getReference("users")
                .child(uid)
                .child("name")
                .get()
                .addOnSuccessListener(snapshot -> {
                    String creatorName = snapshot.getValue(String.class);
                    if (creatorName == null) creatorName = "User";

                    String listId = FirebaseDatabase.getInstance(DB_URL)
                            .getReference("lists").push().getKey();

                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                    data.put("name",        name);
                    data.put("description", description);
                    data.put("createdBy",   uid);
                    data.put("creatorName", creatorName);
                    data.put("timestamp",   System.currentTimeMillis());

                    FirebaseDatabase.getInstance(DB_URL)
                            .getReference("lists")
                            .child(listId)
                            .setValue(data)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(getContext(),
                                        "List created!", Toast.LENGTH_SHORT).show();
                                loadMyLists();
                            });
                });
    }

    private void showEmpty() {
        TextView empty = new TextView(getContext());
        empty.setText("No lists yet.\nTap + Create List to make your first one!");
        empty.setTextColor(getResources().getColor(R.color.text_secondary));
        empty.setTextSize(14);
        empty.setGravity(android.view.Gravity.CENTER);
        empty.setPadding(0, 60, 0, 0);
        llLists.addView(empty);
    }
}