package com.yeray697.imagezoomview;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.content.ContextCompat;
import android.text.style.BackgroundColorSpan;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.io.ByteArrayOutputStream;


public class ImageZoomView extends RelativeLayout {

    private final static String ZOOM_KEY = "zoom";
    private final static String IMAGE_KEY = "image";
    private final static String THUMBVIEW_KEY = "thumbView";
    private final static String CONTAINER_ID_KEY = "containerId";
    private final static String START_COLOR_KEY = "start_color";
    private final static String END_COLOR_KEY = "end_color";
    private final static String SUPER_STATE_KEY = "superState";

    private int startColor;
    private int endColor;
    private int zoomTime;

    ImageView destination;
    RelativeLayout parent;

    private boolean zoomed;
    private Drawable image;
    private View thumbView;
    private int containerId;

    //Animation variables
    private static Animator mCurrentAnimator;
    private Rect startBounds;
    private Rect finalBounds;
    private float startScale;
    private float startScaleFinal;
    private OnAnimationListener onAnimationListener;

    public interface OnAnimationListener {
        void preZoomIn();
        void postZoomIn();
        void preZoomOut();
        void postZoomOut();
    }

    public ImageZoomView(Context context) {
        super(context,null);
        inflateView(null);
    }

    public ImageZoomView(Context context, AttributeSet attrs) {
        super(context, attrs,0);
        inflateView(attrs);
    }

