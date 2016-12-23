/*
 *  Copyright (c) 2015 Dennis Lang (LanDen Labs) landenlabs@gmail.com
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 *  associated documentation files (the "Software"), to deal in the Software without restriction, including
 *  without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the
 *  following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all copies or substantial
 *  portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 *  LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 *  NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 *  SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *  @author Dennis Lang  (Dec-2015)
 *  @see <a href="http://landenlabs.com">http://landenlabs.com</a>
 *
 */

package com.landenlabs.password;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.DragEvent;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;

import com.landenlabs.all_encrypnotes.R;
import com.landenlabs.all_encrypnotes.ui.UiUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Custom grid view to hold buttons to manage drawing a password pattern.
 * 
 * Children can be PasswordButton or any other derived TextView object.
 *
 * @see <a href="http://landenlabs.com">http://landenlabs.com</a>
 * @author Dennis Lang v2 9/1/2014
 *         <p>
 * @author ahmed v1 7/2/2014 <br>
 *         Original version: {@link https ://github.com/asghonim/simple_android_lock_pattern}
 * 
 *         Custom attributes drawOff - drawable for off state drawOn - drawable for on state
 *         pathWidth - path width pathAlpha - path alpha pathColor - path color (rotate when
 *         overlapping)
 */
public class PasswordGrid extends GridLayout {

    public interface OnPasswordListener {
        void onPasswordComplete(String s);

        void onPasswordChanged(String s);
    }

    private OnPasswordListener mListener;

    // -- Path management
    private final List<Point> m_points = new ArrayList<Point>();
    private final StringBuilder m_password = new StringBuilder();
    private final SparseIntArray m_pathMap = new SparseIntArray();
    private final HashMap<View, Integer> m_buttonCnt = new HashMap<View, Integer>();

    private View m_lastView = null;
    private long m_pressMillis = 0;
    private Paint m_paint;
    private Path m_path;

    // -- Custom attributes
    private int m_offId = R.drawable.pattern_off;
    private int m_onId = R.drawable.pattern_on;
    private int m_pathWidth = PATH_WIDTH;
    private int m_pathAlpha = PATH_ALPHA;
    private int m_pathColor = PATH_COLOR;
    private Bitmap m_onImage;

    // Custom attribute constants
    private static final int STYLE_ATTR = R.attr.passwordGridStyle;
    private static final int PATH_WIDTH = 20; 
    private static final int PATH_ALPHA = 128; // 50% opaque
    private static final int PATH_COLOR = 0xff0000; // Red

    /*
     * onTouchEvent only fires when you touch area not covered by a button, we are using
     * onInterceptTouchEvent which fires on any touch over grid.
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            UiUtil.hideSoftKeyboard(this);
            startDrag(null, new DragShadowBuilder(this) {
                @Override
                public void onDrawShadow(Canvas canvas) {
                }
            }, 0, 0);
            break;
        case MotionEvent.ACTION_UP:
            View childHit = findChildAt(event.getX(), event.getY());
            if (childHit != null) {
                addPoint(childHit, true);
            }
            break;
        }

        this.requestFocus();    // Take focus so any open soft keyboard disappears.
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onDragEvent(DragEvent event) {

        if (event.getAction() == DragEvent.ACTION_DRAG_LOCATION
            || event.getAction() == DragEvent.ACTION_DROP) {
            View childHit = findChildAt(event.getX(), event.getY());

            if (childHit != m_lastView && childHit != null) {
                addPoint(childHit, true);

            } else if (childHit == m_lastView) {
                if (SystemClock.uptimeMillis() - m_pressMillis > 2000) {
                    clear();
                    m_pressMillis = SystemClock.uptimeMillis();
                    return false;
                }
            }
        }

        if (event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
            if (mListener != null) {
                mListener.onPasswordComplete(m_password.toString());
            }
            m_lastView = null;
        }

        return true;
    }

    /**
     * Override draw to render connecting path between password cells.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (m_paint != null) {
            paintPath(canvas);
        }
        super.onDraw(canvas);
    }
    
    public PasswordGrid(Context context) {
        super(context);
        init(context, null);
    }

    public PasswordGrid(Context context, AttributeSet attrs) {
        super(context, attrs, STYLE_ATTR);
        init(context, attrs);
    }

    public PasswordGrid(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle == 0 ? STYLE_ATTR : defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {

        if (!isInEditMode()) {

            /*
             * Example how to extract generic attributes such as background
             * 
             * TypedArray stdAttrs = context.obtainStyledAttributes(attrs, new int[] {
             * android.R.attr.background }, STYLE_ATTR, R.style.PasswordGridStyle); // Fallback
             * style // android.R.style.Widget_GridView);
             * 
             * if (stdAttrs != null) { // stdAttrs has sorted responses, so index must access in
             * alpha order. Drawable bgDrawable = stdAttrs.getDrawable(0); stdAttrs.recycle(); }
             */

            // Extract custom attributes
            //
            // drawOff - drawable for off state
            // drawOn - drawable for on state
            // pathWidth - path width
            // pathAlpha - path alpha
            // pathColor - path color (rotate when overlapping)
            //
            TypedArray styledAttrs = context.obtainStyledAttributes(attrs,
                    R.styleable.passwordGrid, STYLE_ATTR, R.style.PasswordGridStyle);

            m_offId = styledAttrs.getResourceId(R.styleable.passwordGrid_drawOff,
                    R.drawable.pattern_off);
            m_onId = styledAttrs.getResourceId(R.styleable.passwordGrid_drawOn,
                    R.drawable.pattern_on);

            m_onImage = BitmapFactory.decodeResource(getResources(), m_onId);
            
            m_pathWidth = styledAttrs.getInt(R.styleable.passwordGrid_pathWidth, PATH_WIDTH);
            m_pathAlpha = styledAttrs.getInt(R.styleable.passwordGrid_pathAlpha, PATH_ALPHA);
            m_pathColor = styledAttrs.getColor(R.styleable.passwordGrid_pathColor, PATH_COLOR);

            styledAttrs.recycle();

            m_path = new Path();
            m_paint = new Paint();
            m_paint.setAntiAlias(true);
            m_paint.setStyle(Paint.Style.STROKE);
            m_paint.setStrokeCap(Paint.Cap.ROUND);
            m_paint.setStrokeJoin(Paint.Join.ROUND);
            m_paint.setShadowLayer(8, 0, 0, Color.BLACK);

            m_paint.setAlpha(m_pathAlpha);
            m_paint.setStrokeWidth(m_pathWidth);
            m_paint.setColor(m_pathColor);
        }
    }

    public String getPassword() {
        return m_password.toString();
    }

    public void setPassword(String newPwd, boolean fireListener) {
        if (!m_password.equals(newPwd)) {
            clear();
            for (char c : newPwd.toCharArray()) {
                View child = findChildFor("" + c);
                if (child != null) {
                    addPoint(child, fireListener);
                }
            }
        }
    }

     /**
     * Clear password and reset grid buttons to off state.
     */
    public void clear() {
        this.playSoundEffect(SoundEffectConstants.CLICK);

        // Reset pattern.
        m_password.delete(0, m_password.length());
        m_points.clear();
        m_buttonCnt.clear();
        m_lastView = null;
        for (int idx = 0; idx < getChildCount(); idx++) {
            View childView = getChildAt(idx);
            if (isPasswordView(childView)) {
                childView.setBackgroundResource(m_offId);
            }
        }
        invalidate();
    }

    public int size() {
        return m_points.size();
    }

    public void deletePoint(int idx) {
        if (m_points.size() == 0)
            return;

        if (idx == -1 || idx >= m_points.size()) {
            idx = m_points.size() -1;
        }

        Point pt = m_points.get(idx);
        View childHit = findChildAt(pt.x, pt.y);

        Integer hitCntObj = m_buttonCnt.get(childHit);
        if (hitCntObj == null)
            return;

        int hitCnt = hitCntObj.intValue() -1;
        m_buttonCnt.put(childHit, hitCnt);
        if (hitCnt > 0) {
            Bitmap rotImage = rotateImage(m_onImage, (hitCnt-1) * 30);
            Drawable rotDraw = new BitmapDrawable(getResources(), rotImage);

            if (Build.VERSION.SDK_INT < 16) {
                //noinspection deprecation,deprecation
                childHit.setBackgroundDrawable(new BitmapDrawable(rotImage));
            } else {
                childHit.setBackground(rotDraw);
            }
        } else {
            childHit.setBackgroundResource(m_offId);
        }


        m_points.remove(idx);
        m_password.delete(idx, idx+1);
        invalidate();

        if (mListener != null) {
            mListener.onPasswordChanged(m_password.toString());
        }
        childHit.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

        if (idx <= 0) {
            m_lastView = null;
        } else {
            pt = m_points.get(idx-1);
            m_lastView = findChildAt(pt.x, pt.y);
        }
    }

    /**
     * Add button child to sequence of password presses.
     * 
     * @param childHit
     */
    public void addPoint(View childHit, boolean fireListener) {

        Integer hitCntObj = m_buttonCnt.get(childHit);
        int hitCnt = (hitCntObj==null) ? 0 : hitCntObj.intValue();
        m_buttonCnt.put(childHit, hitCnt + 1);
        if (hitCnt > 0) {
            Bitmap rotImage = rotateImage(m_onImage, hitCnt * 30);
            Drawable rotDraw = new BitmapDrawable(getResources(), rotImage);

            if (Build.VERSION.SDK_INT < 16) {
                //noinspection deprecation,deprecation
                childHit.setBackgroundDrawable(new BitmapDrawable(rotImage));
            } else {
                childHit.setBackground(rotDraw);
            }
        } else {
            childHit.setBackgroundResource(m_onId);
        }
        
        m_pressMillis = SystemClock.uptimeMillis();
        m_lastView = childHit;
        Point cenPt = new Point((int) childHit.getX(), (int) childHit.getY());
        cenPt.offset(childHit.getWidth() / 2, childHit.getHeight() / 2);
        m_points.add(cenPt);
        m_password.append(getPasswordText(childHit));
        invalidate();

        if (mListener != null && fireListener) {
            mListener.onPasswordChanged(m_password.toString());
        }
        childHit.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
    }


    /**
     * Set Password listener callback.
     * 
     * @param listener
     */
    public void setListener(OnPasswordListener listener) {
        mListener = listener;
    }

    /**
     * Custom paint action to draw connecting path between layout buttons.
     * 
     * @param canvas
     */
    private void paintPath(Canvas canvas) {
        int prevCnt = 1;
        
        m_path.reset();
        m_paint.setStrokeWidth(m_pathWidth);
        m_paint.setColor(m_pathColor);

        // Variables used to handle overlapping paths.
        char prevPwd = '?';
        Point prevPnt = new Point();
        int cnt = 1;
        m_pathMap.clear();

        for (int idx = 0; idx != m_points.size(); idx++) {
            Point point = m_points.get(idx);

            // -- Beginning of overlapping path logic
            char pwd = m_password.charAt(idx);
            if (idx != 0) {
                // Count paths using (from,to) password values as key.
                int pos = Math.min(prevPwd, pwd) * 100 + Math.max(prevPwd, pwd);
                Integer value = m_pathMap.get(pos);
                cnt = 1 + ((value == null) ? 0 : value);
                m_pathMap.put(pos, cnt);
            }
            prevPwd = pwd;

            if (cnt != prevCnt) {
                // Path count change -
                // 1. Draw previous path with previous paint brush
                // 2. Create new path and paint
                // 3. Add current segment to path
                // 4. Shrink path width and rotate color
                if (!m_path.isEmpty()) {
                    canvas.drawPath(m_path, m_paint);
                    m_path = new Path();
                    m_paint = new Paint(m_paint);
                }

                m_path.moveTo(prevPnt.x, prevPnt.y);
                m_path.lineTo(point.x, point.y);

                int zeroCnt = cnt - 1;
                int pathWidth = Math.max(m_pathWidth / 2, m_pathWidth - zeroCnt * 4);
                m_paint.setStrokeWidth(pathWidth);
                int newColor = rotateColor(zeroCnt, m_pathColor);
                m_paint.setColor(newColor);
                prevCnt = cnt;
            }
            // -- end of overlapping path logic

            if (m_path.isEmpty())
                m_path.moveTo(point.x, point.y);
            else
                m_path.lineTo(point.x, point.y);

            prevPnt = point;
        }

        canvas.drawPath(m_path, m_paint); 
    }

    /**
     * Get password value of child object. Child must be a PasswordButton or an instanceOf TextView.
     * Result returned is first of: Tag (if instance of String and not empty) Text if not empty
     * Position of child in gridlayout.
     * 
     * @param childView
     * @return Result string extracted from child view object.
     */
    private String getPasswordText(View childView) {
        String result = null;
        boolean isPwdView = childView instanceof PasswordButton;

        if (!isPwdView || childView instanceof TextView) {
            result = (String) childView.getTag();
            if (TextUtils.isEmpty(result))
                result = (String) ((TextView) childView).getText();
            if (TextUtils.isEmpty(result)) {
                // result is child's position in grid.
                for (int idx = 0; idx != getChildCount(); idx++)
                    if (getChildAt(idx) == childView) {
                        result = String.valueOf(idx);
                        break;
                    }
            }
        }
        return result;
    }

    /**
     * Determine if child view object is part of password pattern.
     * 
     * @param childView
     * @return true if password child.
     */
    private boolean isPasswordView(View childView) {
        return !TextUtils.isEmpty(getPasswordText(childView));
    }

    /**
     * Find child which hits position.
     * 
     * @param xRP
     *            x Relative to parent
     * @param yRP
     *            y Relative to parent
     * @return
     */
    private View findChildAt(float xRP, float yRP) {
        Rect rectRP = new Rect();
        for (int idx = 0; idx < getChildCount(); idx++) {
            View childView = getChildAt(idx);

            childView.getHitRect(rectRP); // Get child rect relative to parent

            if (rectRP.contains((int) xRP, (int) yRP) && isPasswordView(childView))
                return childView;
        }

        return null;
    }

    private View findChildFor(String str) {
        for (int idx = 0; idx < getChildCount(); idx++) {
            View childView = getChildAt(idx);
            if (childView instanceof  TextView) {
                TextView textView = (TextView)childView;
                if (textView.getText().toString().equals(str))
                    return childView;
            }
        }

        return null;
    }

    /**
     * Helper to rotate Red/Green/Blue color values
     * 
     * @param cnt
     *            Rotation count, 0=no rotation
     * @param inColor
     *            Input color
     * @return Output color generate by swapping R/G/B
     */
    private static int rotateColor(int cnt, int inColor) {
        int[] rgb = new int[3];
        rgb[0] = Color.red(inColor);
        rgb[1] = Color.green(inColor);
        rgb[2] = Color.blue(inColor);

        return Color.argb(Color.alpha(inColor), rgb[cnt % 3], rgb[(cnt + 1) % 3],
                rgb[(cnt + 2) % 3]);
    }

    /**
     * Rotate image
     */
    private static Bitmap rotateImage(Bitmap src, float degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotBm = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
        int x = (rotBm.getWidth() - src.getWidth()) / 2;
        int y = (rotBm.getHeight() - src.getHeight()) / 2;
        return Bitmap.createBitmap(rotBm, x, y, src.getWidth(), src.getHeight());
    }
}
