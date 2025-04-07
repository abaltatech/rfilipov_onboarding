package com.example.myapplication;

import static android.content.ContentValues.TAG;
import static com.abaltatech.mcs.logger.MCSLogger.ELogType.eInfo;

import android.app.Application;
import android.content.Context;
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
    private static WLApplication s_Instance = null;
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
        m_wlClient = new WebLinkClient(s_Instance.getApplicationContext(), getAppProperties());
    }

    public void terminate() {
        if (m_wlClient != null) {
            m_wlClient.terminate();
            m_wlClient = null;
        }
    }

    public static Context getAppContext() {
        return s_Instance != null ? s_Instance.getApplicationContext() : null;
    }

    private Map<String, String> getAppProperties() {
        final HashMap<String, String> res = new HashMap<>();

        res.put(WLConstants.PROP_BT_SERVICE_LISTEN_UUID, WLTypes.BT_SERVICE_CONNECT_ID_CLIENT);
        res.put(WLConstants.PROP_BT_SERVICE_CONNECT_UUID, WLTypes.BT_SERVICE_LISTEN_ID_HOST);

        return res;
    }

}
