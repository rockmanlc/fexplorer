package com.rocklee.fexplorer.CustomView;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
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
    private static final int CLICK_INVAILD = 0;
    private static final int[] STATE_NORMAL = {};
    private static final int[] STATE_PRESSED = {
            android.R.attr.state_pressed, android.R.attr.state_window_focused,
    };
    private Drawable hasScrollBarBg;        //滑动条滑动后背景图
    private Drawable notScrollBarBg;        //滑动条未滑动背景图
    private Drawable mThumbLow;         //前滑块
    private Drawable mThumbHigh;        //后滑块

    private int mScollBarWidth;     //控件宽度=滑动条宽度+滑动块宽度
    private int mScollBarHeight;    //滑动条高度

    private int mThumbWidth;        //滑动块宽度
    private int mThumbHeight;       //滑动块高度

    private int thumbWidth;        //滑动块宽度
    private int thumbHeight;       //滑动块高度

    private double mOffsetLow = 0;     //前滑块中心坐标
    private double mOffsetHigh = 0;    //后滑块中心坐标
    private int mDistance = 0;      //总刻度是固定距离 两边各去掉半个滑块距离

    private int mThumbMarginTop = 0;   //滑动块顶部距离上边框距离，也就是距离字体顶部的距离

    private int mFlag = CLICK_INVAILD;
    private OnSeekBarChangeListener mBarChangeListener;

    private double defaultScreenLow = 0;    //默认前滑块位置百分比
    private double defaultScreenHigh = 100;  //默认后滑块位置百分比

    private boolean isEdit = false;     //输入框是否正在输入

    public RangeSeekBar(Context context) {
        this(context, null);
    }

    public RangeSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RangeSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
//        this.setBackgroundColor(Color.BLACK);

        Resources resources = getResources();
        notScrollBarBg = resources.getDrawable(R.drawable.normal_bg);//normal_bg
        hasScrollBarBg = resources.getDrawable(R.drawable.progress_bg);//progress_bg
        mThumbLow = resources.getDrawable(R.drawable.trim_left);
        mThumbHigh = resources.getDrawable(R.drawable.trim_right);

        mThumbLow.setState(STATE_NORMAL);
        mThumbHigh.setState(STATE_NORMAL);

        mScollBarWidth = notScrollBarBg.getIntrinsicWidth();
        mScollBarHeight = notScrollBarBg.getIntrinsicHeight();

        mThumbWidth = mThumbLow.getIntrinsicWidth();
        mThumbHeight = mThumbLow.getIntrinsicHeight();
        thumbWidth = mThumbLow.getIntrinsicWidth();
        thumbHeight = mThumbLow.getIntrinsicHeight();
        Log.d(TAG, "mThumbWidth:" + mThumbWidth + " mThumbHeight:" + mThumbHeight);
    }

    //默认执行，计算view的宽高,在onDraw()之前
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = measureWidth(widthMeasureSpec);
        int height = measureHeight(heightMeasureSpec);
        Log.d(TAG, "width:" + width + " height:" + height);
