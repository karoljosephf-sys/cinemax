package com.example.tmbdmadproject;

public class HistoryMovie {
    public int movieId;
    public String title;
    public String posterPath;
    public int rating;
    public String review;
    public long timestamp;
    public boolean isHeader;      // ← true = this item is a date header
    public String headerLabel;    // ← "Today", "Yesterday", "Jan 01, 2025"

    public HistoryMovie() {}

    // constructor for normal movie items
    public HistoryMovie(int movieId, String title, String posterPath,
                        int rating, String review, long timestamp) {
        this.movieId    = movieId;
        this.title      = title;
        this.posterPath = posterPath;
        this.rating     = rating;
        this.review     = review;
        this.timestamp  = timestamp;
        this.isHeader   = false;
    }

    // constructor for header items
    public HistoryMovie(String headerLabel) {
        this.isHeader     = true;
        this.headerLabel  = headerLabel;
    }

    public String getPosterUrl() {
        if (posterPath == null) return null;
        return "https://image.tmdb.org/t/p/w500" + posterPath;
    }
}