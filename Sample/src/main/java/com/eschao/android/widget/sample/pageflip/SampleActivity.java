/*
 * Copyright (C) 2016 eschao <esc.chao@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.eschao.android.widget.sample.pageflip;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Bundle;

import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.GestureDetector;

import android.view.GestureDetector.OnGestureListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * Sample Activity
 *
 * @author eschao
 */
public class SampleActivity extends Activity implements OnGestureListener {

    PageFlipView mPageFlipView;
    GestureDetector mGestureDetector;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPageFlipView = new PageFlipView(this);
        setContentView(mPageFlipView);
        mGestureDetector = new GestureDetector(this, this);

        if (Build.VERSION.SDK_INT < 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                                 WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            mPageFlipView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        LoadBitmapTask.get(this).start();
        mPageFlipView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        mPageFlipView.onPause();
        LoadBitmapTask.get(this).stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.optionmenus, menu);

        int duration = mPageFlipView.getAnimateDuration();
        if (duration == 1000) {
            menu.findItem(R.id.animation_1s).setChecked(true);
        }
        else if (duration == 2000) {
            menu.findItem(R.id.animation_2s).setChecked(true);
        }
        else if (duration == 5000) {
            menu.findItem(R.id.animation_5s).setChecked(true);
        }

        if (mPageFlipView.isAutoPageEnabled()) {
            menu.findItem(R.id.auoto_page).setChecked(true);
        }
        else {
            menu.findItem(R.id.single_page).setChecked(true);
        }

        SharedPreferences pref = PreferenceManager
                                    .getDefaultSharedPreferences(this);
        int pixels = pref.getInt("MeshPixels", mPageFlipView.getPixelsOfMesh());
        switch (pixels) {
            case 2:
                menu.findItem(R.id.mesh_2p).setChecked(true);
                break;
            case 5:
                menu.findItem(R.id.mesh_5p).setChecked(true);
                break;
            case 10:
                menu.findItem(R.id.mesh_10p).setChecked(true);
                break;
            case 20:
                menu.findItem(R.id.mesh_20p).setChecked(true);
                break;
            default:
                break;
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean isHandled = true;
        SharedPreferences pref = PreferenceManager
                                    .getDefaultSharedPreferences(this);
        Editor editor = pref.edit();
        switch (item.getItemId()) {
            case R.id.animation_1s:
                mPageFlipView.setAnimateDuration(1000);
                editor.putInt(Constants.PREF_DURATION, 1000);
                break;
            case R.id.animation_2s:
                mPageFlipView.setAnimateDuration(2000);
                editor.putInt(Constants.PREF_DURATION, 2000);
                break;
            case R.id.animation_5s:
                mPageFlipView.setAnimateDuration(25000);
                editor.putInt(Constants.PREF_DURATION, 5000);
                break;
            case R.id.auoto_page:
                mPageFlipView.enableAutoPage(true);
                editor.putBoolean(Constants.PREF_PAGE_MODE, true);
                break;
            case R.id.single_page:
                mPageFlipView.enableAutoPage(false);
                editor.putBoolean(Constants.PREF_PAGE_MODE, false);
                break;
            case R.id.mesh_2p:
                editor.putInt(Constants.PREF_MESH_PIXELS, 2);
                break;
            case R.id.mesh_5p:
                editor.putInt(Constants.PREF_MESH_PIXELS, 5);
                break;
            case R.id.mesh_10p:
                editor.putInt(Constants.PREF_MESH_PIXELS, 10);
                break;
            case R.id.mesh_20p:
                editor.putInt(Constants.PREF_MESH_PIXELS, 20);
                break;
            case R.id.about_menu:
                showAbout();
                return true;
            default:
                isHandled = false;
                break;
        }

        if (isHandled) {
            item.setChecked(true);
            editor.apply();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            mPageFlipView.onFingerUp(event.getX(), event.getY());
            return true;
        }

        return mGestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        mPageFlipView.onFingerDown(e.getX(), e.getY());
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY) {
        return false;
    }


    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                            float distanceY) {
        mPageFlipView.onFingerMove(e2.getX(), e2.getY());
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    private void showAbout() {
        View aboutView = getLayoutInflater().inflate(R.layout.about, null,
                                                     false);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setTitle(R.string.app_name);
        builder.setView(aboutView);
        builder.create();
        builder.show();
    }
}
