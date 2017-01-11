package com.yeray697.imagezoomsample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.yeray697.imagezoomview.ImageZoomView;

public class Sample_Activity extends AppCompatActivity {
    ImageZoomView izv;
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        izv = (ImageZoomView) findViewById(R.id.ivZoom);
        imageView = (ImageView) findViewById(R.id.ivSample);
        ImageZoomView.highlightImageOnTouch(imageView);
        /*imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return ImageZoomView.highlightImageOnTouch(view,motionEvent);
            }
        });*/
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //izv = new ImageZoomView(Sample_Activity.this);
                //((RelativeLayout)findViewById(R.id.activity_sample)).addView(izv);
                izv.zoomImageFromThumb(R.id.activity_sample,getResources().getDrawable(R.mipmap.ic_launcher),view, new ImageZoomView.OnAnimationListener() {

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
                });
            }
        });

    }
}
