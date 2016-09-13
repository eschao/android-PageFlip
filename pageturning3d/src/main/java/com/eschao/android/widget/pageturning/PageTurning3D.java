package com.eschao.android.widget.pageturning;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.nio.FloatBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.util.Log;
import android.graphics.drawable.BitmapDrawable;

public class PageTurning3D implements Renderer {
    // texture size and id defines
    final static int FRONT_PAGE_TEXTURE_ID  = 0;
    final static int FOLDER_PAGE_TEXTURE_ID = 1;
    final static int BACK_PAGE_TEXUTRE_ID   = 2;
    final static int TEXTURES_SIZE          = 3;

    final static int PIXEL_OF_EVERY_MESH_VERTEX = 20;
    final static int MAX_TEMP_FLOAT_BUF_SIZE    = 64;
    
    // Page curl action section
    // The min page curl angle (10 degree)
    final static float MIN_PAGE_CURL_ANGLE = (float)(Math.PI/36);

    // The max page curl angle (70 degree)
    final static float MAX_PAGE_CURL_ANGLE = (float)(Math.PI*7/36);

    // correct TouchY
    // r = (3/4*PI)Lto
    // w'/width = (1/2Lto) / ((7/8)*Lto - r/2)
    //          = (1/2Lto) / ((7/8)*Lto - (3/8*PI)*Lto)
    //          = 1/(1.75-0.75/PI)
    // but in fact, we use 1.75-1/PI to get better result
    final static double CORRECT_TOUCHY_FACTOR = (1.75-1/Math.PI);

    // front page vertexes, the front page is divided 3 triangles, use
    // GL_TRIANGLE_FAN mode to draw

    final static int FRONT_PAGE_VERTEXS_COUNT = 5;
    final static int FRONT_PAGE_VERTEXS_SIZE = FRONT_PAGE_VERTEXS_COUNT*3;
    final static int FRONT_PAGE_TEXTURE_COORDS_SIZE = FRONT_PAGE_VERTEXS_COUNT*2;

    // back page vertexes, the back page is a triangle
    final static int BACK_PAGE_VERTEXS_COUNT            = 3;
    final static int BACK_PAGE_VERTEXS_SIZE             = BACK_PAGE_VERTEXS_COUNT*3;
    final static int BACK_PAGE_TEXTURE_COORDS_SIZE      = BACK_PAGE_VERTEXS_COUNT*2;
    final static int BACK_PAGE_SHADOW_VERTEXS_COUNT     = 8;
    final static int BACK_PAGE_SHADOW_VERTEXS_SIZE      = BACK_PAGE_SHADOW_VERTEXS_COUNT*3;
    final static int BACK_PAGE_SHADOW_COLOR_SIZE        = BACK_PAGE_SHADOW_VERTEXS_COUNT*4;

    // folder page shadow color buffer size
    final static int FOLDER_EDGE_SHADOW_VERTEXS_COUNT   = 6;
    final static int FOLDER_EDGE_SHADOW_VERTEXS_SIZE    = FOLDER_EDGE_SHADOW_VERTEXS_COUNT*3;
    final static int FOLDER_EDGE_SHADOW_COLOR_SIZE      = FOLDER_EDGE_SHADOW_VERTEXS_COUNT*4;

    // the color count for triangle of folder page
    final static int FOLDER_PAGE_TRIANGLE_COLOR_COUNT   = 3;

    // folder cylinder color mask
    final static float START_CYLINDER_COLOR_MASK        = 0.6f;
    final static float END_CYLINDER_COLOR_MASK          = 1f;
    final static float CYLINDER_COLOR_MASK_ALPHA        = 0f;

    // folder page shadow 
    final static float START_FOLDER_PAGE_SHADOW_COLOR   = 0.6f;
    final static float END_FOLDER_PAGE_SHADOW_COLOR     = 1f;
    final static float START_FOLDER_PAGE_SHADOW_ALPHA   = 0.1f;
    final static float END_FOLDER_PAGE_SHADOW_ALPHA     = 0f;

    // back page shadow color mask
    final static float START_BACK_PAGE_SHADOW_COLOR     = 0.3f;
    final static float END_BACK_PAGE_SHADOW_COLOR       = 1f;
    final static float START_BACK_PAGE_SHADOW_ALPHA     = 0f;
    final static float END_BACK_PAGE_SHADOW_ALPHA       = 0f;

    // page folder mask color
    final static int FOLDER_PAGE_MASK_ALPHA             = 25;
    final static int FOLDER_PAGE_MASK_BG_ALPHA          = 0xE6000000;

    // screen width and height
    int mWidth;
    int mHeight;
    int mMaxTouchY;
    int mOriginalTouchX;
    int mOriginalTouchY;

    // the pixels interval of every two mesh vertexes
    int mPixelsIntervalOfMeshVertexs; 

    // texture width and height, OpenGL require the texture size is 2 power
    int mTextureWidth;
    int mTextureHeight;

    // the openGL texture range is {0, 1}, we need save the screen width/height
    // ratio in texture size
    float mTextureWidthRatio;
    float mTextureHeightRatio;
    
    // texture ids, only two we used, first one is front page, second is back
    // page
    int[] mTextureIDs;
    
    // origin (x, y) coords againt finger touch coords
    float mOriginX;
    float mOriginY;

    // finger touch coords
    float mTouchX;
    float mTouchY;
    float mAngle;

    // the middle coords between touch coords and origin coords
    float mMiddleX;
    float mMiddleY;

    // from 2D perspective, the line through middle coords and perpendicular to
    // the line from touch coords to origin coords will intersect Y axis and X
    // axis, The coords on Y axis is mYFolder{X, Y}, the mXFolder{X, Y} is on X
    // axis. The mY{X}Folder{X1, Y1} is up mY{X}Folder{X, Y}, The
    // mY{X}Folder{X0, Y0} is under mY{X}Folder{X, Y}
    //
    //                         Y
    //                         ^
    //                         |
    //                         / YFY1
    //                        /|
    //                       / |
    //                      /  |
    //                     /   |
    //                    /    |
    //                   /     |
    //                  /      / YFY
    //    mTouch{X,Y}  /      /|
    //       .        /      / |
    //               /      /  |
    //              /      /   |
    //             /      /    |
    //            /   .  /     |
    //           /      /     /| YFY0
    //          /      /     / |
    //         /      /     /  |
    //X <----------------------+- mOrigin{X, Y}
    //      XFX1    XFX   XFX0 |
    //   
    float mYFolderX;
    float mYFolderY;
    float mYFolderX0;
    float mYFolderY0;
    float mYFolderX1;
    float mYFolderY1;
    float mXFolderX;
    float mXFolderY;
    float mXFolderX0;
    float mXFolderY0;
    float mXFolderX1;
    float mXFolderY1;

    // the folder shadow coords
    // 0 is {mTouchX, mTouchY}
    // 1 is {mYFolderShadowX, mYFolderShadowY}
    // 2 is {mXFolderShadowX, mXFolderShadowY}
    // 3 is {mXYFolderShadowX, mXYFolderShadowY}
    //
    //      Y
    //      ^                
    //      |  ---------------1---3
    //      |                 |  /|
    //      |                 | / |
    //      |                 |/  |
    //      |  ---------------0---2
    //      |                 |   |
    //      |                 |   |
    //      |       
    //      |       
    //      |
    //      +------------------------------> X 
    //
    float mXFolderShadowX;
    float mXFolderShadowY;
    float mYFolderShadowX;
    float mYFolderShadowY;
    float mXYFolderShadowX;
    float mXYFolderShadowY;
    float mRYFolderShadowX;
    float mRYFolderShadowY;
    float mRXFolderShadowX;
    float mRXFolderShadowY;
    float mRYFolderShadowX1;
    float mRYFolderShadowY1;
    float mRXFolderShadowX1;
    float mRXFolderShadowY1;

    // the length of segment from mTouch{X, Y} to mOrigin{X, Y}
    float mLenOfTouchOrigin;
    
    float[] mCylinderVertexs;
    float[] mCylinderTextureCoords;
    float[] mTempFloatBuffer;
    float[] mFolderPageMaskColors;
    
    // Mesh vertexs, texture coords count
    int mMeshVertexsCount;
    int mMiddleMeshVertexsCount;
    int mHalfCylinderVertexsCount;
    int mHalfCylinderVertexsSize;
    int mHalfCylinderTextureCoordsSize;
    int mFolderPageVertexsCount;
    int mFolderPageVertexsSize;
    int mFolderPageTextureCoordsSize;
    int mFolderPageColorMaskSize;
    int mCylinderVertexsSize;
    int mCylinderTextureCoordsSize;
    
