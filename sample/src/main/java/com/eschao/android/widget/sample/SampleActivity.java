package com.eschao.android.widget.sample;

import android.app.Activity;
import android.os.Bundle;

import android.view.GestureDetector;

import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.Window;

import android.view.WindowManager.LayoutParams;

import com.eschao.android.widget.sample.R;

public class SampleActivity extends Activity implements OnGestureListener {
    /** Called when the activity is first created. */

    SampleView mView;
    GestureDetector mGestureDetector;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_main);
        mView = (SampleView)findViewById(R.id.sample_view);
        mGestureDetector = new GestureDetector(this, this);
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mView.isFlipStarted() && event.getAction() == MotionEvent.ACTION_UP) {
            mView.endFlip();
            return true;
        }

        if (mGestureDetector.onTouchEvent(event))
            return true;

        return false;
    }

    protected void onResume() {
        super.onResume();

        Window win = getWindow();
        win.setFlags(LayoutParams.FLAG_FULLSCREEN, LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public boolean onDown(MotionEvent arg0) {
        // TODO Auto-generated method stub
        return false;
    }
    
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
            float velocityY) {
        // TODO Auto-generated method stub
        return false;
    }
    
    @Override
    public void onLongPress(MotionEvent e) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
            float distanceY) {
        //System.out.println("****onScroll");
        // TODO Auto-generated method stub
        float x = e2.getX();
        float y = e2.getY();
        mView.flip(x, y);
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        // TODO Auto-generated method stub
        System.out.println("****onSingleTapUp");
        mView.endFlip();
        return false;
    }
    
}
