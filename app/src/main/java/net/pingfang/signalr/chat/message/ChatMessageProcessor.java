package net.pingfang.signalr.chat.message;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import net.pingfang.signalr.chat.R;
import net.pingfang.signalr.chat.constant.app.AppConstants;
import net.pingfang.signalr.chat.database.AppContract;
import net.pingfang.signalr.chat.database.ChatMessageManager;
import net.pingfang.signalr.chat.database.UserManager;
import net.pingfang.signalr.chat.util.CommonTools;
import net.pingfang.signalr.chat.util.MediaFileUtils;
import net.pingfang.signalr.chat.util.SharedPreferencesHelper;
import net.pingfang.signalr.chat.util.GlobalApplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by gongguopei87@gmail.com on 2015/9/25.
 */
public class ChatMessageProcessor implements ChatMessageListener {

    Context context;

    public ChatMessageProcessor(Context context) {
        this.context = context;
    }

    @Override
    public void onMessageReceive(String messageType, String message) {
        new ProcessMessageTask().execute(messageType, message);
    }

    public void onSendMessage(String messgeType, String messageBody) {
        new ProcessSendMessageTask().execute(messgeType,messageBody);
    }


    /**
     * 实现获取用户在线列表
     * @param message
     */
    private void processOnlineList(String message) {

        String selection = AppContract.UserEntry.COLUMN_NAME_ENTRY_UID + " = ?";

        JSONArray jsonArray;

        try {
            jsonArray = new JSONArray(message);
            for(int i = 0; i < jsonArray.length(); i++) {
                JSONObject object = jsonArray.getJSONObject(i);
                String uid = object.getString("UserId");
                ContentValues values = new ContentValues();
                values.put(AppContract.UserEntry.COLUMN_NAME_STATUS,1);
                context.getContentResolver().update(AppContract.UserEntry.CONTENT_URI,
                        values,
                        selection,
                        new String[]{uid});
            }
            Intent intent = new Intent();
            intent.setAction(GlobalApplication.ACTION_INTENT_UPDATE_ONLINE_LIST);
            context.sendBroadcast(intent);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /** 用户上下线相关状态更新
     * @param message 用户相关信息封装
     * @param  status 用户上下线状态标志
     * **/
    private void updateUserStatus(String message,int status) {
        String selection = AppContract.UserEntry.COLUMN_NAME_ENTRY_UID + " = ?";
        JSONObject object;
        ContentValues values = new ContentValues();
        try {
            object = new JSONObject(message);
            String uid = object.getString("UserId");
            if(uid != null && !TextUtils.isEmpty(uid) && !uid.equals("0")) {
                UserManager userManager = new UserManager(context);
                boolean isExist = userManager.isExist(uid);
                values.put(AppContract.UserEntry.COLUMN_NAME_STATUS,status);
                if(isExist) {
                    context.getContentResolver().update(AppContract.UserEntry.CONTENT_URI,
                            values,
                            selection,
                            new String[]{uid});
                } else {
                    if(status == 1) {
                        String nickname = object.getString("NickName");
                        String portrait = object.getString("HeadPortrait");
                        values.put(AppContract.UserEntry.COLUMN_NAME_ENTRY_UID,uid);
                        values.put(AppContract.UserEntry.COLUMN_NAME_NICK_NAME,nickname);

                        if(portrait != null && !TextUtils.isEmpty(portrait) && !"null".equals(portrait)) {
                            values.put(AppContract.UserEntry.COLUMN_NAME_PORTRAIT,portrait);
                        } else {
                            values.put(AppContract.UserEntry.COLUMN_NAME_PORTRAIT, "");
                        }
                        context.getContentResolver().insert(AppContract.UserEntry.CONTENT_URI, values);
                    }
                }

                Intent intent = new Intent();
                intent.setAction(GlobalApplication.ACTION_INTENT_UPDATE_ONLINE_LIST);
                context.sendBroadcast(intent);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /** 用户退出应用状态更新 **/
    private void exitApp() {
        ContentValues values = new ContentValues();
        values.put(AppContract.UserEntry.COLUMN_NAME_STATUS,0);
        context.getContentResolver().update(AppContract.UserEntry.CONTENT_URI, values, null, null);
    }

    private void processOnlineMessage(String message,boolean direction) {
        JSONObject object;
        try {
            object = new JSONObject(message);
            String from = object.getString("Sender");
            String to = object.getString("Receiver");
            String buddy = from;
            String owner = to;
            int status = MessageConstant.MESSAGE_STATUS_NOT_READ;
            if(direction) {
                buddy = to;
                owner = from;
                status = MessageConstant.MESSAGE_STATUS_READ;
            }
            String fromNickname = object.getString("SenderName");
            String fromPortrait = object.getString("SenderPortrait");
            String content = object.getString("Contents");
            String datetime = object.getString("SendTime");
            String contentType = object.getString("MessageType");

            if(!direction) {
                UserManager userManager = new UserManager(context);
                Cursor cursor = userManager.queryByUid(from);
                if(cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                    fromNickname = cursor.getString(cursor.getColumnIndex(AppContract.UserEntry.COLUMN_NAME_NICK_NAME));
                    fromPortrait = cursor.getString(cursor.getColumnIndex(AppContract.UserEntry.COLUMN_NAME_PORTRAIT));
                    cursor.close();
                } else {
                    userManager.addRecord(from,fromNickname,fromPortrait);
                }
            }


            ChatMessageManager chatMessageManager = new ChatMessageManager(context);
            Uri messageUri = null;
            if(!TextUtils.isEmpty(contentType)) {
                ContentValues values = new ContentValues();
                if(contentType.equals("Text")) {
                   messageUri = chatMessageManager.insert(from, to, owner, MessageConstant.MESSAGE_TYPE_ON_LINE,
                            contentType, content, datetime, status);
                    values.put(AppContract.RecentContactEntry.COLUMN_NAME_CONTENT, content);
                } else if(contentType.equals("Picture")){
                    String fileExtension = object.getString("fileExtension");
                    String filePath = MediaFileUtils.processReceiveFile(context, content,
                            MessageConstant.MESSAGE_FILE_TYPE_IMG, fileExtension);
                    messageUri = chatMessageManager.insert(from, to, owner, MessageConstant.MESSAGE_TYPE_ON_LINE,
                            contentType, filePath, datetime, status);
                    values.put(AppContract.RecentContactEntry.COLUMN_NAME_CONTENT, context.getResources().getString(R.string.content_type_pic));
                } else if(contentType.equals("Audio")) {
                    String fileExtension = object.getString("fileExtension");
                    String filePath = MediaFileUtils.processReceiveFile(context, content,
                            MessageConstant.MESSAGE_FILE_TYPE_AUDIO, fileExtension);
                    messageUri = chatMessageManager.insert(from, to, owner, MessageConstant.MESSAGE_TYPE_ON_LINE,
                            contentType, filePath, datetime, status);
                    values.put(AppContract.RecentContactEntry.COLUMN_NAME_CONTENT, context.getResources().getString(R.string.content_type_voice));
                }

                if(messageUri != null) {

                    String selection =
                            AppContract.RecentContactEntry.COLUMN_NAME_BUDDY + " = ? " +
                                    "AND " +
                                    AppContract.RecentContactEntry.COLUMN_NAME_OWNER + " = ?";
                    String[] selectionArgs = new String[]{buddy,owner};

                    Cursor newCursor = context.getContentResolver().query(AppContract.RecentContactEntry.CONTENT_URI,
                            null, selection, selectionArgs, null);

                    if(newCursor != null && newCursor.getCount() > 0 && newCursor.moveToFirst()){
                        int rowId = newCursor.getInt(newCursor.getColumnIndex(AppContract.RecentContactEntry._ID));
                        Uri appendUri = Uri.withAppendedPath(AppContract.RecentContactEntry.CONTENT_URI, Integer.toString(rowId));

                        values.put(AppContract.RecentContactEntry.COLUMN_NAME_UPDATE_TIME, datetime);
                        context.getContentResolver().update(appendUri, values, null, null);

                        newCursor.close();
                    } else {
                        values.put(AppContract.RecentContactEntry.COLUMN_NAME_BUDDY,buddy);
                        values.put(AppContract.RecentContactEntry.COLUMN_NAME_UPDATE_TIME, datetime);
                        values.put(AppContract.RecentContactEntry.COLUMN_NAME_OWNER,owner);
                        values.put(AppContract.RecentContactEntry.COLUMN_NAME_COUNT,0);
                        context.getContentResolver().insert(AppContract.RecentContactEntry.CONTENT_URI,values);
                    }

                    if(!direction) {
                        Bundle args =  new Bundle();
                        args.putParcelable("messageUri", messageUri);
                        args.putString("fromUid",buddy);

                        Intent intent = new Intent();
                        intent.setAction(GlobalApplication.ACTION_INTENT_ONLINE_MESSAGE_INCOMING);
                        intent.putExtra("message", args);
                        context.sendBroadcast(intent);
                    } else {
                        Bundle args =  new Bundle();
                        args.putParcelable("messageUri", messageUri);
                        args.putString("fromUid",buddy);

                        Intent intent = new Intent();
                        intent.setAction(GlobalApplication.ACTION_INTENT_ONLINE_MESSAGE_SEND);
                        intent.putExtra("message", args);
                        context.sendBroadcast(intent);
                    }

                }

            }
//            args.putString("fromUid",from);
//            args.putString("nameFrom", fromNickname);
//            args.putString("content", content);
//            args.putString("datetime", datetime);
//            args.putString("messageType", contentType);

//            Intent intent = new Intent();
//            if(!TextUtils.isEmpty(contentType)) {
//                if(contentType.equals("Text")) {
//                    intent.setAction(GlobalApplication.ACTION_INTENT_TEXT_MESSAGE_INCOMING);
//                } else if(contentType.equals("Picture")){
//                    intent.setAction(GlobalApplication.ACTION_INTENT_IMAGE_MESSAGE_INCOMING);
//                    String fileExtension = object.getString("fileExtension");
//                    args.putString("fileExtension", fileExtension);
//                } else if(contentType.equals("Audio")) {
//                    intent.setAction(GlobalApplication.ACTION_INTENT_VOICE_MESSAGE_INCOMING);
//                    String fileExtension = object.getString("fileExtension");
//                    args.putString("fileExtension", fileExtension);
//                }
//            }
//            intent.putExtra("message", args);
//            context.sendBroadcast(intent);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void processOfflineMsgShort(String message) {
        SharedPreferencesHelper sharedPreferencesHelper = SharedPreferencesHelper.newInstance(context);
        JSONArray jsonArray;
        try {
            jsonArray = new JSONArray(message);
            for(int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String fromUid = jsonObject.getString("Sender");
                int count = jsonObject.getInt("Count");
                String toUid = sharedPreferencesHelper.getStringValue(AppConstants.KEY_SYS_CURRENT_UID);

                String selection =
                        AppContract.RecentContactEntry.COLUMN_NAME_BUDDY + " = ? " +
                                "AND " +
                                AppContract.RecentContactEntry.COLUMN_NAME_OWNER + " = ?";
                Cursor newCursor = context.getContentResolver().query(AppContract.RecentContactEntry.CONTENT_URI,
                        null, selection, new String[]{fromUid,toUid}, null);

                ContentValues values = new ContentValues();
                values.put(AppContract.RecentContactEntry.COLUMN_NAME_CONTENT,context.getResources().getString(R.string.content_new_offline_msg));
                if(newCursor != null && newCursor.getCount() > 0 && newCursor.moveToFirst()){
                    int rowId = newCursor.getInt(newCursor.getColumnIndex(AppContract.RecentContactEntry._ID));
                    Uri appendUri = Uri.withAppendedPath(AppContract.RecentContactEntry.CONTENT_URI, Integer.toString(rowId));

                    int currentCount = newCursor.getInt(newCursor.getColumnIndex(AppContract.RecentContactEntry.COLUMN_NAME_COUNT));

                    values.put(AppContract.RecentContactEntry.COLUMN_NAME_UPDATE_TIME, CommonTools.TimeConvertString());
                    values.put(AppContract.RecentContactEntry.COLUMN_NAME_COUNT,(currentCount + count));
                    context.getContentResolver().update(appendUri, values, null, null);

                    newCursor.close();
                } else {
                    values.put(AppContract.RecentContactEntry.COLUMN_NAME_BUDDY,fromUid);
                    values.put(AppContract.RecentContactEntry.COLUMN_NAME_UPDATE_TIME, CommonTools.TimeConvertString());
                    values.put(AppContract.RecentContactEntry.COLUMN_NAME_OWNER,toUid);
                    values.put(AppContract.RecentContactEntry.COLUMN_NAME_COUNT,count);
                    context.getContentResolver().insert(AppContract.RecentContactEntry.CONTENT_URI,values);
                }
            }

            Intent intent = new Intent();
            intent.setAction(GlobalApplication.ACTION_INTENT_OFFLINE_USER_LIST_INCOMING);
            context.sendBroadcast(intent);
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    private void processOfflineMessageList(String message) {
        try {
            JSONArray jsonArray = new JSONArray(message);
            ArrayList<Uri> uriArrayList = new ArrayList<>();
            for(int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String fromUid = jsonObject.getString("Sender");
                String to = jsonObject.getString("Receiver");
                String owner = to;
                String contentType = jsonObject.getString("MessageType");
                String content = jsonObject.getString("Contents");
                String datetime = jsonObject.getString("SendTime");

                // 消息存储
                ChatMessageManager chatMessageManager = new ChatMessageManager(context);
                Uri messageUri = null;
                ContentValues values = new ContentValues();
                if(!TextUtils.isEmpty(contentType)) {
                    if(contentType.equals("Text")) {
                        messageUri = chatMessageManager.insert(fromUid, to, owner, MessageConstant.MESSAGE_TYPE_OFF_LINE,
                                contentType, content, datetime, MessageConstant.MESSAGE_STATUS_READ);
                        values.put(AppContract.RecentContactEntry.COLUMN_NAME_CONTENT, content);
                    } else if(contentType.equals("Picture")){
                        String fileExtension = "png";
                        String filePath = MediaFileUtils.processReceiveFile(context, content,
                                MessageConstant.MESSAGE_FILE_TYPE_IMG, fileExtension);
                        messageUri = chatMessageManager.insert(fromUid, to, owner, MessageConstant.MESSAGE_TYPE_OFF_LINE,
                                contentType, filePath, datetime, MessageConstant.MESSAGE_STATUS_READ);
                        values.put(AppContract.RecentContactEntry.COLUMN_NAME_CONTENT, context.getResources().getString(R.string.content_type_pic));
                    } else if(contentType.equals("Audio")) {
                        String fileExtension = GlobalApplication.VOICE_FILE_NAME_SUFFIX;
                        String filePath = MediaFileUtils.processReceiveFile(context, content,
                                MessageConstant.MESSAGE_FILE_TYPE_AUDIO, fileExtension);
                        messageUri = chatMessageManager.insert(fromUid, to, owner, MessageConstant.MESSAGE_TYPE_OFF_LINE,
                                contentType, filePath, datetime, MessageConstant.MESSAGE_STATUS_READ);
                        values.put(AppContract.RecentContactEntry.COLUMN_NAME_CONTENT, context.getResources().getString(R.string.content_type_voice));
                    }
                }

                // 消息查找
                if(messageUri != null) {
                    uriArrayList.add(messageUri);

                    String selection =
                            AppContract.RecentContactEntry.COLUMN_NAME_BUDDY + " = ? " +
                                    "AND " +
                                    AppContract.RecentContactEntry.COLUMN_NAME_OWNER + " = ?";
                    Cursor newCursor = context.getContentResolver().query(AppContract.RecentContactEntry.CONTENT_URI,
                            null, selection, new String[]{fromUid,to}, null);

                    if(newCursor != null && newCursor.getCount() > 0 && newCursor.moveToFirst()){
                        int rowId = newCursor.getInt(newCursor.getColumnIndex(AppContract.RecentContactEntry._ID));
                        Uri appendUri = Uri.withAppendedPath(AppContract.RecentContactEntry.CONTENT_URI, Integer.toString(rowId));

                        int currentCount = newCursor.getInt(newCursor.getColumnIndex(AppContract.RecentContactEntry.COLUMN_NAME_COUNT));

                        values.put(AppContract.RecentContactEntry.COLUMN_NAME_UPDATE_TIME, datetime);
                        values.put(AppContract.RecentContactEntry.COLUMN_NAME_COUNT,(currentCount -1));

                        Log.d("ChatMessageProcessor", "processOfflineMessageList currentCount == " + currentCount);

                        context.getContentResolver().update(appendUri, values, null, null);

                        newCursor.close();
                    } else {
                        values.put(AppContract.RecentContactEntry.COLUMN_NAME_BUDDY,fromUid);
                        values.put(AppContract.RecentContactEntry.COLUMN_NAME_UPDATE_TIME, datetime);
                        values.put(AppContract.RecentContactEntry.COLUMN_NAME_OWNER,to);
                        values.put(AppContract.RecentContactEntry.COLUMN_NAME_COUNT,0);
                        context.getContentResolver().insert(AppContract.RecentContactEntry.CONTENT_URI,values);
                    }
                }
            }

            Intent intent = new Intent();
            intent.setAction(GlobalApplication.ACTION_INTENT_OFFLINE_MESSAGE_LIST_INCOMING);
            intent.putParcelableArrayListExtra("message", uriArrayList);
            context.sendBroadcast(intent);

        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    private void processBulkMessage(String message,boolean direction) {
        SharedPreferencesHelper helper = SharedPreferencesHelper.newInstance(context);
        JSONObject object;
        try {
            object = new JSONObject(message);
            String from = object.getString("Sender");
            String to = helper.getStringValue(AppConstants.KEY_SYS_CURRENT_UID);
            String owner = to;
            int status = MessageConstant.MESSAGE_STATUS_NOT_READ;
            if(direction) {
                status = MessageConstant.MESSAGE_STATUS_READ;
            }
//            String fromNickname = object.getString("SenderName");
//            String fromPortrait = object.getString("SenderPortrait");
            String content = object.getString("Contents");
            String datetime = object.getString("SendTime");
            String contentType = object.getString("MessageType");

//            if(!direction) {
//                UserManager userManager = new UserManager(context);
//                Cursor cursor = userManager.queryByUid(from);
//                if(cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
//                    fromNickname = cursor.getString(cursor.getColumnIndex(AppContract.UserEntry.COLUMN_NAME_NICK_NAME));
//                    fromPortrait = cursor.getString(cursor.getColumnIndex(AppContract.UserEntry.COLUMN_NAME_PORTRAIT));
//                    cursor.close();
//                } else {
//                    userManager.addRecord(from,fromNickname,fromPortrait);
//                }
//            }


            ChatMessageManager chatMessageManager = new ChatMessageManager(context);
            Uri messageUri = null;
            if(!TextUtils.isEmpty(contentType)) {
                ContentValues values = new ContentValues();
                if(contentType.equals("Text")) {
                    messageUri = chatMessageManager.insert(from, to, owner, MessageConstant.MESSAGE_TYPE_BULK,
                            contentType, content, datetime, status);
                    values.put(AppContract.RecentContactEntry.COLUMN_NAME_CONTENT, content);
                } else if(contentType.equals("Picture")){
                    String fileExtension = object.getString("fileExtension");
                    String filePath = MediaFileUtils.processReceiveFile(context, content,
                            MessageConstant.MESSAGE_FILE_TYPE_IMG, fileExtension);
                    messageUri = chatMessageManager.insert(from, to, owner, MessageConstant.MESSAGE_TYPE_BULK,
                            contentType, filePath, datetime, status);
                    values.put(AppContract.RecentContactEntry.COLUMN_NAME_CONTENT, context.getResources().getString(R.string.content_type_pic));
                } else if(contentType.equals("Audio")) {
                    String fileExtension = object.getString("fileExtension");
                    String filePath = MediaFileUtils.processReceiveFile(context, content,
                            MessageConstant.MESSAGE_FILE_TYPE_AUDIO, fileExtension);
                    messageUri = chatMessageManager.insert(from, to, owner, MessageConstant.MESSAGE_TYPE_BULK,
                            contentType, filePath, datetime, status);
                    values.put(AppContract.RecentContactEntry.COLUMN_NAME_CONTENT, context.getResources().getString(R.string.content_type_voice));
                }

                if(messageUri != null) {
                    if (!direction) {
                        Bundle args = new Bundle();
                        args.putParcelable("messageUri", messageUri);
                        args.putString("fromUid", from);

                        Intent intent = new Intent();
                        intent.setAction(GlobalApplication.ACTION_INTENT_BULK_MESSAGE_INCOMING);
                        intent.putExtra("message", args);
                        context.sendBroadcast(intent);
                    } else {
                        Bundle args = new Bundle();
                        args.putParcelable("messageUri", messageUri);
                        args.putString("fromUid", from);

                        Intent intent = new Intent();
                        intent.setAction(GlobalApplication.ACTION_INTENT_BULK_MESSAGE_SEND);
                        intent.putExtra("message", args);
                        context.sendBroadcast(intent);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    private void processShieldMsg(String message) {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(message);
            String owner = jsonObject.getString("UserId");
            String shield = jsonObject.getString("ShieldedObjectId");
            String selection =
                    AppContract.ShieldListView.COLUMN_NAME_UID + " = ? " +
                            "AND " +
                            AppContract.ShieldListView.COLUMN_NAME_OWNER + " = ?";

            String[] selectionArgs = new String[]{shield,owner};

            Cursor newCursor = context.getContentResolver().query(AppContract.ShieldListView.CONTENT_URI,
                    null, selection, selectionArgs, null);

            if(newCursor != null && newCursor.getCount() > 0) {
                return;
            } else {
                ContentValues values = new ContentValues();
                values.put(AppContract.ShieldEntry.COLUMN_NAME_SHIELD,shield);
                values.put(AppContract.ShieldEntry.COLUMN_NAME_OWNER,owner);
                context.getContentResolver().insert(AppContract.ShieldEntry.CONTENT_URI,values);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void processShieldListMsg(String message) {
        JSONArray jsonArray;
        try {
            jsonArray = new JSONArray(message);
            for(int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String owner = jsonObject.getString("UserId");
                String shield = jsonObject.getString("ShieldedObjectId");
                String selection =
                        AppContract.ShieldListView.COLUMN_NAME_UID + " = ? " +
                                "AND " +
                                AppContract.ShieldListView.COLUMN_NAME_OWNER + " = ?";

                String[] selectionArgs = new String[]{shield,owner};

                Cursor newCursor = context.getContentResolver().query(AppContract.ShieldListView.CONTENT_URI,
                        null, selection, selectionArgs, null);

                if(newCursor != null && newCursor.getCount() > 0) {
                    continue;
                } else {
                    ContentValues values = new ContentValues();
                    values.put(AppContract.ShieldEntry.COLUMN_NAME_SHIELD,shield);
                    values.put(AppContract.ShieldEntry.COLUMN_NAME_OWNER,owner);
                    context.getContentResolver().insert(AppContract.ShieldEntry.CONTENT_URI,values);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void processUnShieldMsg(String message) {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(message);
            String owner = jsonObject.getString("UserId");
            String unshield = jsonObject.getString("ShieldedObjectId");
            String selection =
                    AppContract.ShieldEntry.COLUMN_NAME_SHIELD + " = ? " +
                            "AND " +
                            AppContract.ShieldEntry.COLUMN_NAME_OWNER + " = ?";

            String[] selectionArgs = new String[]{unshield,owner};
            context.getContentResolver().delete(AppContract.ShieldEntry.CONTENT_URI,selection,selectionArgs);

            Intent intent = new Intent();
            intent.setAction(GlobalApplication.ACTION_INTENT_SHIELD_LIST_UPDATE);
            context.sendBroadcast(intent);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private class ProcessMessageTask extends AsyncTask<String,String,String> {
        @Override
        protected String doInBackground(String... params) {
                String messageType = params[0];
                String message = params[1];
            if(messageType.equals("OnlineList")) {
                processOnlineList(message);
            } else if(messageType.equals("Online")){
                updateUserStatus(message, 1);
            } else if(messageType.equals("Offline")) {
                updateUserStatus(message, 0);
            } else if(messageType.equals("exitApp")) {
                exitApp();
            } else if(messageType.equals("OnlineMsg")) {
                processOnlineMessage(message,false);
            } else if(messageType.equals("OfflineMsgShort")) {
                processOfflineMsgShort(message);
            } else if(messageType.equals("OfflineMsg")) {
                processOfflineMessageList(message);
            } else if(messageType.equals("Shield")) {
                processShieldMsg(message);
            } else if(messageType.equals("Shields")) {
                processShieldListMsg(message);
            } else if(messageType.equals("UnShield")) {
                processUnShieldMsg(message);
            } else if(messageType.equals("BulkMssaging")) {
                processBulkMessage(message, false);
            }

            return "ok";
        }
    }

    private class ProcessSendMessageTask extends AsyncTask<String,String,String> {
        @Override
        protected String doInBackground(String... params) {
            String messageType = params[0];
            String message = params[1];
            if(messageType.equals("OnlineMsg")) {
                processOnlineMessage(message,true);
            } else if(messageType.equals("BulkMssaging")) {
                processBulkMessage(message,true);
            }

            return "ok";
        }
    }

}
