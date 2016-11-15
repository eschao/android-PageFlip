package com.eschao.android.widget.sample.pageflip;

import android.app.Activity;
import android.os.Bundle;

import android.view.GestureDetector;

import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;

public class SampleActivity extends Activity implements OnGestureListener {
    /** Called when the activity is first created. */

    PageFlipView mPageFlipView;
    GestureDetector mGestureDetector;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		/*
        setContentView(R.layout.main);
        mView = (NeHeView)findViewById(R.id.neheview);
		*/
        mPageFlipView = new PageFlipView(this);
        setContentView(mPageFlipView);
        mGestureDetector = new GestureDetector(this, this);

        mPageFlipView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    //public boolean dispatchTouchEvent(MotionEvent e) {
        /*
        if (mView.isFlipStarted() && e.getAction() == MotionEvent.ACTION_UP) {
            mView.onFingerUp(e.getX(), e.getY());
            return true;
        }*/

    //if (mGestureDetector.onTouchEvent(e))
    //	return true;

    //   return super.dispatchTouchEvent(e);
    //}

    @Override
    protected void onResume() {
        super.onResume();

		/*
        Window win = getWindow();
        win.setFlags(LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN);
        */
        mPageFlipView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPageFlipView.onPause();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        //System.out.println("****onDown");
        mPageFlipView.onFingerDown(e.getX(), e.getY());
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY) {
        //System.out.println("****onFling");
        if (e2.getAction() == MotionEvent.ACTION_UP) {
            mPageFlipView.onFingerUp(e2.getX(), e2.getY());
            return true;
        }

        return false;
    }


    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                            float distanceY) {
        //System.out.println("****onScroll");
        if (e1.getAction() == MotionEvent.ACTION_UP ||
            e2.getAction() == MotionEvent.ACTION_UP) {
            mPageFlipView.onFingerUp(e2.getX(), e2.getY());
        } else {
            mPageFlipView.onFingerMove(e2.getX(), e2.getY());
        }
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        //System.out.println("****onSingleTapUp");
        mPageFlipView.onFingerUp(e.getX(), e.getY());
        return true;
    }
}
