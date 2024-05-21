package com.learningkotlin.orgomaproject;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class Farm {
    private String idOfOwner;
    private String farmName;
    private List<CustomLatLng> coordinates;
    private List<String> oliveVarieties;
    private  boolean isOrganic;
    private boolean sprayed;
    private String workerId;
    // id of document where farm is saved(firebase given document id)
    private String idOfDocument;

    public Farm() {

    }

    public Farm(String idOfOwner, String farmName, List<CustomLatLng> coordinates, List<String> oliveVarieties, boolean isOrganic, boolean sprayed, String workerId) {
        this.idOfOwner = idOfOwner;
        this.farmName = farmName;
        this.coordinates = coordinates;
        this.oliveVarieties = oliveVarieties;
        this.isOrganic = isOrganic;
        this.sprayed = sprayed;
        this.workerId = workerId;
    }

    public String getidOfDocument() {
        return idOfDocument;
    }
    public void setIdOfDocument(String idOfDocument) {
        this.idOfDocument = idOfDocument;
    }
    public boolean isSprayed() {
        return sprayed;
    }

    public void setSprayed(boolean sprayed) {
        this.sprayed = sprayed;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getIdOfOwner() {
        return idOfOwner;
    }

    public void setIdOfOwner(String idOfOwner) {
        this.idOfOwner = idOfOwner;
    }

    public String getFarmName() {
        return farmName;
    }

    public void setFarmName(String farmName) {
        this.farmName = farmName;
    }

    public List<CustomLatLng> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<CustomLatLng> coordinates) {
        this.coordinates = coordinates;
    }

    public List<String> getOliveVarieties() {
        return oliveVarieties;
    }

    public void setOliveVarieties(List<String> oliveVarieties) {
        this.oliveVarieties = oliveVarieties;
    }

    public boolean isOrganic() {
        return isOrganic;
    }

    public void setOrganic(boolean organic) {
        isOrganic = organic;
    }
}
