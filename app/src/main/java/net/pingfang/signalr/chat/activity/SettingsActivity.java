package net.pingfang.signalr.chat.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.IntentCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.openapi.LogoutAPI;
import com.sina.weibo.sdk.utils.LogUtil;
import com.tencent.tauth.Tencent;

import net.pingfang.signalr.chat.R;
import net.pingfang.signalr.chat.constant.app.AppConstants;
import net.pingfang.signalr.chat.constant.qq.TencentConstants;
import net.pingfang.signalr.chat.constant.wechat.WxOauth2AccessToken;
import net.pingfang.signalr.chat.constant.weibo.WeiboRequestListener;
import net.pingfang.signalr.chat.service.ChatService;
import net.pingfang.signalr.chat.util.SharedPreferencesHelper;

import org.json.JSONException;
import org.json.JSONObject;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener{

    TextView btn_activity_back;
    //    TextView tv_settings_item_account;

    TextView tv_settings_item_change_pwd;
    TextView tv_settings_item_update_info;
    TextView tv_settings_item_about;
    TextView tv_settings_item_exit;

    SharedPreferencesHelper sharedPreferencesHelper;
    // qq 登录配置
    Tencent mTencent;
//    ChatService chatService;
    ChatService mService;
    boolean mBound = false;
    /**
     * Access Token 实例
     */
    private Oauth2AccessToken wbAccessToken;
    // 微信登录配置
    private WxOauth2AccessToken mWxOauth2AccessToken;
    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            ChatService.ChatBinder binder = (ChatService.ChatBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferencesHelper = SharedPreferencesHelper.newInstance(getApplicationContext());
        initView();
        initCommunicate();
    }

    private void initView() {
        btn_activity_back = (TextView) findViewById(R.id.btn_activity_back);
        btn_activity_back.setOnClickListener(this);

        //        tv_settings_item_account = (TextView) findViewById(R.id.tv_settings_item_account);
        //        tv_settings_item_account.setOnClickListener(this);
        tv_settings_item_change_pwd = (TextView) findViewById(R.id.tv_settings_item_change_pwd);
        tv_settings_item_change_pwd.setOnClickListener(this);
        tv_settings_item_update_info = (TextView) findViewById(R.id.tv_settings_item_update_info);
        tv_settings_item_update_info.setOnClickListener(this);

        tv_settings_item_about = (TextView) findViewById(R.id.tv_settings_item_about);
        tv_settings_item_about.setOnClickListener(this);
        tv_settings_item_exit = (TextView) findViewById(R.id.tv_settings_item_exit);
        tv_settings_item_exit.setOnClickListener(this);
    }

    private void initCommunicate() {
        if(!mBound) {
            Intent intent = new Intent(this, ChatService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        switch (viewId) {
            case R.id.btn_activity_back:
                navigateUp();
                break;
            //            case R.id.tv_settings_item_account:
            //                Intent AccountSettingsIntent = new Intent();
            //                AccountSettingsIntent.setClass(getApplicationContext(),AccountSettingsActivity.class);
            //                startActivity(AccountSettingsIntent);
            //                break;
            case R.id.tv_settings_item_change_pwd:
                Intent changePwdIntent = new Intent();
                changePwdIntent.setClass(getApplicationContext(), ChangePwdActivity.class);
                startActivity(changePwdIntent);
                break;
            case R.id.tv_settings_item_update_info:
                Intent updateInfoIntent = new Intent();
                updateInfoIntent.setClass(getApplicationContext(), AccountInfoUpdateActivity.class);
                startActivity(updateInfoIntent);
                break;
            case R.id.tv_settings_item_about:
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(),AppAboutActivity.class);
                startActivity(intent);
                break;
            case R.id.tv_settings_item_exit:
                //                sharedPreferencesHelper.clearKey(AppConstants.KEY_SYS_CURRENT_NICKNAME);
                //                sharedPreferencesHelper.clearKey(AppConstants.KEY_SYS_CURRENT_PORTRAIT);

                wbAccessToken = SharedPreferencesHelper.readAccessToken();
                if(wbAccessToken != null && wbAccessToken.isSessionValid()) {
                    new LogoutAPI(wbAccessToken).logout(new WeiboRequestListener() {
                        @Override
                        public void onComplete(String response) {
                            if (!TextUtils.isEmpty(response)) {
                                try {
                                    JSONObject obj = new JSONObject(response);
                                    String value = obj.getString("result");

                                    if ("true".equalsIgnoreCase(value)) {
                                        // XXX: 考虑是否需要将 AccessTokenKeeper 放到 SDK 中？？
                                        //AccessTokenKeeper.clear(getContext());
                                        // 清空当前 Token
                                        wbAccessToken = null;
                                        SharedPreferencesHelper.clearAccessToken();
                                        Toast.makeText(getApplicationContext(),
                                                R.string.weibosdk_demo_toast_auth_canceled, Toast.LENGTH_SHORT).show();
                                    }
                                } catch (JSONException e) {
                                    LogUtil.e(SettingsActivity.class.getSimpleName(), "onComplete JSONException...");
                                }
                            }
                        }
                    });
                }
                mTencent = Tencent.createInstance(TencentConstants.APP_ID, getApplicationContext());
                if(mTencent.isSessionValid()) {
                    SharedPreferencesHelper.clearQqAccessToken();
                    mTencent.logout(getApplicationContext());
                }

                mWxOauth2AccessToken = SharedPreferencesHelper.readWxAccessToken();
                if(mWxOauth2AccessToken != null && mWxOauth2AccessToken.isSessionValid()) {
                    mWxOauth2AccessToken = null;
                    SharedPreferencesHelper.clearWxAccessToken();
                }

                if (mBound) {
                    String exitMsg = "{UserId:" + "\"" + sharedPreferencesHelper.getStringValue(AppConstants.KEY_SYS_CURRENT_UID) +"\"" +  "}";
                    mService.sendMessage("OffLineNotify",exitMsg);

//                    mService.destroy();
//                    unbindService(mConnection);
//                    mBound = false;
                }
                stopService(new Intent(getApplicationContext(),ChatService.class));
                sharedPreferencesHelper.clearKey(AppConstants.KEY_SYS_CURRENT_UID);


                Intent exitIntent = new Intent();
                exitIntent.setClass(getApplicationContext(), LoginActivity.class);
                exitIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(exitIntent);
                finish();
                break;
        }
    }

    public void navigateUp() {
        Intent upIntent = NavUtils.getParentActivityIntent(this);
        if(NavUtils.shouldUpRecreateTask(this, upIntent)) {
            TaskStackBuilder.create(this)
                    .addNextIntentWithParentStack(upIntent)
                    .startActivities();
        } else {
            onBackPressed();
        }
    }
}
