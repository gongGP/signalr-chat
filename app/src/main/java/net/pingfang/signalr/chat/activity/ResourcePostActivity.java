package net.pingfang.signalr.chat.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.model.LatLng;
import com.squareup.okhttp.Response;

import net.pingfang.signalr.chat.R;
import net.pingfang.signalr.chat.constant.app.AppConstants;
import net.pingfang.signalr.chat.location.LocationListenerImpl;
import net.pingfang.signalr.chat.location.LocationNotify;
import net.pingfang.signalr.chat.net.HttpBaseCallback;
import net.pingfang.signalr.chat.net.OkHttpCommonUtil;
import net.pingfang.signalr.chat.util.CommonTools;
import net.pingfang.signalr.chat.util.FileUtil;
import net.pingfang.signalr.chat.util.GlobalApplication;
import net.pingfang.signalr.chat.util.MediaFileUtils;
import net.pingfang.signalr.chat.util.SharedPreferencesHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

public class ResourcePostActivity extends AppCompatActivity implements View.OnClickListener, LocationNotify {

    public static final String TAG = ResourcePostActivity.class.getSimpleName();

    public static final String URL_RESOURCE_POST = GlobalApplication.URL_WEB_API_HOST + "/api/WebAPI/ResourceWall/PublishResource";
    public static final String KEY_RESOURCE_POST_UID = "userId";
    public static final String KEY_RESOURCE_POST_WIDTH = "length";
    public static final String KEY_RESOURCE_POST_HEIGHT = "width";
    public static final String KEY_RESOURCE_POST_ADDRESS = "address";
    public static final String KEY_RESOURCE_POST_CONTACTS = "contactName";
    public static final String KEY_RESOURCE_POST_PHONE = "mobile";
    public static final String KEY_RESOURCE_POST_REMARK = "remark";
    public static final String KEY_RESOURCE_POST_PROFILE = "pic";
    public static final String KEY_URL_RESOURCE_POST_LOCATION_LAT = "lat";
    public static final String KEY_URL_RESOURCE_POST_LOCATION_LNG = "lng";

    private TextView btn_activity_back;
    private EditText et_resource_width;
    private EditText et_resource_height;
    private EditText et_resource_location;
    private EditText et_resource_contacts;
    private EditText et_resource_phone;
    private EditText et_resource_remark;
    private ImageView iv_resource_profile;
    private Button btn_resource_save;
    private Button btn_resource_cancel;


    private LocationClient locationClient;
    public LocationListenerImpl locationListener;
    private LatLng currentLatLng;

    SharedPreferencesHelper sharedPreferencesHelper;

