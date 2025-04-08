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
import com.abaltatech.weblink.core.WLTypes;
import com.abaltatech.weblink.core.authentication.DeviceIdentity;
import com.abaltatech.weblinkclient.WLClientDisplay;
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
import com.abaltatech.weblinkclient.framedecoding.FrameDecoderFactory;
import com.abaltatech.weblinkclient.framedecoding.FrameDecoder_H264;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivity";
    private final WebLinkClient client = WLApplication.getInstance().getWebLinkClient();
    private final WebLinkClientCore wlClient = client.getWebLinkClientCore();
    private WLClientDisplay m_clientDisplay;

    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);

        initWebLinkClient();


        String Address = "10.40.3.54:12345";
        PeerDevice device = new PeerDevice("Alice", "Socket", Address);
        if (client.connectToDevice(device)){
            MCSLogger.log(eInfo, TAG, "********************Connection Established*******************");
        }

        else{
            MCSLogger.log(eInfo, TAG, "********************Connection Failed TO Be Established********************");

        }
    }

    void initWebLinkClient()
    {

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

        wlClient.registerClientDisplay(new WLClientDisplay());


        int clientFeatures = WLClientFeatures.SUPPORTS_CLIENT_ACTIONS;
        String clientFeaturesString = String.format(
                Locale.US, "xdpi=%d|ydpi=%d", xdpi, ydpi);

        wlClient.setEncoderParams(getEncoderParams(WLTypes.FRAME_ENCODING_H264, 30, 2000000));

        wlClient.setMaximumFrameRate(60);
        wlClient.enableAutoFPSManagement(true);


        wlClient.init(
                renderWidth, renderHeight,
                encodeWidth, encodeHeight,
                clientWidth, clientHeight,
                clientFeatures, clientFeaturesString
        );

        m_clientDisplay = wlClient.getDefaultDisplay();

        //unregister all other decoders, re-register only the selected version.
        FrameDecoderFactory.instance().unregisterDecoder(WLTypes.FRAME_ENCODING_H264);
        FrameDecoderFactory.instance().unregisterDecoder(WLTypes.FRAME_ENCODING_I420);
        FrameDecoderFactory.instance().unregisterDecoder(WLTypes.FRAME_ENCODING_YUV);
        //FrameDecoderFactory.instance().registerDecoder(m_sharedPref.getDecoderType(), decoderClass);

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
}





