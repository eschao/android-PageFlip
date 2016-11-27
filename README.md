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
  
  You can enable/disable clicking screen to flip
  
  **Example:**
  ```java
    // enable clicking to flip
    mPageFlip.enableClickToFlip(true);
  ```
  
* **Area of clicking to flip**
  
  You can give a ratio of width from 0 to 0.5f to set a area for reponsing click event to trigger a page flip. The default value is **0.5f**, which means the backfward flip will happen if you click the left half of screen and forward flip will start if you click the right half of screen in single page mode.
  
  **Example:**
  ```java
    // set ratio with 0.3
    mPageFlip.setWidthRatioOfClickToFlip(0.3f);
  ```

* **PageFlip listener**

  You can set a listener to tell **PageFlip** if the forward flip or backward flip is allowed to be happened.
   
  **Example:**
  ```java
    mPageFlip.setListener(mListener);
  ```
 
* **Mesh pixels**

  Set how many pixels are used to for a mesh. The less pxiels the mesh uses, the more fine the drawing is and the lower the  performance is. The default value is 10 pixels.
  
  **Example:**
  ```java
    mPageFlip.setPixelsOfMesh(5);
  ```
  
* **Ratio of semi-peremeter**

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
    +----------------+ original point, that means you drag the page from here to touch point(touchP)
  
    The length from p0 to p1 is peremeter of semi-cylinder and determined by ratio you giving
  ```
  
  **Example:**
  ```java
    mPageFlip.setSemiPerimeterRatio(0.6f);
  ```
  
* **Mask alpha for the back of fold page**

  You can set the mask alpha for back of fold page when page is curled in single page mode. The default value is 0.6f.
  
  **Example:**
  ```java
    mPageFlip.setMaskAlphaOfFold(0.5f);
  ```
  
* **Edge shadow color/alpha of fold page**

  You can set start/end color and start/end alpha for edge shadow of fold page.
  
  **Example:**
  ```java
    // set start color with 0.1f, start alpha with 0.2f, end color with 0.5f
    // and end alpha with 1f
    mPageFlip.setShadowColorOfFoldBase(0.1f, 0.2f, 0.5f, 1f);
  ```

* **Base shadow color/alpha of fold page**

  You can set start/end color and start/end alpha for base shadow of fold page.
  
  **Example:**
  ```java
    mPageFlip.setShadowColorOfFoldBase(0.05f, 0.2f, 0.5f, 1f);
  ```
  
* **Edge shadow width of fold page**

  When page is curled, the size of fold page will be changed following the finger movement and its edge shadow width should be changed accordingly. You can set an appropriate width range for it to get a good flip animation.
  
  **Example:**
  ```java
    // set the minimal width is 5 pixels and maximum width is 40 pixels.
    // set the ratio is 0.3f which means the width will be firstly computed by formula: 
    // width = diameter of semi-cylinder * 0.3f, and then compare it with minimal
    // and maximal value to make sure the width is in range.
    mPageFlip.setShadowWidthOfFoldEdges(5, 40, 0.3f);
  ```

* **Base shadow width of fold page**

  Like **[Edge shadow width of fold page](edge-shadow-width-of-foldpage)**, You can set an appropriate width range for base shadow of fold page.
  
  **Example:**
  ```java
    // see {@link #setShadowWidthOfFoldEdges} function
    mPageFlip.setShadowWidthOfFoldBase(5, 40, 0.4f);
  ```

* **Duration of flip animating**

  You can give a duration for flip animating when you call **onFingerUp** function to handle finger up event.
  
  **Example:**
  ```java
    // the last parameter is duration with millisecond unit, here we set it with 2 seconds.
    mPageFlip.onFingerUp(x, y, 2000);
  ```
  
## License
This project is licensed under the Apache License Version 2.0
