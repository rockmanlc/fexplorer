package com.rocklee.fexplorer;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.rocklee.fexplorer.CustomView.RangeSeekBar;
import com.rocklee.fexplorer.Decoder.VideoDecoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class VideoPlayerActivity extends Activity {
    private final static String TAG = "LC_VideoPlayer";
    //update seekbar time
    private final static int UPDATE_CURRENT_TIME = 0;
    private final static int LOAD_PHOTO = 1;
    private final static int UPDATE_PLAY_STATE = 2;
    //In a sequence the max video duration is 50000000us
    private final static long MAX_SEQUENCE_LENGTH = 50000000;
    //A sequence should have 7 image
    private final static int IMAGE_LIST = 7;

    private SeekBar seekbar = null;
    private ImageButton videoPlay, back, clip;
    private TextView cancel;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private RelativeLayout myCtlPanel, myTrimPanel, myRangeSeekBar;
    private TextView current_time,total_time;
    private ImageView[] imageView_list = new ImageView[IMAGE_LIST];
    private int[] image_id = {R.id.image_1, R.id.image_2, R.id.image_3, R.id.image_4,
            R.id.image_5, R.id.image_6, R.id.image_7};
    //false means bitmap can not use, true means bitmap is buffer ok, can be used.
    private Bitmap[] bitmap_list = new Bitmap[IMAGE_LIST];
    private Bitmap[] temp_list = new Bitmap[IMAGE_LIST];
    private ImageView playbackPreview, trimLeftButton, trimRightButton;
    private RangeSeekBar rangeSeekBar;


    private MediaPlayer mediaPlayer = null;
    private Timer mTimer;
    private TimerTask mTimerTask;
    private String videoPath;
    private boolean isChanging = false;//is seekbar changing
    private boolean isPlaying = false;//is video playing
    private boolean isTrimPanel = false;
    private boolean isExit = false;
    private boolean isupdateImageOver = false;
    private int mSurfaceViewWidth,mSurfaceViewHeight;
    private long sequence_startTime, sequence_duration;
    //0 empty, 1 current, 2 prev, 3 next
    private int temp_list_state = 0;
    private int maxDuration;
    private int mTime;

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case UPDATE_CURRENT_TIME :
                    current_time.setText(intTimeToString(getTime()));
                    break;
                case LOAD_PHOTO :
                    Log.d(TAG, "bitmap id:" + msg.arg1);
                    if (msg.arg1 < bitmap_list.length) {
                        imageView_list[msg.arg1].setImageBitmap(bitmap_list[msg.arg1]);
                        if (msg.arg1 == IMAGE_LIST - 1) {
                            isupdateImageOver = true;
                            myRangeSeekBar.setVisibility(View.VISIBLE);
                        }
                    }
                    break;
                case UPDATE_PLAY_STATE:
                    if (mediaPlayer.isPlaying()) {
                        playbackPreview.setImageDrawable(getResources().getDrawable(R.drawable.play_bg));
                        mediaPlayer.pause();
                    } else {
                        playbackPreview.setImageDrawable(getResources().getDrawable(R.drawable.pause_bg));
                        mediaPlayer.start();
                    }
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_video_player);
        Intent intent = getIntent();
        videoPath = intent.getStringExtra("video_path");
        Log.d(TAG, "video_path is " + videoPath);
        //first panel
        videoPlay = (ImageButton) findViewById(R.id.play);
        videoPlay.setOnClickListener(new ClickEvent());
        back = (ImageButton) findViewById(R.id.back);
        back.setOnClickListener(new ClickEvent());
        clip = (ImageButton) findViewById(R.id.clip);
        clip.setOnClickListener(new ClickEvent());
        seekbar=(SeekBar) findViewById(R.id.seek_bar);
        seekbar.setOnSeekBarChangeListener(new SeekBarChangeEvent());
        current_time = (TextView) findViewById(R.id.start_time);
        total_time = (TextView) findViewById(R.id.total_time);
        myCtlPanel = (RelativeLayout) findViewById(R.id.control_panel);
        surfaceView = (SurfaceView) findViewById(R.id.video);
        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isTrimPanel) {
                    myCtlPanel.setVisibility(
                            myCtlPanel.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
                }
            }
        });
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            public void surfaceCreated(SurfaceHolder holder) {
                initMedia();
            }

            public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                       int height) {
            }

            public void surfaceDestroyed(SurfaceHolder holder) {
            }
        });
        surfaceHolder.setSizeFromLayout();
        //second panel
        cancel = (TextView) findViewById(R.id.cancel);
        playbackPreview = (ImageView) findViewById(R.id.playback_preview);
        playbackPreview.setOnClickListener(new ClickEvent());
        myTrimPanel = (RelativeLayout) findViewById(R.id.trim_panel);
        for (int i = 0; i < imageView_list.length; i++) {
            imageView_list[i] = (ImageView) findViewById(image_id[i]);
        }
        //third panel
        myRangeSeekBar = (RelativeLayout) findViewById(R.id.slide_frame);
        trimLeftButton = (ImageView) findViewById(R.id.left_button);
        trimLeftButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.d(TAG, "sequence_startTime:" + sequence_startTime +
                        " sequence_duration:" + sequence_duration);
                if (mediaPlayer.isPlaying()) {
                    Message message = new Message();
                    message.what = UPDATE_PLAY_STATE;
                    handler.sendMessage(message);
                }
                long next_sequence_startTime = sequence_startTime;
                if (next_sequence_startTime - MAX_SEQUENCE_LENGTH < 0)
                    return false;
                else {
                    sequence_startTime -= MAX_SEQUENCE_LENGTH;
                    sequence_duration = MAX_SEQUENCE_LENGTH;
                }
                isupdateImageOver = false;
                myRangeSeekBar.setVisibility(View.INVISIBLE);
                for (int i = 0; i < imageView_list.length; i++)
                    imageView_list[i].setImageBitmap(null);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        shiftLeft();
                    }
                }).start();
                return true;
            }
        });
        trimRightButton = (ImageView) findViewById(R.id.right_button);
        trimRightButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mediaPlayer.isPlaying()) {
                    Message message = new Message();
                    message.what = UPDATE_PLAY_STATE;
                    handler.sendMessage(message);
                }
                long prev_sequence_startTime = sequence_startTime;
                long prev_sequence_duration = sequence_duration;
                if (prev_sequence_startTime + MAX_SEQUENCE_LENGTH >= (maxDuration * 1000))
                    return false;
                else if (prev_sequence_startTime + prev_sequence_duration + MAX_SEQUENCE_LENGTH > (maxDuration * 1000)) {
                    sequence_startTime += MAX_SEQUENCE_LENGTH;
                    sequence_duration = (maxDuration * 1000) - (prev_sequence_startTime + prev_sequence_duration);
                } else {
                    sequence_startTime += MAX_SEQUENCE_LENGTH;
                    sequence_duration = MAX_SEQUENCE_LENGTH;
                }
                Log.d(TAG, "prev_sequence_startTime:" + prev_sequence_startTime +
                        " prev_sequence_duration:" + prev_sequence_duration);
                Log.d(TAG, "sequence_startTime:" + sequence_startTime +
                        " sequence_duration:" + sequence_duration);
                isupdateImageOver = false;
                myRangeSeekBar.setVisibility(View.INVISIBLE);
                for (int i = 0; i < imageView_list.length; i++)
                    imageView_list[i].setImageBitmap(null);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        shiftRight();
                    }
                }).start();
                return true;
            }
        });

        rangeSeekBar = (RangeSeekBar) findViewById(R.id.range_seekbar);
        rangeSeekBar.setOnSeekBarChangeListener(new RangeSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressBefore() {

            }

            @Override
            public void onProgressChanged(RangeSeekBar seekBar, double progressLow, double progressHigh) {

            }

            @Override
            public void onProgressAfter() {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isChanging = true;
        isExit = true;
        mediaPlayer.release();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (isTrimPanel) {
                myCtlPanel.setVisibility(View.VISIBLE);
                myTrimPanel.setVisibility(View.INVISIBLE);
                isTrimPanel = false;
            } else {
                isExit = true;
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

//    private void playVideo(final String videoPath) {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                videoView.setVideoPath(videoPath);
//                videoView.setMediaController(mediaController);
//                mediaController.setMediaPlayer(videoView);
//                videoView.requestFocus();
//                VideoDecoder videoDecoder = new VideoDecoder();
//                videoDecoder.decodeVideo(videoPath, 5000000, 55000000);
//            }
//        }).start();
//    }

    class ClickEvent implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            if(v==videoPlay && mediaPlayer != null)
            {
                if (mediaPlayer.isPlaying()) {
                    videoPlay.setImageDrawable(getResources().getDrawable(R.drawable.play));
                    mediaPlayer.pause();
                    isPlaying = false;
                } else {
                    videoPlay.setImageDrawable(getResources().getDrawable(R.drawable.pause));
                    mediaPlayer.start();
                    isPlaying = true;
                }
            } else if (v == back) {
                finish();
            } else if (v == clip) {
                myCtlPanel.setVisibility(View.INVISIBLE);
                myTrimPanel.setVisibility(View.VISIBLE);
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        myTrimPanel.setVisibility(View.INVISIBLE);
                        myCtlPanel.setVisibility(View.VISIBLE);
                        isTrimPanel = false;
                        mediaPlayer.pause();
                        isPlaying = false;
                    }
                });
                isTrimPanel = true;
                videoPlay.setImageDrawable(getResources().getDrawable(R.drawable.play));
                mediaPlayer.pause();
                isPlaying = false;
            } else if (v == playbackPreview) {
                Message message = new Message();
                message.what = UPDATE_PLAY_STATE;
                handler.sendMessage(message);
//                if (mediaPlayer.isPlaying()) {
//                    playbackPreview.setImageDrawable(getResources().getDrawable(R.drawable.play_bg));
//                    mediaPlayer.pause();
//                } else {
//                    playbackPreview.setImageDrawable(getResources().getDrawable(R.drawable.pause_bg));
//                    mediaPlayer.start();
//                }
            }
        }
    }

    class SeekBarChangeEvent implements SeekBar.OnSeekBarChangeListener{

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            setTime(progress);
            if (fromUser) {
                mediaPlayer.seekTo(progress);
                current_time.setText(intTimeToString(getTime()));
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            videoPlay.setImageDrawable(getResources().getDrawable(R.drawable.play));
            mediaPlayer.pause();
            isChanging = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            //mediaPlayer.seekTo(seekBar.getProgress());
            if (isPlaying == true) {
                videoPlay.setImageDrawable(getResources().getDrawable(R.drawable.pause));
                mediaPlayer.start();
            }
            isChanging = false;
        }

    }

    private void initMedia() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        mSurfaceViewWidth = dm.widthPixels;
        mSurfaceViewHeight = dm.heightPixels;
        float density = dm.density;
        int densityDpi = dm.densityDpi;
        Log.d(TAG, "mSurfaceViewWidth:" + mSurfaceViewWidth
                + " mSurfaceViewHeight:" + mSurfaceViewHeight
                + " density is:" + density +
                " densityDpi is:" + densityDpi);
        Configuration configuration = getResources().getConfiguration();
        Log.d(TAG, "configuration.orientation is:" + configuration.orientation);
        mediaPlayer = new MediaPlayer();
