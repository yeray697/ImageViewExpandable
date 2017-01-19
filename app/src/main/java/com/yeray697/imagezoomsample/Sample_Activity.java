package com.yeray697.imagezoomsample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.yeray697.imagezoomview.ImageZoomView;

public class Sample_Activity extends AppCompatActivity {
    ImageZoomView izv;
    ImageView imageView1;
    ImageView imageView2;
    ImageView imageView3;
    RelativeLayout rlContainer;

    private ImageZoomView.OnAnimationListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);
        rlContainer = (RelativeLayout) findViewById(R.id.activity_sample);

        listener = new ImageZoomView.OnAnimationListener() {
            @Override
            public void preZoomIn() {

            }

            @Override
            public void postZoomIn() {

            }

            @Override
            public void preZoomOut() {

            }

            @Override
            public void postZoomOut() {

            }
        };
        imageView1 = (ImageView) findViewById(R.id.ivSample);
        izv = (ImageZoomView) findViewById(R.id.ivZoom);
        //izv = new ImageZoomView(Sample_Activity.this);

        //((RelativeLayout)findViewById(R.id.activity_sample)).addView(izv);

        //ImageZoomView.highlightImageOnTouch(imageView1);
        imageView1.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return ImageZoomView.highlightImageOnTouch(view,motionEvent);
            }
        });
        imageView1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                izv.zoomIn(rlContainer,getResources().getDrawable(R.mipmap.ic_launcher),view, listener);

            }
        });

        imageView2 = (ImageView) findViewById(R.id.ivSample2);
        //ImageZoomView.highlightImageOnTouch(imageView2);
        imageView2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return ImageZoomView.highlightImageOnTouch(view,motionEvent);
            }
        });
        imageView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //izv = new ImageZoomView(Sample_Activity.this);
                //((RelativeLayout)findViewById(R.id.activity_sample)).addView(izv);
                izv.zoomIn(rlContainer,getResources().getDrawable(R.mipmap.ic_launcher),view, listener);

            }
        });

        imageView3 = (ImageView) findViewById(R.id.ivSample3);
        //ImageZoomView.highlightImageOnTouch(imageView3);
        imageView3.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return ImageZoomView.highlightImageOnTouch(view,motionEvent);
            }
        });
        imageView3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //izv = new ImageZoomView(Sample_Activity.this);
                //((RelativeLayout)findViewById(R.id.activity_sample)).addView(izv);
                izv.zoomIn(rlContainer,getResources().getDrawable(R.mipmap.ic_launcher),view, listener);

            }
        });
    }
}
