package com.yeray697.imagezoomview;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.io.ByteArrayOutputStream;


public class ImageZoomView extends RelativeLayout {
    ImageView destination;
    RelativeLayout parent;

    private boolean zoomed;
    private Drawable image;

    public ImageZoomView(Context context) {
        super(context,null);
        inflateView(context);
    }

    public ImageZoomView(Context context, AttributeSet attrs) {
        super(context, attrs,0);
        inflateView(context);
    }

    public ImageZoomView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView(context);
    }

    private void inflateView(Context context) {
        String infService = Context.LAYOUT_INFLATER_SERVICE;
        LayoutInflater li =
                (LayoutInflater)getContext().getSystemService(infService);
        li.inflate(R.layout.imagezoomview, this, true);
        parent = (RelativeLayout) findViewById(R.id.rlImageZoom);
        destination = (ImageView) findViewById(R.id.ivExpanded);
        this.setFocusableInTouchMode(true);
        this.requestFocus();
        this.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (zoomed && keyCode == KeyEvent.KEYCODE_BACK) {
                    dismissImage();
                    return true;
                }
                return false;
            }
        });
        zoomed = false;
        destination.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {}
        });
    }

    public void dismissImage() {
        parent.performClick();
    }


    private static Animator mCurrentAnimator;
    private static int mShortAnimationDuration = -1;

    public interface OnAnimationListener {
        void preZoomIn();
        void postZoomIn();
        void preZoomOut();
        void postZoomOut();
    }

    private Rect startBounds;
    private Rect finalBounds;
    private float startScale;
    private float startScaleFinal = startScale;
    private OnAnimationListener onAnimationListener;
    private View thumbView;
    /**
     * Main source: https://developer.android.com/training/animation/zoom.html
     */
    public void zoomImageFromThumb(int containerId, Drawable image, View thumbView, final OnAnimationListener onAnimationListener) {
        this.onAnimationListener = onAnimationListener;
        this.thumbView = thumbView;
        zoomed = true;
        if (onAnimationListener != null)
            onAnimationListener.preZoomIn();
        changeOrientation(getContext().getResources().getConfiguration().orientation);

        mShortAnimationDuration = getContext().getResources().getInteger(
                android.R.integer.config_shortAnimTime);

        if (mCurrentAnimator != null) {
            mCurrentAnimator.cancel();
        }

        this.image = image;
        destination.setImageDrawable(this.image);

        startBounds = new Rect();
        finalBounds = new Rect();
        final Point globalOffset = new Point();


        thumbView.getGlobalVisibleRect(startBounds);
        ((Activity)getContext()).findViewById(containerId)
                .getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);


        if ((float) finalBounds.width() / finalBounds.height()
                > (float) startBounds.width() / startBounds.height()) {
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        destination.setVisibility(View.VISIBLE);
        parent.setVisibility(View.VISIBLE);


        destination.setPivotX(0f);
        destination.setPivotY(0f);

        zoomIn();


        startScaleFinal = startScale;
        parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                zoomed = false;
                if (mCurrentAnimator != null) {
                    mCurrentAnimator.cancel();
                }
                if (onAnimationListener != null)
                    onAnimationListener.preZoomOut();
                zoomOut();
            }
        });
    }

    private void zoomIn(){

        AnimatorSet set = new AnimatorSet();

        set.setTarget(destination);
        set
                .play(ObjectAnimator.ofFloat(destination, View.X,
                        startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(destination, View.Y,
                        startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(destination, View.SCALE_X,
                        startScale, 1f)).with(ObjectAnimator.ofFloat(destination,
                View.SCALE_Y, startScale, 1f));
        set.setDuration(mShortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentAnimator = null;
                if (onAnimationListener != null)
                    onAnimationListener.postZoomIn();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mCurrentAnimator = null;
            }
        });
        set.start();
        AnimatorSet setBackground = (AnimatorSet) AnimatorInflater.loadAnimator(getContext(),R.animator.image_zoom_in);

        setBackground.setTarget(parent);

        setBackground.setInterpolator(new DecelerateInterpolator());
        setBackground.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mCurrentAnimator = null;
            }
        });
        setBackground.start();
        mCurrentAnimator = set;
    }

    private void zoomOut() {
        AnimatorSet set = new AnimatorSet();
        set.play(ObjectAnimator
                .ofFloat(destination, View.X, startBounds.left))
                .with(ObjectAnimator
                        .ofFloat(destination,
                                View.Y,startBounds.top))
                .with(ObjectAnimator
                        .ofFloat(destination,
                                View.SCALE_X, startScaleFinal))
                .with(ObjectAnimator
                        .ofFloat(destination,
                                View.SCALE_Y, startScaleFinal));
        set.setDuration(mShortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                thumbView.setAlpha(1f);
                destination.setVisibility(View.GONE);
                parent.setVisibility(View.GONE);
                mCurrentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                destination.setVisibility(View.GONE);
                parent.setVisibility(View.GONE);
                mCurrentAnimator = null;
            }
        });
        set.start();
        AnimatorSet setBackground = (AnimatorSet) AnimatorInflater.loadAnimator(getContext(),R.animator.image_zoom_out);

        setBackground.setTarget(parent);

        setBackground.setInterpolator(new DecelerateInterpolator());
        setBackground.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentAnimator = null;
                if (onAnimationListener != null)
                    onAnimationListener.postZoomOut();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mCurrentAnimator = null;
            }
        });
        setBackground.start();
        mCurrentAnimator = set;
    }

    public void changeOrientation(int vertical){
        ViewGroup.LayoutParams params = null;
        if (vertical == Configuration.ORIENTATION_PORTRAIT) {
            params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        } else if (vertical == Configuration.ORIENTATION_LANDSCAPE){
            params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        if (params != null)
            destination.setLayoutParams(params);

        this.bringToFront();
    }

    @Override
    public Parcelable onSaveInstanceState()
    {
        Bundle bundle = new Bundle();
        bundle.putParcelable("superState", super.onSaveInstanceState());
        bundle.putBoolean("zoom", this.zoomed);
        if (zoomed){
            Bitmap bitmap = ((BitmapDrawable)this.image).getBitmap();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] bitmapdata = stream.toByteArray();
            bundle.putByteArray("image",bitmapdata);
            int color = Color.TRANSPARENT;
            Drawable background = parent.getBackground();
            if (background instanceof ColorDrawable)
                color = ((ColorDrawable) background).getColor();
            bundle.putInt("back_color",color);
        }
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state)
    {
        if (state instanceof Bundle)
        {
            Bundle bundle = (Bundle) state;
            this.zoomed = bundle.getBoolean("zoom");
            if (zoomed) {
                byte[] image = bundle.getByteArray("image");
                Bitmap bmp = BitmapFactory.decodeByteArray(image, 0, image.length);
                this.image = new BitmapDrawable(getResources(),bmp);
                destination.setImageDrawable(this.image);
                destination.setVisibility(VISIBLE);
                parent.setVisibility(VISIBLE);
                changeOrientation(getContext().getResources().getConfiguration().orientation);
                parent.setBackgroundColor(bundle.getInt("back_color"));
                parent.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        zoomed = false;
                        if (mCurrentAnimator != null) {
                            mCurrentAnimator.cancel();
                        }
                        if (onAnimationListener != null)
                            onAnimationListener.preZoomOut();
                        zoomOut();
                    }
                });
            }
            state = bundle.getParcelable("superState");
        }

        super.onRestoreInstanceState(state);
    }
}
