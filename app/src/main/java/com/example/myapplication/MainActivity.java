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

public class MainActivity extends AppCompatActivity {
    String TAG = "MainActivity";

    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        MCSLogger.registerLogger(new LoggerAndroid());
        MCSLogger.setLogLevel(MCSLogger.eAll);
        MCSLogger.log(eInfo, TAG, "This is an Info message");
        MCSLogger.log(eDebug, TAG, "This is an Debug message");
        MCSLogger.log(eWarning, TAG, "This is an Warning message");
        MCSLogger.log(eError, TAG, "This is an Error message");

    }

}
