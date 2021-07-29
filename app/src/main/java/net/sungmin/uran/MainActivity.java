package net.sungmin.uran;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.icu.util.Output;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class MainActivity extends Activity {

    static String LOG_TAG = "URAN_MAIN";
    public static final String ACTIVITY_LOG = "ACTIVITY_LOG";

    DevicePolicyManager mDpm;
    ConnectivityManager mCm;
    SSLContext mSslContext;

    TextView mTxtIsDeviceOwner;
    TextView mTxtActivityLogger;
    Button mBtnRemoveAdmin;
    Button mBtnAllowCamera;
    Button mBtnDisallowCamera;
    Button mBtnSendHello;
    Button mBtnStartPolling;
    Button mBtnStopPolling;

    EditText mEtServerIp;
    EditText mEtServerPort;
    String mPackageName;
    ComponentName mComponentName;

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(LOG_TAG, "onReceive() " + action);
            switch(action) {
                case ACTIVITY_LOG:
                    activityLog(intent.getStringExtra(ACTIVITY_LOG));
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + intent.getAction());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mCm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mPackageName = getApplicationContext().getPackageName();
        mComponentName = UranAdminReceiver.getComponentName(getApplicationContext());

        // prepare button and listeners
        mTxtIsDeviceOwner = findViewById(R.id.txt_is_device_owner);
        mBtnRemoveAdmin = findViewById(R.id.btn_remove_admin);
        mBtnRemoveAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(LOG_TAG, "btnRemoveAdminClicked");
                mDpm.clearDeviceOwnerApp(mPackageName);
                activityLog("URAN admin removed.");
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
                activityLog("Camera allowed.");
            }
        });
        mBtnDisallowCamera = findViewById(R.id.btn_disallow_camera);
        mBtnDisallowCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(LOG_TAG, "btnDisallowCameraClicked");
                mDpm.setCameraDisabled(mComponentName, true);
                activityLog("Camera disallowed.");
            }
        });
        mBtnSendHello = findViewById(R.id.btn_send_hello);
        mEtServerIp = findViewById(R.id.et_server_ip);
        mEtServerPort = findViewById(R.id.et_server_port);
        mBtnSendHello.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Log.i(LOG_TAG, "Send Hello clicked.");
                Intent intent = new Intent(getApplicationContext(), UranAdminService.class);
                intent.setAction(UranAdminService.ACTION_SEND_HELLO);
                intent.putExtra("SERVER_IP", getServerIp());
                intent.putExtra("SERVER_PORT", getServerPort());
                startService(intent);
            }
        });

        mTxtActivityLogger = findViewById(R.id.activity_logger);
        mTxtActivityLogger.setMovementMethod(new ScrollingMovementMethod());
        mTxtActivityLogger.setText(getTime() + "Activity logger started.");

        mBtnStartPolling = findViewById(R.id.btn_start_polling);
        mBtnStopPolling = findViewById(R.id.btn_stop_polling);
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
            mTxtIsDeviceOwner.setText("This app is not a device admin.\n"
                    + "To enable, set below ADB command.\n"
                    + "\"adb shell dpm set-device-owner net.sungmin.uran/.UranAdminReceiver\"");
            mBtnRemoveAdmin.setEnabled(false);
            mBtnAllowCamera.setEnabled(false);
            mBtnDisallowCamera.setEnabled(false);
        }

        // Check connectivity. Note: This code will not work on Android 10 or higher.
        NetworkInfo activeNetwork = mCm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnected();

        if (isConnected) {
            mBtnSendHello.setEnabled(true);
        } else {
            mBtnSendHello.setEnabled(false);
        }

        if (isConnected && isDeviceOwner) {
            mBtnStartPolling.setEnabled(true);
            mBtnStopPolling.setEnabled(true);
        } else {
            mBtnStartPolling.setEnabled(false);
            mBtnStopPolling.setEnabled(false);
        }
        registerReceiver(mReceiver, new IntentFilter(ACTIVITY_LOG));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    protected String getServerIp() {
        return mEtServerIp.getText().toString();
    }

    protected String getServerPort() {
        return mEtServerPort.getText().toString();
    }

    protected void activityLog(String msg) {
        mTxtActivityLogger.append("\n" + getTime() + msg);
    }

    private String getTime() {
        SimpleDateFormat format = new SimpleDateFormat("[yy.MM.dd HH:mm:ss] ");
        return format.format(new Date());
    }
}