    // cylinder coords on X axis and Y axis
    Vertex3D[] mYVertexs;
    Vertex3D[] mXVertexs;

    // font/back page vertexs buffer and texture coords buffer
    FloatBuffer mFrontPageVertexsBuf;
    FloatBuffer mFrontPageTextureCoordsBuf;
    FloatBuffer mBackPageVertexsBuf;
    FloatBuffer mBackPageTextureCoordsBuf;

    // front cylinder and folder page 
    FloatBuffer mFrontHalfCylinderVertexsBuf;
    FloatBuffer mFrontHalfCylinderTextureCoordsBuf;
    FloatBuffer mFolderPageVertexsBuf;
    FloatBuffer mFolderPageTextureCoordsBuf;

    FloatBuffer mBackPageShadowVertexsBuf;
    FloatBuffer mBackPageShadowColorsBuf;
    FloatBuffer mFolderPageMaskColorsBuf;
    FloatBuffer mFolderEdgeShadowColorsBuf;
    FloatBuffer mFolderEdgeShadowVertexsBuf;

    Bitmap mFrontBitmap;
    Bitmap mBackBitmap;

    // if caller has a background bitmap, build folder page texture with background image
    // and front bitmap
    Bitmap mBackgrounBitmap;
    // if caller has a pure background color, build folder page texture with this color
    // and front bitmap
    int mBackgroundColor;
    
    boolean mIsStart;
    Context mContext;
    
    public PageTurning3D(Context context) {
        mContext            = context;
        mTextureIDs         = new int[TEXTURES_SIZE];
        mTempFloatBuffer    = new float[MAX_TEMP_FLOAT_BUF_SIZE];
        mPixelsIntervalOfMeshVertexs = 10;
    }

    public final void setBitmap(GL10 gl, Bitmap front, Bitmap back,
                                Bitmap bgImage, int bgColor) {
        mFrontBitmap = front;
        mBackBitmap  = back;
        mBackgrounBitmap = bgImage;
        mBackgroundColor = bgColor;
        createTextures(gl);
    }

    /**
     * Init the mesh count and create related buffers
     */
    public final void init(int width, int height) {
        computeMeshVertexsCount(width, height);
        createBuffers();
        createFolderPageMaskColorsBuf();
        createFolderEdgeShadowColorsBuf();

        // if the screen is less than 480*800, the pixels interval is set to 20
        // to get better performance
        if (width <= 480 && height < 800)
            mPixelsIntervalOfMeshVertexs = 20;

        // compute the max folder height
        mMaxTouchY= (int)(width/(1.75-0.75/Math.PI));
    }

    public final boolean isStarted() {
        return mIsStart;
    }

    /**
     * Start page flip
     */
    public final void start(float touchX, float touchY) {
        if (touchX > mWidth/2) {
            mOriginX = mWidth;
        }
        else {
            mOriginX = 0;
        }
        
        // the touch point might out of the screen size in Android
        // so we need to check and correct it
        if (touchY > mHeight-2) {
            touchY = mHeight;
        }
        else if (touchY < 2) {
            touchY = 0;
        }

        if (touchY > mHeight/2) {
            mOriginY = mHeight;
        }
        else {
            mOriginY = 0;
        }

        mOriginX    = translateToOpenGLCoordX(mOriginX);
        mOriginY    = translateToOpenGLCoordY(mOriginY);
        mTouchX     = translateToOpenGLCoordX(touchX);
        mTouchY     = translateToOpenGLCoordY(touchY);

        // correct touch y, it must be less than max TouchY
        correctTouchXY();
        mMiddleX    = (mTouchX+mOriginX)*0.5f;
        mMiddleY    = (mTouchY+mOriginY)*0.5f;
        mIsStart    = true;

        computeKeyPointsCoords();
    }

    /**
     *  Move page 
     */
    public final void move(float touchX, float touchY) {
        // the touch point might out of the screen size in Android
        // so we need check and correct it
        if (touchY > mHeight-2) {
            touchY = mHeight;
        }
        else if (touchY < 2) {
            touchY = 0;
        }

        mTouchX     = translateToOpenGLCoordX(touchX);
        mTouchY     = translateToOpenGLCoordY(touchY);

        // correct touch y, it must be less than max TouchY
        correctTouchXY();
        mMiddleX    = (mTouchX+mOriginX)*0.5f;
        mMiddleY    = (mTouchY+mOriginY)*0.5f;

        computeKeyPointsCoords();
    }
    
    /**
     * Stop page flip
     */
    public final void stop() {
        mIsStart    = false;
        mBackgrounBitmap = null;
    }

