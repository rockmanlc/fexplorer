package com.rocklee.fexplorer.Decoder;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
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
import android.media.browse.MediaBrowser;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class VideoDecoder {
    private final static String TAG = "LC_VideoDecoder";
    private static final String DIR_NAME = "trim";
    private static final int DEFAULT_BUFFER_SIZE = 1 * 1024 * 1024;
    private String mOutputPath;
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

    private interface ContentResolverQueryCallback {
        void onCursorResult(Cursor cursor);
    }


    private static void querySource(ContentResolver contentResolver, Uri uri,
                                    String[] projection, ContentResolverQueryCallback callback) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(uri, projection, null, null, null);
            if ((cursor != null) && cursor.moveToNext()) {
                callback.onCursorResult(cursor);
            }
        } catch (Exception e) {
            // Ignore error for lacking the data column from the source.
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static File getSaveDirectory(ContentResolver contentResolver, Uri uri) {
        final File[] dir = new File[1];
        querySource(contentResolver, uri,
                new String[] { MediaStore.Video.VideoColumns.DATA },
                new ContentResolverQueryCallback() {
                    @Override
                    public void onCursorResult(Cursor cursor) {
                        dir[0] = new File(cursor.getString(0)).getParentFile();
                    }
                });
        return dir[0];
    }

    private static final File getCaptureFile(final String type, final String ext) {
        final File dir = new File(Environment.getExternalStoragePublicDirectory(type), DIR_NAME);
        Log.d(TAG, "path=" + dir.toString());
        dir.mkdirs();
        if (dir.canWrite()) {
            return new File(dir, ext);
        }
        return null;
    }

    private static void genVideo(Context context, Uri srcPath, String dstPath, long start, long end) throws IOException {
        MediaExtractor mediaExtractor = new MediaExtractor();
        mediaExtractor.setDataSource(context, srcPath, null);

        int trackCount = mediaExtractor.getTrackCount();
        MediaMuxer mediaMuxer = new MediaMuxer(dstPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        HashMap<Integer, Integer> indexMap = new HashMap<>(trackCount);
        int bufferSize = -1;
        for (int i = 0; i < trackCount; i++) {
            MediaFormat format = mediaExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);

            boolean selectCurrentTrack = false;

            if (mime.startsWith("audio/")) {
                selectCurrentTrack = true;
            } else if (mime.startsWith("video/")) {
                selectCurrentTrack = true;
            }

            if (selectCurrentTrack) {
                mediaExtractor.selectTrack(i);
                try {
                    int dstIndex = mediaMuxer.addTrack(format);
                    indexMap.put(i, dstIndex);
                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        int newSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                        bufferSize = newSize > bufferSize ? newSize : bufferSize;
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Unsupported format '" + mime + "'");
                    throw new IOException("MediaMuxer does not support " + mime);
                }
            }
        }

        if (bufferSize < 0) {
            bufferSize = DEFAULT_BUFFER_SIZE;
        }

        MediaMetadataRetriever retrieverSrc = new MediaMetadataRetriever();
        retrieverSrc.setDataSource(context, srcPath);
        String degreesString = retrieverSrc.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        if (degreesString != null) {
            int degrees = Integer.parseInt(degreesString);
            if (degrees >= 0) {
                mediaMuxer.setOrientationHint(degrees);
            }
        }

        if (start > 0) {
            mediaExtractor.seekTo(start, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
        }

        // Copy the samples from MediaExtractor to MediaMuxer. We will loop
        // for copying each sample and stop when we get to the end of the source
        // file or exceed the end time of the trimming.
        int offset = 0;
        int trackIndex = -1;
        ByteBuffer dstBuf = ByteBuffer.allocate(bufferSize);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        try {
            mediaMuxer.start();
            while (true) {
                bufferInfo.offset = offset;
                bufferInfo.size = mediaExtractor.readSampleData(dstBuf, offset);
                if (bufferInfo.size < 0) {
                    Log.d(TAG, "Saw input EOS.");
                    bufferInfo.size = 0;
                    break;
                } else {
                    bufferInfo.presentationTimeUs = mediaExtractor.getSampleTime();
                    if (end > 0 && bufferInfo.presentationTimeUs > end) {
                        Log.d(TAG, "The current sample is over the trim end time.");
                        break;
                    } else {
                        bufferInfo.flags = mediaExtractor.getSampleFlags();
                        trackIndex = mediaExtractor.getSampleTrackIndex();

                        mediaMuxer.writeSampleData(indexMap.get(trackIndex), dstBuf,
                                bufferInfo);
                        mediaExtractor.advance();
                    }
                }
            }

            mediaMuxer.stop();
        } catch (IllegalStateException e) {
            // Swallow the exception due to malformed source.
            Log.w(TAG, "The source video file is malformed");
            File f = new File(dstPath);
            if (f.exists()) {
                f.delete();
            }
            throw e;
        } finally {
            mediaMuxer.release();
        }
        return;
    }

    public boolean decodeVideo(Context context, Uri inputFile, String outputFile, long clipPoint, long clipDuration) throws FileNotFoundException {
        //MediaMetadataRetriever
        int videoTrackIndex = -1;
        int audioTrackIndex = -1;
        int videoMaxInputSize = 0;
        int audioMaxInputSize = 0;
        int sourceVTrack = 0;
        int sourceATrack = 0;
        long videoDuration, audioDuration;
        Uri outputUri;
        //String videoPhoto = inputFile.substring(0, inputFile.lastIndexOf(".")) + "_photo.png";
        //saveBitmap(videoPhoto, getVideoThumbnail(url, 180, 180, MediaStore.Video.Thumbnails.MINI_KIND));
        Log.i(TAG, "decodeVideo entry");
        File tempFile;
        try {
            tempFile = File.createTempFile(outputFile, null, context.getCacheDir());
        } catch (IOException e) {
            tempFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DCIM), "/" + outputFile);
        }
        mOutputPath = tempFile.toString();

        Log.d(TAG, "output file is " + mOutputPath);

        ContentResolver contentResolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, outputFile);
        contentValues.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM");
        contentValues.put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis());
        outputUri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
        mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource(context, inputFile, null);
            mediaMuxer = new MediaMuxer(mOutputPath, OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (Exception e) {
            Log.e(TAG, "error path" + e.getMessage());
            return false;
        }
        for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {
            try {
                mediaFormat = mediaExtractor.getTrackFormat(i);
                mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    Log.i(TAG, "video entry");
                    for (String x : key) {
                        if (mediaFormat.containsKey(x))
                            Log.d(TAG, x + " is exist");
                    }
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
//                    Log.d(TAG, "width and height is " + width + " " + height
//                                    + ";maxInputSize is " + videoMaxInputSize
//                                    + ";duration is " + videoDuration
//                    );
                    videoTrackIndex = mediaMuxer.addTrack(mediaFormat);
                }
                else if (mime.startsWith("audio/")) {
                    Log.i(TAG, "audio entry");
                    for (String x : key) {
                        if (mediaFormat.containsKey(x))
                            Log.d(TAG, x + " is exist");
                    }
                    sourceATrack = i;
                    int sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    int channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    audioMaxInputSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                    audioDuration = mediaFormat.getLong(MediaFormat.KEY_DURATION);
//                    Log.d(TAG, "sampleRate is " + sampleRate
//                                    + ";channelCount is " + channelCount
//                                    + ";audioMaxInputSize is " + audioMaxInputSize
//                                    + ";audioDuration is " + audioDuration
//                    );
                    audioTrackIndex = mediaMuxer.addTrack(mediaFormat);
                }
            } catch (Exception e) {
                Log.e(TAG, " read error " + e.getMessage());
            }
        }

        MediaMetadataRetriever retrieverSrc = new MediaMetadataRetriever();
        retrieverSrc.setDataSource(context, inputFile);
        String degreesString = retrieverSrc.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        if (degreesString != null) {
            int degrees = Integer.parseInt(degreesString);
            if (degrees >= 0) {
                mediaMuxer.setOrientationHint(degrees);
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
            if (mediaExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
                mediaExtractor.advance();
                Log.i(TAG, "skip first I frame");
            }
            mediaExtractor.readSampleData(inputBuffer, 0);
            long firstVideoPTS = mediaExtractor.getSampleTime();
            mediaExtractor.advance();
            mediaExtractor.readSampleData(inputBuffer, 0);
            long SecondVideoPTS = mediaExtractor.getSampleTime();
            videoSampleTime = Math.abs(SecondVideoPTS - firstVideoPTS);
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
            //long presentationTimeUs = mediaExtractor.getSampleTime();
            videoInfo.presentationTimeUs = mediaExtractor.getSampleTime();
            int sampleFlag = mediaExtractor.getSampleFlags();
            if (sampleFlag == 1) {
//                String IFramePhoto = url.substring(0, url.lastIndexOf(".")) + "_" + IFrameCount + ".png";
//                saveBitmap(IFramePhoto, bitmap);
//                IFrameCount++;
            }
//            Log.d(TAG, "trackIndex is " + trackIndex
//                    + ";presentationTimeUs is " + presentationTimeUs
//                    + ";sampleFlag is " + sampleFlag
//                    + ";sampleSize is " + sampleSize);
            //if ((clipDuration != 0) && (presentationTimeUs > (clipPoint + clipDuration))) {
            if ((clipDuration != 0) && (videoInfo.presentationTimeUs > (clipPoint + clipDuration))) {
                mediaExtractor.unselectTrack(sourceVTrack);
                break;
            }
            //mediaExtractor.advance();
            videoInfo.offset = 0;
            videoInfo.size = sampleSize;
            videoInfo.flags = sampleFlag;
            mediaMuxer.writeSampleData(videoTrackIndex, inputBuffer, videoInfo);
            //videoInfo.presentationTimeUs += videoSampleTime;//presentationTimeUs;
            mediaExtractor.advance();
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
//            Log.d(TAG, "trackIndex is " + trackIndex
//                    + ";presentationTimeUs is " + presentationTimeUs);
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
        BufferedInputStream inputStream = null;
        OutputStream os = context.getContentResolver().openOutputStream(outputUri);
        byte[] buffer = new byte[1024];
        int len;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(mOutputPath));
            Log.i(TAG, "try to read");
            while ((len = inputStream.read(buffer)) != -1) {
                os.write(buffer, 0, len);
                //total += len;
            }
            os.flush();
            inputStream.close();
            os.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!tempFile.delete()) {
            if(context.deleteFile(tempFile.toString()))
                Log.i(TAG,  "delete temp file " + tempFile + " at DCIM dir");
        } else {
            Log.i(TAG,  "delete temp file " + tempFile + " at cache dir");
        }
        return true;
    }

    private Bitmap getImageThumbnail(String imagePath, int width, int height) {
        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        bitmap = BitmapFactory.decodeFile(imagePath, options);
        options.inJustDecodeBounds = false; // 设为 false
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
        bitmap = BitmapFactory.decodeFile(imagePath, options);
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height,
                ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        return bitmap;
    }

    private Bitmap getVideoThumbnail(String videoPath, int width, int height,
                                     int kind) {
        Bitmap bitmap = null;
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
