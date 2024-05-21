package com.learningkotlin.orgomaproject;

public class User {
    boolean signedIn;
    private String email;
    private String userID;
    private String type; // Farmer or Sprayer
    private double latitude;
    private double longitude;

    public User() {
    }

    public User(boolean signedIn, String email, String userID, String type, double latitude, double longitude) {
        this.signedIn = signedIn;
        this.email = email;
        this.userID = userID;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public boolean isSignedIn() {
        return signedIn;
    }

    public void setSignedIn(boolean signedIn) {
        this.signedIn = signedIn;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