    Dialog dialog;
    Uri targetUri;
    String tmpFilePath;
    String fileContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resource_post);

        sharedPreferencesHelper = SharedPreferencesHelper.newInstance(getApplicationContext());
        initView();
        initLocation();
    }

    private void initView() {
        btn_activity_back = (TextView) findViewById(R.id.btn_activity_back);
        btn_activity_back.setOnClickListener(this);

        et_resource_width = (EditText) findViewById(R.id.et_resource_width);
        et_resource_height = (EditText) findViewById(R.id.et_resource_height);
        et_resource_location = (EditText) findViewById(R.id.et_resource_location);
        et_resource_contacts = (EditText) findViewById(R.id.et_resource_contacts);
        et_resource_phone = (EditText) findViewById(R.id.et_resource_phone);
        et_resource_remark = (EditText) findViewById(R.id.et_resource_remark);
        iv_resource_profile = (ImageView) findViewById(R.id.iv_resource_profile);
        iv_resource_profile.setOnClickListener(this);

        btn_resource_save = (Button) findViewById(R.id.btn_resource_save);
        btn_resource_save.setOnClickListener(this);
        btn_resource_cancel = (Button) findViewById(R.id.btn_resource_cancel);
    }

    /**
     * 初始化定位设置并开始定位
     */
    public void initLocation() {
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        option.setCoorType("bd09ll");
        option.setOpenGps(true);
        option.setIsNeedAddress(true);
        option.setIgnoreKillProcess(false);

        locationClient = new LocationClient(getApplicationContext(),option);
        locationListener = new LocationListenerImpl(this);
        locationClient.registerLocationListener(locationListener);
        locationClient.start();
    }

    @Override
    public void updateLoc(BDLocation bdLocation) {
        if (bdLocation == null)
            return;

        currentLatLng = new LatLng(bdLocation.getLatitude(),
                bdLocation.getLongitude());
        et_resource_location.setText(bdLocation.getAddrStr());
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        switch(viewId) {
            case R.id.btn_activity_back:
                navigateUp();
                break;
            case R.id.iv_resource_profile:
                showDialog();
                break;
            case R.id.btn_resource_save:
                storeOrPostRes();
                break;
            case R.id.btn_resource_cancel:
                navigateUp();
                break;
        }
    }

    private void showDialog() {
        View view = getLayoutInflater().inflate(R.layout.photo_choose_dialog,
                null);
        dialog = new Dialog(this, R.style.transparentFrameWindowStyle);
        dialog.setContentView(view, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        Window window = dialog.getWindow();
        // 设置显示动画
        //        window.setWindowAnimations(R.style.main_menu_animstyle);
        WindowManager.LayoutParams wl = window.getAttributes();
        wl.x = 0;
        wl.y = getWindowManager().getDefaultDisplay().getHeight();
        // 以下这两句是为了保证按钮可以水平满屏
        wl.width = ViewGroup.LayoutParams.MATCH_PARENT;
        wl.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        // 设置显示位置
        dialog.onWindowAttributesChanged(wl);
        // 设置点击外围解散
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    public void on_click(View v) {
        switch (v.getId()) {
            case R.id.openCamera:
                openCamera();
                dialog.cancel();
                break;
            case R.id.openPhones:
                pickImage();
                dialog.cancel();
                break;
            case R.id.cancel:
                dialog.cancel();
                break;
            default:
                break;
        }
    }

    private void openCamera() {
        tmpFilePath = MediaFileUtils.genarateFilePath(getApplicationContext(),
                Environment.DIRECTORY_PICTURES, "Photos", "jpg");
        File file = new File(tmpFilePath);
        if (file.exists()) {
            file.delete();
        }

        targetUri = Uri.fromFile(file);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, targetUri);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, GlobalApplication.REQUEST_IMAGE_CAPTURE);
        }

    }

    private void pickImage() {
        Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getIntent.setType("image/*");

        Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");

        if (getIntent.resolveActivity(getPackageManager()) != null ||
                pickIntent.resolveActivity(getPackageManager()) != null) {

            Intent chooserIntent = Intent.createChooser(getIntent, getString(R.string.action_select_image));
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{pickIntent});

            startActivityForResult(chooserIntent, GlobalApplication.REQUEST_IMAGE_GET);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(resultCode == Activity.RESULT_OK) {
            if(requestCode == GlobalApplication.REQUEST_IMAGE_CAPTURE) {

                String filePath = tmpFilePath;
                Bitmap bitmap = MediaFileUtils.decodeBitmapFromPath(filePath,
                        MediaFileUtils.dpToPx(getApplicationContext(), 150),
                        MediaFileUtils.dpToPx(getApplicationContext(), 150));
                iv_resource_profile.setImageBitmap(bitmap);
                fileContent = CommonTools.bitmapToBase64(bitmap);
            } else if(requestCode == GlobalApplication.REQUEST_IMAGE_GET) {
                if(data != null && data.getData() != null) {
                    Uri uri = data.getData();
                    String filePath = FileUtil.getPath(getApplicationContext(), uri);
                    Bitmap bitmap = MediaFileUtils.decodeBitmapFromPath(filePath,
                            MediaFileUtils.dpToPx(getApplicationContext(), 150),
                            MediaFileUtils.dpToPx(getApplicationContext(), 150));
                    iv_resource_profile.setImageBitmap(bitmap);
                    fileContent = CommonTools.bitmapToBase64(bitmap);
                } else if(data == null) {
                    Toast.makeText(getApplicationContext(),getString(R.string.image_get_data_null),Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(),getString(R.string.image_get_file_null),Toast.LENGTH_SHORT).show();
                }
            }
        } else if(resultCode == Activity.RESULT_CANCELED) {
            if(requestCode == GlobalApplication.REQUEST_IMAGE_CAPTURE) {
                Toast.makeText(getApplicationContext(),getString(R.string.image_capture_user_canceled),Toast.LENGTH_SHORT).show();
            }

            if(requestCode == GlobalApplication.REQUEST_IMAGE_GET) {
                Toast.makeText(getApplicationContext(),getString(R.string.image_get_file_user_canceled),Toast.LENGTH_SHORT).show();
            }

        } else {
            if(requestCode == GlobalApplication.REQUEST_IMAGE_CAPTURE) {
                Toast.makeText(getApplicationContext(),getString(R.string.image_capture_user_error),Toast.LENGTH_SHORT).show();
            }

            if(requestCode == GlobalApplication.REQUEST_IMAGE_GET) {
                Toast.makeText(getApplicationContext(),getString(R.string.image_get_file_user_error),Toast.LENGTH_SHORT).show();
            }
        }

    }

    private void storeOrPostRes() {
        if(!TextUtils.isEmpty(fileContent)) {
            OkHttpCommonUtil okhtp = OkHttpCommonUtil.newInstance(getApplicationContext());
            okhtp.postRequest(
                    URL_RESOURCE_POST,
                    new OkHttpCommonUtil.Param[]{
                            new OkHttpCommonUtil.Param(KEY_RESOURCE_POST_UID, sharedPreferencesHelper.getStringValue(AppConstants.KEY_SYS_CURRENT_UID)),
                            new OkHttpCommonUtil.Param(KEY_RESOURCE_POST_WIDTH, et_resource_width.getText().toString().trim()),
                            new OkHttpCommonUtil.Param(KEY_RESOURCE_POST_HEIGHT, et_resource_height.getText().toString().trim()),
                            new OkHttpCommonUtil.Param(KEY_RESOURCE_POST_ADDRESS, et_resource_location.getText().toString().trim()),
                            new OkHttpCommonUtil.Param(KEY_RESOURCE_POST_CONTACTS, et_resource_contacts.getText().toString().trim()),
                            new OkHttpCommonUtil.Param(KEY_RESOURCE_POST_PHONE, et_resource_phone.getText().toString().trim()),
                            new OkHttpCommonUtil.Param(KEY_RESOURCE_POST_REMARK, et_resource_remark.getText().toString().trim()),
                            new OkHttpCommonUtil.Param(KEY_URL_RESOURCE_POST_LOCATION_LAT, currentLatLng.latitude),
                            new OkHttpCommonUtil.Param(KEY_URL_RESOURCE_POST_LOCATION_LNG, currentLatLng.longitude),
                            new OkHttpCommonUtil.Param(KEY_RESOURCE_POST_PROFILE, fileContent)
                    },
                    new HttpBaseCallback() {
                        @Override
                        public void onResponse(Response response) throws IOException {
                            String result = response.body().string();
                            Log.d(TAG, "URL_RESOURCE_POST return == " + result);
                            JSONObject jsonObject = null;
                            try {
                                jsonObject = new JSONObject(result);
                                int status = jsonObject.getInt("status");
                                String message = jsonObject.getString("message");
                                if(status == 0) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(getApplicationContext(),
                                                    getString(R.string.toast_resource_post_ok),
                                                    Toast.LENGTH_SHORT).show();
                                            navigateUp();
                                        }
                                    });
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
            );
        } else {
            Toast.makeText(getApplicationContext(),getString(R.string.image_data_null),Toast.LENGTH_SHORT).show();
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