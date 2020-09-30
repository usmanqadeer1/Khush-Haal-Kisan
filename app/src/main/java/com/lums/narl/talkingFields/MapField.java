package com.lums.narl.talkingFields;

public class MapField {
    private String fieldName;
    private String coordinates;
    private String polygonID;
    private double area;
    private String cropType;
    private String date;

    public MapField(){

    }

    public MapField(String fieldName, String polygonID, String coordinates, double area, String date, String cropType){
        this.fieldName = fieldName;
        this.polygonID = polygonID;
        this.coordinates = coordinates;
        this.area = area;
        this.cropType=cropType;
        this.date = date;

    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(String coordinates) {
        this.coordinates = coordinates;
    }

    public String getPolygonID() {
        return polygonID;
    }

    public void setPolygonID(String polygonID) {
        this.polygonID = polygonID;
    }

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public String getCropType() {
        return cropType;
    }

    public void setCropType(String cropType) {
        this.cropType = cropType;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "MapField{" +
                "fieldName='" + fieldName + '\'' +
                ", coordinates='" + coordinates + '\'' +
                ", polygonID='" + polygonID + '\'' +
                ", area=" + area +
                ", cropType='" + cropType + '\'' +
                ", date='" + date + '\'' +
                '}';
    }
}
