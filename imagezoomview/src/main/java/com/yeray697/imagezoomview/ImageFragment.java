package com.yeray697.imagezoomview;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;

/**
 * Created by yeray697 on 18/01/17.
 */

public class ImageFragment extends Fragment {
    //Zoom in and out variables
    private boolean zoomed;
    private Drawable image;
    private View thumbView;
    private View containerId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public boolean isZoomed() {
        return zoomed;
    }

    public void setZoomed(boolean zoomed) {
        this.zoomed = zoomed;
    }

    public Drawable getImage() {
        return image;
    }

    public void setImage(Drawable image) {
        this.image = image;
    }

    public View getThumbView() {
        return thumbView;
    }

    public void setThumbView(View thumbView) {
        this.thumbView = thumbView;
    }

    public View getContainerId() {
        return containerId;
    }

    public void setContainerId(View containerId) {
        this.containerId = containerId;
    }
}
