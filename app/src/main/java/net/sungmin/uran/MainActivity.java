package net.sungmin.uran;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends Activity {

    String LOG_TAG = "URAN_MAIN";

    DevicePolicyManager mDpm;

    TextView mTxtIsDeviceOwner;
    Button mBtnRemoveAdmin;
    Button mBtnAllowCamera;
    Button mBtnDisallowCamera;
    String mPackageName;
    ComponentName mComponentName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDpm = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
        mPackageName = getApplicationContext().getPackageName();
        mComponentName = UranAdminReceiver.getComponentName(getApplicationContext());
        mTxtIsDeviceOwner = findViewById(R.id.txt_is_device_owner);
        mBtnRemoveAdmin = findViewById(R.id.btn_remove_admin);
        mBtnRemoveAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(LOG_TAG, "btnRemoveAdminClicked");
                mDpm.clearDeviceOwnerApp(mPackageName);
                showToast("URAN admin removed.");
                mBtnAllowCamera.setEnabled(false);
                mBtnDisallowCamera.setEnabled(false);
            }
        });
        mBtnAllowCamera = findViewById(R.id.btn_allow_camera);
        mBtnAllowCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(LOG_TAG, "btnAllowCameraClicked");
                mDpm.setCameraDisabled(mComponentName, false);
                showToast("Camera allowed.");
            }
        });
        mBtnDisallowCamera = findViewById(R.id.btn_disallow_camera);
        mBtnDisallowCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(LOG_TAG, "btnDisallowCameraClicked");
                mDpm.setCameraDisabled(mComponentName, true);
                showToast("Camera disallowed.");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResumed");
        Log.d(LOG_TAG, "onResumed" + getApplicationContext().getPackageName());
        boolean isDeviceOwner = mDpm.isDeviceOwnerApp(mPackageName);
        if (isDeviceOwner) {
            mTxtIsDeviceOwner.setText("This app is a device admin!");
            mBtnRemoveAdmin.setEnabled(true);
            mBtnAllowCamera.setEnabled(true);
            mBtnDisallowCamera.setEnabled(true);
        } else {
            mTxtIsDeviceOwner.setText("This app is not a device admin.\n" +
                    "How to use: set the belw ADB command.\n" +
                    "\"adb shell dpm set-device-owner net.sungmin.uran/.UranAdminReceiver\"");
            mBtnRemoveAdmin.setEnabled(false);
            mBtnAllowCamera.setEnabled(false);
            mBtnDisallowCamera.setEnabled(false);
        }
    }

    private void showToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }
}