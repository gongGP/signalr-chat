package net.pingfang.signalr.chat.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import net.pingfang.signalr.chat.R;
import net.pingfang.signalr.chat.database.AppContract;
import net.pingfang.signalr.chat.database.AppDbHelper;

import java.util.HashMap;

/**
 * Created by gongguopei87@gmail.com on 2015/9/28.
 */
public class ChatAppProvider extends ContentProvider {

    AppDbHelper dbHelper;

    // Uri工具类
    private static final UriMatcher sUriMatcher;
    // 查询、更新条件
    private static final int USER = 1;
    private static final int USER_COLUMN_ID = 2;
    private static final int MESSAGE = 3;
    private static final int MESSAGE_COLUMN_ID = 4;

    // 查询列集合
    private static HashMap<String, String> userProjectionMap;
    private static HashMap<String, String> messageProjectionMap;

    static {
        // Uri匹配工具类
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AppContract.AUTHORITY, "user", USER);
        sUriMatcher.addURI(AppContract.AUTHORITY, "user/#", USER_COLUMN_ID);
        sUriMatcher.addURI(AppContract.AUTHORITY, "message", MESSAGE);
        sUriMatcher.addURI(AppContract.AUTHORITY, "message/#", MESSAGE_COLUMN_ID);

        // 实例化查询列集合
        userProjectionMap = new HashMap<String, String>();
        // 添加查询列
        userProjectionMap.put(AppContract.UserEntry._ID, AppContract.UserEntry._ID);
        userProjectionMap.put(AppContract.UserEntry.COLUMN_NAME_ENTRY_UID, AppContract.UserEntry.COLUMN_NAME_ENTRY_UID);
        userProjectionMap.put(AppContract.UserEntry.COLUMN_NAME_NICK_NAME, AppContract.UserEntry.COLUMN_NAME_NICK_NAME);
        userProjectionMap.put(AppContract.UserEntry.COLUMN_NAME_PORTRAIT, AppContract.UserEntry.COLUMN_NAME_PORTRAIT);
        userProjectionMap.put(AppContract.UserEntry.COLUMN_NAME_STATUS,AppContract.UserEntry.COLUMN_NAME_STATUS);

