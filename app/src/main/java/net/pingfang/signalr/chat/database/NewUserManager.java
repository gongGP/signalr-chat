package net.pingfang.signalr.chat.database;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * Created by gongguopei87@gmail.com on 2015/10/10.
 */
public class NewUserManager {

    Context context;

    public NewUserManager(Context context) {
        this.context = context;
    }

    public Uri insert(String uid, String nickname, String portrait) {
        return insert(uid,nickname,portrait,0);
    }

    public Uri insert(String uid, String nickname, String portrait, int status) {
        ContentResolver contentResolver = context.getContentResolver();

        ContentValues values = new ContentValues();

        values.put(AppContract.UserEntry.COLUMN_NAME_ENTRY_UID,uid);
        values.put(AppContract.UserEntry.COLUMN_NAME_NICK_NAME,nickname);
        values.put(AppContract.UserEntry.COLUMN_NAME_PORTRAIT,portrait);
        values.put(AppContract.UserEntry.COLUMN_NAME_STATUS, status);

        Uri uri = contentResolver.insert(AppContract.UserEntry.CONTENT_URI, values);
        return uri;
    }

    public Cursor queryByUid(String uid) {
        String selection = AppContract.UserEntry.COLUMN_NAME_ENTRY_UID + " = ?";
        String[] selectionArgs = new String[]{uid};

        ContentResolver contentResolver = context.getContentResolver();

        Cursor cursor = context.getContentResolver().query(AppContract.UserEntry.CONTENT_URI, null, selection, selectionArgs, null);

        return cursor;
    }

    public int update(String uid, String nickname, String portrait) {

        ContentValues values = new ContentValues();
        values.put(AppContract.UserEntry.COLUMN_NAME_NICK_NAME,nickname);
        values.put(AppContract.UserEntry.COLUMN_NAME_PORTRAIT,portrait);

        String selection = AppContract.UserEntry.COLUMN_NAME_ENTRY_UID + " = ?";
        String[] selectionArgs = new String[]{uid};

        ContentResolver contentResolver = context.getContentResolver();

        int count = contentResolver.update(AppContract.UserEntry.CONTENT_URI, values, selection, selectionArgs);

        return count;
    }

    public int updateStatus(String uid, int status) {

        ContentValues values = new ContentValues();
        values.put(AppContract.UserEntry.COLUMN_NAME_STATUS,status);

        String selection = AppContract.UserEntry.COLUMN_NAME_ENTRY_UID + " = ?";
        String[] selectionArgs = new String[]{uid};

        ContentResolver contentResolver = context.getContentResolver();

        int count = contentResolver.update(AppContract.UserEntry.CONTENT_URI, values, selection, selectionArgs);

        return count;
    }

    public boolean isExist(String uid) {
        Cursor cursor = queryByUid(uid);
        if(cursor != null && cursor.getCount() > 0) {
            return true;
        }

        return false;
    }

    public void addRecord(String uid, String nickname, String portrait) {
        if(isExist(uid)) {
            update(uid,nickname,portrait);
        } else {
            insert(uid,nickname,portrait);
        }
    }
}