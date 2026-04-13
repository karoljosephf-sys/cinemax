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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ActorDetailsFragment extends Fragment {

    private static final String API_KEY  = "801da655375c1e627e8d97311ff30488";
    private static final String BASE_URL = "https://api.themoviedb.org/3/";

    ImageView imgActorPhoto, btnBack;
    TextView tvActorName, tvActorBirthday, tvActorBirthplace,
            tvActorDepartment, tvActorBio;
    RecyclerView rvActorMovies;

    int personId;
    List<Movie> movieList = new ArrayList<>();
    MovieAdapter movieAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_actor_details, container, false);

        imgActorPhoto      = view.findViewById(R.id.imgActorPhoto);
        btnBack            = view.findViewById(R.id.btnActorBack);
        tvActorName        = view.findViewById(R.id.tvActorName);
        tvActorBirthday    = view.findViewById(R.id.tvActorBirthday);
        tvActorBirthplace  = view.findViewById(R.id.tvActorBirthplace);
        tvActorDepartment  = view.findViewById(R.id.tvActorDepartment);
        tvActorBio         = view.findViewById(R.id.tvActorBio);
        rvActorMovies      = view.findViewById(R.id.rvActorMovies);

        rvActorMovies.setLayoutManager(new GridLayoutManager(getContext(), 3));
        movieAdapter = new MovieAdapter(getContext(), movieList,
                R.layout.item_movie_search, movie -> {
            Bundle args = new Bundle();
            args.putInt("movie_id",  movie.id);
            args.putString("title",  movie.title);
            args.putString("poster", movie.posterPath);
            args.putDouble("rating", movie.rating);

            MovieDetailsFragment fragment = new MovieDetailsFragment();
            fragment.setArguments(args);

            ((AppCompatActivity) getContext())
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit();
        });
        rvActorMovies.setAdapter(movieAdapter);

        btnBack.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        if (getArguments() != null) {
            personId = getArguments().getInt("person_id");
            loadActorDetails();
            loadActorMovies();
        }

        return view;
    }

    private void loadActorDetails() {
        String url = BASE_URL + "person/" + personId + "?api_key=" + API_KEY;
        RequestQueue queue = Volley.newRequestQueue(getContext());

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String name        = response.getString("name");
                        String birthday    = response.isNull("birthday")
                                ? "Unknown" : response.getString("birthday");
                        String birthplace  = response.isNull("place_of_birth")
                                ? "Unknown" : response.getString("place_of_birth");
                        String department  = response.isNull("known_for_department")
                                ? "Acting" : response.getString("known_for_department");
                        String bio         = response.isNull("biography")
                                ? "No biography available." : response.getString("biography");
                        String profilePath = response.isNull("profile_path")
                                ? null : response.getString("profile_path");

                        tvActorName.setText(name);
                        tvActorBirthday.setText("Born: " + birthday);
                        tvActorBirthplace.setText(birthplace);
                        tvActorDepartment.setText(department);
                        tvActorBio.setText(bio);

                        if (profilePath != null) {
                            Picasso.get()
                                    .load("https://image.tmdb.org/t/p/w500" + profilePath)
                                    .into(imgActorPhoto);
                        }

                    } catch (JSONException e) {
                        Log.e("ACTOR_DETAILS", e.toString());
                    }
                },
                error -> Log.e("ACTOR_DETAILS", error.toString())
        );

        queue.add(request);
    }

    private void loadActorMovies() {
        String url = BASE_URL + "person/" + personId
                + "/movie_credits?api_key=" + API_KEY;
        RequestQueue queue = Volley.newRequestQueue(getContext());

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray cast = response.getJSONArray("cast");
                        movieList.clear();

                        // sort by popularity
                        List<JSONObject> movies = new ArrayList<>();
                        for (int i = 0; i < cast.length(); i++) {
                            movies.add(cast.getJSONObject(i));
                        }
                        movies.sort((a, b) -> {
                            try {
                                return Double.compare(
                                        b.getDouble("popularity"),
                                        a.getDouble("popularity")
                                );
                            } catch (JSONException e) { return 0; }
                        });

                        for (JSONObject movie : movies) {
                            if (movie.isNull("poster_path")) continue;
                            if (!movie.has("title")) continue;

                            int id            = movie.getInt("id");
                            String title      = movie.getString("title");
                            String posterPath = movie.getString("poster_path");
                            double rating     = movie.optDouble("vote_average", 0);

                            movieList.add(new Movie(id, title, posterPath, rating));
                        }

                        movieAdapter.notifyDataSetChanged();

                    } catch (JSONException e) {
                        Log.e("ACTOR_MOVIES", e.toString());
                    }
                },
                error -> Log.e("ACTOR_MOVIES", error.toString())
        );

        queue.add(request);
    }
}