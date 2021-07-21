// ref: https://github.com/googlesamples/android-testdpc/blob/cf9374bc4c7d1a548bbeb8d9bd05828a9d05cd66/app/src/main/java/com/afwsamples/testdpc/DeviceAdminReceiver.java
package net.sungmin.uran;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.view.KeyEventDispatcher;

public class UranAdminReceiver extends DeviceAdminReceiver {

    String LOG_TAG = "URAN_ADMIN_RECEIVER";

    @Override
    public void onProfileProvisioningComplete(Context context, Intent intent) {
        // enable admin
        DevicePolicyManager dpm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName cn = getComponentName(context);
        dpm.setProfileName(cn, "URAN Device admin");
    }

    public static ComponentName getComponentName(Context context) {
        return new ComponentName(context.getApplicationContext(), UranAdminReceiver.class);
    }
}
