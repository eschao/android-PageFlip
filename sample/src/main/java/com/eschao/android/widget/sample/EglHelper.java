package com.eschao.android.widget.sample;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import javax.microedition.khronos.opengles.GL10;

import android.opengl.EGL14;
import android.opengl.GLSurfaceView;
import android.util.Log;

/**
 * This is EGL config helper class
 */
public class EglHelper {

    static final int CONFIG_SPEC_NUM        = 13;
    static final int CONFIG_SPEC_NUM_V2        = 15;
    private int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

    int mRedSize;
    int mGreenSize;
    int mBlueSize;
    int mAlphaSize;
    int mDepthSize;
    int mStencilSize;

    boolean mIsLogging;

    EGLContext mEglContext;
    EGLSurface mEglSurface;
    EGLDisplay mEglDisplay;
    GL10       mGL10;
    EGL10      mEgl;

    public EglHelper() {
        mRedSize        = 5;
        mGreenSize      = 6;
        mBlueSize       = 5;
        mAlphaSize      = 0;
        mDepthSize      = 0;
        mStencilSize    = 0;
        mIsLogging      = false;
    }

    public EglHelper(int red, int green, int blue, int alpha, int depth, int stencil) {
        mRedSize        = red;
        mGreenSize      = green;
        mBlueSize       = blue;
        mAlphaSize      = alpha;
        mDepthSize      = depth;
        mStencilSize    = stencil;
        mIsLogging      = false;
    }

    public final EGLContext getEglContext() {
        return mEglContext;
    }

    public final EGLSurface getEglSurface() {
        return mEglSurface;
    }

    public final EGLDisplay getEglDisplay() {
        return mEglDisplay;
    }

    public final GL10 getGL() {
        return mGL10;
    }

    public final EGL10 getEgl() {
        return mEgl;
    }

    public final boolean isCreated() {
        return (null != mEglContext && null != mEglDisplay && null != mEglSurface && null != mGL10);
    }

    public final void initEgl(int red, int green, int blue, int alpha, int depth, int stencil, Object nativeWindow)
        throws IllegalArgumentException, RuntimeException {
        mRedSize        = red;
        mGreenSize      = green;
        mBlueSize       = blue;
        mAlphaSize      = alpha;
        mDepthSize      = depth;
        mStencilSize    = stencil;

        initEgl(nativeWindow);
    }

    public final void initEgl(Object nativeWindow) throws IllegalArgumentException {
        // Get an EGL instance
        EGL10 egl = (EGL10)EGLContext.getEGL();

        // Get the the default display
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

        // Init EGL version
        int[] version = new int[2];
        egl.eglInitialize(display, version);

        //printAllEglConfigs(egl, display);
        
        // Get config count 
        int[] configSpec = buildEglConfigSpec();
        int[] num_config = new int[1];
        if (!egl.eglChooseConfig(display, configSpec, null, 0, num_config)) {
            throw new IllegalArgumentException("EglHelper: eglChooseConfig failed");
        }

        // Check configu count 
        int numConfigs = num_config[0];
        if (numConfigs <= 0) {
            throw new IllegalArgumentException( "EglHelper: No configs match configSpec");
        }

        // Choose config
        EGLConfig[] configs = new EGLConfig[numConfigs];
        if (!egl.eglChooseConfig(display, configSpec, configs, numConfigs, num_config)) {
            throw new IllegalArgumentException("EglHelper: eglChooseConfig#2 failed");
        }
        
        EGLConfig config = chooseConfig(egl, display, configs);
        if (config == null) {
            throw new IllegalArgumentException("EglHelper: No config chosen");
        }

        // Create EGL context
        int[] contextAttrs = new int[] {
                EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE
        };
        EGLContext context = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, contextAttrs);
        if (context == null || context == EGL10.EGL_NO_CONTEXT) {
            throwEglException("EglHelper: createContext failed", egl.eglGetError());
        }

        // Create EGL surface
        EGLSurface surface = egl.eglCreateWindowSurface(display, config, nativeWindow, null);
        if (null == surface) {
            egl.eglDestroyContext(display, context);
            throwEglException("EglHelper: createWindowSurface failed", egl.eglGetError());
        }

        // Bind EGL 
        if (!egl.eglMakeCurrent(display, surface, surface, context)) {
            egl.eglDestroySurface(display, surface);
            egl.eglDestroyContext(display, context);
            throwEglException("EglHelper: eglMakeCurrent failed", egl.eglGetError());
        }

