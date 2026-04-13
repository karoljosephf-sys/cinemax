package com.example.tmbdmadproject;

public class Movie {
    public int id;
    public String title;
    public String posterPath;
    public double rating;

    public Movie(int id, String title, String posterPath, double rating) {
        this.id = id;
        this.title = title;
        this.posterPath = posterPath;
        this.rating = rating;
    }

    public String getPosterUrl() {
        return "https://image.tmdb.org/t/p/w500" + posterPath;
    }
}