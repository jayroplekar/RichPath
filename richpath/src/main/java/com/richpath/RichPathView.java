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
import static java.lang.Math.sqrt;

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
    int mode = NONE;

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
    private float PhysicsFactor=0.01F;
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    private class ScaleListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor = detector.getScaleFactor();
            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));
            invalidate();
            Log.d(DEBUG_TAG,"onScale: " + mScaleFactor +" FocusX: "+detector.getFocusX()+" FocusY: "+detector.getFocusY());
            return true;
        }
    }
    class MyScrollListener extends GestureDetector.SimpleOnGestureListener {
//        @Override
//        public boolean onDown(MotionEvent event) {
//            Log.d(DEBUG_TAG,"onDown: " + event.toString());
//            return true;
//        }
        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            Log.d(DEBUG_TAG, "onFling: Vx:  " + velocityX + " Vy: "+velocityY);
            mPosX= (float) 0.0;
            mPosY= (float) 0.0;
            if( abs(velocityX) >= SWIPE_VELOCITY_THRESHOLD) {
                mPosX = velocityX * PhysicsFactor;
            }
            if( abs(velocityY) >= SWIPE_VELOCITY_THRESHOLD) {
                mPosY = velocityY * PhysicsFactor;
            }
            if( abs(mPosX)> 5 || abs(mPosY) > 5)  invalidate();

            return true;
        }
//        @Override
//        public boolean onScroll(MotionEvent e1, MotionEvent e2,
//                               float dX, float dY) {
//            float diffY = e2.getY() - e1.getY();
//            float diffX = e2.getX() - e1.getX();
//            if (abs(diffX) >= SWIPE_THRESHOLD)
//            {
//                mPosX=diffX*PhysicsFactor;
//            }
//            else{
//                mPosX= (float) 0.0;
//            }
//            if (abs(diffY) >= SWIPE_THRESHOLD)
//            {
//                mPosY=diffY*PhysicsFactor;
//            }
//            else{
//                mPosY= (float) 0.0;
//            }
//            invalidate();
//            Log.d(DEBUG_TAG, "onScroll:  dX, dY" + diffX +":  "+ diffY);
//            return true;
//        }
    }

    @Override
    public void onDraw(Canvas canvas) {

//        canvas.save();
//        canvas.translate(mPosX, mPosY);
//
//        if (mScaleDetector.isInProgress()) {
//            canvas.scale(mScaleFactor, mScaleFactor, zoomFocusX, zoomFocusY);
//        }
//        else{
//            canvas.scale(mScaleFactor, mScaleFactor, zoomFocusX, zoomFocusY);
//        }
//        super.onDraw(canvas);
//        canvas.restore();


            //Let''s initialize to zero matrix
            matrix=savedMatrix;
            matrix.postTranslate(mPosX, mPosY);
            matrix.postScale(mScaleFactor, mScaleFactor, zoomFocusX, zoomFocusY);
            richPathDrawable.applyZoomPan(matrix);
        // let's reset now that we have consumed this.
            mPosX= (float) 0.0;
            mPosY= (float) 0.0;
            mScaleFactor= (float) 1.0;

        super.onDraw(canvas);

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
    public void addTags(String []Pathnames, String []TagTexts, float Xhints[], float Yhints[]) {
        if (richPathDrawable != null) {
            richPathDrawable.addTags(Pathnames,TagTexts,Xhints,Yhints);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float scale;
        int action = event.getActionMasked();
        mScaleDetector.onTouchEvent(event);
        mScrollDetector.onTouchEvent(event);
        //Log.d(TAG, "event:"+event);
        switch (action) {
            case MotionEvent.ACTION_DOWN: //first finger down only
                performClick();
                break;
        }
        RichPath richPath = richPathDrawable.getTouchedPath(event);
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

    public void setOnPathClickListener(RichPath.OnPathClickListener onPathClickListener) {
        this.onPathClickListener = onPathClickListener;
    }

}
