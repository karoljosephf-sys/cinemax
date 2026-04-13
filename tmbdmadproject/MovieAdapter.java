package com.example.tmbdmadproject;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {

    Context context;
    List<Movie> movieList;
    int layoutRes; // ← which layout to use

    public interface OnMovieClickListener {
        void onMovieClick(Movie movie);
    }

    OnMovieClickListener listener;

    public MovieAdapter(Context context, List<Movie> movieList, int layoutRes, OnMovieClickListener listener) {
        this.context = context;
        this.movieList = movieList;
        this.layoutRes = layoutRes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(layoutRes, parent, false); // ← uses whatever layout was passed
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Movie movie = movieList.get(position);

        holder.tvTitle.setText(movie.title);
        holder.tvRating.setText(String.format("%.1f", movie.rating));

        if (movie.posterPath != null) {
            Picasso.get()
                    .load(movie.getPosterUrl())
                    .into(holder.imgPoster);
        } else {
            holder.imgPoster.setImageResource(R.drawable.ic_launcher_foreground);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMovieClick(movie);
            }
        });
    }

    @Override
    public int getItemCount() {
        return movieList.size();
    }

    static class MovieViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPoster;
        TextView tvTitle, tvRating;

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPoster = itemView.findViewById(R.id.imgPoster);
            tvTitle   = itemView.findViewById(R.id.tvTitle);
            tvRating  = itemView.findViewById(R.id.tvRating);
        }
    }
}