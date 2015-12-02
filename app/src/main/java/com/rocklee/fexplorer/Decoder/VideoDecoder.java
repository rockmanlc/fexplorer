package com.rocklee.fexplorer.Decoder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.media.MediaMuxer.OutputFormat;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * Created by admin on 2015/11/9.
 */
public class VideoDecoder {
    private final static String TAG = "LC_VideoDecoder";
    private MediaCodec mediaDecoder;
    private MediaExtractor mediaExtractor;
    private MediaFormat mediaFormat;
    private MediaMuxer mediaMuxer;
    private String mime = null;
    private String[] key = {MediaFormat.KEY_SAMPLE_RATE, MediaFormat.KEY_CHANNEL_COUNT,
            MediaFormat.KEY_WIDTH, MediaFormat.KEY_HEIGHT,
            MediaFormat.KEY_MAX_WIDTH, MediaFormat.KEY_MAX_HEIGHT,
            MediaFormat.KEY_MAX_INPUT_SIZE, MediaFormat.KEY_BIT_RATE,
            MediaFormat.KEY_COLOR_FORMAT, MediaFormat.KEY_FRAME_RATE,
            MediaFormat.KEY_CAPTURE_RATE, MediaFormat.KEY_I_FRAME_INTERVAL,
            MediaFormat.KEY_TEMPORAL_LAYERING, MediaFormat.KEY_STRIDE,
            MediaFormat.KEY_SLICE_HEIGHT, MediaFormat.KEY_OPERATING_RATE,
            MediaFormat.KEY_PROFILE, MediaFormat.KEY_LEVEL,
            MediaFormat.KEY_BITRATE_MODE, MediaFormat.KEY_DURATION,
            MediaFormat.KEY_IS_ADTS, MediaFormat.KEY_CHANNEL_MASK,
            MediaFormat.KEY_AAC_PROFILE, MediaFormat.KEY_AAC_SBR_MODE,
            MediaFormat.KEY_AAC_MAX_OUTPUT_CHANNEL_COUNT, MediaFormat.KEY_AAC_DRC_TARGET_REFERENCE_LEVEL,
            MediaFormat.KEY_AAC_ENCODED_TARGET_LEVEL, MediaFormat.KEY_AAC_DRC_BOOST_FACTOR,
            MediaFormat.KEY_AAC_DRC_ATTENUATION_FACTOR, MediaFormat.KEY_AAC_DRC_HEAVY_COMPRESSION,
            MediaFormat.KEY_COMPLEXITY, MediaFormat.KEY_PRIORITY,
            MediaFormat.KEY_OPERATING_RATE, MediaFormat.KEY_BITRATE_MODE,
            MediaFormat.KEY_AUDIO_SESSION_ID};

