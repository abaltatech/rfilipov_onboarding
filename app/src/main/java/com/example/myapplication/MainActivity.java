package com.example.myapplication;

import static android.content.ContentValues.TAG;
import static com.abaltatech.mcs.logger.MCSLogger.ELogType.eDebug;
import static com.abaltatech.mcs.logger.MCSLogger.ELogType.eError;
import static com.abaltatech.mcs.logger.MCSLogger.ELogType.eInfo;
import static com.abaltatech.mcs.logger.MCSLogger.ELogType.eWarning;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.abaltatech.mcs.connectionmanager.PeerDevice;
import com.abaltatech.mcs.logger.MCSLogger;
import com.abaltatech.mcs.logger.android.LoggerAndroid;
import com.abaltatech.weblink.core.WLClientFeatures;
import com.abaltatech.weblink.core.authentication.DeviceIdentity;
import com.abaltatech.weblinkclient.WebLinkClientCore;

import static android.content.ContentValues.TAG;
import static com.abaltatech.mcs.logger.MCSLogger.ELogType.eDebug;
import static com.abaltatech.mcs.logger.MCSLogger.ELogType.eError;
import static com.abaltatech.mcs.logger.MCSLogger.ELogType.eInfo;
import static com.abaltatech.mcs.logger.MCSLogger.ELogType.eWarning;

import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.abaltatech.mcs.connectionmanager.PeerDevice;
import com.abaltatech.mcs.logger.MCSLogger;
import com.abaltatech.mcs.logger.android.LoggerAndroid;
import com.abaltatech.weblink.core.WLClientFeatures;
import com.abaltatech.weblinkclient.WebLinkClientCore;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    String TAG = "MainActivity";
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        MCSLogger.log(eInfo, TAG, "This is an Info message");
        MCSLogger.log(eDebug, TAG, "This is an Debug message");
        MCSLogger.log(eWarning, TAG, "This is an Warning message");
        MCSLogger.log(eError, TAG, "This is an Error message");


        final WebLinkClient client = WLApplication.getInstance().getWebLinkClient();
        final WebLinkClientCore wlClient = client.getWebLinkClientCore();

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

        int clientFeatures = WLClientFeatures.SUPPORTS_CLIENT_ACTIONS;
        String clientFeaturesString = String.format(
                Locale.US, "xdpi=%d|ydpi=%d", xdpi, ydpi);

        wlClient.init(
                renderWidth, renderHeight,
                encodeWidth, encodeHeight,
                clientWidth, clientHeight,
                clientFeatures, clientFeaturesString
        );

        String Address = "10.40.3.54:12345";
        PeerDevice device = new PeerDevice("Alice", "Socket", Address);
        if (client.connectToDevice(device)){
            MCSLogger.log(eInfo, TAG, "********************Connection Established*******************");
        }

        else{
            MCSLogger.log(eInfo, TAG, "********************Connection Failed TO Be Established********************");

        }


    }
}





