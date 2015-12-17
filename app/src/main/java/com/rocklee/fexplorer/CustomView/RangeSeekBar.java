package com.rocklee.fexplorer.CustomView;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import com.rocklee.fexplorer.R;

import java.math.BigDecimal;

/**
 * Created by admin on 2015/12/2.
 */
public class RangeSeekBar extends View {
    private static final String TAG = "RangeSeekBar";
    private static final int CLICK_ON_LOW = 1;      //click at the low slider
    private static final int CLICK_ON_HIGH = 2;     //click at the high slider
    private static final int CLICK_IN_LOW_AREA = 3;
    private static final int CLICK_IN_HIGH_AREA = 4;
    private static final int CLICK_OUT_AREA = 5;
    private static final int CLICK_IN_MIDDLE_AREA = 5;
    private static final int CLICK_INVAILD = 0;
    private static final int[] STATE_NORMAL = {};
    private static final int[] STATE_PRESSED = {
            android.R.attr.state_pressed, android.R.attr.state_window_focused,
    };
    private Drawable leftScrollBarBg;        //left scroll bar background
    private Drawable rightScrollBarBg;        //right scroll bar background
    private Drawable mThumbLow;         //low slider
    private Drawable mThumbHigh;        //high slider
    private Drawable mThumbLowLimit;         //low slider limit
    private Drawable mThumbHighLimit;        //high slider limit

    private int mScollBarWidth;     //scroll width
    private int mScollBarHeight;    //scroll high

    private int mThumbWidth;        //slider original width
    private int mThumbHeight;       //slider original height

    private int thumbWidth;         //slider zoom width
    private int thumbHeight;        //slider zoom height

    private double mOffsetLow = 0;     //low slider center coordinate
    private double mOffsetHigh = 0;    //high slider center coordinate
    private double mPreOffsetLow = 0;
    private double mPreOffsetHigh = 0;
    private float preXPosition = 0;
    private double lowHighDistance = 0;
    private int mDistance = 0;      //total distance

    private int mFlag = CLICK_INVAILD;
    private OnSeekBarChangeListener mBarChangeListener;

    private double minDistance;//min scroll range percentage,whole scroll percentage is 100
    private double maxDistance;//max scroll range percentage,whole scroll percentage is 100

    private long startTime;
    private long durationTime;

    private boolean isEdit = false;
    private boolean isOutOfRange = false;

    private int mLastMotionX;
    private boolean isMoved;
    private boolean isReleased;
    private int mCounter;
    private Runnable mLongPressLeftRunnable;
    private Runnable mLongPressRightRunnable;
    private int whichSliderLongPressed = 0;//1 left slider, 2 right slider

    public RangeSeekBar(Context context) {
        this(context, null);
    }

    public RangeSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RangeSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Resources resources = getResources();
        leftScrollBarBg = resources.getDrawable(R.drawable.progress_bg);//normal_bg
        rightScrollBarBg = resources.getDrawable(R.drawable.progress_bg);//progress_bg
        mThumbLow = resources.getDrawable(R.drawable.trim_left);
        mThumbHigh = resources.getDrawable(R.drawable.trim_right);
        mThumbLowLimit = resources.getDrawable(R.drawable.trim_left_limit);
        mThumbHighLimit = resources.getDrawable(R.drawable.trim_right_limit);

        mThumbLow.setState(STATE_NORMAL);
        mThumbHigh.setState(STATE_NORMAL);
        mThumbLowLimit.setState(STATE_NORMAL);
        mThumbHighLimit.setState(STATE_NORMAL);

        mScollBarWidth = leftScrollBarBg.getIntrinsicWidth();
        mScollBarHeight = leftScrollBarBg.getIntrinsicHeight();

