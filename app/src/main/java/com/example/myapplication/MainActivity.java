package com.example.myapplication;

import static android.content.ContentValues.TAG;
import static com.abaltatech.mcs.logger.MCSLogger.ELogType.eDebug;
import static com.abaltatech.mcs.logger.MCSLogger.ELogType.eError;
import static com.abaltatech.mcs.logger.MCSLogger.ELogType.eInfo;
import static com.abaltatech.mcs.logger.MCSLogger.ELogType.eWarning;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.abaltatech.mcs.connectionmanager.PeerDevice;
import com.abaltatech.mcs.logger.MCSLogger;
import com.abaltatech.mcs.logger.android.LoggerAndroid;
import com.abaltatech.weblink.core.WLClientFeatures;
import com.abaltatech.weblink.core.WLTypes;
import com.abaltatech.weblink.core.authentication.DeviceIdentity;
import com.abaltatech.weblinkclient.WLClientDisplay;
import com.abaltatech.weblinkclient.WebLinkClientCore;
import com.abaltatech.weblinkclient.framedecoding.FrameDecoderFactory;
import com.abaltatech.weblinkclient.framedecoding.FrameDecoder_H264;
import com.example.myapplication.permissions.WLPermissionManager;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements WLPermissionManager.IPermissionUIHandler {

    private String TAG = "MainActivity";
    private WebLinkClient client = WLApplication.getInstance().getWebLinkClient();
    private WebLinkClientCore wlClientCore = client.getWebLinkClientCore();
    private WLClientDisplay m_clientDisplay;
    private Handler m_permissionHandler;
    private WLPermissionManager m_permissionManager;

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable connectionCheckRunnable = new Runnable() {
        @Override
        public void run() {
            if (client.isM_isConnected()) {
                MCSLogger.log(eInfo, TAG, "********************Connection Running*******************");
                mHandler.postDelayed(this, 1000);
            } else {
                MCSLogger.log(eInfo, TAG, "********************Connection Stopped*******************");
            }
        }
    };

    // Permission request codes
    private static final int PERMISSIONS_REQUEST_CODE = 1996;
    private static final int ACCESS_FINE_LOCATION_PERMISSION_CODE = 6555;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize WLPermissionManager
        m_permissionManager = WLPermissionManager.getInstance();
        m_permissionManager.setPermissionUIHandler(this);

        // Register and request permissions
        registerPermissions();
        requestRequiredPermissions();

        initWebLinkClient();

        String Address = "10.40.3.54:12345";
        PeerDevice device = new PeerDevice("Alice", "Socket", Address);
        if (client.connectToDevice(device)) {
            MCSLogger.log(eInfo, TAG, "********************Connection Established*******************");
        } else {
            MCSLogger.log(eInfo, TAG, "********************Connection Failed TO Be Established********************");
        }

        mHandler.post(connectionCheckRunnable);
    }

    void initWebLinkClient() {
        client = WLApplication.getInstance().getWebLinkClient();
        wlClientCore = client.getWebLinkClientCore();

        wlClientCore.terminate();

        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point clientSize = new Point();
        display.getSize(clientSize);

        int encodeWidth = 800;
        int encodeHeight = 480;

        int renderWidth = encodeWidth;
        int renderHeight = encodeHeight;

        int clientWidth = encodeWidth;
        int clientHeight = encodeHeight;

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int xdpi = (int) metrics.xdpi;
        int ydpi = (int) metrics.ydpi;

        wlClientCore.registerClientDisplay(new WLClientDisplay());

        int clientFeatures = WLClientFeatures.SUPPORTS_CLIENT_ACTIONS;
        String clientFeaturesString = String.format(Locale.US, "xdpi=%d|ydpi=%d", xdpi, ydpi);

        wlClientCore.setEncoderParams(getEncoderParams(WLTypes.FRAME_ENCODING_H264, 30, 2000000));
        wlClientCore.setMaximumFrameRate(60);
        wlClientCore.enableAutoFPSManagement(true);

        wlClientCore.init(
                renderWidth, renderHeight,
                encodeWidth, encodeHeight,
                clientWidth, clientHeight,
                clientFeatures, clientFeaturesString
        );

        m_clientDisplay = wlClientCore.getDefaultDisplay();

        FrameDecoderFactory.instance().unregisterDecoder(WLTypes.FRAME_ENCODING_H264);
        FrameDecoderFactory.instance().unregisterDecoder(WLTypes.FRAME_ENCODING_I420);
        FrameDecoderFactory.instance().unregisterDecoder(WLTypes.FRAME_ENCODING_YUV);
        FrameDecoderFactory.instance().registerDecoder(WLTypes.FRAME_ENCODING_H264, FrameDecoder_H264.class);

        if (m_clientDisplay != null) {
            int decoderMask = FrameDecoderFactory.instance().getRegisteredDecodersMask();
            m_clientDisplay.setSupportedDecodersMask(decoderMask);
        }
    }

    private String getEncoderParams(int decoderType, int keyFrameInterval, int bitRate) {
        return String.format(Locale.US, "%d:maxKeyFrameInterval=%d,bitrate=%d",
                decoderType, keyFrameInterval, bitRate);
    }

    private void registerPermissions() {
        WLPermissionManager permissionManager = WLPermissionManager.getInstance();
        permissionManager.addRequiredPermission(Manifest.permission.ACCESS_FINE_LOCATION, ACCESS_FINE_LOCATION_PERMISSION_CODE);
        // Add more permissions if needed, e.g., ACCESS_BACKGROUND_LOCATION for API 29+
    }

    private void requestRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (m_permissionHandler != null) {
                m_permissionHandler.removeCallbacks(null);
            }
            m_permissionHandler = new Handler(Looper.getMainLooper());
            m_permissionHandler.postDelayed(() -> m_permissionManager.requestRequiredPermissions(), 1500);
        } else {
            m_permissionManager.requestRequiredPermissions();
        }
        MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "Requesting permissions requestRequiredPermissions()");
    }

    @Override
    public boolean checkBasicPermission(String permission) {
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void requestBasicPermission(String[] permissions, Integer requestCode) {
        ActivityCompat.requestPermissions(this, permissions, requestCode);
    }

    @Override
    public void requestExtPermission(Intent intent, Integer requestCode, boolean cancelable, boolean canBeDismissed) {
        // Not implemented unless required
    }

    @Override
    public boolean shouldShowExplanationForBasicPermission(String[] permissions) {
        if (permissions.length != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return shouldShowRequestPermissionRationale(permissions[0]);
        } else {
            return false;
        }
    }

    @Override
    public void requestBasicPermissionWithExplanation(String[] permissions, int requestCode, boolean cancelable, boolean canBeDismissed) {
        String title = "Permission Required";
        String message = "This app requires the following permission to function properly: " + permissions[0];
        showExplanationAndRequestPermission(title, message, permissions, requestCode, cancelable, canBeDismissed);
    }

    @Override
    public void requestBasicPermissions(List<String> permissions, List<Integer> requestCodes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void showDeniedPermissionExplanation(String[] permissions, int requestCode) {
        String title = "Permission Denied";
        String message = "The permission '" + permissions[0] + "' was denied. Some features may not work.";
        showExplanationPopup(title, message);
    }

    private void showExplanationAndRequestPermission(String title, String message, String[] permissions, int requestCode, boolean cancelable, boolean canBeDismissed) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> {
            ActivityCompat.requestPermissions(this, permissions, requestCode);
        });
        if (canBeDismissed) {
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        }
        builder.setCancelable(cancelable);
        builder.show();
    }

    private void showExplanationPopup(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; ++i) {
                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    WLPermissionManager.getInstance().onPermissionResult(ACCESS_FINE_LOCATION_PERMISSION_CODE, grantResults[i], null);
                }
            }
        } else {
            if (permissions != null && grantResults != null && permissions.length > 0 && grantResults.length > 0) {
                WLPermissionManager.getInstance().onPermissionResult(requestCode, grantResults[0], null);
            }
        }
    }
}