    public boolean decodeVideo(String url, long clipPoint, long clipDuration) {
        //MediaMetadataRetriever
        int videoTrackIndex = -1;
        int audioTrackIndex = -1;
        int videoMaxInputSize = 0;
        int audioMaxInputSize = 0;
        int sourceVTrack = 0;
        int sourceATrack = 0;
        long videoDuration, audioDuration;
        String videoPhoto = url.substring(0, url.lastIndexOf(".")) + "_photo.png";
        //saveBitmap(videoPhoto, getVideoThumbnail(url, 180, 180, MediaStore.Video.Thumbnails.MINI_KIND));
        mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource(url);
            mediaMuxer = new MediaMuxer(url.substring(0, url.lastIndexOf(".")) + "_output.mp4", OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (Exception e) {
            Log.e(TAG, "error path" + e.getMessage());
        }
        for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
            try {
                mediaFormat = mediaExtractor.getTrackFormat(i);
                mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
//                    for (String x : key) {
//                        if (mediaFormat.containsKey(x))
//                            Log.d(TAG, x + " is exist");
//                    }
                    sourceVTrack = i;
                    int width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
                    int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
                    videoMaxInputSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                    videoDuration = mediaFormat.getLong(MediaFormat.KEY_DURATION);
                    mediaDecoder = MediaCodec.createDecoderByType(mime);
                    if (clipPoint >= videoDuration) {
                        Log.e(TAG, "clip point is error!");
                        return false;
                    }
                    if ((clipDuration != 0) && ((clipDuration + clipPoint) >= videoDuration)) {
                        Log.e(TAG, "clip duration is error!");
                        return false;
                    }
                    Log.d(TAG, "width and height is " + width + " " + height
                                    + ";maxInputSize is " + videoMaxInputSize
                                    + ";duration is " + videoDuration
                    );
                    videoTrackIndex = mediaMuxer.addTrack(mediaFormat);
                }
                else if (mime.startsWith("audio/")) {
//                    for (String x : key) {
//                        if (mediaFormat.containsKey(x))
//                            Log.d(TAG, x + " is exist");
//                    }
                    sourceATrack = i;
                    int sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    int channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    audioMaxInputSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                    audioDuration = mediaFormat.getLong(MediaFormat.KEY_DURATION);
                    Log.d(TAG, "sampleRate is " + sampleRate
                                    + ";channelCount is " + channelCount
                                    + ";audioMaxInputSize is " + audioMaxInputSize
                                    + ";audioDuration is " + audioDuration
                    );
                    audioTrackIndex = mediaMuxer.addTrack(mediaFormat);
                }
                Log.d(TAG, "file mime is " + mime);
            } catch (Exception e) {
                Log.e(TAG, " read error " + e.getMessage());
            }
        }
        ByteBuffer inputBuffer = ByteBuffer.allocate(videoMaxInputSize);
        //save some frame
//        int count = 0;
//        String dataName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/data.dat";
//        FileOutputStream outFile = null;
//        try {
//            outFile = new FileOutputStream(dataName);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
        //
        mediaMuxer.start();
        //video part
        mediaExtractor.selectTrack(sourceVTrack);
        MediaCodec.BufferInfo videoInfo = new MediaCodec.BufferInfo();
        videoInfo.presentationTimeUs = 0;
        long videoSampleTime;
        //get video sample time
        {
            mediaExtractor.readSampleData(inputBuffer, 0);
            //skip first I frame
            if (mediaExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC)
                mediaExtractor.advance();
            mediaExtractor.readSampleData(inputBuffer, 0);
            long firstVideoPTS = mediaExtractor.getSampleTime();
            mediaExtractor.advance();
            mediaExtractor.readSampleData(inputBuffer, 0);
            long SecondVideoPTS = mediaExtractor.getSampleTime();
            videoSampleTime = Math.abs(SecondVideoPTS - firstVideoPTS);
            Log.d(TAG, "videoSampleTime is " + videoSampleTime);
        }
        mediaExtractor.seekTo(clipPoint, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        int IFrameCount = 0;
        while (true) {
            int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
            if (sampleSize < 0) {
                mediaExtractor.unselectTrack(sourceVTrack);
                break;
            }
            //save some frame
//            if (count < 4) {
//                try {
//                    outFile.write(inputBuffer.array(), 0, sampleSize);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            } else {
//                try {
//                    outFile.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            count++;
            //save some frame
            int trackIndex = mediaExtractor.getSampleTrackIndex();
            long presentationTimeUs = mediaExtractor.getSampleTime();
            int sampleFlag = mediaExtractor.getSampleFlags();
            if (sampleFlag == 1) {
//                String IFramePhoto = url.substring(0, url.lastIndexOf(".")) + "_" + IFrameCount + ".png";
//                saveBitmap(IFramePhoto, bitmap);
//                IFrameCount++;
            }
            Log.d(TAG, "trackIndex is " + trackIndex
                    + ";presentationTimeUs is " + presentationTimeUs
                    + ";sampleFlag is " + sampleFlag
                    + ";sampleSize is " + sampleSize);
            if ((clipDuration != 0) && (presentationTimeUs > (clipPoint + clipDuration))) {
                mediaExtractor.unselectTrack(sourceVTrack);
                break;
            }
            mediaExtractor.advance();
            videoInfo.offset = 0;
            videoInfo.size = sampleSize;
            videoInfo.flags = sampleFlag;
            mediaMuxer.writeSampleData(videoTrackIndex, inputBuffer, videoInfo);
            videoInfo.presentationTimeUs += videoSampleTime;//presentationTimeUs;
        }
        //audio part
        mediaExtractor.selectTrack(sourceATrack);
        MediaCodec.BufferInfo audioInfo = new MediaCodec.BufferInfo();
        audioInfo.presentationTimeUs = 0;
        long audioSampleTime;
        //get audio sample time
        {
            mediaExtractor.readSampleData(inputBuffer, 0);
            //skip first sample
            if (mediaExtractor.getSampleTime() == 0)
                mediaExtractor.advance();
            mediaExtractor.readSampleData(inputBuffer, 0);
            long firstAudioPTS = mediaExtractor.getSampleTime();
            mediaExtractor.advance();
            mediaExtractor.readSampleData(inputBuffer, 0);
            long SecondAudioPTS = mediaExtractor.getSampleTime();
            audioSampleTime = Math.abs(SecondAudioPTS - firstAudioPTS);
            Log.d(TAG, "AudioSampleTime is " + audioSampleTime);
        }
        mediaExtractor.seekTo(clipPoint, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        while (true) {
            int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
            if (sampleSize < 0) {
                mediaExtractor.unselectTrack(sourceATrack);
                break;
            }
            int trackIndex = mediaExtractor.getSampleTrackIndex();
            long presentationTimeUs = mediaExtractor.getSampleTime();
            Log.d(TAG, "trackIndex is " + trackIndex
                    + ";presentationTimeUs is " + presentationTimeUs);
            if ((clipDuration != 0) && (presentationTimeUs > (clipPoint + clipDuration))) {
                mediaExtractor.unselectTrack(sourceATrack);
                break;
            }
            mediaExtractor.advance();
            audioInfo.offset = 0;
            audioInfo.size = sampleSize;
            //audioInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
            mediaMuxer.writeSampleData(audioTrackIndex, inputBuffer, audioInfo);
            audioInfo.presentationTimeUs += audioSampleTime;//presentationTimeUs;
        }
        mediaMuxer.stop();
        mediaMuxer.release();
        mediaExtractor.release();
        mediaExtractor = null;
        return true;
    }

    private Bitmap getImageThumbnail(String imagePath, int width, int height) {
        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        // 获取这个图片的宽和高，注意此处的bitmap为null
        bitmap = BitmapFactory.decodeFile(imagePath, options);
        options.inJustDecodeBounds = false; // 设为 false
        // 计算缩放比
        int h = options.outHeight;
        int w = options.outWidth;
        int beWidth = w / width;
        int beHeight = h / height;
        int be = 1;
        if (beWidth < beHeight) {
            be = beWidth;
        } else {
            be = beHeight;
        }
        if (be <= 0) {
            be = 1;
        }
        options.inSampleSize = be;
        // 重新读入图片，读取缩放后的bitmap，注意这次要把options.inJustDecodeBounds 设为 false
        bitmap = BitmapFactory.decodeFile(imagePath, options);
        // 利用ThumbnailUtils来创建缩略图，这里要指定要缩放哪个Bitmap对象
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        return bitmap;
    }

    private Bitmap getVideoThumbnail(String videoPath, int width, int height,
                                     int kind) {
        Bitmap bitmap = null;
        // 获取视频的缩略图
        bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, kind);
        System.out.println("w"+bitmap.getWidth());
        System.out.println("h"+bitmap.getHeight());
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        return bitmap;
    }

    public void saveBitmap(String photoName, Bitmap bitmap) {
        File f = new File(photoName);
        if (f.exists()) {
            f.delete();
        }
        try {
            if (bitmap != null) {
                float width = bitmap.getWidth();
                float height = bitmap.getHeight();
                Matrix matrix = new Matrix();
                float scaleWidth = ((float) 180) / width;
                float scaleHeight = ((float) 180) / height;
                matrix.postScale(scaleWidth, scaleHeight);
                Bitmap reBitmap = Bitmap.createBitmap(bitmap, 0, 0, (int) width,
                        (int) height, matrix, true);
                FileOutputStream out = new FileOutputStream(f);
                reBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();
                Log.i(TAG, "已经保存");
            } else
                Log.e(TAG, "bitmap error");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Bitmap getImageFromVideo(String file, long pts) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(file);
        Bitmap bitmap = mmr.getFrameAtTime(pts);
        mmr.release();
        return bitmap;
    }
}