        mThumbWidth = mThumbLow.getIntrinsicWidth();
        mThumbHeight = mThumbLow.getIntrinsicHeight();
        thumbWidth = mThumbLow.getIntrinsicWidth();
        thumbHeight = mThumbLow.getIntrinsicHeight();
        mLongPressLeftRunnable = new Runnable() {
            @Override
            public void run() {
                mCounter--;
                if (mCounter > 0 || isReleased || isMoved)
                    return;
                whichSliderLongPressed = 1;
                performLongClick();
            }
        };
        mLongPressRightRunnable = new Runnable() {
            @Override
            public void run() {
                mCounter--;
                if (mCounter > 0 || isReleased || isMoved)
                    return;
                whichSliderLongPressed = 2;
                performLongClick();
            }
        };
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = measureWidth(widthMeasureSpec);
        int height = measureHeight(heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    private int measureWidth(int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        //wrap_content
        if (specMode == MeasureSpec.AT_MOST) {
        }
        //match_parent
        else if (specMode == MeasureSpec.EXACTLY) {
        }
        return specSize;
    }

    private int measureHeight(int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        //wrap_content
        if (specMode == MeasureSpec.AT_MOST) {
        }
        ////match_parent
        else if (specMode == MeasureSpec.EXACTLY) {
        }
        return specSize;
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        Log.d(TAG, "onLayout");
        super.onLayout(changed, l, t, r, b);
        int vwidth = getWidth();
        int vheight = getHeight();
        mThumbHeight = vheight;
        mThumbWidth = (thumbWidth * vheight)/thumbHeight;
//        Log.d(TAG, " vwidth:" + vwidth + " vheight:" + vheight);
        mScollBarWidth = vwidth;
        mScollBarHeight = vheight;
        mDistance = vwidth - mThumbWidth;

        minDistance = formatDouble(mDistance * (10 * 1.0/100));
        maxDistance = formatDouble(mDistance * (80 * 1.0/100));
        mOffsetLow = formatDouble(mDistance * (0 * 1.0/100)) + mThumbWidth / 2;
        mOffsetHigh = formatDouble(mDistance * (59 * 1.0/100)) + mThumbWidth / 2;
//        Log.d(TAG, "mDistance:" + mDistance +
//                " minDistance:" + minDistance + " maxDistance" + maxDistance +
//                " mOffsetLow:" + mOffsetLow + " mOffsetHigh:" + mOffsetHigh);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //Log.d(TAG, "onDraw" + " mOffsetHigh" + mOffsetHigh);
        Paint text_Paint = new Paint();
        text_Paint.setTextAlign(Paint.Align.CENTER);
        text_Paint.setColor(Color.RED);
        text_Paint.setTextSize(20);

        Paint topFrame = new Paint();
        Paint bottomFrame = new Paint();
        topFrame.setStrokeWidth(14.0f);
        bottomFrame.setStrokeWidth(14.0f);

        Paint durationText = new Paint();
        durationText.setTextAlign(Paint.Align.CENTER);
        durationText.setColor(Color.YELLOW);
        durationText.setTextSize(20);

        //lowSlider
        mThumbLow.setBounds((int) (mOffsetLow - mThumbWidth / 2), 0, (int) (mOffsetLow + mThumbWidth / 2), mThumbHeight);

        mThumbLowLimit.setBounds((int) (mOffsetLow - mThumbWidth / 2), 0, (int) (mOffsetLow + mThumbWidth / 2), mThumbHeight);

        //leftScroll
        leftScrollBarBg.setBounds(0, 0, (int) mOffsetLow - mThumbWidth / 2, mScollBarHeight);
        leftScrollBarBg.draw(canvas);

        //highSlider
        mThumbHigh.setBounds((int) (mOffsetHigh - mThumbWidth / 2), 0, (int) (mOffsetHigh + mThumbWidth / 2), mThumbHeight);

        mThumbHighLimit.setBounds((int) (mOffsetHigh - mThumbWidth / 2), 0, (int) (mOffsetHigh + mThumbWidth / 2), mThumbHeight);

        if (isOutOfRange) {
            mThumbLowLimit.draw(canvas);
            mThumbHighLimit.draw(canvas);
            topFrame.setARGB(0xff, 100, 100, 100);
            bottomFrame.setARGB(0xff, 100, 100, 100);
        } else {
            mThumbLow.draw(canvas);
            mThumbHigh.draw(canvas);
            topFrame.setARGB(0xff, 255, 109, 109);
            bottomFrame.setARGB(0xff, 255, 109, 109);
        }

        //rightScoll
        rightScrollBarBg.setBounds((int)mOffsetHigh + mThumbWidth / 2, 0, mScollBarWidth, mScollBarHeight);
        rightScrollBarBg.draw(canvas);

        double progressLow = formatDouble((mOffsetLow - mThumbWidth / 2) * 100 / mDistance);
        double progressHigh = formatDouble((mOffsetHigh - mThumbWidth / 2) * 100 / mDistance);
//        Log.d(TAG, "onDraw-->mOffsetLow: " + mOffsetLow + "  mOffsetHigh: " + mOffsetHigh   + "  progressLow: " + progressLow + "  progressHigh: " + progressHigh);
//        canvas.drawText((int) progressLow + "", (int) mOffsetLow - 2 - 2, 15, text_Paint);
//        canvas.drawText((int) progressHigh + "", (int)mOffsetHigh - 2, 15, text_Paint);
        int duration = (int)((durationTime * 1.0) * (progressHigh - progressLow)/100)/1000000;
        canvas.drawText("00:" + String.format("%02d",duration), (int)mOffsetLow + (int)(mOffsetHigh - mOffsetLow)/2, 24, durationText);
        canvas.drawLine((int) mOffsetLow + mThumbWidth / 2, 0, (int) mOffsetHigh - mThumbWidth / 2, 0, topFrame);
        canvas.drawLine((int) mOffsetLow + mThumbWidth / 2, mScollBarHeight, (int)mOffsetHigh - mThumbWidth / 2, mScollBarHeight, bottomFrame);

        if (mBarChangeListener != null) {
            if (!isEdit) {
                mBarChangeListener.onProgressChanged(this, progressLow, progressHigh);
            }

        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        //press down
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            if (mBarChangeListener != null) {
                mBarChangeListener.onProgressBefore();
                isEdit = false;
            }
            mFlag = getAreaFlag(e);
//            Log.d(TAG, "e.getX: " + e.getX() + "mFlag: " + mFlag);
//            Log.d("ACTION_DOWN", "------------------");
            if (mFlag == CLICK_ON_LOW) {
                if ((int)mOffsetLow == mThumbWidth/2) {
                    mLastMotionX = (int)e.getX();
                    mCounter++;
                    isReleased = false;
                    isMoved = false;
                    postDelayed(mLongPressLeftRunnable, 1000);
                }
                mThumbLow.setState(STATE_PRESSED);
                mThumbLowLimit.setState(STATE_PRESSED);
            } else if (mFlag == CLICK_ON_HIGH) {
                if ((int)mOffsetHigh == mThumbWidth / 2 + mDistance) {
                    mLastMotionX = (int)e.getX();
                    mCounter++;
                    isReleased = false;
                    isMoved = false;
                    postDelayed(mLongPressRightRunnable, 1000);
                }
                mThumbHigh.setState(STATE_PRESSED);
                mThumbHighLimit.setState(STATE_PRESSED);
            } else if (mFlag == CLICK_IN_LOW_AREA) {
//                mThumbLow.setState(STATE_PRESSED);
//                if (e.getX() < 0 || e.getX() <= mThumbWidth/2) {
//                    mOffsetLow = mThumbWidth/2;
//                } else if (e.getX() > mScollBarWidth - mThumbWidth/2) {
////                    mOffsetLow = mDistance - mDuration;
//                    mOffsetLow = mThumbWidth/2 + mDistance;
//                } else {
//                    mOffsetLow = formatDouble(e.getX());
////                    if (mOffsetHigh<= mOffsetLow) {
////                        mOffsetHigh = (mOffsetLow + mDuration <= mDistance) ? (mOffsetLow + mDuration)
////                                : mDistance;
////                        mOffsetLow = mOffsetHigh - mDuration;
////                    }
//                }
            } else if (mFlag == CLICK_IN_HIGH_AREA) {
//                mThumbHigh.setState(STATE_PRESSED);
////                if (e.getX() < mDuration) {
////                    mOffsetHigh = mDuration;
////                    mOffsetLow = mOffsetHigh - mDuration;
////                } else if (e.getX() >= mScollBarWidth - mThumbWidth/2) {
////                    mOffsetHigh = mDistance + mThumbWidth/2;
//                if(e.getX() >= mScollBarWidth - mThumbWidth/2) {
//                    mOffsetHigh = mDistance + mThumbWidth/2;
//                } else {
//                    mOffsetHigh = formatDouble(e.getX());
////                    if (mOffsetHigh <= mOffsetLow) {
////                        mOffsetLow = (mOffsetHigh - mDuration >= 0) ? (mOffsetHigh - mDuration) : 0;
////                        mOffsetHigh = mOffsetLow + mDuration;
////                    }
//                }
            } else if (mFlag == CLICK_IN_MIDDLE_AREA) {
                mPreOffsetLow = mOffsetLow;
                mPreOffsetHigh = mOffsetHigh;
                preXPosition = e.getX();
                lowHighDistance = mPreOffsetHigh - mPreOffsetLow;
                Log.d(TAG, "mPreOffsetLow: " + mPreOffsetLow + " mPreOffsetHigh:" + mPreOffsetHigh + " preXPosition:" + preXPosition);
            }
            //set progress bar
            refresh();

            //move
        } else if (e.getAction() == MotionEvent.ACTION_MOVE) {
//            Log.d("ACTION_MOVE", "------------------");
//            Log.d(TAG, " e.getX():" + e.getX() + " mOffsetLow:" + mOffsetLow);
            if (Math.abs(mLastMotionX - (int)e.getX()) > mThumbWidth / 2) {
                isMoved = true;
            }
            if (mFlag == CLICK_ON_LOW) {
                if (e.getX() > mOffsetHigh - minDistance){
                    isOutOfRange = true;
                    mOffsetLow = mOffsetHigh - minDistance;
                }
                else {
                    //when high slider higher than maxDistance
                    if (mOffsetHigh > maxDistance) {
                        if ((mOffsetHigh - e.getX()) > maxDistance) {
                            isOutOfRange = true;
                            mOffsetLow = mOffsetHigh - maxDistance;
                        } else {
                            isOutOfRange = false;
                            mOffsetLow = formatDouble(e.getX());
                        }
                    } else if (mOffsetHigh <= maxDistance) {
                        if (e.getX() > mThumbWidth/2) {
                            isOutOfRange = false;
                            mOffsetLow = formatDouble(e.getX());
                        }
                        else {
                            isOutOfRange = false;
                            mOffsetLow = mThumbWidth / 2;
                        }
                    }
                }
            } else if (mFlag == CLICK_ON_HIGH) {
//                Log.d(TAG, " e.getX():" + e.getX() + " mOffsetLow:" + mOffsetHigh);
                if (e.getX() < mOffsetLow + minDistance) {
                    isOutOfRange = true;
                    mOffsetHigh = mOffsetLow + minDistance;
                }
                else {
                    if (mOffsetLow < mDistance - maxDistance ) {
                        if (e.getX() - mOffsetLow > maxDistance) {
                            isOutOfRange = true;
                            mOffsetHigh = mOffsetLow + maxDistance;
                        } else {
                            isOutOfRange = false;
                            mOffsetHigh = formatDouble(e.getX());
                        }
                    } else if (mOffsetLow >= mDistance - maxDistance) {
                        if (e.getX() <= mScollBarWidth - mThumbWidth/2) {
                            isOutOfRange = false;
                            mOffsetHigh = formatDouble(e.getX());
                        }
                        else {
                            isOutOfRange = false;
                            mOffsetHigh = mThumbWidth / 2 + mDistance;
                        }
                    }
                }
            } else if (mFlag == CLICK_IN_MIDDLE_AREA) {
                if (mOffsetLow > mThumbWidth/2 || mOffsetHigh < mScollBarWidth - mThumbWidth/2) {
                    Log.d(TAG, " e.getX():" + e.getX() + " mOffsetLow:" + mOffsetLow + " mOffsetHigh:" + mOffsetHigh);
                    if (e.getX() < preXPosition) {
                        mOffsetLow = mPreOffsetLow - (preXPosition - e.getX());
                        mOffsetHigh = mPreOffsetHigh - (preXPosition - e.getX());
                        if (mOffsetLow <= mThumbWidth/2) {
                            mOffsetLow = mThumbWidth/2;
                            mOffsetHigh = mOffsetLow + lowHighDistance;
                        }
                    } else if (e.getX() > preXPosition) {
                        mOffsetLow = mPreOffsetLow + (e.getX() - preXPosition);
                        mOffsetHigh = mPreOffsetHigh + (e.getX() - preXPosition);
                        if (mOffsetHigh >= mScollBarWidth - mThumbWidth/2) {
                            mOffsetHigh = mScollBarWidth - mThumbWidth/2;
                            mOffsetLow = mOffsetHigh - lowHighDistance;
                        }
                    }

                }
            }
            //set progress bar
            refresh();
            //press up
        } else if (e.getAction() == MotionEvent.ACTION_UP) {
//            Log.d("ACTION_UP", "------------------");

            mThumbLow.setState(STATE_NORMAL);
            mThumbHigh.setState(STATE_NORMAL);
            mThumbLowLimit.setState(STATE_NORMAL);
            mThumbHighLimit.setState(STATE_NORMAL);
            if (mFlag == CLICK_ON_LOW || mFlag == CLICK_ON_HIGH) {
                isReleased = true;
            }

            if (mBarChangeListener != null) {
                mBarChangeListener.onProgressAfter();
            }
        }
        return true;
    }

    public int getAreaFlag(MotionEvent e) {

        int top = 0;
        int bottom = mThumbHeight;
        if (e.getY() >= top && e.getY() <= bottom && e.getX() >= (mOffsetLow - mThumbWidth / 2) && e.getX() <= mOffsetLow + mThumbWidth / 2) {
            return CLICK_ON_LOW;
        } else if (e.getY() >= top && e.getY() <= bottom && e.getX() >= (mOffsetHigh - mThumbWidth / 2) && e.getX() <= (mOffsetHigh + mThumbWidth / 2)) {
            return CLICK_ON_HIGH;
        } else if (e.getY() >= top && e.getY() <= bottom
                && e.getX() > (mOffsetLow + mThumbWidth / 2)
                && e.getX() < (mOffsetHigh - mThumbWidth / 2)) {
            return CLICK_IN_MIDDLE_AREA;
//        } else if (e.getY() >= top
//                && e.getY() <= bottom
//                && ((e.getX() >= 0 && e.getX() < (mOffsetLow - mThumbWidth / 2)) || ((e.getX() > (mOffsetLow + mThumbWidth / 2))
//                && e.getX() <= (mOffsetHigh + mOffsetLow) / 2))) {
//            return CLICK_IN_LOW_AREA;
//        } else if (e.getY() >= top
//                && e.getY() <= bottom
//                && (((e.getX() > (mOffsetHigh + mOffsetLow) / 2) && e.getX() < (mOffsetHigh - mThumbWidth / 2)) || (e
//                .getX() > (mOffsetHigh + mThumbWidth/2) && e.getX() <= mScollBarWidth))) {
//            return CLICK_IN_HIGH_AREA;
        } else if (!(e.getX() >= 0 && e.getX() <= mScollBarWidth && e.getY() >= top && e.getY() <= bottom)) {
            return CLICK_OUT_AREA;
        } else {
            return CLICK_INVAILD;
        }
    }

    //update slider
    private void refresh() {
        invalidate();
    }

    //set low slider position
    public void setProgressLow(double  progressLow) {
        mOffsetLow = formatDouble(progressLow / 100 * (mDistance ))+ mThumbWidth / 2;
        isEdit = true;
        refresh();
    }

    //set high slider position
    public void setProgressHigh(double  progressHigh) {
        mOffsetHigh = formatDouble(progressHigh / 100 * (mDistance)) + mThumbWidth / 2;
        isEdit = true;
        refresh();
    }

    public void setOnSeekBarChangeListener(OnSeekBarChangeListener mListener) {
        this.mBarChangeListener = mListener;
    }

    public interface OnSeekBarChangeListener {
        void onProgressBefore();
        void onProgressChanged(RangeSeekBar seekBar, double progressLow, double progressHigh);
        void onProgressAfter();
    }

/*    private int formatInt(double value) {
        BigDecimal bd = new BigDecimal(value);
        BigDecimal bd1 = bd.setScale(0, BigDecimal.ROUND_HALF_UP);
        return bd1.intValue();
    }*/

    public static double formatDouble(double pDouble) {
//        Log.d(TAG, "pDouble" + pDouble);
        BigDecimal bd = new BigDecimal(pDouble);
        BigDecimal bd1 = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        pDouble = bd1.doubleValue();
        return pDouble;
    }

    public synchronized void setStartAndDuration(long start, long duration) {
        startTime = start;
        durationTime = duration;
    }

    public synchronized void setMinMaxRange(double min, double max) {
        minDistance = formatDouble(mDistance * (min/100));
        maxDistance = formatDouble(mDistance * (max/100));
        isEdit = true;
        refresh();
    }

    public synchronized double[] getLowHighCursor() {
        double[] cursor = {0.0, 0.0};
        cursor[0] = formatDouble((mOffsetLow - mThumbWidth / 2) * 100 / mDistance);
        cursor[1] = formatDouble((mOffsetHigh - mThumbWidth / 2) * 100 / mDistance);
        return cursor;
    }

    public int getWhichSliderLongPressed() {
        return whichSliderLongPressed;
    }
}
