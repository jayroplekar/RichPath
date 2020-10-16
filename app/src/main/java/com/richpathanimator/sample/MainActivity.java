package com.richpathanimator.sample;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import com.richpath.RichPath;
import com.richpath.RichPath.OnPathClickListener;
import com.richpath.RichPathView;
import com.richpathanimator.AnimationListener;
import com.richpathanimator.RichPathAnimator;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RichPathView richPathView = findViewById(R.id.ic_android);
        richPathView.setOnPathClickListener(new OnPathClickListener() {
            @Override
            public void onClick(RichPath richPath) {
                int color = richPath.getFillColor();
                String mState = richPath.getName();
                if (mState != null) {
                    Log.d(getLocalClassName(), "Name: " + mState + "Current Color: " + color);
                    if (color == Color.RED) {
                        richPath.setFillColor(Color.GRAY);
                    } else if (color == Color.BLUE) {
                        richPath.setFillColor(Color.RED);
                    } else {
                        richPath.setFillColor(Color.BLUE);
                    }
                }
            }
            @Override
            public void onResume() {

            }
        });
    }

            @Override
            public void onResume() {
                MainActivity.super.onResume();
            }


    public void animateAndroid(View view) {
        animateAndroid();
    }

    private void animateAndroid() {

        RichPathView androidRichPathView = findViewById(R.id.ic_android);

        final RichPath[] allPaths = androidRichPathView.findAllRichPaths();
        final RichPath head = androidRichPathView.findRichPathByName("head");
        final RichPath body = androidRichPathView.findRichPathByName("body");
        final RichPath rHand = androidRichPathView.findRichPathByName("r_hand");
        final RichPath lHand = androidRichPathView.findRichPathByName("l_hand");

        RichPathAnimator.animate(allPaths)
                .trimPathEnd(0, 1)
                .duration(800)
                .animationListener(new AnimationListener() {
                    @Override
                    public void onStart() {
                        head.setFillColor(Color.TRANSPARENT);
                        body.setFillColor(Color.TRANSPARENT);
                        rHand.setFillColor(Color.TRANSPARENT);
                        lHand.setFillColor(Color.TRANSPARENT);
                        rHand.setRotation(0);
                    }

                    @Override
                    public void onStop() {
                    }
                })
                .thenAnimate(allPaths)
                .fillColor(Color.TRANSPARENT, 0xFFa4c639)
                .interpolator(new AccelerateInterpolator())
                .duration(900)
                .thenAnimate(rHand)
                .rotation(-150)
                .duration(700)
                .thenAnimate(rHand)
                .rotation(-150, -130, -150, -130, -150, -130, -150)
                .duration(2000)
                .thenAnimate(rHand)
                .rotation(0)
                .duration(500)
                .start();
    }

}