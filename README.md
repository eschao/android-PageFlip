[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-3D%20Style%20PageFlip-brightgreen.svg?style=flat)]()

# PageFlip
This project is aimed to implement 3D style page flip on Android system based on OpenGL 2.0.

## Table of Contents
 
 * [Preview](#preview)
 * [Installation](#installation)
   - [Gradle](#gradle)
 * [Usage](#usage)
   - [Introduce PageFlip Into Your Project](#i-simple-steps-for-introducing-pageflip-into-your-project)
   - [Configure PageFilp](#ii-configure-pageflip)
     + [Page Mode](#1-page-mode)
     + [Click Screen To Flip](#2-click-screen-to-flip)
     + [Area Of Clicking To Flip](#3-area-of-clicking-to-flip)
     + [PageFlip Listener](#4-pageflip-listener)
     + [Mesh Pixels](#5-mesh-pixels)
     + [Ratio Of Semi-peremeter](#6-ratio-of-semi-peremeter)
     + [Mask Alpha For The Back Of Fold Page](#7-mask-alpha-for-the-back-of-fold-page)
     + [Edge Shadow Color/Alpha Of Fold Page](#8-edge-shadow-coloralpha-of-fold-page)
     + [Base Shadow Color/Alpha Of Fold Page](#9-base-shadow-coloralpha-of-fold-page)
     + [Edge Shadow Width Of Fold Page](#10-edge-shadow-width-of-fold-page)
     + [Base Shadow Width Of Fold Page](#11-base-shadow-width-of-fold-page)
     + [Duration Of Flip Animating](#12-duration-of-flip-animating)
     
 * [License](#license)
 
## Preview

![SinglePage](https://cloud.githubusercontent.com/assets/20178358/20646678/df7c6ba4-b4ba-11e6-8753-6f764f825cc2.png)    ![DoublePages](https://cloud.githubusercontent.com/assets/20178358/20646731/20f6ebc6-b4bc-11e6-9857-efd8367db80c.png)

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

### I. Simple steps for introducing PageFlip into your project

* Creates a surface view class extending from **GLSurfaceView**
* Implements android **Renderer** interface to draw your content on a bitmap and set it as a texture of **PageFlip**
* Instanitiates a **PageFlip** object in the constructor of your surface view
* Configures **PageFlip**, For example: set animating duration, page mode or mesh pixels
* Handles the below android events:

  * **onFingerDown**: notify *PageFlip* object to prepare flip 
  * **onFingerMove**: notify *PageFlip* object to compute data for drawing flip frame
  * **onFingerUp**: notify *PageFlip* object to determine whether or not launching a flip animation
  * **onSurfaceCreated**: notify *PageFlip* object to handle usreface creating event
  * **onSurfaceChanged**: notify *PageFlip* object to handle surface changing event
  
* You may need a message handler to send/receive an drawing message. Please refer to **PageFlipView** in sample application.
* You may need a lock to avoid conflicts between main thread and OpenGL rendering thread. Please refer to **PageFlipView** in sample application.

More details, please take a look **PageFlipView** in sample application.

### II. Configure PageFlip

**PageFlip** library provides some configurations for customizing its behaviors. For example: shadow color and alpha, mesh pixels and page mode. 

#### 1. Page Mode

  There are two page modes provided by **PageFlip**:
  
  * **Auto Page Mode**: In this mode, **PageFlip** will automatically determine use single page or double pages to present content on screen. That means single page is used for portrait mode and double pages is used for lanscape mode.
  * **Single Page Mode**: No matter screen is portait or landscape mode, **PageFlip** always use single page to show content


You can use **enableAutoPage** to enable auto page mode or disable it(equally enable single page mode).

  Example:
  ```java
    // enable auto page mode
    mPageFlip.enableAutopage(true); 
  ```
  
#### 2. Click screen to flip
  
  You can enable/disable clicking screen to flip
  
  Example:
  ```java
    // enable clicking to flip
    mPageFlip.enableClickToFlip(true);
  ```
  
#### 3. Area of clicking to flip
  
  You can give a ratio of page width from 0 to 0.5f to set an area for reponsing click event to trigger a page flip. The default value is **0.5f**, which means the backfward flip will happen if you click the left half of screen and forward flip will start if you click the right half of screen in single page mode.
  
  Example:
  ```java
    // set ratio with 0.3
    mPageFlip.setWidthRatioOfClickToFlip(0.3f);
  ```

#### 4. PageFlip listener

  You can set a listener to tell **PageFlip** if the forward flip or backward flip could happen.
   
  Example:
  ```java
    mPageFlip.setListener(mListener);
  ```
 
#### 5. Mesh pixels

  Set how many pixels are used for a mesh. The less pxiels the mesh uses, the more fine the drawing is and the lower the  performance is. The default value is 10 pixels.
  
  Example:
  ```java
    mPageFlip.setPixelsOfMesh(5);
  ```
  
#### 6. Ratio of semi-peremeter

  When page is curled, it is actually tackled as a semi-cylinder by **PageFlip**. You can set size of the semi-cylinder to change the flip shap. Since the semi-cylinder dependeds on the line length from the touch point to original point(see the below illustration), you need to provide a ratio of this line length to tell **PageFlip** the peremeter of the semi-cylinder. The default value is 0.8f.
  
  ```
    +----------------+
    |   touchP       |
    |       .        | 
    |        \       |
    |         + p0   |
    |          \     |
    |           \    |
    |        p1  +   |
    |              \ |
    +----------------+
                original point, that means you drag the page from here to touch point(touchP)
  
    The length from p0 to p1 is peremeter of semi-cylinder and determined by ratio your giving
  ```
  
  Example:
  ```java
    mPageFlip.setSemiPerimeterRatio(0.6f);
  ```
  
#### 7. Mask alpha for the back of fold page

  You can set the mask alpha for the back of fold page when page is curled in single page mode. The default value is 0.6f.
  
  Example:
  ```java
    mPageFlip.setMaskAlphaOfFold(0.5f);
  ```
  
#### 8. Edge shadow color/alpha of fold page

  You can set start/end color and start/end alpha for edge shadow of fold page.
  
  Example:
  ```java
    // set start color with 0.1f, start alpha with 0.2f, end color with 0.5f
    // and end alpha with 1f
    mPageFlip.setShadowColorOfFoldBase(0.1f, 0.2f, 0.5f, 1f);
  ```

#### 9. Base shadow color/alpha of fold page

  You can set start/end color and start/end alpha for base shadow of fold page.
  
  Example:
  ```java
    mPageFlip.setShadowColorOfFoldBase(0.05f, 0.2f, 0.5f, 1f);
  ```
  
#### 10. Edge shadow width of fold page

  When page is curled, the size of fold page will follow the finger movement to be changed and its edge shadow width should be changed accordingly. You can set an appropriate width range for shadow width.
  
  Example:
  ```java
    // set the minimal width is 5 pixels and maximum width is 40 pixels.
    // set the ratio is 0.3f which means the width will be firstly computed by formula: 
    // width = diameter of semi-cylinder * 0.3f, and then compare it with minimal
    // and maximal value to make sure the width is in range.
    mPageFlip.setShadowWidthOfFoldEdges(5, 40, 0.3f);
  ```

#### 11. Base shadow width of fold page

  Like **[Edge shadow width of fold page](10-edge-shadow-width-of-fold-page)**, You can set an appropriate width range for base shadow of fold page.
  
  Example:
  ```java
    // see {@link #setShadowWidthOfFoldEdges} function
    mPageFlip.setShadowWidthOfFoldBase(5, 40, 0.4f);
  ```

#### 12. Duration of flip animating

  You can give a duration for flip animating when you call **onFingerUp** function to handle the finger up event.
  
  Example:
  ```java
    // the last parameter is duration with millisecond unit, here we set it with 2 seconds.
    mPageFlip.onFingerUp(x, y, 2000);
  ```
  
## License
This project is licensed under the Apache License Version 2.0.
