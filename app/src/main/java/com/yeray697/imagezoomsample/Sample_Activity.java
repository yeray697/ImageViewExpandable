package com.yeray697.imagezoomsample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.yeray697.imagezoomview.ImageZoomView;

public class Sample_Activity extends AppCompatActivity {
    ImageZoomView izv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);

        izv = (ImageZoomView) findViewById(R.id.ivZoom);
        (findViewById(R.id.ivSample)).setOnClickListener(new View.OnClickListener() {
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
