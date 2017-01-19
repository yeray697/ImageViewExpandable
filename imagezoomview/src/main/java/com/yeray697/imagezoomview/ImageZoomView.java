package com.yeray697.imagezoomview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.io.ByteArrayOutputStream;


public class ImageZoomView extends RelativeLayout {

    private ImageZoomFragment fragment;
    //Save and restore instance keys
    private final static String ZOOM_KEY = "zoom";
    private final static String IMAGE_KEY = "image";
    private final static String THUMBVIEW_KEY = "thumbView";
    private final static String START_COLOR_KEY = "start_color";
    private final static String END_COLOR_KEY = "end_color";
    private final static String SUPER_STATE_KEY = "superState";

    //Properties
    private int startColor;
    private int endColor;
    private int zoomTime;

    //Views
    private ImageView destination;
    private RelativeLayout parent;

    //Zoom in and out variables
    private boolean zoomed;
    private Drawable image;
    private View thumbView;
    private OnBackPressedListener onBackPressedListener;

    //Animation variables
    private static Animator mCurrentAnimator;
    private Rect startBounds;
    private Rect finalBounds;
    private float startScale;
    private float startScaleFinal;
    private OnAnimationListener onAnimationListener;

    //ImageView highlight
    private static Rect rect;

    /**
     * Listener to manage event after user press back key
     */
    public interface OnBackPressedListener{
        /**
         * User pressed back key
         */
        void pressed();
    }

    /**
     * Listener to manage events before and after zoom in and out
     */
    public interface OnAnimationListener {
        /**
         * Called before zoom in
         */
        void preZoomIn();
        /**
         * Called after zoom in
         */
        void postZoomIn();
        /**
         * Called before zoom out
         */
        void preZoomOut();
        /**
         * Called after zoom out
         */
        void postZoomOut();
    }

    /* TODO: When you create the view programmatically, it doesn't save the state when it is rotated
    public ImageZoomView(Context context) {
        super(context,null);
        inflateView(null);
    }
   */

    public ImageZoomView(Context context, AttributeSet attrs) {
        super(context, attrs,0);
        inflateView(attrs);
    }

    public ImageZoomView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inflateView(attrs);
    }

    /**
     * Inflate the view and check if you pass xml attributes, they will be checked it
     * @param attrs Attributes
     */
    private void inflateView(AttributeSet attrs) {
        FragmentManager fragmentManager = ((android.support.v4.app.FragmentActivity) getContext()).getSupportFragmentManager();

        fragment = (ImageZoomFragment) fragmentManager.findFragmentByTag("IMAGE_FRAGMENT");
        if (fragment == null) {
            fragment = new ImageZoomFragment();
            fragmentManager.beginTransaction().add(fragment,"IMAGE_FRAGMENT").commit();
        }

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
            endColor = a.getColor(R.styleable.ImageZoomView_endColor,Color.parseColor("#55000000"));
            zoomTime = a.getInt(R.styleable.ImageZoomView_zoomTime,
                    getContext().getResources().getInteger(android.R.integer.config_shortAnimTime));

            a.recycle();
        } else {
            startColor = Color.parseColor("#00000000");
            endColor = Color.parseColor("#55000000");
            zoomTime = getContext().getResources().getInteger(android.R.integer.config_shortAnimTime);
        }
        this.setFocusableInTouchMode(true);
        this.requestFocus();
        this.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (zoomed && keyCode == KeyEvent.KEYCODE_BACK) {
                    zoomOut();
                    if (onBackPressedListener != null)
                        onBackPressedListener.pressed();
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

    /**
     * Zoom out the image
     */
    public void zoomOut() {
        destination.performClick();
    }

    /**
     * Method that zoom a imageview to full screen
     * Main sources:
     *      https://developer.android.com/training/animation/zoom.html
     *      https://www.youtube.com/watch?v=bSgUn2rZiko
     * @param container View container
     * @param image Image to display
     * @param thumbView ThumView view
     * @param onAnimationListener Listener to handle pre/post zoom in and out
     */
    public void zoomIn(View container, Drawable image, View thumbView, @Nullable final OnAnimationListener onAnimationListener) {

        this.onAnimationListener = onAnimationListener;
        this.thumbView = thumbView;
        fragment.setContainer(container);
        this.image = image;

        configInitialPoints(new ConfigInitialPointsListener() {
            @Override
            public void ended() {
                zoomInAnimation();

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
                        zoomOutAnimation();
                    }
                });
            }
        });
    }

    /**
     * Method that runs the zoom in animation
     */
    private void zoomInAnimation(){
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
    private void zoomOutAnimation() {
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
                        zoomOutAnimation();
                    }
                });

                configInitialPoints(null);
            }
            state = bundle.getParcelable(SUPER_STATE_KEY);
        }
        super.onRestoreInstanceState(state);
    }

    /**
     * Inner interface used to check if configInitialPoints() is called to zoom in or to restore instance state
     */
    private interface ConfigInitialPointsListener {
        /**
         * configInitialPoints ended
         */
        void ended();
    }

    /**
     * Contains operations to get coordinates that are used to play the animation
     */
    private void configInitialPoints(final ConfigInitialPointsListener configInitialPointsListener) {
        zoomed = true;
        if (onAnimationListener != null)
            onAnimationListener.preZoomIn();

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
                        fragment.getContainer().getGlobalVisibleRect(finalBounds, globalOffset);
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
        this.thumbView.requestLayout();
        this.bringToFront();
    }

    /**
     * Method that higlight an image when it is touched.
     * To use it, you must add it on view.OnTouchListener.
     * It only works when you add it to an ImageView that is going to be zoomed
     * @param v Touched view
     * @param event OnTouch event
     * @return Return false (not consuming on touch events
     */
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

    /**
     * Add to the passed view OnTouchListener, which calls within highlightImageOnTouch(View v, MotionEvent event)
     * It only works when you add it to an ImageView that is going to be zoomed
     * @param imageView ImageView that will be higlighted
     */
    public static void highlightImageOnTouch(ImageView imageView) {
        imageView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return highlightImageOnTouch(view,motionEvent);
            }
        });
    }

    /**
     * Set a listener to handle when the user presses back key
     * @param onBackPressedListener
     */
    public void setOnBackPressedListener(OnBackPressedListener onBackPressedListener) {
        this.onBackPressedListener = onBackPressedListener;
    }

    /**
     * Return the start color of the animation
     * @return
     */
    public int getStartColor() {
        return startColor;
    }

    /**
     * Set the start color of the animation
     * @param startColor
     */
    public void setStartColor(int startColor) {
        this.startColor = startColor;
    }

    /**
     * Return the zoom time of the animation
     * @return
     */
    public int getZoomTime() {
        return zoomTime;
    }

    /**
     * Set the zoom time of the animation
     * @param zoomTime
     */
    public void setZoomTime(int zoomTime) {
        this.zoomTime = zoomTime;
    }

    /**
     * Return the end color of the animation
     * @return
     */
    public int getEndColor() {
        return endColor;
    }

    /**
     * Set the end color of the animation
     * @param endColor
     */
    public void setEndColor(int endColor) {
        this.endColor = endColor;
    }

    /**
     * Return if the image is zoomed
     * @return
     */
    public boolean isZoomed() {
        return zoomed;
    }
}
