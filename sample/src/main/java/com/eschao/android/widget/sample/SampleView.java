package com.eschao.android.widget.sample;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;

import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.content.res.Resources;

import android.util.Log;

import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.eschao.android.widget.pageturning.PageTurning3D;
import com.eschao.android.widget.pageturning.Utility;

public class SampleView extends SurfaceView implements SurfaceHolder.Callback {

    static final int MSG_START_DRAWING      = 0;
    static final int MSG_END_DRAWING        = 1;

    EGLContext mEGLContext;
    EGLSurface mEGLSurface;
    EGLDisplay mEGLDisplay;
    Handler    mHandler;

    Bitmap mFrontBitmap;
    Bitmap mBackBitmap;
    Bitmap mBackgroundBitmap;

    EglHelper   mEglHelper;
    PageTurning3D mPageTurning;
    
    public SampleView(Context context) {
        super(context);
        init(context);
    }

    public SampleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public SampleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        init(context);
    }
    
    
    private void init(Context context) {
        //mRenderer = new Lesson06Renderer(context);
        mEglHelper  = new EglHelper();
        mPageTurning = new PageTurning3D(context);
        //setRenderer(mPageCurlAnimation);
        //setRenderMode(RENDERMODE_WHEN_DIRTY);
        //setDebugFlags(DEBUG_LOG_GL_CALLS);
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        holder.setType(SurfaceHolder.SURFACE_TYPE_GPU);

        newHandler();
    }

    public void flip(float x, float y) {
        Message msg = mHandler.obtainMessage();
        msg.what = MSG_START_DRAWING;
        msg.arg1 = (int)x;
        msg.arg2 = (int)y;
        mHandler.sendMessage(msg);
    }

    public void endFlip() {
        Message msg = mHandler.obtainMessage();
        msg.what = MSG_END_DRAWING;
        mHandler.sendMessage(msg);
    }

    public final boolean isFlipStarted() {
        return mPageTurning.isStarted();
    }

    public void surfaceCreated(SurfaceHolder holder) { 
        //super.surfaceCreated(holder);
        
        try {
            mEglHelper.initEgl(8, 8, 8, 0, 0, 0, holder);
            mPageTurning.onSurfaceCreated(mEglHelper.getGL(), null);
        }
        catch (IllegalArgumentException e) {
            Log.d("NeheView", "Init EGL failed: "+e);
        }
        //mPageCurlAnimation.setBitmap(mFrontBitmap, mBackBitmap, null, 0xff444444);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //super.surfaceChanged(holder, format, width, height);
        
        if (mEglHelper.isCreated()) {
            GL10 gl = mEglHelper.getGL();
            mPageTurning.init(width, height);
            mPageTurning.onSurfaceChanged(gl, width, height);

            boolean is320 = width <=480 && height < 800;
            loadBitmap(is320);
            mPageTurning.setBitmap(gl, mFrontBitmap, mBackBitmap, null, 0xffb56d6a);
        }
        //mPageCurlAnimation.init(width, height);
        
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mEglHelper.isCreated()) {
            mEglHelper.destroy(true);
        }
    }

    private void initEGL(SurfaceHolder holder) {
        System.out.println("***initEGL");
       
        // Get an EGL instance
        EGL10 egl = (EGL10)EGLContext.getEGL();

        // Get the the default display
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

        // Init EGL 
        int[] version = new int[2];
        egl.eglInitialize(display, version);

        // Specify configuration
        int[] configSpec = {
            EGL10.EGL_DEPTH_SIZE, 16, 
            EGL10.EGL_NONE
        };

        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfig = new int[1];
        egl.eglChooseConfig(display, configSpec, configs, 1, numConfig);
        EGLConfig config = configs[0];

        // Get OpenGL context
        EGLContext ctx = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, null);

        // create EGL surface
        EGLSurface surface = egl.eglCreateWindowSurface(display, config, holder, null);

        // Bind Context, Display and Surface
        egl.eglMakeCurrent(display, surface, surface, ctx);

        mEGLContext = ctx;
        mEGLDisplay = display;
        mEGLSurface = surface;
    }

    private void draw() {
        GL10 gl = mEglHelper.getGL();
        mPageTurning.onDrawFrame(gl);
        mEglHelper.swapBuffer();
    }
    
    private void draw1() {
        //EGL10 egl = (EGL10)EGLContext.getEGL();
        //GL10 gl = (GL10)mEGLContext.getGL();
        EGL10 egl = mEglHelper.getEgl();
        GL10 gl = mEglHelper.getGL();
        
        float[] vertexArr = new float[] {
                0.0f, 50f, 0f,
                50f, -50f, 0f,
                -50f, -50f, 0f
        };
        
        FloatBuffer mVertexBuf = Utility.toFloatBuffer(vertexArr);
        // TODO Auto-generated method stub
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl.glLoadIdentity();

            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuf);
            
            gl.glPushMatrix();
            gl.glColor4f(1, 0, 0, 1);
            gl.glTranslatef(0, 100f, 0);
            gl.glDrawArrays(GL10.GL_TRIANGLES, 0, 3);
            gl.glPopMatrix();
            mEglHelper.swapBuffer();
    }

    private void loadBitmap(boolean is320) {
        int frontId = R.drawable.front;
        int backId = R.drawable.back;
        int bgId = R.drawable.bg2;
        if (is320) {
            frontId = R.drawable.front320;
            backId = R.drawable.back320;
            bgId = R.drawable.bg2;
        }

        Resources res = getContext().getResources();
        InputStream front = res.openRawResource(frontId);
        InputStream back  = res.openRawResource(backId);
        InputStream bgImage = res.openRawResource(bgId);
        //Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.t1);
        
        try {
            mFrontBitmap = BitmapFactory.decodeStream(front);
            mBackBitmap  = BitmapFactory.decodeStream(back);
            mBackgroundBitmap = BitmapFactory.decodeStream(bgImage);
        } finally {
            try {
                front.close();
                back.close(); 
                bgImage.close();
            } catch (IOException e) {
                
            }
        }
    }

    private void newHandler() {
        mHandler = new Handler() {    
            public void handleMessage(Message msg) {
                
                switch (msg.what) {
                case MSG_START_DRAWING:
                    int x = msg.arg1;
                    int y = msg.arg2;
                    if (mPageTurning.isStarted()) {
                        mPageTurning.move(x, y);
                    }
                    else {
                        mPageTurning.start(x, y);
                    }
                    //requestRender();
                    draw();
                    break;

                case MSG_END_DRAWING:
                    mPageTurning.stop();
                    break;

                default:
                    break;
                }
            }
        };
    }
}