    public ImageZoomView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView(attrs);
    }

    private void inflateView(AttributeSet attrs) {
        String infService = Context.LAYOUT_INFLATER_SERVICE;
        LayoutInflater li = (LayoutInflater)getContext().getSystemService(infService);
        li.inflate(R.layout.imagezoomview, this, true);
        parent = (RelativeLayout) findViewById(R.id.rlImageZoom);
        destination = (ImageView) findViewById(R.id.ivExpanded);
        if (attrs != null) {
            TypedArray a =
                    getContext().obtainStyledAttributes(attrs,
                            R.styleable.ImageZoomView);

            startColor = a.getColor(R.styleable.ImageZoomView_startColor,Color.parseColor("#00000000"));
            endColor = a.getColor(R.styleable.ImageZoomView_endColor,Color.parseColor("#BB000000"));
            zoomTime = a.getInt(R.styleable.ImageZoomView_zoomTime,
                    getContext().getResources().getInteger(android.R.integer.config_shortAnimTime));

            a.recycle();
        } else {
            startColor = Color.parseColor("#00000000");
            endColor = Color.parseColor("#BB000000");
            zoomTime = getContext().getResources().getInteger(android.R.integer.config_shortAnimTime);
        }
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
        destination.performClick();
    }

    /**
     *
     * Method that zoom a imageview to full screen
     * Main sources:
     *      https://developer.android.com/training/animation/zoom.html
     *      https://www.youtube.com/watch?v=bSgUn2rZiko
     * @param containerId
     * @param image
     * @param thumbView
     * @param onAnimationListener
     */
    public void zoomImageFromThumb(int containerId, Drawable image, View thumbView, final OnAnimationListener onAnimationListener) {

        this.onAnimationListener = onAnimationListener;
        this.thumbView = thumbView;
        this.containerId = containerId;
        this.image = image;

        configInitialPoints(new ConfigInitialPointsListener() {
            @Override
            public void ended() {
                zoomIn();

                startScaleFinal = startScale;

                destination.setOnClickListener(new View.OnClickListener() {
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
        });
    }

    /**
     * Method that runs the zoom in animation
     */
    private void zoomIn(){
        //Background fade in animation
        ObjectAnimator fadeIn = ObjectAnimator.ofInt(parent, "backgroundColor",startColor,endColor);
        fadeIn.setRepeatCount(0);
        fadeIn.setRepeatMode(ValueAnimator.REVERSE);
        fadeIn.setEvaluator(new ArgbEvaluator());
        //
        AnimatorSet set = new AnimatorSet();

        set.setTarget(destination);
        set
                .play(ObjectAnimator.ofFloat(destination, View.X,
                        startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(destination, View.Y,
                        startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(destination, View.SCALE_X,
                        startScale, 1f)).with(ObjectAnimator.ofFloat(destination,
                View.SCALE_Y, startScale, 1f))
                .with(fadeIn);
        set.setDuration(zoomTime);
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
        mCurrentAnimator = set;
    }

    /**
     * Method that runs the zoom out animation
     */
    private void zoomOut() {
        //Background fade out animation
        ObjectAnimator fadeOut = ObjectAnimator.ofInt(parent, "backgroundColor",endColor,startColor);
        fadeOut.setRepeatCount(0);
        fadeOut.setRepeatMode(ValueAnimator.REVERSE);
        fadeOut.setEvaluator(new ArgbEvaluator());
        //
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
                                View.SCALE_Y, startScaleFinal))
                .with(fadeOut);
        set.setDuration(zoomTime);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
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
        AnimatorSet setBackground = new AnimatorSet();
        setBackground.setTarget(parent);

        ObjectAnimator fadeIn = ObjectAnimator.ofInt(parent, "backgroundColor",endColor,startColor);
        fadeIn.setRepeatCount(0);
        fadeIn.setRepeatMode(ValueAnimator.REVERSE);
        fadeIn.setEvaluator(new ArgbEvaluator());
        setBackground.play(fadeIn);
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

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(SUPER_STATE_KEY, super.onSaveInstanceState());
        bundle.putBoolean(ZOOM_KEY, this.zoomed);
        if (zoomed){
            Bitmap bitmap = ((BitmapDrawable)this.image).getBitmap();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] bitmapdata = stream.toByteArray();
            bundle.putByteArray(IMAGE_KEY,bitmapdata);
            bundle.putInt(START_COLOR_KEY,startColor);
            bundle.putInt(END_COLOR_KEY,endColor);
            bundle.putInt(THUMBVIEW_KEY,thumbView.getId());
            bundle.putInt(CONTAINER_ID_KEY,containerId);
        }
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle)
        {
            Bundle bundle = (Bundle) state;
            this.zoomed = bundle.getBoolean(ZOOM_KEY);
            if (zoomed) {
                byte[] image = bundle.getByteArray(IMAGE_KEY);
                Bitmap bmp = BitmapFactory.decodeByteArray(image, 0, image.length);
                this.image = new BitmapDrawable(getResources(),bmp);
                destination.setImageDrawable(this.image);

                destination.setVisibility(VISIBLE);
                parent.setVisibility(VISIBLE);

                thumbView = ((View)this.getParent()).findViewById(bundle.getInt(THUMBVIEW_KEY));
                containerId = bundle.getInt(CONTAINER_ID_KEY);
                startColor = bundle.getInt(START_COLOR_KEY);
                endColor = bundle.getInt(END_COLOR_KEY);

                parent.setBackgroundColor(endColor);

                destination.setOnClickListener(new View.OnClickListener() {
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

                configInitialPoints(null);
            }
            state = bundle.getParcelable(SUPER_STATE_KEY);
        }
        super.onRestoreInstanceState(state);
    }

    private interface ConfigInitialPointsListener {
        void ended();
    }

    /**
     * Contains operations to get coordinates, used to play the animation
     */
    private void configInitialPoints(final ConfigInitialPointsListener configInitialPointsListener) {
        zoomed = true;
        if (onAnimationListener != null)
            onAnimationListener.preZoomIn();
        this.bringToFront();

        if (mCurrentAnimator != null) {
            mCurrentAnimator.cancel();
        }

        destination.setImageDrawable(this.image);

        startBounds = new Rect();
        finalBounds = new Rect();
        final Point globalOffset = new Point();


        this.thumbView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {

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

                        if (configInitialPointsListener != null)
                            configInitialPointsListener.ended();

                        //Removing listener
                        thumbView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });

    }

    private static Rect rect;
    public static boolean highlightImageOnTouch(View v, MotionEvent event) {
        ImageView image = (ImageView) v;
        if(event.getAction() == MotionEvent.ACTION_DOWN){
            image.setColorFilter(Color.argb(50, 0, 0, 0));
            rect = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
        }
        if(event.getAction() == MotionEvent.ACTION_UP){
            image.setColorFilter(Color.argb(0, 0, 0, 0));
        }
        if(event.getAction() == MotionEvent.ACTION_MOVE){
            if(!rect.contains(v.getLeft() + (int) event.getX(), v.getTop() + (int) event.getY())){
                image.setColorFilter(Color.argb(0, 0, 0, 0));
            }
        }
        return false;
    }
    public static void highlightImageOnTouch(ImageView imageView) {
        imageView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return highlightImageOnTouch(view,motionEvent);
            }
        });
    }

    public int getStartColor() {
        return startColor;
    }

    public void setStartColor(int startColor) {
        this.startColor = startColor;
    }

    public int getZoomTime() {
        return zoomTime;
    }

    public void setZoomTime(int zoomTime) {
        this.zoomTime = zoomTime;
    }

    public int getEndColor() {
        return endColor;
    }

    public void setEndColor(int endColor) {
        this.endColor = endColor;
    }
}
