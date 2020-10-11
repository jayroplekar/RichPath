package com.richpath;

import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Path;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.FloatMath;

import android.view.View;
import android.widget.ImageView;

import com.richpath.model.Vector;
import com.richpath.pathparser.PathParser;
import com.richpath.util.XmlParser;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import static java.lang.Math.sqrt;

/**
 * Created by tarek on 6/29/17.
 */

public class RichPathView extends androidx.appcompat.widget.AppCompatImageView {

    private Vector vector;
    private RichPathDrawable richPathDrawable;
    private RichPath.OnPathClickListener onPathClickListener;


    private ScaleGestureDetector mScaleGestureDetector;private float mScaleFactor = 1.0f;

    static final String TAG="RichPathView  zoom+pan";

    // These matrices will be used to move and zoom image
    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();

    // We can be in one of these 3 states
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;

    // Remember some things for zooming
    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;

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
//
//    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {@Override
//    public boolean onScale(ScaleGestureDetector scaleGestureDetector){
//        mScaleFactor *= scaleGestureDetector.getScaleFactor();
//        mScaleFactor = Math.max(0.1f,Math.min(mScaleFactor, 10.0f));
//        mImageView.setScaleX(mScaleFactor);
//        mImageView.setScaleY(mScaleFactor);
//        return true;    }
//    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float scale;
        int action = event.getAction();
        switch (action) {

            case MotionEvent.ACTION_DOWN: //first finger down only
                savedMatrix.set(matrix);
                start.set(event.getX(), event.getY());
                Log.d(TAG, "mode=DRAG");
                mode = DRAG;
                break;
            case MotionEvent.ACTION_UP: //first finger lifted
                if (mode == DRAG){
                    performClick();
                    break;
                }

            case MotionEvent.ACTION_POINTER_UP: //second finger lifted
                mode = NONE;
                Log.d(TAG, "mode=NONE" );
                break;
            case MotionEvent.ACTION_POINTER_DOWN: //second finger down
                oldDist = spacing(event); // calculates the distance between two points where user touched.
                Log.d(TAG, "oldDist=" + oldDist);
                // minimal distance between both the fingers
                if (oldDist > 5f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event); // sets the mid-point of the straight line between two points where user touched.
                    mode = ZOOM;
                    Log.d(TAG, "mode=ZOOM" );
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG)
                { //movement of first finger
                    matrix.set(savedMatrix);
                    if (this.getLeft() >= -392)
                    {
                        matrix.postTranslate(event.getX() - start.x, event.getY() - start.y);
                    }
                }
                else if (mode == ZOOM) { //pinch zooming
                    float newDist = spacing(event);
                    Log.d(TAG, "newDist=" + newDist);
                    if (newDist > 5f) {
                        matrix.set(savedMatrix);
                        scale = newDist/oldDist; //thinking I need to play around with this value to limit it**
                        matrix.postScale(scale, scale, mid.x, mid.y);
                    }
                }
                break;
        }

        this.setImageMatrix(matrix);
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

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }
}