    /**
     * Draw on screen
     */
    public void onDrawFrame(GL10 gl) {
        if (!mIsStart)
            return;
        
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        /*
        gl.glDisable(GL10.GL_TEXTURE_2D);
        float[] temp = new float[9];
        temp[0] = 0; 
        temp[1] = 0;
        temp[2] = 0;

        temp[3] = 20;
        temp[4] = 0;
        temp[5] = 0;

        temp[6] = 20;
        temp[7] = 20;
        temp[8] = 0;
        
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(9*4);
        byteBuf.order(ByteOrder.nativeOrder());
        FloatBuffer buf = byteBuf.asFloatBuffer();
        buf.put(temp, 0, 9);
        buf.position(0);
        
        gl.glColor4f(1, 1, 1, 1);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, buf);
        gl.glDrawArrays(GL10.GL_TRIANGLES, 0, 3);
        /*
        float[] temp = new float[12];
        temp[0] = -mWidth/2; 
        temp[1] = -mHeight/2;
        temp[2] = 0;

        temp[3] = mWidth/2;
        temp[4] = -mHeight/2;
        temp[5] = 0;

        temp[6] = mWidth/2;
        temp[7] = mHeight/2;
        temp[8] = 0;

        temp[9] = -mWidth/2;
        temp[10] = mHeight/2;
        temp[11] = 0;
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(12*4);
        byteBuf.order(ByteOrder.nativeOrder());
        FloatBuffer buf = byteBuf.asFloatBuffer();
        buf.put(temp, 0, 12);
        buf.position(0);

        temp[0] = 0;
        temp[1] = 0;
        temp[2] = mTextureWidthRatio;
        temp[3] = 0;
        temp[4] = mTextureWidthRatio;
        temp[5] = mTextureHeightRatio;
        temp[6] = 0;
        temp[7] = mTextureHeightRatio;
        byteBuf = ByteBuffer.allocateDirect(8*4);
        byteBuf.order(ByteOrder.nativeOrder());
        FloatBuffer buf1 = byteBuf.asFloatBuffer();
        buf1.put(temp, 0, 8);
        buf1.position(0);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIDs[FRONT_PAGE_TEXTURE_ID]);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, buf);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, buf1);
        gl.glDrawArrays(GL10.GL_TRIANGLES, 0, 3);
        */
        
        // 1. draw back page
        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIDs[BACK_PAGE_TEXUTRE_ID]);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mBackPageVertexsBuf);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mBackPageTextureCoordsBuf);
        gl.glDrawArrays(GL10.GL_TRIANGLES, 0, BACK_PAGE_VERTEXS_COUNT);
        
        // 2. draw back shadow page
        gl.glDisable(GL10.GL_TEXTURE_2D);
        gl.glEnable(GL10.GL_BLEND);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mBackPageShadowVertexsBuf);
        gl.glColorPointer(4, GL10.GL_FLOAT, 0, mBackPageShadowColorsBuf);
        gl.glBlendFunc(GL10.GL_DST_COLOR, GL10.GL_SRC_ALPHA);
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 4, 4);
        gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
        gl.glDisable(GL10.GL_BLEND);

        // 3. draw front page
        gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIDs[FRONT_PAGE_TEXTURE_ID]);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mFrontPageVertexsBuf);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mFrontPageTextureCoordsBuf);
        gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, FRONT_PAGE_VERTEXS_COUNT);

        // 4. draw front cylinder page
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mFrontHalfCylinderVertexsBuf);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mFrontHalfCylinderTextureCoordsBuf);
        gl.glDrawArrays(GL10.GL_TRIANGLES, 0, mHalfCylinderVertexsCount);//HALF_CYLINDER_VERTEXS_COUNT);

        // 5. draw folder edge shadow
        gl.glDisable(GL10.GL_TEXTURE_2D);
        gl.glEnable(GL10.GL_BLEND);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
        gl.glBlendFunc(GL10.GL_DST_COLOR, GL10.GL_SRC_ALPHA_SATURATE);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mFolderEdgeShadowVertexsBuf);
        gl.glColorPointer(4, GL10.GL_FLOAT, 0, mFolderEdgeShadowColorsBuf);
        gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 6);
        gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
        gl.glDisable(GL10.GL_BLEND);

        // 6. draw folder page
        gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIDs[FOLDER_PAGE_TEXTURE_ID]);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mFolderPageVertexsBuf);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mFolderPageTextureCoordsBuf);
        gl.glDrawArrays(GL10.GL_TRIANGLES, 0, mFolderPageVertexsCount);//FOLDER_PAGE_VERTEXS_COUNT);

        // 7. draw shadow
        gl.glDisable(GL10.GL_TEXTURE_2D);
        gl.glEnable(GL10.GL_BLEND);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
        gl.glColorPointer(4, GL10.GL_FLOAT, 0, mFolderPageMaskColorsBuf);
        gl.glBlendFunc(GL10.GL_DST_COLOR, GL10.GL_SRC_ALPHA);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mFolderPageVertexsBuf);
        gl.glDrawArrays(GL10.GL_TRIANGLES, 0, mFolderPageVertexsCount);
        gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
        gl.glDisable(GL10.GL_BLEND);
    }


    /*
     * This function should be called when surface is changed
     */
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glViewport(0, 0, width, height);
        GLU.gluOrtho2D(gl, -width/2, width/2, -height/2, height/2);
        mWidth              = width;
        mHeight             = height;
        mTextureWidth       = getNextHighestPO2(mWidth);
        mTextureHeight      = getNextHighestPO2(mHeight);
        mTextureWidthRatio  = (float)mWidth/mTextureWidth;
        mTextureHeightRatio = (float)mHeight/mTextureHeight;
    }

    /*
     * This function should be called when surface is created
     */
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glClearColor(0, 0, 0, 1f);
        gl.glClearDepthf(1.0f);
        gl.glShadeModel(GL10.GL_SMOOTH);
        /*
        gl.glEnable(GL10.GL_POINT_SMOOTH);
        gl.glEnable(GL10.GL_LINE_SMOOTH);
        gl.glHint(GL10.GL_POINT_SMOOTH_HINT, GL10.GL_NICEST);
        gl.glHint(GL10.GL_LINE_SMOOTH_HINT, GL10.GL_NICEST);*/
    }

    /*
     * Create texture according to bitmap
     */
    private void createTextures(GL10 gl) {
        System.gc();

        gl.glEnable(GL10.GL_TEXTURE_2D);
        
        Canvas c        = new Canvas();
        Paint paint     = new Paint(Paint.ANTI_ALIAS_FLAG);
        Bitmap bitmap   = Bitmap.createBitmap(mTextureWidth, mTextureHeight,
                                                Config.ARGB_8888);

        // 1. Create front page texture
        c.setBitmap(bitmap);
        c.drawBitmap(mFrontBitmap, 0, 0, null);
        gl.glGenTextures(TEXTURES_SIZE, mTextureIDs, 0);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIDs[FRONT_PAGE_TEXTURE_ID]);
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
                            GL10.GL_LINEAR);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
                            GL10.GL_LINEAR);

        // 2. Create folder page texture
        if (null != mBackgrounBitmap) {
            BitmapDrawable drawable = new BitmapDrawable(mBackgrounBitmap);
            drawable.setAlpha(255-FOLDER_PAGE_MASK_ALPHA);
            drawable.setBounds(0, 0, mWidth, mHeight);
            drawable.draw(c);
        } else {
            c.drawColor(FOLDER_PAGE_MASK_BG_ALPHA|mBackgroundColor);
        }

        paint.setAlpha(FOLDER_PAGE_MASK_ALPHA);
        c.drawBitmap(mFrontBitmap, 0, 0, paint);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIDs[FOLDER_PAGE_TEXTURE_ID]);
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
                            GL10.GL_LINEAR);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
                            GL10.GL_LINEAR);

        // 3. Create back page texture
        c.drawBitmap(mBackBitmap, 0, 0, null);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIDs[BACK_PAGE_TEXUTRE_ID]);
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
                            GL10.GL_LINEAR);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
                            GL10.GL_LINEAR);

        bitmap.recycle();
        bitmap = null;
        System.gc();
    }


    /**
     * Correct TouchXY, compute the max TouchY for current TouchX
     */
    private final void correctTouchXY() {
        // simulate iBooks, make the touchX alaways is 6/5 of finger point
        mTouchX = mOriginX + 1.2f*(mTouchX-mOriginX);
        float x = Math.abs(mTouchX-mOriginX)*0.5f;
        float y = (float)Math.sqrt((mWidth/CORRECT_TOUCHY_FACTOR-x)*x)*2;
        if (mOriginY > 0) {
            if (mOriginY-mTouchY > y) {
                mTouchY = mOriginY - y;
            }
        }
        else if (mTouchY-mOriginY > y) {
                mTouchY = mOriginY+y;
        }

        //return mOriginY+y*2;
        //return (float)(mOriginY+y/(0.875-0.375/Math.PI));
    }

    /**
     * Compute key points coords
     */
    private void computeKeyPointsCoords() {
        if (Math.abs(mTouchY - mOriginY) < 2) {
            computePointsCoordsVerticalCase();
        } else {
            computePointsCoordsNormalCase();
        }

        //printBasicInfos();
    }

    /**
     * Compute key points coords in vertcal case
     */
    private void computePointsCoordsVerticalCase() {
        mXFolderX = mMiddleX;
        mXFolderY = mOriginY;

        mYFolderX = mMiddleX;
        mYFolderY = -mOriginY;

        mXFolderX0 = mOriginX + (mXFolderX-mOriginX)*0.25f;
        mXFolderY0 = mXFolderY;

        // compute intersection point X1 which is 1/4 length from touchXY to XFolderXY
        mXFolderX1 = mOriginX + 1.75f*(mXFolderX - mOriginX);
        mXFolderY1 = mXFolderY;

        // compute intersection point Y0 which is 1/4 length from YFolderXY to OriginalXY
        mYFolderX0 = mXFolderX0;
        mYFolderY0 = mYFolderY;

        // compute intersection point Y1 which is 1/4 length from touchXY to YFolderXY 
        mYFolderX1 = mXFolderX1;
        mYFolderY1 = mYFolderY;

        // compute length from TouchXY to OriginalXY
        mLenOfTouchOrigin = Math.abs(mTouchX-mOriginX);//(float)Math.hypot((mTouchX-mOriginX), (mTouchY-mOriginY));


        float tempX = mLenOfTouchOrigin*0.06f/Math.abs(mXFolderX-mOriginX);
        float tempY = mLenOfTouchOrigin*0.06f/Math.abs(mYFolderY-mOriginY);
        mYFolderShadowX = mTouchX + tempX*(mTouchX-mXFolderX);
        mYFolderShadowY = mOriginY;//mTouchY + tempX*(mTouchY-mXFolderY);
        mXFolderShadowX = mTouchX + tempY*(mTouchX-mYFolderX);
        mXFolderShadowY = mOriginY;//mTouchY + tempY*(mTouchY-mYFolderY);

        // compute RYFolderShadowXY
        float temp = (float)(0.75f-1.5f/Math.PI);
        mRYFolderShadowX = mOriginX + temp*(mTouchX-mOriginX);
        mRYFolderShadowY = mOriginY;//mYFolderY - temp*(mYFolderY-mTouchY);
        mRXFolderShadowX = mXFolderX - temp*(mXFolderX-mTouchX);
        mRXFolderShadowY = mOriginY;//mOriginY + temp*(mTouchY-mOriginY);

        /*
        mRYFolderShadowX1 = mRYFolderShadowX - mTouchX + mYFolderShadowX;
        mRYFolderShadowY1 = mRYFolderShadowY - mTouchY + mYFolderShadowY;
        mRXFolderShadowX1 = mRXFolderShadowX - mTouchX + mXFolderShadowX;
        mRXFolderShadowY1 = mRXFolderShadowY - mTouchY + mXFolderShadowY;
        */

        // compute RYFolderShadowXY1
        temp = (mYFolderShadowY-mTouchY)/(mYFolderShadowY-mRXFolderShadowY);
        mRYFolderShadowX1 = (mRYFolderShadowX-temp*mRXFolderShadowX)/(1-temp);
        mRYFolderShadowY1 = mOriginY;//(mRYFolderShadowY-temp*mRXFolderShadowY)/(1-temp);

        temp = (mXFolderShadowX-mTouchX)/(mXFolderShadowX-mRYFolderShadowX);
        mRXFolderShadowX1 = (mRXFolderShadowX-temp*mRYFolderShadowX)/(1-temp);
        mRXFolderShadowY1 = mOriginY;//(mRXFolderShadowY-temp*mRYFolderShadowY)/(1-temp);

        mXYFolderShadowX = mXFolderShadowX;
        mXYFolderShadowY = mXFolderShadowY;
    }

    /**
     * Compute key points coords in normal case
     */
    private void computePointsCoordsNormalCase() {
        float distanceX = mMiddleX - mOriginX;
        float distanceY = mMiddleY - mOriginY;
        
        // compute intersection point on X axis
        mXFolderX = mMiddleX + distanceY*distanceY/distanceX;
        mXFolderY = mOriginY;
        
        // compute intersection point X1 which is 1/4 length from touchXY to XFolderXY
        mXFolderX1 = mOriginX + 1.75f*(mXFolderX - mOriginX);
        mXFolderY1 = mXFolderY;

        // compute intersection point X0 which is 1/4 length from XFolderXY to OriginalXY
        mXFolderX0 = mOriginX + (mXFolderX-mOriginX)*0.25f;
        mXFolderY0 = mXFolderY;

        // compute intersection point on Y axis
        mYFolderX = mOriginX;
        mYFolderY = mMiddleY + distanceX*distanceX/distanceY;

        // compute intersection point Y0 which is 1/4 length from YFolderXY to OriginalXY
        mYFolderX0 = mYFolderX;
        mYFolderY0 = mOriginY + (mYFolderY-mOriginY)*0.25f;

        // compute intersection point Y1 which is 1/4 length from touchXY to YFolderXY 
        mYFolderX1 = mYFolderX;
        mYFolderY1 = mOriginY + 1.75f*(mYFolderY - mOriginY);

        // compute length from TouchXY to OriginalXY
        mLenOfTouchOrigin = (float)Math.hypot((mTouchX-mOriginX), (mTouchY-mOriginY));


        float tempX = mLenOfTouchOrigin*0.06f/Math.abs(mXFolderX-mOriginX);
        float tempY = mLenOfTouchOrigin*0.06f/Math.abs(mYFolderY-mOriginY);
        mYFolderShadowX = mTouchX + tempX*(mTouchX-mXFolderX);
        mYFolderShadowY = mTouchY + tempX*(mTouchY-mXFolderY);
        mXFolderShadowX = mTouchX + tempY*(mTouchX-mYFolderX);
        mXFolderShadowY = mTouchY + tempY*(mTouchY-mYFolderY);

        // compute RYFolderShadowXY
        float temp = (float)(0.75f-1.5f/Math.PI);
        mRYFolderShadowX = mOriginX + temp*(mTouchX-mOriginX);
        mRYFolderShadowY = mYFolderY - temp*(mYFolderY-mTouchY);
        mRXFolderShadowX = mXFolderX - temp*(mXFolderX-mTouchX);
        mRXFolderShadowY = mOriginY + temp*(mTouchY-mOriginY);

        /*
        mRYFolderShadowX1 = mRYFolderShadowX - mTouchX + mYFolderShadowX;
        mRYFolderShadowY1 = mRYFolderShadowY - mTouchY + mYFolderShadowY;
        mRXFolderShadowX1 = mRXFolderShadowX - mTouchX + mXFolderShadowX;
        mRXFolderShadowY1 = mRXFolderShadowY - mTouchY + mXFolderShadowY;
        */

        // compute RYFolderShadowXY1
        temp = (mYFolderShadowY-mTouchY)/(mYFolderShadowY-mRXFolderShadowY);
        mRYFolderShadowX1 = (mRYFolderShadowX-temp*mRXFolderShadowX)/(1-temp);
        mRYFolderShadowY1 = (mRYFolderShadowY-temp*mRXFolderShadowY)/(1-temp);

        temp = (mXFolderShadowX-mTouchX)/(mXFolderShadowX-mRYFolderShadowX);
        mRXFolderShadowX1 = (mRXFolderShadowX-temp*mRYFolderShadowX)/(1-temp);
        mRXFolderShadowY1 = (mRXFolderShadowY-temp*mRYFolderShadowY)/(1-temp);

        if ((mXFolderShadowX > mTouchX && mOriginY < 0) ||
            (mXFolderShadowX < mTouchX && mOriginY > 0)) {
            if (mXFolderShadowY > mTouchY) {
                mXYFolderShadowX = mXFolderShadowX - mXFolderShadowY+mTouchY;
                mXYFolderShadowY = mXFolderShadowY + mXFolderShadowX-mTouchX;
            } else {
                mXYFolderShadowX = mXFolderShadowX + mTouchY-mXFolderShadowY;
                mXYFolderShadowY = mXFolderShadowY + mXFolderShadowX-mTouchX;
            }
        } else {
            if (mXFolderShadowY > mTouchY) {
                mXYFolderShadowX = mXFolderShadowX + mXFolderShadowY - mTouchY;
                mXYFolderShadowY = mXFolderShadowY + mTouchX - mXFolderShadowX;
            } else {
                mXYFolderShadowX = mXFolderShadowX - mTouchY + mXFolderShadowY;
                mXYFolderShadowY = mXFolderShadowY + mTouchX - mXFolderShadowX;
            }
        }

        createFrontPageAndBackPage();
        computeCylinderVertexs();
        createFolderPage();
    }
    
    /**
     * Create vertexs and texture coords for front and back page
     */
    private final void createFrontPageAndBackPage() {
        float cornerX = -mOriginX;
        float cornerY = -mOriginY;

        /*
         * [0]--------------------[1]
         *  |                      |
         *  |                      |
         *  |                      |
         *  |                      |
         * [4]                     |
         *  |\                     |
         *  | \                    |
         *  |  \                   |
         *  |   \                  |
         *  |    \                 |
         * [5]--[3]---------------[2]
         *
         * [5]: mOriginX, mOriginY
         * [1]: cornerX, cornerY
         * [2]: cornerX, -cornerY
         * [3]: mXFolderX1, mXFolderY1
         * [4]: mYFolderX1, mYFolderY1
         * [0]: -cornerX, cornerY
         */
        // 1. build 3 triangles for front page, use GL_TRIANGLE_FAN to draw,
        // so only 5 vertexes are needed
        // start front mOrigin{X, Y} 
        // Triangle 1: [1] -> [2] -> [3]
        // Triangle 2: [1] -> [3] -> [4]
        // Triangle 3: [1] -> [4] -> [5]
        mTempFloatBuffer[0] = cornerX;
        mTempFloatBuffer[1] = cornerY;
        mTempFloatBuffer[2] = 0;
        
        mTempFloatBuffer[3] = cornerX;
        mTempFloatBuffer[4] = -cornerY;
        mTempFloatBuffer[5] = 0;
        
        mTempFloatBuffer[6] = mXFolderX1;
        mTempFloatBuffer[7] = mXFolderY1;
        mTempFloatBuffer[8] = 0;
        
        mTempFloatBuffer[9] = mYFolderX1;
        mTempFloatBuffer[10] = mYFolderY1;
        mTempFloatBuffer[11] = 0;
        
        mTempFloatBuffer[12] = -cornerX;
        mTempFloatBuffer[13] = cornerY;
        mTempFloatBuffer[14] = 0;
        
        // the Front page vertexes coords
        mFrontPageVertexsBuf.put(mTempFloatBuffer, 0, FRONT_PAGE_VERTEXS_SIZE);
        mFrontPageVertexsBuf.position(0);
        
        // 2. create vertexes for Back page
        // Triangle: [5] -> [3] -> [4]
        mTempFloatBuffer[0] = mOriginX;
        mTempFloatBuffer[1] = mOriginY;
        mTempFloatBuffer[2] = 0;
        
        mTempFloatBuffer[3] = mXFolderX1;
        mTempFloatBuffer[4] = mXFolderY1;
        mTempFloatBuffer[5] = 0;
        
        mTempFloatBuffer[6] = mYFolderX1;
        mTempFloatBuffer[7] = mYFolderY1;
        mTempFloatBuffer[8] = 0;
        
        mBackPageVertexsBuf.put(mTempFloatBuffer, 0, BACK_PAGE_VERTEXS_SIZE);
        mBackPageVertexsBuf.position(0);

        // 3. create shadow vertexes for back page
        // 4 triangles, the first 4 vertexes to draw 2 triangles with STRIP mode
        // and the last 4 vertexes to draw 2 triangles with STRIP mode
        float shadowX = (mXFolderX1+mXFolderX)*0.5f;
        float shadowY = (mYFolderY1+mYFolderY)*0.5f;
        mTempFloatBuffer[0] = mYFolderX1;
        mTempFloatBuffer[1] = mYFolderY1;
        mTempFloatBuffer[2] = 0;

        mTempFloatBuffer[3] = mYFolderX1;
        mTempFloatBuffer[4] = shadowY;
        mTempFloatBuffer[5] = 0;

        mTempFloatBuffer[6] = mXFolderX1;
        mTempFloatBuffer[7] = mXFolderY1;
        mTempFloatBuffer[8] = 0;

        mTempFloatBuffer[9]  = shadowX;
        mTempFloatBuffer[10] = mXFolderY1;
        mTempFloatBuffer[11] = 0;

        mTempFloatBuffer[12] = mYFolderX1;
        mTempFloatBuffer[13] = shadowY;
        mTempFloatBuffer[14] = 0;

        mTempFloatBuffer[15] = mYFolderX;
        mTempFloatBuffer[16] = mYFolderY;
        mTempFloatBuffer[17] = 0;

        mTempFloatBuffer[18] = shadowX;
        mTempFloatBuffer[19] = mXFolderY1;
        mTempFloatBuffer[20] = 0;

        mTempFloatBuffer[21] = mXFolderX;
        mTempFloatBuffer[22] = mXFolderY;
        mTempFloatBuffer[23] = 0;

        mBackPageShadowVertexsBuf.put(mTempFloatBuffer, 0,
                                      BACK_PAGE_SHADOW_VERTEXS_SIZE);
        mBackPageShadowVertexsBuf.position(0);
        
        // Set textures coords for vertexes
        // First we need transfer openGL world coordinate system to Android 2D
        // drawing coordinate system
        float halfWidth     = (float)mWidth/2;
        float halfHeight    = (float)mHeight/2;
        float folderX       = (mXFolderX1+halfWidth)/mTextureWidth;
        float folderY       = (halfHeight-mYFolderY1)/mTextureHeight;

        float originX = 0;
        if (mOriginX > 0) {
            originX = mTextureWidthRatio;
        }

        float originY = mTextureHeightRatio;
        if (mOriginY > 0) {
            originY = 0;
        }

        float originX1 = mTextureWidthRatio-originX;
        float originY1 = mTextureHeightRatio-originY;
        
        // 1. Create front page texture coords
        mTempFloatBuffer[0] = originX1;
        mTempFloatBuffer[1] = originY1;

        mTempFloatBuffer[2] = originX1;
        mTempFloatBuffer[3] = originY;

        mTempFloatBuffer[4] = folderX;
        mTempFloatBuffer[5] = originY;

        mTempFloatBuffer[6] = originX;
        mTempFloatBuffer[7] = folderY;
        
        mTempFloatBuffer[8] = originX;
        mTempFloatBuffer[9] = originY1;

        mFrontPageTextureCoordsBuf.put(mTempFloatBuffer, 0,
                                       FRONT_PAGE_TEXTURE_COORDS_SIZE);
        mFrontPageTextureCoordsBuf.position(0);

        // 2. Create back page texture coords
        mTempFloatBuffer[0] = originX;
        mTempFloatBuffer[1] = originY;

        mTempFloatBuffer[2] = folderX;
        mTempFloatBuffer[3] = originY;

        mTempFloatBuffer[4] = originX;
        mTempFloatBuffer[5] = folderY;

        mBackPageTextureCoordsBuf.put(mTempFloatBuffer, 0,
                                      BACK_PAGE_TEXTURE_COORDS_SIZE);
        mBackPageTextureCoordsBuf.position(0);
    }

    /**
     * compute cylinder vertexes
     */
    private void computeCylinderVertexs() {
        final int count = mMeshVertexsCount-1;

        // radius = Lto*3/4PI
        float r             = (float)(mLenOfTouchOrigin/Math.PI*0.75f);
        float Xto           = mTouchX-mOriginX;
        float Yto           = mTouchY-mOriginY;
        float stepY         = (mYFolderY1-mYFolderY0)/count;
        float stepX         = (mXFolderX1-mXFolderX0)/count;
        float halfWidth     = (float)mWidth/2;
        float halfHeight    = (float)mHeight/2;
        float coordX        = (mOriginX+halfWidth)/mTextureWidth;
        float coordY        = (halfHeight-mOriginY)/mTextureHeight; 
        float t             = 7.0f/6;
        float x             = mXFolderX1;
        float y             = mYFolderY1;

        for (int i=0; i<mMeshVertexsCount; ++i) {
            //float x = mXFolderX1-stepX*i;
            //float y = mYFolderY1-stepY*i;
            float yValue    = (mYFolderY1-y)/(mYFolderY1-mOriginY);
            float xValue    = (mXFolderX1-x)/(mXFolderX1-mOriginX);
            float yRadian   = (float)(yValue*Math.PI*t); 
            float xRadian   = (float)(xValue*Math.PI*t);
            float tempY     = (float)(0.875f*yValue-Math.sin(yRadian)*0.75f/Math.PI);
            float tempX     = (float)(0.875f*xValue-Math.sin(xRadian)*0.75f/Math.PI);
            // 1) we think the z is same for points on Y axis and X axis
            // 2) since the default z range is -1 to 1 in openGL, use 0.001f 
            // to scale the z value
            //float z         = (float)(r*(1-Math.cos(yRadian)))*0.001f;

            // compute point on Y axis
            mYVertexs[i].mVertexX = mOriginX + Xto*tempY;
            mYVertexs[i].mVertexY = y + Yto*tempY;
            mYVertexs[i].mVertexZ = (float)(r*(1-Math.cos(yRadian)))*0.001f;
            mYVertexs[i].mCoordX  = coordX;
            mYVertexs[i].mCoordY  = (halfHeight-y)/mTextureHeight;

            // compute point on X axis
            mXVertexs[i].mVertexY = mOriginY+Yto*tempX;
            mXVertexs[i].mVertexX = x + Xto*tempX;
            mXVertexs[i].mVertexZ = (float)(r*(1-Math.cos(xRadian)))*0.001f;
            mXVertexs[i].mCoordX  = (x+halfWidth)/mTextureWidth;
            mXVertexs[i].mCoordY  = coordY;

            // for the next points on X aixs and Y aixs
            x -= stepX;
            y -= stepY;
        }
    }

    /**
     * Create Folder page vertexs and texture coords
     */
    private final void createFolderPage() {
        int i = 0;
        int j = 0;
        int k = 0;

        // 1. create Cylinder vertexes
        for (; i<mMeshVertexsCount-1; ++i) {
            // Triangle 1: made up by two coords on Y axis and one coord on X
            // axis
            mCylinderVertexs[j++] = mYVertexs[i].mVertexX;
            mCylinderVertexs[j++] = mYVertexs[i].mVertexY;
            mCylinderVertexs[j++] = mYVertexs[i].mVertexZ;
            mCylinderVertexs[j++] = mYVertexs[i+1].mVertexX;
            mCylinderVertexs[j++] = mYVertexs[i+1].mVertexY;
            mCylinderVertexs[j++] = mYVertexs[i+1].mVertexZ;
            mCylinderVertexs[j++] = mXVertexs[i].mVertexX;
            mCylinderVertexs[j++] = mXVertexs[i].mVertexY;
            mCylinderVertexs[j++] = mXVertexs[i].mVertexZ;

            // triangle texture coords
            mCylinderTextureCoords[k++] = mYVertexs[i].mCoordX;
            mCylinderTextureCoords[k++] = mYVertexs[i].mCoordY;
            mCylinderTextureCoords[k++] = mYVertexs[i+1].mCoordX;
            mCylinderTextureCoords[k++] = mYVertexs[i+1].mCoordY;
            mCylinderTextureCoords[k++] = mXVertexs[i].mCoordX;
            mCylinderTextureCoords[k++] = mXVertexs[i].mCoordY;

            // Triangle 2: made up by two coords on X axis and one coord on Y
            // axis
            mCylinderVertexs[j++] = mXVertexs[i].mVertexX;
            mCylinderVertexs[j++] = mXVertexs[i].mVertexY;
            mCylinderVertexs[j++] = mXVertexs[i].mVertexZ;
            mCylinderVertexs[j++] = mXVertexs[i+1].mVertexX;
            mCylinderVertexs[j++] = mXVertexs[i+1].mVertexY;
            mCylinderVertexs[j++] = mXVertexs[i+1].mVertexZ;
            mCylinderVertexs[j++] = mYVertexs[i+1].mVertexX;
            mCylinderVertexs[j++] = mYVertexs[i+1].mVertexY;
            mCylinderVertexs[j++] = mYVertexs[i+1].mVertexZ;

            // triangle texture coords
            mCylinderTextureCoords[k++] = mXVertexs[i].mCoordX;
            mCylinderTextureCoords[k++] = mXVertexs[i].mCoordY;
            mCylinderTextureCoords[k++] = mXVertexs[i+1].mCoordX;
            mCylinderTextureCoords[k++] = mXVertexs[i+1].mCoordY;
            mCylinderTextureCoords[k++] = mYVertexs[i+1].mCoordX;
            mCylinderTextureCoords[k++] = mYVertexs[i+1].mCoordY;
        }

        // 2. create folder triangle vertexes
        float zOnY = mYVertexs[i].mVertexZ;
        float zOnX = mXVertexs[i].mVertexZ;
        mCylinderVertexs[j++] = mYVertexs[i].mVertexX;
        mCylinderVertexs[j++] = mYVertexs[i].mVertexY;
        mCylinderVertexs[j++] = zOnY;
        mCylinderVertexs[j++] = mTouchX;
        mCylinderVertexs[j++] = mTouchY;
        mCylinderVertexs[j++] = zOnY;
        mCylinderVertexs[j++] = mXVertexs[i].mVertexX;
        mCylinderVertexs[j++] = mXVertexs[i].mVertexY;
        mCylinderVertexs[j++] = zOnX;

        // create folder triangle texture coords
        mCylinderTextureCoords[k++] = mYVertexs[i].mCoordX;
        mCylinderTextureCoords[k++] = mYVertexs[i].mCoordY;
        mCylinderTextureCoords[k++] = (mOriginX+mWidth/2)/mTextureWidth;
        mCylinderTextureCoords[k++] = (mHeight/2-mOriginY)/mTextureHeight;
        mCylinderTextureCoords[k++] = mXVertexs[i].mCoordX;
        mCylinderTextureCoords[k++] = mXVertexs[i].mCoordY;

        // 3. Create front half cylinder vertexes buffer
        mFrontHalfCylinderVertexsBuf.put(mCylinderVertexs, 0,
                                         mHalfCylinderVertexsSize);//HALF_CYLINDER_VERTEXS_SIZE);
        mFrontHalfCylinderVertexsBuf.position(0);

        // 4. Create front half cylinder texture coords buffer
        mFrontHalfCylinderTextureCoordsBuf.put(mCylinderTextureCoords, 0,
                                               mHalfCylinderTextureCoordsSize);//HALF_CYLINDER_TEXTURE_COORDS_SIZE);
        mFrontHalfCylinderTextureCoordsBuf.position(0);

        // 5. Create folder page vertexes buffer
        mFolderPageVertexsBuf.put(mCylinderVertexs, mHalfCylinderVertexsSize,
                                  mFolderPageVertexsSize);//HALF_CYLINDER_VERTEXS_SIZE, FOLDER_PAGE_VERTEXS_SIZE);
        mFolderPageVertexsBuf.position(0);

        // 6. Create folder page texture coords buffer
        mFolderPageTextureCoordsBuf.put(mCylinderTextureCoords,
                                        mHalfCylinderTextureCoordsSize,
                                        mFolderPageTextureCoordsSize);//HALF_CYLINDER_TEXTURE_COORDS_SIZE, FOLDER_PAGE_TEXTURE_COORDS_SIZE);
        mFolderPageTextureCoordsBuf.position(0);

        // 7. Create folder edge shadow vertexes buffer
        // the order is [XYFolderShadowXY] -> [RYFolderShadowXY1] ->
        // [mRYFolderShadowXY] -> [TouchXY] -> [RXFolderShadowXY] ->
        // [RXFolderShadowXY1]
        mTempFloatBuffer[0] = mXYFolderShadowX;
        mTempFloatBuffer[1] = mXYFolderShadowY;
        mTempFloatBuffer[2] = zOnY;

        mTempFloatBuffer[3] = mRYFolderShadowX1;
        mTempFloatBuffer[4] = mRYFolderShadowY1;
        mTempFloatBuffer[5] = zOnY;

        mTempFloatBuffer[6] = mRYFolderShadowX;
        mTempFloatBuffer[7] = mRYFolderShadowY;
        mTempFloatBuffer[8] = zOnY;

        mTempFloatBuffer[9]  = mTouchX;
        mTempFloatBuffer[10] = mTouchY;
        mTempFloatBuffer[11] = zOnY;

        mTempFloatBuffer[12] = mRXFolderShadowX;
        mTempFloatBuffer[13] = mRXFolderShadowY;
        mTempFloatBuffer[14] = zOnY;

        mTempFloatBuffer[15] = mRXFolderShadowX1;
        mTempFloatBuffer[16] = mRXFolderShadowY1;
        mTempFloatBuffer[17] = zOnY;

        mFolderEdgeShadowVertexsBuf.put(mTempFloatBuffer, 0,
                                        FOLDER_EDGE_SHADOW_VERTEXS_SIZE);
        mFolderEdgeShadowVertexsBuf.position(0);
    }


    /**
     * Create folder edge shadow color buffer
     */
    private void createFolderEdgeShadowColorsBuf() {
        
        // setup folder edge shadow color buffer, the order is:
        // [XYFolderShadowXY] -> [RYFolderShadowXY1] -> [RYFolderShadowXY] ->
        // [TouchXY] -> [RXFolderShadowXY] -> [RXFolderShadowXY1]
        // 1. XYFolderShadowXY
        mTempFloatBuffer[0] = END_FOLDER_PAGE_SHADOW_COLOR;
        mTempFloatBuffer[1] = END_FOLDER_PAGE_SHADOW_COLOR;
        mTempFloatBuffer[2] = END_FOLDER_PAGE_SHADOW_COLOR;
        mTempFloatBuffer[3] = END_FOLDER_PAGE_SHADOW_ALPHA;

        // 2. RYFolderShadowXY1
        mTempFloatBuffer[4] = END_FOLDER_PAGE_SHADOW_COLOR;
        mTempFloatBuffer[5] = END_FOLDER_PAGE_SHADOW_COLOR;
        mTempFloatBuffer[6] = END_FOLDER_PAGE_SHADOW_COLOR;
        mTempFloatBuffer[7] = END_FOLDER_PAGE_SHADOW_ALPHA;

        // 3. RYFolderShadowXY
        mTempFloatBuffer[8]  = START_FOLDER_PAGE_SHADOW_COLOR;
        mTempFloatBuffer[9]  = START_FOLDER_PAGE_SHADOW_COLOR;
        mTempFloatBuffer[10] = START_FOLDER_PAGE_SHADOW_COLOR;
        mTempFloatBuffer[11] = START_FOLDER_PAGE_SHADOW_ALPHA;

        // 4. TouchXY
        mTempFloatBuffer[12] = START_FOLDER_PAGE_SHADOW_COLOR;
        mTempFloatBuffer[13] = START_FOLDER_PAGE_SHADOW_COLOR;
        mTempFloatBuffer[14] = START_FOLDER_PAGE_SHADOW_COLOR;
        mTempFloatBuffer[15] = START_FOLDER_PAGE_SHADOW_ALPHA;

        // 5. RXFolderShadowXY
        mTempFloatBuffer[16] = START_FOLDER_PAGE_SHADOW_COLOR;
        mTempFloatBuffer[17] = START_FOLDER_PAGE_SHADOW_COLOR;
        mTempFloatBuffer[18] = START_FOLDER_PAGE_SHADOW_COLOR;
        mTempFloatBuffer[19] = START_FOLDER_PAGE_SHADOW_ALPHA;

        // 6. RXFolderShadowXY1
        mTempFloatBuffer[20] = END_FOLDER_PAGE_SHADOW_COLOR;
        mTempFloatBuffer[21] = END_FOLDER_PAGE_SHADOW_COLOR;
        mTempFloatBuffer[22] = END_FOLDER_PAGE_SHADOW_COLOR;
        mTempFloatBuffer[23] = END_FOLDER_PAGE_SHADOW_ALPHA;

        mFolderEdgeShadowColorsBuf.put(mTempFloatBuffer, 0,
                                       FOLDER_EDGE_SHADOW_COLOR_SIZE);
        mFolderEdgeShadowColorsBuf.position(0);
    }


    /**
     * Create Folder page mask color buffers
     */
    private void createFolderPageMaskColorsBuf() {
        // Folder page shadow colors
        int i = 0;
        int j = 0;
        int lastVertex      = mMiddleMeshVertexsCount / 2 + 2;
        float step          = (END_CYLINDER_COLOR_MASK -
                               START_CYLINDER_COLOR_MASK) / lastVertex;
        float startColor    = START_CYLINDER_COLOR_MASK;
        float endColor      = startColor + step;

        for (; i<lastVertex; ++i) {
            // 2 triangles, 6 vertexs to be colored
            mFolderPageMaskColors[j++]  = startColor;
            mFolderPageMaskColors[j++]  = startColor;
            mFolderPageMaskColors[j++]  = startColor;
            mFolderPageMaskColors[j++]  = CYLINDER_COLOR_MASK_ALPHA;

            mFolderPageMaskColors[j++]  = endColor;
            mFolderPageMaskColors[j++]  = endColor;
            mFolderPageMaskColors[j++]  = endColor;
            mFolderPageMaskColors[j++]  = CYLINDER_COLOR_MASK_ALPHA;

            mFolderPageMaskColors[j++]  = startColor;
            mFolderPageMaskColors[j++]  = startColor;
            mFolderPageMaskColors[j++]  = startColor;
            mFolderPageMaskColors[j++]  = CYLINDER_COLOR_MASK_ALPHA;

            mFolderPageMaskColors[j++]  = startColor;
            mFolderPageMaskColors[j++]  = startColor;
            mFolderPageMaskColors[j++]  = startColor;
            mFolderPageMaskColors[j++]  = CYLINDER_COLOR_MASK_ALPHA;

            mFolderPageMaskColors[j++]  = endColor;
            mFolderPageMaskColors[j++]  = endColor;
            mFolderPageMaskColors[j++]  = endColor;
            mFolderPageMaskColors[j++]  = CYLINDER_COLOR_MASK_ALPHA;

            mFolderPageMaskColors[j++]  = endColor;
            mFolderPageMaskColors[j++]  = endColor;
            mFolderPageMaskColors[j++]  = endColor;
            mFolderPageMaskColors[j++]  = CYLINDER_COLOR_MASK_ALPHA;

            startColor  += step;
            endColor    += step;
        }

        // set color for left triangles
        lastVertex = (mMiddleMeshVertexsCount - i) * 6 +
                     FOLDER_PAGE_TRIANGLE_COLOR_COUNT;
        for (i=0; i<lastVertex; ++i) {
            mFolderPageMaskColors[j++] = END_CYLINDER_COLOR_MASK; 
            mFolderPageMaskColors[j++] = END_CYLINDER_COLOR_MASK;
            mFolderPageMaskColors[j++] = END_CYLINDER_COLOR_MASK;
            mFolderPageMaskColors[j++] = CYLINDER_COLOR_MASK_ALPHA;
        }

        mFolderPageMaskColorsBuf.put(mFolderPageMaskColors);
        mFolderPageMaskColorsBuf.position(0);

    }

    private void saveBitmap(Bitmap bitmap) {
        OutputStream outStream = null;
        File file = new File("/sdcard/temp.png");
        try {
            outStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.flush();
            outStream.close();

            Log.i("Hub", "OK, Image Saved to SD");
            Log.i("Hub", "height = "+ bitmap.getHeight() + ", width = " + bitmap.getWidth());

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i("Hub", "FileNotFoundException: "+ e.toString());
        } catch (IOException e) {
            e.printStackTrace();
            Log.i("Hub", "IOException: "+ e.toString());
        }
    }

    private final Bitmap getBitmap(int id) {
        InputStream front = mContext.getResources().openRawResource(id);
        Bitmap bitmap = null;    
        try {
            bitmap = BitmapFactory.decodeStream(front);
        } finally {
            try {
                front.close();
            } catch (IOException e) {
                
            }
        }

        return bitmap;
    }

    private final int getBgColor(int id) {
        InputStream front = mContext.getResources().openRawResource(id);
        Bitmap bitmap = null;    
        try {
            bitmap = BitmapFactory.decodeStream(front);
        } finally {
            try {
                front.close();
            } catch (IOException e) {
                
            }
        }

        int color = 0;
        if (null != bitmap) {
            color = Utility.computeAverageColor(bitmap, 50);
            bitmap.recycle();
            bitmap = null;
            System.gc();
        }
        
        System.out.println("****Color:"+color);
        return color;
    }

    private int getNextHighestPO2(int n) {
        n -= 1;
        n = n | (n >> 1);
        n = n | (n >> 2);
        n = n | (n >> 4);
        n = n | (n >> 8);
        n = n | (n >> 16);
        n = n | (n >> 32);
        return n + 1;
    }

    /**
     * Translate Android coord to OpenGL coord
     */
    private float translateToOpenGLCoordX(float x) {
        return x-mWidth*0.5f;
    }

    /**
     * Translate Android coord to OpenGL coord
     */
    private float translateToOpenGLCoordY(float y) {
        return mHeight*0.5f-y;
    }

    /**
     * Compute vertexs, texture coords count, it is depended on screen size
     */
    private void computeMeshVertexsCount(int width, int height) {
        // get the max pixels from screen width and height
        int maxPixels = Math.max(width, height);

        // there is a vertex every 20 pixles
        mMeshVertexsCount       = maxPixels/mPixelsIntervalOfMeshVertexs;
        // make sure the vertex count is odd number
        if (mMeshVertexsCount%2 == 0)
            mMeshVertexsCount++;
        mMiddleMeshVertexsCount = mMeshVertexsCount/2;

        // when the page is curled, the curl page is divided two part: one is
        // front half cylinder, another is back half cylinder and a folder page
        // triangle
        mHalfCylinderVertexsCount       = (mMeshVertexsCount-1)*3;
        mHalfCylinderVertexsSize        = mHalfCylinderVertexsCount*3;
        mHalfCylinderTextureCoordsSize  = mHalfCylinderVertexsCount<<1;

        // Folder page vertexes = half cylinder vertexes count + last folder
        // page triangle
        mFolderPageVertexsCount         = mHalfCylinderVertexsCount+3;
        mFolderPageVertexsSize          = mFolderPageVertexsCount*3;
        mFolderPageTextureCoordsSize    = mFolderPageVertexsCount<<1;

        // The start vertex of folder page triangle 
        // mFolderPageTriangleVertexStart  = mHalfCylinderVertexsSize>>1; 
        //mFolderPageTriangleTextureCoordsStart = mHalfCylinderTextureCoordsSize>>1;

        // Cylinder vertexes and folder triangle vertexes
        mCylinderVertexsSize            = mHalfCylinderVertexsSize<<1+9;
        mCylinderTextureCoordsSize      = mHalfCylinderTextureCoordsSize<<1+6;

        // Folder page color mask size
        mFolderPageColorMaskSize        = mFolderPageVertexsCount<<2;

        // create buffers 
        mFolderPageMaskColors           = new float[mFolderPageColorMaskSize];
        mCylinderTextureCoords          = new float[mCylinderTextureCoordsSize];
        mCylinderVertexs                = new float[mCylinderVertexsSize];

        // create vertexes buffers
        mYVertexs = new Vertex3D[mMeshVertexsCount];
        mXVertexs = new Vertex3D[mMeshVertexsCount];
        for (int i=0; i<mMeshVertexsCount; ++i) {
            mYVertexs[i] = new Vertex3D();
            mXVertexs[i] = new Vertex3D();
        }
    }

    /*
     * Create buffer for vertexes, colors and texture coords
     */
    private void createBuffers() {
        // 1. Front page vertexes buffer
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(FRONT_PAGE_VERTEXS_SIZE<<2);
        byteBuf.order(ByteOrder.nativeOrder());
        mFrontPageVertexsBuf = byteBuf.asFloatBuffer();

        // 2. Back page vertexes buffer
        byteBuf = ByteBuffer.allocateDirect(BACK_PAGE_VERTEXS_SIZE<<2);
        byteBuf.order(ByteOrder.nativeOrder());
        mBackPageVertexsBuf = byteBuf.asFloatBuffer();

        // 3. Front half cylinder vertexes buffer
        byteBuf = ByteBuffer.allocateDirect(mHalfCylinderVertexsSize<<2);//HALF_CYLINDER_VERTEXS_SIZE*4);
        byteBuf.order(ByteOrder.nativeOrder());
        mFrontHalfCylinderVertexsBuf = byteBuf.asFloatBuffer();

        // 4. Back half cylinder vertexes buffer
        byteBuf = ByteBuffer.allocateDirect(mCylinderVertexsSize<<2);//CYLINDER_VERTEXS_SIZE*4);
        byteBuf.order(ByteOrder.nativeOrder());
        mFolderPageVertexsBuf = byteBuf.asFloatBuffer();

        // 6. Front page texture coords buffer
        byteBuf = ByteBuffer.allocateDirect(FRONT_PAGE_TEXTURE_COORDS_SIZE<<2);
        byteBuf.order(ByteOrder.nativeOrder());
        mFrontPageTextureCoordsBuf = byteBuf.asFloatBuffer();

        // 7. Back page texture coords buffer
        byteBuf = ByteBuffer.allocateDirect(BACK_PAGE_TEXTURE_COORDS_SIZE<<2);
        byteBuf.order(ByteOrder.nativeOrder());
        mBackPageTextureCoordsBuf = byteBuf.asFloatBuffer();
    
        // 8. Front half cylinder texture coords buffer
        byteBuf = ByteBuffer.allocateDirect(mHalfCylinderTextureCoordsSize<<2);//HALF_CYLINDER_TEXTURE_COORDS_SIZE*4);
        byteBuf.order(ByteOrder.nativeOrder());
        mFrontHalfCylinderTextureCoordsBuf = byteBuf.asFloatBuffer();

        // 9. Back half cylinder texture coords buffer
        byteBuf = ByteBuffer.allocateDirect(mFolderPageTextureCoordsSize<<2);//FOLDER_PAGE_TEXTURE_COORDS_SIZE*4);
        byteBuf.order(ByteOrder.nativeOrder());
        mFolderPageTextureCoordsBuf = byteBuf.asFloatBuffer();

        // 11. Folder page shadow color buffer
        byteBuf = ByteBuffer.allocateDirect(mFolderPageColorMaskSize<<2);//FOLDER_PAGE_COLOR_MASK_SIZE*4);
        byteBuf.order(ByteOrder.nativeOrder());
        mFolderPageMaskColorsBuf = byteBuf.asFloatBuffer();

        // 12. Back page shadow vertexes buffer
        byteBuf = ByteBuffer.allocateDirect(BACK_PAGE_SHADOW_VERTEXS_SIZE<<2);
        byteBuf.order(ByteOrder.nativeOrder());
        mBackPageShadowVertexsBuf = byteBuf.asFloatBuffer();

        // 13. Back page shadow color buffer
        byteBuf = ByteBuffer.allocateDirect(32<<2);
        byteBuf.order(ByteOrder.nativeOrder());
        mBackPageShadowColorsBuf = byteBuf.asFloatBuffer();
        float[] colorTemp = new float[] {
            START_BACK_PAGE_SHADOW_COLOR, START_BACK_PAGE_SHADOW_COLOR,
            START_BACK_PAGE_SHADOW_COLOR, START_BACK_PAGE_SHADOW_ALPHA,
            START_BACK_PAGE_SHADOW_COLOR, START_BACK_PAGE_SHADOW_COLOR,
            START_BACK_PAGE_SHADOW_COLOR, START_BACK_PAGE_SHADOW_ALPHA,
            START_BACK_PAGE_SHADOW_COLOR, START_BACK_PAGE_SHADOW_COLOR,
            START_BACK_PAGE_SHADOW_COLOR, START_BACK_PAGE_SHADOW_ALPHA,
            START_BACK_PAGE_SHADOW_COLOR, START_BACK_PAGE_SHADOW_COLOR,
            START_BACK_PAGE_SHADOW_COLOR, START_BACK_PAGE_SHADOW_ALPHA,

            START_BACK_PAGE_SHADOW_COLOR, START_BACK_PAGE_SHADOW_COLOR,
            START_BACK_PAGE_SHADOW_COLOR, START_BACK_PAGE_SHADOW_ALPHA,
            END_BACK_PAGE_SHADOW_COLOR, END_BACK_PAGE_SHADOW_COLOR,
            END_BACK_PAGE_SHADOW_COLOR, END_BACK_PAGE_SHADOW_ALPHA,
            START_BACK_PAGE_SHADOW_COLOR, START_BACK_PAGE_SHADOW_COLOR,
            START_BACK_PAGE_SHADOW_COLOR, START_BACK_PAGE_SHADOW_ALPHA,
            END_BACK_PAGE_SHADOW_COLOR, END_BACK_PAGE_SHADOW_COLOR,
            END_BACK_PAGE_SHADOW_COLOR, END_BACK_PAGE_SHADOW_ALPHA,
        };
        mBackPageShadowColorsBuf.put(colorTemp);
        mBackPageShadowColorsBuf.position(0);

        // 14. Folder edge shadow vertexes buffer
        byteBuf = ByteBuffer.allocateDirect(FOLDER_EDGE_SHADOW_VERTEXS_SIZE<<2);
        byteBuf.order(ByteOrder.nativeOrder());
        mFolderEdgeShadowVertexsBuf = byteBuf.asFloatBuffer();

        // 15. Folder page shadow color buffer
        byteBuf = ByteBuffer.allocateDirect(FOLDER_EDGE_SHADOW_COLOR_SIZE<<2);
        byteBuf.order(ByteOrder.nativeOrder());
        mFolderEdgeShadowColorsBuf = byteBuf.asFloatBuffer();

    }


    private void printBasicInfos() {
        System.out.println("************************************");
        System.out.println(" Mesh Vertexs:  "+mMeshVertexsCount);
        System.out.println(" Pixel Interval:"+mPixelsIntervalOfMeshVertexs);
        System.out.println(" MaxTouchY:     "+mMaxTouchY);
        System.out.println(" Origin:        "+mOriginX+", "+mOriginY);
        System.out.println(" TouchXY:       "+mTouchX+", "+mTouchY);
        System.out.println(" MiddleXY:      "+mMiddleX+", "+mMiddleY);
        System.out.println(" XFolder:       "+mXFolderX+", "+mXFolderY);
        System.out.println(" XFolder0:      "+mXFolderX0+", "+mXFolderY0);
        System.out.println(" XFolder1:      "+mXFolderX1+", "+mXFolderY1);
        System.out.println(" YFolder:       "+mYFolderX+", "+mYFolderY);
        System.out.println(" YFolder0:      "+mYFolderX0+", "+mYFolderY0);
        System.out.println(" YFolder1:      "+mYFolderX1+", "+mYFolderY1);
        System.out.println(" XShadow:       "+mXFolderShadowX+", "+mXFolderShadowY);
        System.out.println(" RXShadow:      "+mRXFolderShadowX+", "+mRXFolderShadowY);
        System.out.println(" RXShadow1:     "+mRXFolderShadowX1+", "+mRXFolderShadowY1);
        System.out.println(" YShadow:       "+mYFolderShadowX+", "+mYFolderShadowY);
        System.out.println(" RYShadow:      "+mRYFolderShadowX+", "+mRYFolderShadowY);
        System.out.println(" RYShadow1:     "+mRYFolderShadowX1+", "+mRYFolderShadowY1);
        System.out.println(" XYShadowXY:    "+mXYFolderShadowX+", "+mXYFolderShadowY);
        System.out.println(" LengthT->O:    "+mLenOfTouchOrigin);
    }

    /*
     * Draw on screen
     */
    class Vertex3D {
        float mVertexX;
        float mVertexY;
        float mVertexZ;
        float mCoordX;
        float mCoordY;
    }
}
