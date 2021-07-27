package net.sungmin.uran;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.icu.util.Output;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class MainActivity extends Activity {

    String LOG_TAG = "URAN_MAIN";

    DevicePolicyManager mDpm;
    ConnectivityManager mCm;
    SSLContext mSslContext;

    TextView mTxtIsDeviceOwner;
    Button mBtnRemoveAdmin;
    Button mBtnAllowCamera;
    Button mBtnDisallowCamera;
    Button mBtnSendHello;
    EditText mEtServerIp;
    EditText mEtServerPort;
    String mPackageName;
    ComponentName mComponentName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mCm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mPackageName = getApplicationContext().getPackageName();
        mComponentName = UranAdminReceiver.getComponentName(getApplicationContext());

        // prepare ssl context
        try {
            // load cert
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = getResources().openRawResource(R.raw.uran_mdm_server);
            Certificate cert = cf.generateCertificate(caInput);

            // create keystore
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", cert);

            // create TrustManager
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            // create SSLContext
            mSslContext = SSLContext.getInstance("TLS");
            mSslContext.init(null, tmf.getTrustManagers(), null);

        } catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }

        // prepare button and listeners
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
        mBtnSendHello = findViewById(R.id.btn_send_hello);
        mEtServerIp = findViewById(R.id.et_server_ip);
        mEtServerPort = findViewById(R.id.et_server_port);
        mBtnSendHello.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                try {
                    SSLSocketFactory factory = mSslContext.getSocketFactory();
                    SSLSocket sslSocket = (SSLSocket) factory.createSocket(
                            mEtServerIp.getText().toString(),
                            Integer.parseInt(mEtServerPort.getText().toString()));

                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                            sslSocket.getOutputStream()));
                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                            sslSocket.getInputStream()));

                    // TODO
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
    }

    private void showToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }
}