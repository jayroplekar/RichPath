package com.richpath;

import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Canvas;
import android.graphics.Path;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;

import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.graphics.Matrix;
import android.graphics.PointF;

import android.view.View;

import com.richpath.model.Vector;
import com.richpath.pathparser.PathParser;
import com.richpath.util.XmlParser;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import static java.lang.Math.abs;

/**
 * Created by tarek on 6/29/17.
 */

public class RichPathView extends androidx.appcompat.widget.AppCompatImageView {



    private Vector vector;
    private RichPathDrawable richPathDrawable;
    private RichPath.OnPathClickListener onPathClickListener;

    static final String TAG="RichPathView  zoom+pan";

    // These matrices will be used to move and zoom image
    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();

    // We can be in one of these 3 states
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    static final int TOUCH = 3;
    static final int DOUBLETAP = 4;
    int mode=NONE;

    private static final int INVALID_POINTER_ID = -1;

    // The ‘active pointer’ is the one currently moving our object.
    private int mActivePointerId = INVALID_POINTER_ID;

    // Remember some things for zooming
    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;

    // Let's cluster android sample into this
    private static final String DEBUG_TAG = "Gestures";
    private ScaleGestureDetector mScaleDetector;
    private final GestureDetectorCompat mScrollDetector;
    private float mScaleFactor = 1.0f;
    private float mPosX, mPosY,zoomFocusX, zoomFocusY;
    private float mLastTouchX;
    private float mLastTouchY;

    private float PhysicsFactor=0.001F;
    private static final int SWIPE_THRESHOLD = 20;
    private static final int SWIPE_VELOCITY_THRESHOLD = 50;

