package com.lums.narl.talkingFields;

public class User {
    private String username, phone;
    private MapField[] Field_Data;
    public User(){

    }

    public User(String username, String phone, MapField[] Field_Data){
        this.username = username;
        this.phone = phone;
        this.Field_Data = Field_Data;

    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String Phone) {
        this.phone = Phone;
    }

    public MapField[] getField_Data() {
        return Field_Data;
    }

    public void setField_Data(MapField[] field_Data) {
        Field_Data = field_Data;
    }
}