//        int width = widthMeasureSpec;
//        int height = heightMeasureSpec;
        mScollBarWidth = width;
        mScollBarHeight = height;
        mOffsetHigh = width - mThumbWidth / 2;
        mOffsetLow = mThumbWidth / 2;
        mDistance = width - mThumbWidth;

        mOffsetLow = formatDouble(defaultScreenLow / 100 * (mDistance )) + mThumbWidth / 2;
        mOffsetHigh = formatDouble(defaultScreenHigh / 100 * (mDistance)) + mThumbWidth / 2;
        setMeasuredDimension(width, mThumbHeight + mThumbMarginTop+2);
    }


    private int measureWidth(int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        Log.d(TAG, "specMode:" + specMode + " specSize:" + specSize);
        //wrap_content
        if (specMode == MeasureSpec.AT_MOST) {
        }
        //fill_parent或者精确值
        else if (specMode == MeasureSpec.EXACTLY) {
        }

        return specSize;
    }

    private int measureHeight(int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        //int defaultHeight = 100;
        //wrap_content
        if (specMode == MeasureSpec.AT_MOST) {
        }
        //fill_parent或者精确值
        else if (specMode == MeasureSpec.EXACTLY) {
            //defaultHeight = specSize;
        }

        //return defaultHeight;
        return specSize;
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        int vwidth = getWidth();
        int vheight = getHeight();
        mThumbHeight = vheight;
        mThumbWidth = (thumbWidth * vheight)/thumbHeight;
        Log.d(TAG, " vwidth:" + vwidth + " vheight:" + vheight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.d(TAG, "onDraw");
        Paint text_Paint = new Paint();
        text_Paint.setTextAlign(Paint.Align.CENTER);
        text_Paint.setColor(Color.RED);
        text_Paint.setTextSize(20);

        int aaa = 0;//mThumbMarginTop + mThumbHeight / 2 - mScollBarHeight / 2;
        int bbb = aaa + mScollBarHeight;

        //白色，不会动
        notScrollBarBg.setBounds(mThumbWidth / 2, aaa, mScollBarWidth - mThumbWidth / 2, bbb);
        notScrollBarBg.draw(canvas);

        //蓝色，中间部分会动
        hasScrollBarBg.setBounds((int)mOffsetLow, aaa, (int)mOffsetHigh, bbb);
        hasScrollBarBg.draw(canvas);

        //前滑块
        mThumbLow.setBounds((int)(mOffsetLow - mThumbWidth / 2), mThumbMarginTop, (int)(mOffsetLow + mThumbWidth / 2), mThumbHeight + mThumbMarginTop);
        mThumbLow.draw(canvas);

        //后滑块
        mThumbHigh.setBounds((int)(mOffsetHigh - mThumbWidth / 2), mThumbMarginTop, (int)(mOffsetHigh + mThumbWidth / 2), mThumbHeight + mThumbMarginTop);
        mThumbHigh.draw(canvas);

//        double progressLow = formatDouble((mOffsetLow - mThumbWidth / 2) * 100 / mDistance);
//        double progressHigh = formatDouble((mOffsetHigh - mThumbWidth / 2) * 100 / mDistance);
        Log.d(TAG, "123");
        double progressLow = formatDouble((mOffsetLow) * 100 / mDistance);
        Log.d(TAG, "234");
        double progressHigh = formatDouble((mOffsetHigh) * 100 / mDistance);
        Log.d(TAG, "onDraw-->mOffsetLow: " + mOffsetLow + "  mOffsetHigh: " + mOffsetHigh   + "  progressLow: " + progressLow + "  progressHigh: " + progressHigh);
        canvas.drawText((int) progressLow + "", (int)mOffsetLow - 2 - 2, 15, text_Paint);
        canvas.drawText((int) progressHigh + "", (int)mOffsetHigh - 2, 15, text_Paint);

        if (mBarChangeListener != null) {
            if (!isEdit) {
                mBarChangeListener.onProgressChanged(this, progressLow, progressHigh);
            }

        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        //按下
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            if (mBarChangeListener != null) {
                mBarChangeListener.onProgressBefore();
                isEdit = false;
            }
            mFlag = getAreaFlag(e);
//            Log.d(TAG, "e.getX: " + e.getX() + "mFlag: " + mFlag);
//            Log.d("ACTION_DOWN", "------------------");
            if (mFlag == CLICK_ON_LOW) {
                mThumbLow.setState(STATE_PRESSED);
            } else if (mFlag == CLICK_ON_HIGH) {
                mThumbHigh.setState(STATE_PRESSED);
            } else if (mFlag == CLICK_IN_LOW_AREA) {
                mThumbLow.setState(STATE_PRESSED);
                //如果点击0-mThumbWidth/2坐标
                if (e.getX() < 0 || e.getX() <= mThumbWidth/2) {
                    mOffsetLow = mThumbWidth/2;
                } else if (e.getX() > mScollBarWidth - mThumbWidth/2) {
//                    mOffsetLow = mDistance - mDuration;
                    mOffsetLow = mThumbWidth/2 + mDistance;
                } else {
                    mOffsetLow = formatDouble(e.getX());
//                    if (mOffsetHigh<= mOffsetLow) {
//                        mOffsetHigh = (mOffsetLow + mDuration <= mDistance) ? (mOffsetLow + mDuration)
//                                : mDistance;
//                        mOffsetLow = mOffsetHigh - mDuration;
//                    }
                }
            } else if (mFlag == CLICK_IN_HIGH_AREA) {
                mThumbHigh.setState(STATE_PRESSED);
//                if (e.getX() < mDuration) {
//                    mOffsetHigh = mDuration;
//                    mOffsetLow = mOffsetHigh - mDuration;
//                } else if (e.getX() >= mScollBarWidth - mThumbWidth/2) {
//                    mOffsetHigh = mDistance + mThumbWidth/2;
                if(e.getX() >= mScollBarWidth - mThumbWidth/2) {
                    mOffsetHigh = mDistance + mThumbWidth/2;
                } else {
                    mOffsetHigh = formatDouble(e.getX());
//                    if (mOffsetHigh <= mOffsetLow) {
//                        mOffsetLow = (mOffsetHigh - mDuration >= 0) ? (mOffsetHigh - mDuration) : 0;
//                        mOffsetHigh = mOffsetLow + mDuration;
//                    }
                }
            }
            //设置进度条
            refresh();

            //移动move
        } else if (e.getAction() == MotionEvent.ACTION_MOVE) {
//            Log.d("ACTION_MOVE", "------------------");
            if (mFlag == CLICK_ON_LOW) {
                if (e.getX() < 0 || e.getX() <= mThumbWidth/2) {
                    mOffsetLow = mThumbWidth/2;
                } else if (e.getX() >= mScollBarWidth - mThumbWidth/2) {
                    mOffsetLow = mThumbWidth/2 + mDistance;
                    mOffsetHigh = mOffsetLow;
                } else {
                    mOffsetLow = formatDouble(e.getX());
                    if (mOffsetHigh - mOffsetLow <= 0) {
                        mOffsetHigh = (mOffsetLow <= mDistance+mThumbWidth/2) ? (mOffsetLow) : (mDistance+mThumbWidth/2);
                    }
                }
            } else if (mFlag == CLICK_ON_HIGH) {
                if (e.getX() <  mThumbWidth/2) {
                    mOffsetHigh = mThumbWidth/2;
                    mOffsetLow = mThumbWidth/2;
                } else if (e.getX() > mScollBarWidth - mThumbWidth/2) {
                    mOffsetHigh = mThumbWidth/2 + mDistance;
                } else {
                    mOffsetHigh = formatDouble(e.getX());
                    if (mOffsetHigh - mOffsetLow <= 0) {
                        mOffsetLow = (mOffsetHigh >= mThumbWidth/2) ? (mOffsetHigh) : mThumbWidth/2;
                    }
                }
            }
            //设置进度条
            refresh();
            //抬起
        } else if (e.getAction() == MotionEvent.ACTION_UP) {
//            Log.d("ACTION_UP", "------------------");
            mThumbLow.setState(STATE_NORMAL);
            mThumbHigh.setState(STATE_NORMAL);

            if (mBarChangeListener != null) {
                mBarChangeListener.onProgressAfter();
            }
            //这两个for循环 是用来自动对齐刻度的，注释后，就可以自由滑动到任意位置
//            for (int i = 0; i < money.length; i++) {
//            	 if(Math.abs(mOffsetLow-i* ((mScollBarWidth-mThumbWidth)/ (money.length-1)))<=(mScollBarWidth-mThumbWidth)/(money.length-1)/2){
//            		 mprogressLow=i;
//                     mOffsetLow =i* ((mScollBarWidth-mThumbWidth)/(money.length-1));
//                     invalidate();
//                     break;
//                }
//			}
//
//            for (int i = 0; i < money.length; i++) {
//            	  if(Math.abs(mOffsetHigh-i* ((mScollBarWidth-mThumbWidth)/(money.length-1) ))<(mScollBarWidth-mThumbWidth)/(money.length-1)/2){
//            		  mprogressHigh=i;
//                	   mOffsetHigh =i* ((mScollBarWidth-mThumbWidth)/(money.length-1));
//                       invalidate();
//                       break;
//                }
//			}
        }
        return true;
    }

    public int getAreaFlag(MotionEvent e) {

        int top = mThumbMarginTop;
        int bottom = mThumbHeight + mThumbMarginTop;
        if (e.getY() >= top && e.getY() <= bottom && e.getX() >= (mOffsetLow - mThumbWidth / 2) && e.getX() <= mOffsetLow + mThumbWidth / 2) {
            return CLICK_ON_LOW;
        } else if (e.getY() >= top && e.getY() <= bottom && e.getX() >= (mOffsetHigh - mThumbWidth / 2) && e.getX() <= (mOffsetHigh + mThumbWidth / 2)) {
            return CLICK_ON_HIGH;
        } else if (e.getY() >= top
                && e.getY() <= bottom
                && ((e.getX() >= 0 && e.getX() < (mOffsetLow - mThumbWidth / 2)) || ((e.getX() > (mOffsetLow + mThumbWidth / 2))
                && e.getX() <= ((double) mOffsetHigh + mOffsetLow) / 2))) {
            return CLICK_IN_LOW_AREA;
        } else if (e.getY() >= top
                && e.getY() <= bottom
                && (((e.getX() > ((double) mOffsetHigh + mOffsetLow) / 2) && e.getX() < (mOffsetHigh - mThumbWidth / 2)) || (e
                .getX() > (mOffsetHigh + mThumbWidth/2) && e.getX() <= mScollBarWidth))) {
            return CLICK_IN_HIGH_AREA;
        } else if (!(e.getX() >= 0 && e.getX() <= mScollBarWidth && e.getY() >= top && e.getY() <= bottom)) {
            return CLICK_OUT_AREA;
        } else {
            return CLICK_INVAILD;
        }
    }

    //更新滑块
    private void refresh() {
        invalidate();
    }

    //设置前滑块的值
    public void setProgressLow(double  progressLow) {
        this.defaultScreenLow = progressLow;
        mOffsetLow = formatDouble(progressLow / 100 * (mDistance ))+ mThumbWidth / 2;
        isEdit = true;
        refresh();
    }

    //设置后滑块的值
    public void setProgressHigh(double  progressHigh) {
        this.defaultScreenHigh = progressHigh;
        mOffsetHigh = formatDouble(progressHigh / 100 * (mDistance)) + mThumbWidth / 2;
        isEdit = true;
        refresh();
    }

    public void setOnSeekBarChangeListener(OnSeekBarChangeListener mListener) {
        this.mBarChangeListener = mListener;
    }

    //回调函数，在滑动时实时调用，改变输入框的值
    public interface OnSeekBarChangeListener {
        //滑动前
        public void onProgressBefore();

        //滑动时
        public void onProgressChanged(RangeSeekBar seekBar, double progressLow,
                                      double progressHigh);

        //滑动后
        public void onProgressAfter();
    }

/*    private int formatInt(double value) {
        BigDecimal bd = new BigDecimal(value);
        BigDecimal bd1 = bd.setScale(0, BigDecimal.ROUND_HALF_UP);
        return bd1.intValue();
    }*/

    public static double formatDouble(double pDouble) {
        Log.d(TAG, "pDouble" + pDouble);
        BigDecimal bd = new BigDecimal(pDouble);
        BigDecimal bd1 = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        pDouble = bd1.doubleValue();
        return pDouble;
    }
}