        mEgl        = egl;
        mEglContext = context;
        mEglDisplay = display;
        mEglSurface = surface;
        mGL10       = (GL10)mEglContext.getGL();
    }

    public final void swapBuffer() {
        if (!mEgl.eglSwapBuffers(mEglDisplay, mEglSurface)) {
            Log.e("EglHelper", "eglSwapBuffers returned error: " + mEgl.eglGetError());
        }
    }

    public final void destroy(boolean isTerminate) {
        if (null != mEgl) {
            mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            mEgl.eglDestroyContext(mEglDisplay, mEglContext);
            mEgl.eglDestroySurface(mEglDisplay, mEglSurface);

            if (isTerminate)
                mEgl.eglTerminate(mEglDisplay);

            mEglContext = null;
            mEglDisplay = null;
            mEglSurface = null;
            mGL10       = null;
        }
    }

    public final int getEglConfigList(EGL10 egl, EGLDisplay display, EGLConfig[] configs, int numConfigs) {
        int[] ret = new int[1];
        egl.eglGetConfigs(display, configs, numConfigs, ret);
        return ret[0];
    }
    
    public final void printEglConfig(EGL10 egl, EGLDisplay display, EGLConfig config) {
        int[] value = new int[1];
        egl.eglGetConfigAttrib(display, config, EGL10.EGL_RED_SIZE, value);
        System.out.print("Red Size:"+value[0]);
        egl.eglGetConfigAttrib(display, config, EGL10.EGL_GREEN_SIZE, value);
        System.out.print(" Green Size:"+value[0]);
        egl.eglGetConfigAttrib(display, config, EGL10.EGL_BLUE_SIZE, value);
        System.out.print(" Blue Size:"+value[0]);
        egl.eglGetConfigAttrib(display, config, EGL10.EGL_ALPHA_SIZE, value);
        System.out.print(" Alpha Size:"+value[0]);
        egl.eglGetConfigAttrib(display, config, EGL10.EGL_DEPTH_SIZE, value);
        System.out.print(" Depth Size:"+value[0]);
        egl.eglGetConfigAttrib(display, config, EGL10.EGL_STENCIL_SIZE, value);
        System.out.print(" Stencil Size:"+value[0]);
        egl.eglGetConfigAttrib(display, config, EGL10.EGL_RENDERABLE_TYPE, value);
        System.out.print(" Renderable Type:"+value[0]);
    }
    
    public final void printAllEglConfigs(EGL10 egl, EGLDisplay display) {
        EGLConfig[] eglConfigs = new EGLConfig[20];
        int count = getEglConfigList(egl, display, eglConfigs, 20);
        for (int i=0; i<count; ++i) {
            System.out.println("****["+i+"]****");
            printEglConfig(egl, display, eglConfigs[i]);
        }
    }
    
    private int[] buildEglConfigSpec() {
        // Refer to Android::GLSurfaceView class
        int[] configSpec = new int[CONFIG_SPEC_NUM];
        configSpec[0] = EGL10.EGL_RED_SIZE;
        configSpec[1] = mRedSize;
        configSpec[2] = EGL10.EGL_GREEN_SIZE;
        configSpec[3] = mGreenSize;
        configSpec[4] = EGL10.EGL_BLUE_SIZE;
        configSpec[5] = mBlueSize;
        configSpec[6] = EGL10.EGL_ALPHA_SIZE;
        configSpec[7] = mAlphaSize;
        configSpec[8] = EGL10.EGL_DEPTH_SIZE;
        configSpec[9] = mDepthSize;
        configSpec[10] = EGL10.EGL_STENCIL_SIZE;
        configSpec[11] = mStencilSize;
        configSpec[12] = EGL10.EGL_NONE;
        return configSpec;
    }

    
    private EGLConfig chooseConfig(EGL10 egl, EGLDisplay display, EGLConfig[] configs) {
        for (EGLConfig config : configs) {
            int d = findConfigAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE, 0);
            int s = findConfigAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0);
            if ((d >= mDepthSize) && (s >= mStencilSize)) {
                int r = findConfigAttrib(egl, display, config, EGL10.EGL_RED_SIZE, 0);
                int g = findConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE, 0);
                int b = findConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE, 0);
                int a = findConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0);
                if ((r == mRedSize) && (g == mGreenSize) && (b == mBlueSize) && (a == mAlphaSize)) {
                    return config;
                }
            }
        }

        return null;
    }

    private int findConfigAttrib(EGL10 egl, EGLDisplay display, EGLConfig config, int attribute, int defaultValue) {
        final int[] value = new int[1];
        if (egl.eglGetConfigAttrib(display, config, attribute, value)) {
            return value[0];
        }

        return defaultValue;
    }

    private void throwEglException(String function, int error) {
        String message = function + " failed: " + error;
        if (mIsLogging) {
            Log.e("EglHelper", "throwEglException: " + message);
        }
        throw new RuntimeException(message);
    }
}
