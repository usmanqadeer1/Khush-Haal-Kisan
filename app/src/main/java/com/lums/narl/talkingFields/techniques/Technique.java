package com.lums.narl.talkingFields.techniques;

public class Technique {

    String name;
    int imageDrawable;

    public Technique(){

    }

    public Technique(String name, int imageDrawable){
        this.name = name;
        this.imageDrawable = imageDrawable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getImageDrawable() {
        return imageDrawable;
    }

    public void setImageDrawable(int imageDrawable) {
        this.imageDrawable = imageDrawable;
    }
}
