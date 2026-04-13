package com.example.tmbdmadproject;

public class UserList {
    public String listId;
    public String name;
    public String description;
    public String createdBy;
    public String creatorName;
    public long timestamp;
    public int movieCount;

    public UserList() {}

    public UserList(String listId, String name, String description,
                    String createdBy, String creatorName, long timestamp) {
        this.listId      = listId;
        this.name        = name;
        this.description = description;
        this.createdBy   = createdBy;
        this.creatorName = creatorName;
        this.timestamp   = timestamp;
        this.movieCount  = 0;
    }
}