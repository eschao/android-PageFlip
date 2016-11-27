# PageFlip
An android library of 3D style page flip. It needs OpenGL 2.0!

## Preview

## Installation

#### Gradle

Add it to your build.gradle with:
```gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```
and:

```gradle
dependencies {
    compile 'com.github.eschao:android-PageFlip:v1.0'
}
```

## Usage

### 1. Simple steps for introducing PageFlip into your project:

* Creates a surface view class extending from **GLSurfaceView**
* Implements android **Renderer** interface to draw your content on screen
* Instanitiates a **PageFlip** object in the constructor of your surface view
* Configures **PageFlip**, for example: set animating duration, page mode or mesh pixels
* Handles the below android events:

  * **onFingerDown**: notify PageFlip object to prepare flip 
  * **onFingerMove**: notify PageFlip object to compute data for drawing flip frame
  * **onFingerUp**: notify PageFlip object to determine whether or not launching a flip animation
  * **onSurfaceCreated**: notify PageFlip object to handle usreface creating event
  * **onSurfaceChanged**: notify PageFlip object to handle surface changing event
  
* You may need a message handler to receiver a end drawing message from OpenGL rendering thread. Refers to **PageFlipView** in sample application.
* You may need a locker to avoid conflicts between main thread and OpenGL rendering thread. Refers to **PageFlipView** in sample application.

More details, please take a look **PageFlipView** in sample application

### 2. Configure PageFlip

**PageFlip** library provides some configurations for customizing its behaviors. For example: shadow color and alpha, mesh pixels and page mode. 

* **Page Mode**

  There are two page modes provided by **PageFlip**:
    * **Auto Page Mode**: In this mode, **PageFlip** will automatically determine use single page or double pages to present content on screen. That means single page is used for portrait mode and double pages is used for lanscape mode.
    * **Single Page Mode**: No matter what screen is portait or landscape mode, **PageFlip** always use single page to show content

  You can use **enableAutoPage** to enable auto page mode or disable it(enable single page mode).

  **Example:**
  ```java
    // enable auto page mode
    mPageFlip.enableAutopage(true); 
  ```
  
* **Click screen to flip**
  
  You can enable/disable clicking screen to flip page
  
  **Example:**
  ```java
    // enable clicking to flip
    mPageFlip.enableClickToFlip(true);
  ```
  
* **Set screen area for clicking to flip**
  
  You can give a ratio of width from 0 to 0.5f to set a area for reponsing click event to trigger a page flip. The default value is **0.5f** which means the backfward flip will happen if you click the left half of screen and forward flip will start if you click the right half of screen in single page mode.
  
  **Example:**
  ```java
    // set ratio with 0.3
    mPageFlip.setWidthRatioOfClickToFlip(0.3f);
  ```

 
## License
This project is licensed under the Apache License Version 2.0
