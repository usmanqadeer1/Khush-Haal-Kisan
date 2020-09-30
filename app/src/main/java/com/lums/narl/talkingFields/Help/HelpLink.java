package com.lums.narl.talkingFields.Help;

public class HelpLink {
    private String linkName;
    private String link;
    private int imageDrawable;

    public HelpLink(){

    }

    public HelpLink(String linkName, String link, int imageDrawable){
        this.linkName = linkName;
        this.link = link;
        this.imageDrawable = imageDrawable;
    }

    public String getLinkName() {
        return linkName;
    }

    public void setLinkName(String linkName) {
        this.linkName = linkName;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public int getImageDrawable() {
        return imageDrawable;
    }

    public void setImageDrawable(int imageDrawable) {
        this.imageDrawable = imageDrawable;
    }
}
