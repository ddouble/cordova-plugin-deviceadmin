package com.mama.deviceadmin;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;


/**
 * 云之讯的cordova插件
 *
 * 可连接云之讯VOIP服务，接收来电通知
 */
public class DeviceAdmin extends CordovaPlugin {
    public static final String TAG = "DeviceAdmin";
    private static final int REQUEST_CODE_ENABLE_ADMIN = 1;
    private ComponentName mCordovaDeviceAdmin;
    private DevicePolicyManager mDPM;
    private CallbackContext listener = null;
    private CallbackContext callbackCtx;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;

    /**
     * Constructor.
     */
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        Activity a = cordova.getActivity();

        powerManager = (PowerManager) cordova.getActivity().getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock((PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE), "TAG");

        // Prepare to work with the DPM
        mDPM = (DevicePolicyManager) a.getSystemService(Context.DEVICE_POLICY_SERVICE);
        mCordovaDeviceAdmin = new ComponentName(a, CordovaDeviceAdminReceiver.class);
    }

    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("register")) {
            // String token = args.getString(0);
            // int screenOrientation = args.getInt(1);
            this.register(callbackContext);
            return true;
        } else if (action.equals("unRegister")) {
            this.unRegister(callbackContext);
            return true;
        } else if (action.equals("listen")) {
            this.listen(callbackContext);
            return true;
        } else if (action.equals("isAdmin")) {
            this.isAdmin(callbackContext);
            return true;
        } else if (action.equals("lock")) {
            this.lock(callbackContext);
            return true;
        } else if (action.equals("turnOffScreen")) {
            this.turnOffScreen(callbackContext);
            return true;
        } else if (action.equals("turnOnScreen")) {
            Boolean keepOn = args.getBoolean(0);
            this.turnOnScreen(keepOn, callbackContext);
            return true;
        }

        return false;
    }

    private void turnOffScreen(final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Window window = cordova.getActivity().getWindow();
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
                window.clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                window.clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

//                        WindowManager.LayoutParams params = window.getAttributes();
////                        params.flags |= LayoutParams.FLAG_KEEP_SCREEN_ON;
//                        params.screenBrightness = 0;
//                        window.setAttributes(params);

                final int screenOffTimeout = Settings.System.getInt(cordova.getActivity().getContentResolver(),
                    Settings.System.SCREEN_OFF_TIMEOUT, 60000);

                Settings.System.putInt(cordova.getActivity().getContentResolver(),
                    Settings.System.SCREEN_OFF_TIMEOUT, 1000);

                Timer timer = new Timer(true);
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        Settings.System.putInt(cordova.getActivity().getContentResolver(),
                            Settings.System.SCREEN_OFF_TIMEOUT, screenOffTimeout);
                    }
                };
                timer.schedule(task, 30000);

                if(wakeLock.isHeld()) {
                    wakeLock.release();
                }

                Log.v(TAG, "Screen has scheduled to turned off for several second.");
                callbackContext.success();
            }
        });

    }

    private void turnOnScreen(final boolean keepOn, final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Window window = cordova.getActivity().getWindow();
                if (keepOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
                window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

                if (wakeLock.isHeld()) {
                    wakeLock.release();
                }
//        if(acquireTime > 0) {
//            wakeLock.acquire(acquireTime);
//        }
//        else
//        {
                wakeLock.acquire();
//        }
                Log.v(TAG, "Screen has been turned on.");
                callbackContext.success();
            }
        });
    }


    private void isAdmin(CallbackContext callbackContext) {
        boolean isAdmin = mDPM.isAdminActive(mCordovaDeviceAdmin);

        PluginResult dataResult = new PluginResult(PluginResult.Status.OK, isAdmin);
//        dataResult.setKeepCallback(true);
        if (callbackContext != null) callbackContext.sendPluginResult(dataResult);
    }

    private void lock(CallbackContext callbackContext) {
        mDPM.lockNow();
    }


    private void unRegister(CallbackContext callbackContext) {
        mDPM.removeActiveAdmin(mCordovaDeviceAdmin);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (REQUEST_CODE_ENABLE_ADMIN == requestCode) {
            PluginResult dataResult = new PluginResult(PluginResult.Status.OK, resultCode);
//        dataResult.setKeepCallback(true);
            if (callbackCtx != null) callbackCtx.sendPluginResult(dataResult);
        }


    }

    /**
     * 注册 device admin
     *
     */
    private void register(CallbackContext callbackContext) {
        Activity a = cordova.getActivity();

        // Launch the activity to have the user enable our admin.
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mCordovaDeviceAdmin);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "");

        cordova.setActivityResultCallback(this);
        a.startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN);

        this.callbackCtx = callbackContext;
        // return false - don't update checkbox until we're really active
//        return false;
    }

    private void listen(CallbackContext callbackContext) {
        listener = callbackContext;
    }

    /**
     * 原生代码向js发送消息
     * @param message
     */
    public void sendMessage(String message) {
        PluginResult dataResult = new PluginResult(PluginResult.Status.OK, message);
        dataResult.setKeepCallback(true);
        if (listener != null) listener.sendPluginResult(dataResult);
    }
}
