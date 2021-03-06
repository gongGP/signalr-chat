package net.pingfang.signalr.chat.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;

/**
 * Created by gongguopei87@gmail.com on 2015/8/5.
 */
public class MediaFileUtils {

    public static String createFilePath(Context context, String type, String albumName, String fileExtension) {
        Calendar c = Calendar.getInstance();
        File fileDir = FileUtil.getAlbumStorageDir(context, type, albumName);
        StringBuilder stringBuilder = new StringBuilder();
        if(!TextUtils.isEmpty(type)) {
            if(type.equals(Environment.DIRECTORY_PICTURES)) {
                stringBuilder.append(GlobalApplication.IMAGE_TITLE_NAME_PREFIX);
            } else if(type.equals(Environment.DIRECTORY_MUSIC)) {
                stringBuilder.append(GlobalApplication.VOICE_FILE_NAME_PREFIX);
            }

            stringBuilder.append(c.get(Calendar.YEAR));
            stringBuilder.append(c.get(Calendar.MONTH) + 1);
            stringBuilder.append(c.get(Calendar.DAY_OF_MONTH));
            stringBuilder.append(c.get(Calendar.HOUR_OF_DAY));
            stringBuilder.append(c.get(Calendar.MINUTE));
            stringBuilder.append(c.get(Calendar.MILLISECOND));

            if(type.equals(Environment.DIRECTORY_PICTURES)) {
                stringBuilder.append(".");
                stringBuilder.append(fileExtension);
            } else if(type.equals(Environment.DIRECTORY_MUSIC)) {
                stringBuilder.append(GlobalApplication.VOICE_FILE_NAME_SUFFIX);
            }

            String fileName = stringBuilder.toString();

            File file = new File(fileDir,fileName);
            if(!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
            return file.toString();
        }

        return  null;
    }

    public static String getFileExtension(String filePath) {
        if(!TextUtils.isEmpty(filePath)) {
            int lastIndex = filePath.lastIndexOf(".");
            return filePath.substring(lastIndex + 1);
        }
        return null;
    }


    public static String processReceiveFile(Context context, String fileBody, String fileType,String fileExtension) {
        try {
            String filePath = null;
            if(!TextUtils.isEmpty(fileType) && !TextUtils.isEmpty(fileExtension)) {
                if(fileType.equals("IMAGE")) {
                    filePath = createFilePath(context, Environment.DIRECTORY_PICTURES, "pic", fileExtension);
                } else if(fileType.equals("AUDIO")) {
                    filePath = createFilePath(context, Environment.DIRECTORY_MUSIC, "voice", GlobalApplication.VOICE_FILE_NAME_SUFFIX);
                }

                File file;
                if(filePath != null && !TextUtils.isEmpty(filePath)) {
                    file = new File(filePath);
                    if(!file.exists()) {
                        file.createNewFile();
                    }

                    byte[] bitmapArray = Base64.decode(fileBody, Base64.DEFAULT);

                    if(file != null) {
                        FileUtils.writeByteArrayToFile(file, bitmapArray);
                    } else {
                        Log.e("MediaFileUtils", "file not created!");
                    }

                } else {
                    Log.e("MediaFileUtils", "file path error");
                }

                return filePath;
            } else {
                Log.e("MediaFileUtils", "file type error");
                return null;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e("MediaFileUtils", "file not found errors");
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("MediaFileUtils", "io error");
            return null;
        }
    }



    public static Bitmap decodeBitmapFromPath(String filePath,int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath,options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath,options);

    }

    public static Bitmap decodeBitmapFromRes(Context context,int resId,int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(), resId,options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(context.getResources(), resId,options);
    }


    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dpToPx(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dp(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }
}
