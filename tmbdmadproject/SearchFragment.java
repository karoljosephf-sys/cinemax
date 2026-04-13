package com.example.tmbdmadproject;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.net.URLEncoder;

public class SearchFragment extends Fragment {

    private static final String V4_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI4MDFkYTY1NTM3NWMxZTYyN2U4ZDk3MzExZmYzMDQ4OCIsIm5iZiI6MTc3NDg2MDAwOC4zMzQsInN1YiI6IjY5Y2EzNmU4ODc3MzNlNzNkYTgzOTRjOSIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.YXbJDsLRLS-MK_dZKsMjqUTo14wg6HS5N9hafG5bet4"; // 🔒 replace
    public class Language {
        private String iso;
        private String name;

        public Language(String iso, String name) {
            this.iso = iso;
            this.name = name;
        }

        public String getIso() { return iso; }
        public String getName() { return name; }

        @Override
        public String toString() {
            return name;
        }
    }

    private LinearLayout llSearchEmpty;
    private TextView tvSearchEmpty;
    // add at top with other fields
    private ScrollView scrollListResults;
    private ScrollView scrollActorResults;
    private LinearLayout llListResults;
    private LinearLayout llActorResults;
    private RecyclerView rvSearchResults;
    private MovieAdapter movieAdapter;
    private List<Movie> movieList = new ArrayList<>();

    private List<Integer> selectedGenres = new ArrayList<>();