    private class ScaleListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();
            zoomFocusX=detector.getFocusX();
            zoomFocusY=detector.getFocusY();
            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));
           // Log.d(DEBUG_TAG,"onScale: " + mScaleFactor +" FocusX: "+detector.getFocusX()+" FocusY: "+detector.getFocusY());
            mode=ZOOM;
            invalidate();
            return true;
        }
    }
    class MyScrollListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            //Let's center the view reset.
            mode=DOUBLETAP;
            //Log.d(DEBUG_TAG, "Received Double Tap Let's recenter");
            mPosX= (float) (getWidth()/2.0); mPosY= (float) (getHeight()/2.0); mScaleFactor=1;
            invalidate();
            return true;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        float scale;
        int action = ev.getActionMasked();
        //passing all touch event through scale detector
        mScaleDetector.onTouchEvent(ev);

        //use the Scroll detector to reset on double tap
        mScrollDetector.onTouchEvent(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                mLastTouchX = x;
                mLastTouchY = y;
                mActivePointerId = ev.getPointerId(0);
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                final float x = ev.getX(pointerIndex);
                final float y = ev.getY(pointerIndex);

                // Only move if the ScaleGestureDetector isn't processing a gesture.
                if (!mScaleDetector.isInProgress()) {
                    final float dx = x - mLastTouchX;
                    final float dy = y - mLastTouchY;
                    mPosX += dx;
                    mPosY += dy;
                    mode=DRAG;
                    invalidate();
                }

                mLastTouchX = x;
                mLastTouchY = y;

                break;
            }

            case MotionEvent.ACTION_UP: {
                mActivePointerId = INVALID_POINTER_ID;
                //mPosX=0;mPosX=0;
                break;
            }

            case MotionEvent.ACTION_CANCEL: {
                mActivePointerId = INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                        >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = ev.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mLastTouchX = ev.getX(newPointerIndex);
                    mLastTouchY = ev.getY(newPointerIndex);
                    mActivePointerId = ev.getPointerId(newPointerIndex);
                    //mPosX=0;mPosX=0;
                }
                break;
            }
        }

        RichPath richPath = richPathDrawable.getTouchedPath(ev);
        if (richPath != null) {
            RichPath.OnPathClickListener onPathClickListener = richPath.getOnPathClickListener();
            if (onPathClickListener != null) {
                onPathClickListener.onClick(richPath);
            }
            if (this.onPathClickListener != null) {
                this.onPathClickListener.onClick(richPath);
            }
        }
        return true;
    }
    @Override
    public void onDraw(Canvas canvas) {
         super.onDraw(canvas);
         //Log.d(TAG, "OnDraw Mode:"+ mode + " mPosX: " + mPosX+ " mPosY:" + mPosY);
         //Log.d(TAG, "Get Height Width:"+ getHeight()+ " "+getWidth());
         matrix=new Matrix();
         matrix.postTranslate(mPosX, mPosY);
         matrix.postScale(mScaleFactor, mScaleFactor, zoomFocusX, zoomFocusY);
         richPathDrawable.applyZoomPan(matrix, mode==DOUBLETAP);
    }

    public RichPathView(Context context) {
        this(context, null);
    }

    public RichPathView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RichPathView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        setupAttributes(attrs);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mScrollDetector = new GestureDetectorCompat(context, new MyScrollListener());
    }

    private void init() {
        //Disable hardware
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    private void setupAttributes(AttributeSet attrs) {

        // Obtain a typed array of attributes
        TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.RichPathView, 0, 0);
        // Extract custom attributes into member variables
        int resID = -1;
        try {
            resID = a.getResourceId(R.styleable.RichPathView_vector, -1);
        } finally {
            // TypedArray objects are shared and must be recycled.
            a.recycle();
        }

        if (resID != -1) {
            setVectorDrawable(resID);
        }

    }

    /**
     * Set a VectorDrawable resource ID.
     *
     * @param resId the resource ID for VectorDrawableCompat object.
     */
    public void setVectorDrawable(@DrawableRes int resId) {

        @SuppressWarnings("ResourceType")
        XmlResourceParser xpp = getContext().getResources().getXml(resId);
        vector = new Vector();

        try {
            XmlParser.parseVector(vector, xpp, getContext());
            richPathDrawable = new RichPathDrawable(vector, getScaleType());
            setImageDrawable(richPathDrawable);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        if (vector == null) return;

        int desiredWidth = (int) vector.getWidth();
        int desiredHeight = (int) vector.getHeight();

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);


        //Measure Width
        int width;
        if (widthMode == MeasureSpec.EXACTLY) {
            //Must be this size
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            width = Math.min(desiredWidth, widthSize);
        } else {
            //Be whatever you want
            width = desiredWidth;
        }

        //Measure Height
        int height;
        if (heightMode == MeasureSpec.EXACTLY) {
            //Must be this size
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            //Can't be bigger than...
            height = Math.min(desiredHeight, heightSize);
        } else {
            //Be whatever you want
            height = desiredHeight;
        }

        //MUST CALL THIS
        setMeasuredDimension(width, height);
    }


    @NonNull
    public RichPath[] findAllRichPaths() {
        return richPathDrawable == null ? new RichPath[0] : richPathDrawable.findAllRichPaths();
    }

    @Nullable
    public RichPath findRichPathByName(String name) {
        return richPathDrawable == null ? null : richPathDrawable.findRichPathByName(name);
    }

    /**
     * find the first {@link RichPath} or null if not found
     * <p>
     * This can be in handy if the vector consists of 1 path only
     *
     * @return the {@link RichPath} object found or null
     */
    @Nullable
    public RichPath findFirstRichPath() {
        return richPathDrawable == null ? null : richPathDrawable.findFirstRichPath();
    }

    @Nullable
    public RichPath findRichPathByIndex(@IntRange(from = 0) int index) {
        return richPathDrawable == null ? null : richPathDrawable.findRichPathByIndex(index);
    }

    public void addPath(String path) {
        if (richPathDrawable != null) {
            richPathDrawable.addPath(PathParser.createPathFromPathData(path));
        }
    }

    public void addPath(Path path) {
        if (richPathDrawable != null) {
            richPathDrawable.addPath(path);
        }
    }
    public void addTags(String []Pathnames, String []TagTexts) {
        if (richPathDrawable != null) {
            richPathDrawable.addTags(Pathnames,TagTexts);
        }
    }
    public void adjustTags(String []Pathnames,  float[] Xhints, float[] Yhints) {
        if (richPathDrawable != null) {
            richPathDrawable.adjustTags(Pathnames,Xhints,Yhints);
        }
    }

    public void setOnPathClickListener(RichPath.OnPathClickListener onPathClickListener) {
        this.onPathClickListener = onPathClickListener;
    }

}
