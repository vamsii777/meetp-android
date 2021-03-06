package download.crossally.apps.hizkiyyah.bean;

import android.graphics.drawable.Drawable;

import java.io.Serializable;

public class Intro implements Serializable {

    Drawable img ;
    String title;

    public Intro(Drawable img, String title) {
        this.img = img;
        this.title = title;
    }

    public Drawable getImg() {
        return img;
    }

    public void setImg(Drawable img) {
        this.img = img;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
