/****************************************************************************
 *
 * @file App.java
 * @brief
 *
 * Contains the App class.
 *
 * @author Abalta Technologies, Inc.
 * @date Jan, 2014
 *
 * @cond Copyright
 *
 * COPYRIGHT 2014 ABALTA TECHNOLOGIES ALL RIGHTS RESERVED.<br>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @endcond
 *****************************************************************************/
package com.example.myapplication;

import android.app.Application;
import android.content.Context;

import com.abaltatech.mcs.BuildConfig;
import com.abaltatech.mcs.logger.MCSLogger;
import com.abaltatech.mcs.logger.android.LoggerAndroid;
import com.abaltatech.weblink.core.WLTypes;
import com.abaltatech.weblink.core.commandhandling.EAPSecurity;
import com.abaltatech.weblink.core.logging_helper.CommandLoggingHelper;
import com.abaltatech.weblinkclient.WebLinkClientCore;
import com.weblink.androidext.wifi.IWiFiControlHandlerAP;
import com.weblink.androidext.wifi.NetworkConfigAP;
import com.weblink.androidext.wifi.WiFiManager;

import java.util.HashMap;
import java.util.Map;

public class App extends Application {
    static final String TAG = "WLClientApp";
    private static App ms_instance = null;
    
    private WebLinkClient m_wlClient;
    
    @Override
    public void onCreate() {
        super.onCreate();
        ms_instance = this;

        // Register MCSLogger. used for internal logs.
        MCSLogger.registerLogger(new LoggerAndroid());
        MCSLogger.setLogLevel(MCSLogger.eAll);

        // Configure AP
        // TODO:
        //  It is Integrator's responsibility to implement a platform-specific IWiFiControlHandlerAP taking an advantage of
        //  hardware specifics, OS API and underlying Linux kernel API
        IWiFiControlHandlerAP contollerHandler = new WiFiControlHandlerAP_Custom();

        // TODO: the experimental implementation provided by Abalta does not work on all platforms and in all edge cases!
        contollerHandler = WiFiManager.instance().getWiFiControlHandlerAP(); // TODO: This must not be executed!

        // Continue configuring of AP
        contollerHandler.setNetworkConfigAP(new NetworkConfigAP(
                "", // ssid is usually generated runtime
                "", // psk is usually generated runtime
                EAPSecurity.APSecurity_WPA3_Enterprise, // this is platform specific
                NetworkConfigAP.KeyManagementType.KEY_MGMT_WPA_PSK,
                NetworkConfigAP.PairwiseType.PAIRWISE_NONE,
                NetworkConfigAP.SecurityProtocolType.PROTO_NONE,
                -1 // network ID is usually provided runtime by the OS
        ));
        WiFiManager.instance().setWiFiControlHandlerAP(contollerHandler);

        // Initialize WebLinkClient
        Map<String, String> properties = getAppProperties();
        m_wlClient = new WebLinkClient(this, properties);

        // Enable advanced logging for the debug version
        if (BuildConfig.DEBUG) {
            CommandLoggingHelper.setIsCommandReceiveLogsEnabled(true);
            CommandLoggingHelper.setIsCommandSendLogsEnabled(true);
        }
    }

    /**
     * Get the Application instance.
     */
    public static App instance() {
        return ms_instance;
    }
    /**
     * Get the application context.
     */
    public static Context getAppContext() {
        return ms_instance != null ? ms_instance.getApplicationContext() : null;
    }
    
    /**
     * Get the WebLinkClient object.
     */
    public WebLinkClient getWLClient() {
        return m_wlClient;
    }

    /**
     * Get the WebLinkClientCore object
     * @return
     */
    public WebLinkClientCore getWLClientCore() {
        return m_wlClient.getWebLinkClientCore();
    }

    private Map<String, String> getAppProperties() {
        final HashMap<String, String> res = new HashMap<>();

        res.put(WLConstants.PROP_BT_SERVICE_LISTEN_UUID, WLTypes.BT_SERVICE_CONNECT_ID_CLIENT);
        res.put(WLConstants.PROP_BT_SERVICE_CONNECT_UUID, WLTypes.BT_SERVICE_LISTEN_ID_HOST);

        return res;
    }

}
