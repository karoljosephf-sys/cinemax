package com.example.tmbdmadproject;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_MOVIE  = 1;

    Context context;
    List<HistoryMovie> historyList;

    public HistoryAdapter(Context context, List<HistoryMovie> historyList) {
        this.context     = context;
        this.historyList = historyList;
    }

    @Override
    public int getItemViewType(int position) {
        return historyList.get(position).isHeader ? TYPE_HEADER : TYPE_MOVIE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.item_history_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.item_history, parent, false);
            return new HistoryViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        HistoryMovie item = historyList.get(position);

        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).tvHeaderLabel.setText(item.headerLabel);
            return;
        }

        HistoryViewHolder h = (HistoryViewHolder) holder;

        h.tvTitle.setText(item.title);

        if (item.rating > 0) {
            h.tvRating.setText(item.rating + " / 5");
        } else {
            h.tvRating.setText("Not rated");
        }

        if (item.review != null && !item.review.isEmpty()) {
            h.tvReview.setText("\"" + item.review + "\"");
            h.tvReview.setVisibility(View.VISIBLE);
        } else {
            h.tvReview.setVisibility(View.GONE);
        }

        if (item.timestamp > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
            h.tvDate.setText("Watched " + sdf.format(new Date(item.timestamp)));
        } else {
            h.tvDate.setText("Watched —");
        }

        if (item.posterPath != null && !item.posterPath.isEmpty()) {
            Picasso.get()
                    .load(item.getPosterUrl())
                    .placeholder(R.drawable.bg_card_rounded)
                    .error(R.drawable.bg_card_rounded)
                    .into(h.imgPoster);
        } else {
            h.imgPoster.setImageResource(R.drawable.bg_card_rounded);
        }

        h.itemView.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("movie_id",  item.movieId);
            args.putString("title",  item.title);
            args.putString("poster", item.posterPath);
            args.putDouble("rating", item.rating*2);

            MovieDetailsFragment fragment = new MovieDetailsFragment();
            fragment.setArguments(args);

            ((AppCompatActivity) context)
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    // ── ViewHolders ───────────────────────────────────────────────────

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeaderLabel;
        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeaderLabel = itemView.findViewById(R.id.tvHeaderLabel);
        }
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPoster;
        TextView tvTitle, tvRating, tvReview, tvDate;
        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPoster = itemView.findViewById(R.id.imgHistoryPoster);
            tvTitle   = itemView.findViewById(R.id.tvHistoryTitle);
            tvRating  = itemView.findViewById(R.id.tvHistoryRating);
            tvReview  = itemView.findViewById(R.id.tvHistoryReview);
            tvDate    = itemView.findViewById(R.id.tvHistoryDate);
        }
    }
}