    private android.os.Handler handler = new android.os.Handler();
    private Runnable searchRunnable;
    private LinearLayout layoutFilters;
    private Button btnToggleFilters, btnApplyFilters;
    private Spinner spinnerYear, spinnerSort, spinnerLanguage;
    private SeekBar sbRating;
    private LinearLayout genreContainer;
    private boolean isSearchMode = false;
    private EditText etSearch;

    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    View view;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_search, container, false);

        layoutFilters = view.findViewById(R.id.layoutFilters);
        btnToggleFilters = view.findViewById(R.id.btnToggleFilters);
        btnApplyFilters = view.findViewById(R.id.btnApplyFilters);
        spinnerYear = view.findViewById(R.id.spinnerYear);
        spinnerSort = view.findViewById(R.id.spinnerSort);
        spinnerLanguage = view.findViewById(R.id.spinnerLanguage);
        sbRating = view.findViewById(R.id.sbRating);
        genreContainer = view.findViewById(R.id.genreContainer);
        etSearch = view.findViewById(R.id.etSearch);
        scrollListResults  = view.findViewById(R.id.scrollListResults);
        scrollActorResults = view.findViewById(R.id.scrollActorResults);
        llSearchEmpty = view.findViewById(R.id.llSearchEmpty);
        tvSearchEmpty = view.findViewById(R.id.tvSearchEmpty);
        // wire up RecyclerView
        rvSearchResults = view.findViewById(R.id.rvSearchResults);
        rvSearchResults.setLayoutManager(new GridLayoutManager(getContext(), 3));
        movieAdapter = new MovieAdapter(getContext(), movieList, R.layout.item_movie_search, movie -> openMovieDetails(movie));
        rvSearchResults.setAdapter(movieAdapter);


        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentPage = 1;
                isLastPage = false;

                if (searchRunnable != null) handler.removeCallbacks(searchRunnable);

                searchRunnable = () -> {
                    String query = s.toString().trim();

                    // actors mode — check scrollActorResults not llActorResults
                    if (scrollActorResults != null &&
                            scrollActorResults.getVisibility() == View.VISIBLE) {
                        if (!query.isEmpty()) searchActors(query, llActorResults);
                        return;
                    }

                    // lists mode — check scrollListResults not llListResults
                    if (scrollListResults != null &&
                            scrollListResults.getVisibility() == View.VISIBLE) {
                        searchLists(query, llListResults);
                        return;
                    }

                    // movies mode (default)
                    if (!query.isEmpty()) {
                        isSearchMode = true;
                        disableFiltersUI();
                        searchMovies(query);
                    } else {
                        isSearchMode = false;
                        enableFiltersUI();
                        discoverMovies();
                    }
                };

                handler.postDelayed(searchRunnable, 500);
            }
        });



        btnToggleFilters.setOnClickListener(v -> {
            if (layoutFilters.getVisibility() == View.GONE) {
                expand(layoutFilters);
                btnToggleFilters.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_up_black, 0);
            } else {
                collapse(layoutFilters);
                btnToggleFilters.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_down_black, 0);
            }
        });

        populateFilters();


        btnApplyFilters.setOnClickListener(v -> {
            currentPage = 1;
            isLastPage = false;
            if (isSearchMode) {
                // 🚫 Prevent filter in search mode
                return;
            }

            discoverMovies(); // 👈 call discover API

            collapse(layoutFilters);
            btnToggleFilters.setCompoundDrawablesWithIntrinsicBounds(
                    0, 0, R.drawable.ic_arrow_down_black, 0);
        });

        rvSearchResults.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {

                        currentPage++;
                        isLoading = true;

                        if (isSearchMode) {
                            searchMovies(etSearch.getText().toString());
                        } else {
                            discoverMovies();
                        }
                    }
                }
            }
        });

        // load movies immediately on open
        currentPage = 1;
        isLastPage = false;
        discoverMovies();
        setupListSearch(view);
        return view;
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

    private void disableFiltersUI() {
        spinnerYear.setEnabled(false);
        spinnerSort.setEnabled(false);
        spinnerLanguage.setEnabled(false);
        sbRating.setEnabled(false);
        btnApplyFilters.setEnabled(false);

        layoutFilters.setAlpha(0.5f);
    }

    private void enableFiltersUI() {
        spinnerYear.setEnabled(true);
        spinnerSort.setEnabled(true);
        spinnerLanguage.setEnabled(true);
        sbRating.setEnabled(true);
        btnApplyFilters.setEnabled(true);

        layoutFilters.setAlpha(1f);
    }


    private void setupListSearch(View view) {
        androidx.appcompat.widget.AppCompatButton btnSearchMovies =
                view.findViewById(R.id.btnSearchMovies);
        androidx.appcompat.widget.AppCompatButton btnSearchLists =
                view.findViewById(R.id.btnSearchLists);
        androidx.appcompat.widget.AppCompatButton btnSearchActors =
                view.findViewById(R.id.btnSearchActors);

        llListResults  = view.findViewById(R.id.llListResults);
        llActorResults = view.findViewById(R.id.llActorResults);

        btnSearchMovies.setOnClickListener(v -> {
            btnSearchMovies.setBackgroundResource(R.drawable.button_style);
            btnSearchMovies.setTextColor(getResources().getColor(R.color.black));
            btnSearchLists.setBackgroundResource(R.drawable.bg_card_rounded);
            btnSearchLists.setTextColor(getResources().getColor(R.color.text_primary));
            btnSearchActors.setBackgroundResource(R.drawable.bg_card_rounded);
            btnSearchActors.setTextColor(getResources().getColor(R.color.text_primary));

            scrollListResults.setVisibility(View.GONE);
            scrollActorResults.setVisibility(View.GONE);

            rvSearchResults.setVisibility(View.VISIBLE);
            llListResults.setVisibility(View.GONE);
            llActorResults.setVisibility(View.GONE);
            btnToggleFilters.setVisibility(View.VISIBLE);

            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty()) searchMovies(query);
        });

        btnSearchLists.setOnClickListener(v -> {
            btnSearchLists.setBackgroundResource(R.drawable.button_style);
            btnSearchLists.setTextColor(getResources().getColor(R.color.black));
            btnSearchMovies.setBackgroundResource(R.drawable.bg_card_rounded);
            btnSearchMovies.setTextColor(getResources().getColor(R.color.text_primary));
            btnSearchActors.setBackgroundResource(R.drawable.bg_card_rounded);
            btnSearchActors.setTextColor(getResources().getColor(R.color.text_primary));

            scrollListResults.setVisibility(View.VISIBLE);
            scrollActorResults.setVisibility(View.GONE);

            rvSearchResults.setVisibility(View.GONE);
            llListResults.setVisibility(View.VISIBLE);
            llActorResults.setVisibility(View.GONE);
            btnToggleFilters.setVisibility(View.GONE);

            String query = etSearch.getText().toString().trim();
            searchLists(query, llListResults);
        });

        btnSearchActors.setOnClickListener(v -> {
            btnSearchActors.setBackgroundResource(R.drawable.button_style);
            btnSearchActors.setTextColor(getResources().getColor(R.color.black));
            btnSearchMovies.setBackgroundResource(R.drawable.bg_card_rounded);
            btnSearchMovies.setTextColor(getResources().getColor(R.color.text_primary));
            btnSearchLists.setBackgroundResource(R.drawable.bg_card_rounded);
            btnSearchLists.setTextColor(getResources().getColor(R.color.text_primary));

            scrollListResults.setVisibility(View.GONE);
            scrollActorResults.setVisibility(View.VISIBLE);

            rvSearchResults.setVisibility(View.GONE);
            llListResults.setVisibility(View.GONE);
            llActorResults.setVisibility(View.VISIBLE);
            btnToggleFilters.setVisibility(View.GONE);

            String query = etSearch.getText().toString().trim();
            llActorResults.removeAllViews();
            if (!query.isEmpty()) searchActors(query, llActorResults);
            else{
                TextView empty = new TextView(getContext());
                empty.setText("Search for an Actor or Actress by Name…");
                empty.setTextColor(getResources().getColor(R.color.text_secondary));
                empty.setTextSize(14);
                empty.setGravity(android.view.Gravity.CENTER);
                empty.setPadding(0, 40, 0, 0);
                llActorResults.addView(empty);
            }
        });
    }

    private void searchLists(String query, LinearLayout llListResults) {
        FirebaseDatabase
                .getInstance("https://tmbd-mad-project-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("lists")
                .get()
                .addOnSuccessListener(snapshot -> {
                    llListResults.removeAllViews();
                    llSearchEmpty.setVisibility(View.GONE);
                    if (!snapshot.exists()) {
                        showNoListResults(llListResults);
                        return;
                    }

                    boolean found = false;
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String name = child.child("name").getValue(String.class);
                        String description = child.child("description").getValue(String.class);
                        String creatorName = child.child("creatorName").getValue(String.class);
                        long movieCount = child.child("movies").getChildrenCount();
                        String listId = child.getKey();

                        // filter by name if query is not empty
                        if (!query.isEmpty() && name != null
                                && !name.toLowerCase().contains(query.toLowerCase())) continue;

                        found = true;

                        View cardView = LayoutInflater.from(getContext())
                                .inflate(R.layout.item_user_list, llListResults, false);

                        TextView tvName        = cardView.findViewById(R.id.tvListName);
                        TextView tvDesc        = cardView.findViewById(R.id.tvListDescription);
                        TextView tvCount       = cardView.findViewById(R.id.tvListMovieCount);
                        ImageView btnDelete    = cardView.findViewById(R.id.btnDeleteList);

                        tvName.setText(name);
                        tvDesc.setText("By " + (creatorName != null ? creatorName : "User")
                                + (description != null && !description.isEmpty()
                                ? " • " + description : ""));
                        tvCount.setText(movieCount + " movies");
                        btnDelete.setVisibility(View.GONE);

                        cardView.setOnClickListener(v -> {
                            Bundle args = new Bundle();
                            args.putString("list_id",   listId);
                            args.putString("list_name", name);
                            args.putBoolean("is_owner", false);

                            ListDetailFragment fragment = new ListDetailFragment();
                            fragment.setArguments(args);

                            requireActivity().getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.fragmentContainer, fragment)
                                    .addToBackStack(null)
                                    .commit();
                        });

                        llListResults.addView(cardView);
                    }

                    if (!found){
                        rvSearchResults.setVisibility(View.GONE);
                        llSearchEmpty.setVisibility(View.VISIBLE);
                        tvSearchEmpty.setText("No results for \"" + query + "\"");
                    }
                });
    }

    private void showNoListResults(LinearLayout llListResults) {
        TextView empty = new TextView(getContext());
        empty.setText("No lists found");
        empty.setTextColor(getResources().getColor(R.color.text_secondary));
        empty.setTextSize(14);
        empty.setGravity(android.view.Gravity.CENTER);
        empty.setPadding(0, 40, 0, 0);
        llListResults.addView(empty);
    }

    private void searchMovies(String query) {
        new Thread(() -> {
            try {
                String urlString = "https://api.themoviedb.org/3/search/movie?query="
                        + URLEncoder.encode(query, "UTF-8")
                        + "&page=" + currentPage;

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + V4_TOKEN);
                conn.setRequestProperty("accept", "application/json");

                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) result.append(line);

                JSONObject json = new JSONObject(result.toString());
                JSONArray results = json.getJSONArray("results");
                int totalPages = json.getInt("total_pages"); // ← get total pages

                List<Movie> parsed = new ArrayList<>();
                for (int i = 0; i < results.length(); i++) {
                    JSONObject obj = results.getJSONObject(i);
                    if (obj.isNull("poster_path")) continue;

                    int id            = obj.getInt("id");
                    String title      = obj.getString("title");
                    String posterPath = obj.getString("poster_path");
                    double rating     = obj.getDouble("vote_average");
                    parsed.add(new Movie(id, title, posterPath, rating));
                }

                if (!isAdded()) return;

                requireActivity().runOnUiThread(() -> {
                    if (currentPage == 1) movieList.clear();

                    movieList.addAll(parsed);
                    movieAdapter.notifyDataSetChanged();

                    isLoading = false;
                    isLastPage = currentPage >= totalPages; // ← proper last page check

                    // empty state
                    if (movieList.isEmpty()) {
                        rvSearchResults.setVisibility(View.GONE);
                        llSearchEmpty.setVisibility(View.VISIBLE);
                        tvSearchEmpty.setText("No results for \"" + query + "\"");
                    } else {
                        rvSearchResults.setVisibility(View.VISIBLE);
                        llSearchEmpty.setVisibility(View.GONE);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> isLoading = false);
            }
        }).start();
    }

    private void discoverMovies() {
        new Thread(() -> {
            try {
                StringBuilder urlBuilder = new StringBuilder(
                        "https://api.themoviedb.org/3/discover/movie");
                urlBuilder.append("?language=en-US");
                urlBuilder.append("&page=").append(currentPage);

                if (!selectedGenres.isEmpty()) {
                    StringBuilder genreParam = new StringBuilder();
                    for (int i = 0; i < selectedGenres.size(); i++) {
                        genreParam.append(selectedGenres.get(i));
                        if (i < selectedGenres.size() - 1) genreParam.append(",");
                    }
                    urlBuilder.append("&with_genres=").append(genreParam);
                }

                String year = spinnerYear.getSelectedItem().toString();
                if (!year.equals("All"))
                    urlBuilder.append("&primary_release_year=").append(year);

                int ratingProgress = sbRating.getProgress();
                if (ratingProgress > 0) {
                    float rating = ratingProgress / 10f;
                    urlBuilder.append("&vote_average.gte=").append(rating);
                }

                Language lang = (Language) spinnerLanguage.getSelectedItem();
                if (lang != null && !lang.getIso().isEmpty())
                    urlBuilder.append("&with_original_language=").append(lang.getIso());

                String sort = spinnerSort.getSelectedItem().toString();
                switch (sort) {
                    case "Popularity Desc":   urlBuilder.append("&sort_by=popularity.desc"); break;
                    case "Popularity Asc":    urlBuilder.append("&sort_by=popularity.asc"); break;
                    case "Rating Desc":       urlBuilder.append("&sort_by=vote_average.desc"); break;
                    case "Rating Asc":        urlBuilder.append("&sort_by=vote_average.asc"); break;
                    case "Release Date Desc": urlBuilder.append("&sort_by=primary_release_date.desc"); break;
                    case "Release Date Asc":  urlBuilder.append("&sort_by=primary_release_date.asc"); break;
                }

                URL url = new URL(urlBuilder.toString());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Authorization", "Bearer " + V4_TOKEN);
                conn.setRequestProperty("accept", "application/json");

                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) result.append(line);

                JSONObject json = new JSONObject(result.toString());
                JSONArray results = json.getJSONArray("results");
                int totalPages = json.getInt("total_pages"); // ← get total pages

                List<Movie> parsed = new ArrayList<>();
                for (int i = 0; i < results.length(); i++) {
                    JSONObject obj = results.getJSONObject(i);
                    if (obj.isNull("poster_path")) continue;

                    int id            = obj.getInt("id");
                    String title      = obj.getString("title");
                    String posterPath = obj.getString("poster_path");
                    double rating     = obj.getDouble("vote_average");
                    parsed.add(new Movie(id, title, posterPath, rating));
                }

                if (!isAdded()) return;

                requireActivity().runOnUiThread(() -> {
                    if (currentPage == 1) movieList.clear();

                    movieList.addAll(parsed);
                    movieAdapter.notifyDataSetChanged();

                    isLoading = false;
                    isLastPage = currentPage >= totalPages; // ← proper last page check

                    // empty state
                    if (movieList.isEmpty()) {
                        rvSearchResults.setVisibility(View.GONE);
                        llSearchEmpty.setVisibility(View.VISIBLE);
                        tvSearchEmpty.setText("No movies found for your filters");
                    } else {
                        rvSearchResults.setVisibility(View.VISIBLE);
                        llSearchEmpty.setVisibility(View.GONE);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> isLoading = false);
            }
        }).start();
    }

    private void populateFilters() {

        TextView tvRatingValue = view.findViewById(R.id.tvRatingValue);

        sbRating.setMax(100); // allow decimal precision 0-10 (multiply by 10)
        sbRating.setProgress(0);
        tvRatingValue.setText("Rating: All");
        sbRating.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 0) {
                    tvRatingValue.setText("Rating: All"); // 0 = no filter
                } else {
                    float decimalRating = progress / 10f; // e.g., 75 -> 7.5
                    tvRatingValue.setText("Rating: " + decimalRating);
                }
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // --- Year ---
        List<String> years = new ArrayList<>();
        years.add("All"); // Add "All" at the top
        for (int y = 2026; y >= 1900; y--) years.add(String.valueOf(y));

        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(
                requireContext(), R.layout.spinner_item, years);
        yearAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);

        // --- Sort ---
        String[] sortOptions = {"Popularity Desc", "Popularity Asc", "Rating Desc",
                "Rating Asc", "Release Date Desc", "Release Date Asc"};
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(
                requireContext(), R.layout.spinner_item, sortOptions);
        sortAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerSort.setAdapter(sortAdapter);

        // --- Loading state for language spinner ---
        List<Language> temp = new ArrayList<>();
        temp.add(new Language("", "Loading..."));
        spinnerLanguage.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, temp));
        fetchLanguages();

        // --- Genres with TMDB ID mapping ---
        String[] genres = {"Action", "Adventure", "Animation", "Comedy", "Crime",
                "Documentary", "Drama", "Family", "Fantasy", "History",
                "Horror", "Music", "Mystery", "Romance", "Sci-Fi",
                "Thriller", "War", "Western"};

        // TMDB genre IDs (aligned with your labels, "Sci-Fi" kept as label)
        int[] genreIDs = {28, 12, 16, 35, 80, 99, 18, 10751, 14, 36,
                27, 10402, 9648, 10749, 878, 53, 10752, 37};

        genreContainer.removeAllViews();


        for (int i = 0; i < genres.length; i++) {
            final int index = i;

            TextView chip = new TextView(requireContext());
            chip.setText(genres[i]);
            chip.setTextColor(getResources().getColor(R.color.text_primary));
            chip.setBackgroundResource(R.drawable.bg_chip); // default background
            chip.setPadding(24, 12, 24, 12);

            // --- key property: store TMDB genre ID ---
            chip.setTag(genreIDs[i]);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(8, 0, 8, 0);
            chip.setLayoutParams(lp);

            chip.setOnClickListener(v -> {
                int genreID = (int) v.getTag();

                if (selectedGenres.contains(genreID)) {
                    // Deselect
                    selectedGenres.remove(Integer.valueOf(genreID));
                    chip.setBackgroundResource(R.drawable.bg_chip);
                    chip.setTextColor(getResources().getColor(R.color.text_primary));
                } else {
                    // Select
                    selectedGenres.add(genreID);
                    chip.setBackgroundResource(R.drawable.bg_chip_selected);
                    chip.setTextColor(getResources().getColor(R.color.black));
                }
            });

            genreContainer.addView(chip);
        }
    }

    private void fetchLanguages() {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.themoviedb.org/3/configuration/languages");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + V4_TOKEN);
                conn.setRequestProperty("accept", "application/json");

                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                JSONArray jsonArray = new JSONArray(result.toString());
                List<Language> languageList = new ArrayList<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    String iso = obj.getString("iso_639_1");
                    String name = obj.getString("english_name");
                    languageList.add(new Language(iso, name));
                }

                // Sort alphabetically
                Collections.sort(languageList,
                        (a, b) -> a.getName().compareToIgnoreCase(b.getName()));

                // Fallback
                if (languageList.isEmpty()) {
                    languageList.add(new Language("en", "English"));
                }

                if (!isAdded()) return;

                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;

                    // Prepend "All" option
                    List<Language> displayList = new ArrayList<>();
                    displayList.add(new Language("", "All")); // empty ISO = no filter
                    displayList.addAll(languageList);

                    ArrayAdapter<Language> adapter = new ArrayAdapter<>(
                            requireContext(),
                            R.layout.spinner_item,
                            displayList
                    );
                    adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                    spinnerLanguage.setAdapter(adapter);
                });

            } catch (Exception e) {
                e.printStackTrace();

                if (!isAdded()) return;

                requireActivity().runOnUiThread(() -> {
                    List<Language> fallback = new ArrayList<>();
                    fallback.add(new Language("en", "English"));

                    spinnerLanguage.setAdapter(new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_spinner_dropdown_item,
                            fallback
                    ));
                });
            }
        }).start();
    }


    private void searchActors(String query, LinearLayout llActorResults) {
        if (query.isEmpty()) return;

        String url;

        // wrap in try-catch for URLEncoder
        try {
            url = "https://api.themoviedb.org/3/search/person"
                    + "?api_key=801da655375c1e627e8d97311ff30488"
                    + "&query=" + java.net.URLEncoder.encode(query, "UTF-8");
        } catch (Exception e) {
            url = "https://api.themoviedb.org/3/search/person"
                    + "?api_key=801da655375c1e627e8d97311ff30488"
                    + "&query=" + query;
        }

        final String finalUrl = url;

        new Thread(() -> {
            try {
                java.net.URL urlObj = new java.net.URL(finalUrl);
                java.net.HttpURLConnection conn =
                        (java.net.HttpURLConnection) urlObj.openConnection();
                conn.setRequestProperty("Authorization",
                        "Bearer " + SearchFragment.V4_TOKEN);
                conn.setRequestProperty("accept", "application/json");

                java.io.InputStream is = conn.getInputStream();
                java.io.BufferedReader reader =
                        new java.io.BufferedReader(new java.io.InputStreamReader(is));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) result.append(line);

                org.json.JSONObject json = new org.json.JSONObject(result.toString());
                org.json.JSONArray results = json.getJSONArray("results");

                if (!isAdded()) return;

                requireActivity().runOnUiThread(() -> {
                    llActorResults.removeAllViews();

                    try {
                        if (results.length() == 0) {
                            rvSearchResults.setVisibility(View.GONE);
                            llSearchEmpty.setVisibility(View.VISIBLE);
                            tvSearchEmpty.setText("No results for \"" + query + "\"");
                            return;
                        }
                        llSearchEmpty.setVisibility(View.GONE);
                        for (int i = 0; i < Math.min(results.length(), 15); i++) {
                            org.json.JSONObject actor = results.getJSONObject(i);

                            int personId      = actor.getInt("id");
                            String name       = actor.getString("name");
                            String profilePath = actor.isNull("profile_path")
                                    ? null : actor.getString("profile_path");

                            // known for
                            StringBuilder knownFor = new StringBuilder();
                            if (actor.has("known_for")) {
                                org.json.JSONArray kf = actor.getJSONArray("known_for");
                                for (int j = 0; j < Math.min(kf.length(), 2); j++) {
                                    org.json.JSONObject kfItem = kf.getJSONObject(j);
                                    if (kfItem.has("title")) {
                                        if (knownFor.length() > 0) knownFor.append(", ");
                                        knownFor.append(kfItem.getString("title"));
                                    }
                                }
                            }

                            View itemView = LayoutInflater.from(getContext())
                                    .inflate(R.layout.item_actor, llActorResults, false);

                            ImageView imgPhoto    = itemView.findViewById(R.id.imgActorThumb);
                            TextView tvName       = itemView.findViewById(R.id.tvActorItemName);
                            TextView tvKnownFor   = itemView.findViewById(R.id.tvActorItemKnownFor);

                            tvName.setText(name);
                            tvKnownFor.setText(knownFor.length() > 0
                                    ? "Known for: " + knownFor : "Actor");

                            if (profilePath != null) {
                                com.squareup.picasso.Picasso.get()
                                        .load("https://image.tmdb.org/t/p/w185" + profilePath)
                                        .placeholder(R.drawable.bg_card_rounded)
                                        .into(imgPhoto);
                            }

                            int finalPersonId = personId;
                            itemView.setOnClickListener(v -> {
                                Bundle args = new Bundle();
                                args.putInt("person_id", finalPersonId);

                                ActorDetailsFragment fragment = new ActorDetailsFragment();
                                fragment.setArguments(args);

                                requireActivity().getSupportFragmentManager()
                                        .beginTransaction()
                                        .replace(R.id.fragmentContainer, fragment)
                                        .addToBackStack(null)
                                        .commit();
                            });

                            llActorResults.addView(itemView);
                        }
                    } catch (Exception e) {
                        Log.e("ACTOR_SEARCH", e.toString());
                    }
                });

            } catch (Exception e) {
                Log.e("ACTOR_SEARCH", e.toString());
            }
        }).start();
    }



    // --- Animations ---
    public static void expand(final View v) {
        v.measure(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        final int targetHeight = v.getMeasuredHeight();
        v.getLayoutParams().height = 0;
        v.setVisibility(View.VISIBLE);

        Animation a = new Animation() {
            protected void applyTransformation(float t, Transformation tr) {
                v.getLayoutParams().height = t == 1
                        ? LinearLayout.LayoutParams.WRAP_CONTENT
                        : (int) (targetHeight * t);
                v.requestLayout();
            }
        };

        a.setDuration((int) (targetHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }
    public static void collapse(final View v) {
        final int initialHeight = v.getMeasuredHeight();

        Animation a = new Animation() {
            protected void applyTransformation(float t, Transformation tr) {
                if (t == 1) v.setVisibility(View.GONE);
                else {
                    v.getLayoutParams().height = initialHeight - (int) (initialHeight * t);
                    v.requestLayout();
                }
            }
        };

        a.setDuration((int) (initialHeight / v.getContext().getResources().getDisplayMetrics().density));
        v.startAnimation(a);
    }
}