//        mediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
//            @Override
//            public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
//                if (width == 0 || height == 0) {
//                    Log.e(TAG, "invalid video width(" + width + ") or height(" + height
//                            + ")");
//                    return;
//                }
//                Log.d(TAG, "onVideoSizeChanged width:" + width + " height:" + height);
//                int w = mSurfaceViewHeight * width / height;
//                int h = mSurfaceViewWidth * height / width;
//                int wMargin = (mSurfaceViewWidth - w) / 2;
//                int hMargin = (mSurfaceViewHeight - h) / 2;
//                if (wMargin == 0 && hMargin == 0) {
//                    if (mSurfaceViewWidth > width && mSurfaceViewHeight > height) {
//                        wMargin = (mSurfaceViewWidth - width) / 2;
//                        hMargin = (mSurfaceViewHeight - height) / 2;
//                    }
//                }
//                Log.d(TAG, "wMargin:" + wMargin + " hMargin:" + hMargin);
//                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
//                        FrameLayout.LayoutParams.MATCH_PARENT,
//                        FrameLayout.LayoutParams.MATCH_PARENT);
//                lp.setMargins(wMargin, hMargin, wMargin, hMargin);
//                surfaceView.setLayoutParams(lp);
//            }
//        });
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setDisplay(surfaceHolder);
        try {
            mediaPlayer.setDataSource(videoPath);
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mediaPlayer.start();
                mediaPlayer.pause();
                maxDuration = mediaPlayer.getDuration();
                seekbar.setMax(maxDuration);
                total_time.setText(intTimeToString(maxDuration));
                mTimer = new Timer();
                mTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        if (isChanging == true)
                            return;
                        seekbar.setProgress(mediaPlayer.getCurrentPosition());
                        Message message = new Message();
                        message.what = UPDATE_CURRENT_TIME;
                        handler.sendMessage(message);
                    }
                };
                mTimer.schedule(mTimerTask, 0, 500);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sequence_startTime = 0;
                        if (maxDuration > MAX_SEQUENCE_LENGTH/1000)
                            sequence_duration = MAX_SEQUENCE_LENGTH;
                        else
                            sequence_duration = (long)(maxDuration*1000);
                        firstGetImage();
                    }
                }).start();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            if (isExit)
                                break;
                            if (mediaPlayer.isPlaying() && isupdateImageOver) {
                                if (mediaPlayer.getCurrentPosition() * 1000 >= sequence_startTime + sequence_duration) {
                                    Message message = new Message();
                                    message.what = UPDATE_PLAY_STATE;
                                    handler.sendMessage(message);
                                }
                            }
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer arg0) {
                playbackPreview.setImageDrawable(getResources().getDrawable(R.drawable.play_bg));
                Toast.makeText(VideoPlayerActivity.this, "video end!", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private String intTimeToString(int msTime) {
        int sTime = msTime/1000;
        if (sTime > (99*60+59) || sTime < 0)
            return "--:--";
        else
            return String.format("%02d:%02d", sTime/60, sTime%60);
    }

    public void setTime(int time) {
        mTime = time;
    }

    public int getTime() {
        return mTime;
    }

    private Bitmap compressImage(Bitmap image) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        image.compress(Bitmap.CompressFormat.PNG, 0, baos);
        int options = 100;
        //循环判断如果压缩后图片是否大于100kb,大于继续压缩
//        while ( baos.toByteArray().length / 1024>100) {
//            //重置baos即清空baos
//            baos.reset();
//            //这里压缩options%，把压缩后的数据存放到baos中
//            image.compress(Bitmap.CompressFormat.JPEG, options, baos);
//            //每次都减少10
//            options -= 10;
//        }
        //把压缩后的数据baos存放到ByteArrayInputStream中
        Log.d(TAG,"compress size is :" + baos.toByteArray().length / 1024);
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
        //把ByteArrayInputStream数据生成图片
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);
        return bitmap;
    }

    private Boolean getImageFromVideo() {
        int thumbnail_width = playbackPreview.getWidth();
        int thumbnail_height = playbackPreview.getHeight();
        Log.d(TAG, "thumbnail_width:" + thumbnail_width + " thumbnail_height:" + thumbnail_height);
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(videoPath);
        long tempStart = sequence_startTime;
        for (int i = 0; i < IMAGE_LIST; i++) {
            if (isExit)
                break;
            Log.d(TAG, "tempStart:" + tempStart);
            bitmap_list[i] = mmr.getFrameAtTime(tempStart);
            float width = bitmap_list[i].getWidth();
            float height = bitmap_list[i].getHeight();
            Matrix matrix = new Matrix();
            float scaleWidth = ((float) thumbnail_width) / width;
            float scaleHeight = ((float) thumbnail_height) / height;
            matrix.postScale(scaleWidth, scaleHeight);
            Log.d(TAG, "width:" + width + " height:" + height +
                    " scaleWidth:" + scaleWidth + " scaleHeight:" + scaleHeight);
            Bitmap reBitmap = Bitmap.createBitmap(bitmap_list[i],
                    0, 0, (int) width, (int) height, matrix, true);
            Log.d(TAG,"reBitmap size is :" + reBitmap.getByteCount());
            //bitmap_list[i] = compressImage(reBitmap);
            bitmap_list[i] = reBitmap;
            tempStart += sequence_duration / IMAGE_LIST;
            Message message = new Message();
            message.what = LOAD_PHOTO;
            message.arg1 = i;
            handler.sendMessage(message);
        }
        mmr.release();
        return true;
    }

    public void firstGetImage() {
        getImageFromVideo();
        for (int i = 0; i < IMAGE_LIST; i++)
            temp_list[i] = bitmap_list[i];
        temp_list_state = 1;
    }

    public void shiftRight() {
        mediaPlayer.seekTo((int)(sequence_startTime/1000));
        if (temp_list_state == 3) {
            for (int i = 0; i < temp_list.length; i++) {
                bitmap_list[i] = temp_list[i];
                Message message = new Message();
                message.what = LOAD_PHOTO;
                message.arg1 = i;
                handler.sendMessage(message);
            }
            temp_list_state = 1;
        } else if (temp_list_state == 2) {
            for (int i = 0; i < IMAGE_LIST; i++)
                temp_list[i] = bitmap_list[i];
            temp_list_state = 2;
            getImageFromVideo();
        } else if (temp_list_state == 1) {
            getImageFromVideo();
            temp_list_state = 2;
        }
    }

    public void shiftLeft() {
        mediaPlayer.seekTo((int)(sequence_startTime/1000));
        if (temp_list_state == 2) {
            for (int i = 0; i < temp_list.length; i++) {
                bitmap_list[i] = temp_list[i];
                Message message = new Message();
                message.what = LOAD_PHOTO;
                message.arg1 = i;
                handler.sendMessage(message);
            }
            temp_list_state = 1;
        } else if (temp_list_state == 3) {
            for (int i = 0; i < IMAGE_LIST; i++)
                temp_list[i] = bitmap_list[i];
            temp_list_state = 3;
            getImageFromVideo();
        } else if (temp_list_state == 1) {
            getImageFromVideo();
            temp_list_state = 3;
        }
    }
}