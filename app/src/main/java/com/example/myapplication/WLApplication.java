package com.example.myapplication;

import static android.content.ContentValues.TAG;
import static com.abaltatech.mcs.logger.MCSLogger.ELogType.eInfo;

import android.app.Application;
import android.widget.Toast;

import com.abaltatech.mcs.connectionmanager.PeerDevice;
import com.abaltatech.mcs.logger.MCSLogger;
import com.abaltatech.mcs.logger.android.LoggerAndroid;
import com.abaltatech.weblink.core.WLTypes;
import com.abaltatech.weblinkclient.WebLinkClientCore;

import java.util.HashMap;
import java.util.Map;

public class WLApplication extends Application {

    private WebLinkClient m_wlClient;
    private static WLApplication s_Instance;
    public static WLApplication getInstance() {
        return s_Instance;
    }

    public WebLinkClientCore getWebLinkClientCore() {
        return m_wlClient.getWebLinkClientCore();
    }

    public WebLinkClient getWebLinkClient(){
        return m_wlClient;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MCSLogger.registerLogger(new LoggerAndroid());
        MCSLogger.setLogLevel(MCSLogger.eAll);
        s_Instance = this;
        m_wlClient = new WebLinkClient(s_Instance.getApplicationContext(), null);
    }

    public void terminate() {
        if (m_wlClient != null) {
            m_wlClient.terminate();
            m_wlClient = null;
        }
    }

}
