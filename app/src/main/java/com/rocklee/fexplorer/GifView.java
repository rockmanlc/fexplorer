package com.rocklee.fexplorer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.rocklee.fexplorer.Decoder.GifDecoder;
import com.rocklee.fexplorer.activities.PhotoPlayerActivity;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by admin on 2015/11/5.
 */
public class GifView extends View {
    private GifDecoder gDecoder;
    private boolean isStop = false;
    private int delta = 1;

    private Bitmap bmp;
    private InputStream is;

    private android.graphics.Rect src = new android.graphics.Rect();
    private android.graphics.Rect dst = new android.graphics.Rect();

    private Thread updateTimer;

    /**
     *  construct - refer for java
     * @param context
     */
    public GifView(Context context) {
        this(context, null);

    }

    /**
     *  construct - refer for xml
     * @param context
     * @param attrs
     */
    public GifView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDelta(1);
    }

    /**
     * stop
     * @param stop
     */
    public void setStop() {
        isStop = true;
    }

    /**
     * start
     */
    public void setStart() {
        if(updateTimer != null && updateTimer.isAlive()){
            isStop = true;
            try {
                Thread.sleep(getPlayTimeEachFrame());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        updateTimer = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if(isStop){
                        return;
                    }
                    GifView.this.postInvalidate();
                    try {
                        Thread.sleep(getPlayTimeEachFrame());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        isStop = false;
        updateTimer.start();
    }

    public int getFrameCount(){
        return gDecoder.getFrameCount();
    }

    /**
     * Through the subscript the zoomed image display
     * @param id
     */
    public boolean setSrc(String path, PhotoPlayerActivity player) {
        if(bmp != null){
            bmp.recycle();
        }
        if(gDecoder == null){
        }else{
            gDecoder.reset();
        }
        gDecoder = new GifDecoder();

        try {
            is = new FileInputStream(path);
            if(gDecoder != null){
                gDecoder.read(is);
                if(gDecoder.err()){
                    //bmp = player.decodeBitmap(path);
                }else{
                    bmp = gDecoder.getImage();// first
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        this.postInvalidate();
        return true;
    }

    public boolean decodeBitmapFromNet(String path, PhotoPlayerActivity player){
        if(bmp != null){
            bmp.recycle();
        }
        if(gDecoder == null){
        }else{
            gDecoder.reset();
            //gDecoder.resetFrame();
        }
        gDecoder = new GifDecoder();
        try {
            is = new URL(path).openStream();
            if(gDecoder != null){
                gDecoder.read(is);
                if(gDecoder.err()){
                    //bmp = player.decodeBitmap(path);
                }else{
                    bmp = gDecoder.getImage();// first
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.postInvalidate();
        return true;
    }

    public void setDelta(int time) {
        delta = time;
    }

    public int getPlayTimeEachFrame(){
        if(gDecoder != null){
            return Math.max(100, gDecoder.nextDelay()) / delta;
        }
        return 0;
    }

    public int getPlayTime(){
        if(gDecoder != null){
            return gDecoder.getFrameCount() * getPlayTimeEachFrame();
        }
        return 0;
    }

/*  @Override
    public void layout(int arg0, int arg1, int arg2, int arg3) {
        super.layout(0, 0, 1920, 1080);
    }
*/

    protected void onDraw(Canvas canvas) {
        if (bmp != null) {
            Paint paint = new Paint();
            src.left = 0;
            src.top = 0;
            src.bottom = bmp.getHeight();
            src.right = bmp.getWidth();
            dst.left = 0;
            dst.top = 0;
            dst.bottom = this.getHeight();
            dst.right = this.getWidth();
            //Log.i("***************", "****src****" + src.top + " " + src.bottom + " "
            //      + src.left + " " + src.right);
            if(!bmp.isRecycled()){
                center();
                canvas.drawBitmap(bmp, src, dst, paint);
                bmp = gDecoder.nextBitmap();
            }
        }
    }

    protected void center() {
        bmp = resizeDownIfTooBig(bmp, true);
        float height = bmp.getHeight();
        float width = bmp.getWidth();
        float deltaX = 0, deltaY = 0;
        int viewHeight = getHeight();
        if (height <= viewHeight) {
            deltaY = (viewHeight - height) / 2 - src.top;
        }  else if (src.top > 0) {
            deltaY = -src.top;
        } else if (src.bottom < viewHeight) {
            deltaY = getHeight() - src.bottom;
        }

        int viewWidth = getWidth();
        if (width <= viewWidth) {
            deltaX = (viewWidth - width) / 2 - src.left;
        } else if (src.left > 0) {
            deltaX = -src.left;
        } else if (src.right < viewWidth) {
            deltaX = viewWidth - src.right;
        }

        dst.top = src.top + (int)deltaY;
        dst.left = src.left + (int)deltaX;
        dst.bottom = bmp.getHeight() + (int)deltaY;
        dst.right = bmp.getWidth() + (int)deltaX;
/*      Log.i("***************", "****src****" + height + " " + width + " " +
                viewHeight + " " + viewWidth);
        Log.i("***************", "****src****" + src.top + " " + src.bottom + " "
                + src.left + " " + src.right);*/
    }

    // Resize the bitmap if each side is >= targetSize * 2
    private Bitmap resizeDownIfTooBig(Bitmap bitmap,
                                      boolean recycle) {
        int srcWidth = bitmap.getWidth();
        int srcHeight = bitmap.getHeight();
        float scale = Math.min((float) getWidth() / srcWidth,
                (float) getHeight() / srcHeight);
        //Log.d("************", "srcWidth : " + srcWidth + " srcHeight : " + srcHeight
        //      + " scale : " + scale);
        if (scale > 1.0f) {
            return bitmap;
        }
        return resizeBitmapByScale(bitmap, scale, recycle);
    }

    private Bitmap resizeBitmapByScale(Bitmap bitmap, float scale,
                                       boolean recycle) {
        int width = Math.round(bitmap.getWidth() * scale);
        int height = Math.round(bitmap.getHeight() * scale);
        if (width == bitmap.getWidth() && height == bitmap.getHeight()) {
            return bitmap;
        }
        Bitmap target = Bitmap.createBitmap(width, height, getConfig(bitmap));
        Canvas canvas = new Canvas(target);
        canvas.scale(scale, scale);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        if (recycle) {
            bitmap.recycle();
        }
        return target;
    }

    private Bitmap.Config getConfig(Bitmap bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            config = Bitmap.Config.ARGB_8888;
        }
        return config;
    }
}
