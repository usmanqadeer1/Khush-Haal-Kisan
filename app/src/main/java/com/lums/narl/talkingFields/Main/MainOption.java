package com.lums.narl.talkingFields.Main;

public class MainOption {
    private String optionName;
    private int imageDrawable;

    public MainOption(){

    }

    public MainOption(String optionName, int imageDrawable){
        this.optionName = optionName;
        this.imageDrawable = imageDrawable;
    }

    public String getOptionName() {
        return optionName;
    }

    public void setOptionName(String optionName) {
        this.optionName = optionName;
    }

    public int getImageDrawable() {
        return imageDrawable;
    }

    public void setImageDrawable(int imageDrawable) {
        this.imageDrawable = imageDrawable;
    }
}
