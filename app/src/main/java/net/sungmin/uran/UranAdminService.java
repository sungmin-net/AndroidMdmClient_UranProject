package net.sungmin.uran;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class UranAdminService extends Service {

    String LOG_TAG = "URAN_SERVICE";

    public static final String ACTION_SEND_HELLO = "action_send_hello";
    SSLSocketFactory mSslSocketFactory;

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "onCreate");

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
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);
            mSslSocketFactory = sslContext.getSocketFactory();

        } catch (CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException
                | KeyManagementException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand(): " + intent.getAction());
        switch (intent.getAction()) {
            case ACTION_SEND_HELLO:
                String serverIp = intent.getStringExtra("SERVER_IP");
                int serverPort = Integer.parseInt(intent.getStringExtra("SERVER_PORT"));
                Log.d(LOG_TAG, serverIp + " / " + serverPort);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        SSLSocket socket = null;
                        try {
                            String serverIp = intent.getStringExtra("SERVER_IP");
                            int serverPort = Integer.parseInt(intent.getStringExtra("SERVER_PORT"));
                            socket = (SSLSocket) mSslSocketFactory.createSocket(
                                    serverIp, serverPort);
                            socket.setEnabledProtocols(socket.getEnabledProtocols());
                            socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

                            BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(socket.getInputStream()));

                            BufferedWriter bw = new BufferedWriter(
                                    new OutputStreamWriter(socket.getOutputStream()));
                            PrintWriter writer = new PrintWriter(bw, true /*auto flush*/);

                            String msg = "Hello! this is android.";
                            writer.println(msg);
                            Log.d(LOG_TAG, "Device sent \"" + msg + "\"");
                            activityLog("Device sent \"" + msg + "\"");

                            String reply = reader.readLine();
                            activityLog("Device received \"" + reply + "\"");
                            Log.d(LOG_TAG, "Device received \"" + reply + "\"");

                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (socket != null && !socket.isClosed()) {
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }).start();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + intent.getAction());
        }
        return START_NOT_STICKY;
    }

    private void activityLog(String msg) {
        Intent intent = new Intent(MainActivity.ACTIVITY_LOG);
        intent.putExtra(MainActivity.ACTIVITY_LOG, msg);
        sendBroadcast(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