        messageProjectionMap = new HashMap<String, String>();
        messageProjectionMap.put(AppContract.ChatMessageEntry._ID, AppContract.ChatMessageEntry._ID);
        messageProjectionMap.put(AppContract.ChatMessageEntry.COLUMN_NAME_ENTRY_M_FROM, AppContract.ChatMessageEntry.COLUMN_NAME_ENTRY_M_FROM);
        messageProjectionMap.put(AppContract.ChatMessageEntry.COLUMN_NAME_NICK_M_TO, AppContract.ChatMessageEntry.COLUMN_NAME_NICK_M_TO);
        messageProjectionMap.put(AppContract.ChatMessageEntry.COLUMN_NAME_M_TYPE, AppContract.ChatMessageEntry.COLUMN_NAME_M_TYPE);
        messageProjectionMap.put(AppContract.ChatMessageEntry.COLUMN_NAME_M_CONTENT_TYPE, AppContract.ChatMessageEntry.COLUMN_NAME_M_CONTENT_TYPE);
        messageProjectionMap.put(AppContract.ChatMessageEntry.COLUMN_NAME_M_CONTENT, AppContract.ChatMessageEntry.COLUMN_NAME_M_CONTENT);
        messageProjectionMap.put(AppContract.ChatMessageEntry.COLUMN_NAME_M_DATETIME, AppContract.ChatMessageEntry.COLUMN_NAME_M_DATETIME);
        messageProjectionMap.put(AppContract.ChatMessageEntry.COLUMN_NAME_M_STATUS, AppContract.ChatMessageEntry.COLUMN_NAME_M_STATUS);
    }

    @Override
    public boolean onCreate() {
        dbHelper = new AppDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {
            case USER:
                qb.setTables(AppContract.UserEntry.TABLE_NAME);
                qb.setProjectionMap(userProjectionMap);
                break;
            // 根据ID查询
            case USER_COLUMN_ID:
                qb.setTables(AppContract.UserEntry.TABLE_NAME);
                qb.setProjectionMap(userProjectionMap);
                qb.appendWhere(AppContract.UserEntry._ID + "=" + uri.getPathSegments().get(1));
                break;
            case MESSAGE:
                qb.setTables(AppContract.ChatMessageEntry.TABLE_NAME);
                qb.setProjectionMap(messageProjectionMap);
                break;
            case MESSAGE_COLUMN_ID:
                qb.setTables(AppContract.ChatMessageEntry.TABLE_NAME);
                qb.setProjectionMap(messageProjectionMap);
                qb.appendWhere(AppContract.ChatMessageEntry._ID + "=" + uri.getPathSegments().get(1));
                break;
            default:
                throw new IllegalArgumentException(getContext().getString(R.string.illegal_uri_exception)  + " = " + uri);
        }

        // 使用默认排序
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = AppContract.UserEntry.DEFAULT_SORT_ORDER;
        } else {
            orderBy = sortOrder;
        }

        // 获得数据库实例
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        // 返回游标集合
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // 获得数据库实例
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowId = -1;
        switch (sUriMatcher.match(uri)) {
            case USER:
                rowId = db.insert(AppContract.UserEntry.TABLE_NAME, AppContract.UserEntry.COLUMN_NAME_ENTRY_UID, values);
                if(rowId > 0) {
                    Uri userUri = ContentUris.withAppendedId(AppContract.UserEntry.CONTENT_URI,rowId);
                    getContext().getContentResolver().notifyChange(userUri, null);
                    return userUri;
                }
                break;
            case MESSAGE:
                rowId = db.insert(AppContract.ChatMessageEntry.TABLE_NAME, AppContract.ChatMessageEntry.COLUMN_NAME_ENTRY_M_FROM, values);
                if(rowId > 0) {
                    Uri userUri = ContentUris.withAppendedId(AppContract.ChatMessageEntry.CONTENT_URI,rowId);
                    getContext().getContentResolver().notifyChange(userUri, null);
                    return userUri;
                }
                break;
        }
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // 获得数据库实例
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // 获得数据库实例
        int count;
        switch (sUriMatcher.match(uri)) {
            // 根据指定条件删除
            case USER:
                count = db.delete(AppContract.UserEntry.TABLE_NAME, selection, selectionArgs);
                break;
            // 根据指定条件和ID删除
            case USER_COLUMN_ID:
                String userColumnId = uri.getPathSegments().get(1);
                count = db.delete(AppContract.UserEntry.TABLE_NAME, AppContract.UserEntry._ID + "=" + userColumnId
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;
            case MESSAGE:
                count = db.delete(AppContract.ChatMessageEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case MESSAGE_COLUMN_ID:
                String messageColumnId = uri.getPathSegments().get(1);
                count = db.delete(AppContract.ChatMessageEntry.TABLE_NAME, AppContract.ChatMessageEntry._ID + "=" + messageColumnId
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException(getContext().getString(R.string.illegal_uri_exception)  + " = " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // 获得数据库实例
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            // 根据指定条件更新
            case USER:
                count = db.update(AppContract.UserEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            // 根据指定条件和ID更新
            case USER_COLUMN_ID:
                String userColumnId = uri.getPathSegments().get(1);
                count = db.update(AppContract.UserEntry.TABLE_NAME, values, AppContract.UserEntry._ID + "=" + userColumnId
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;
            case MESSAGE:
                count = db.update(AppContract.ChatMessageEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case MESSAGE_COLUMN_ID:
                String messageColumnId = uri.getPathSegments().get(1);
                count = db.update(AppContract.ChatMessageEntry.TABLE_NAME, values, AppContract.ChatMessageEntry._ID + "=" + messageColumnId
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException(getContext().getString(R.string.illegal_uri_exception)  + " = " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}
