package com.example.myapplication;

import android.content.Context;

import com.abaltatech.mcs.connectionmanager.ConnectionManager;
import com.abaltatech.mcs.logger.MCSLogger;
import com.abaltatech.weblink.core.WLConnectionManager;
import com.abaltatech.weblink.core.authentication.DeviceIdentity;
import com.abaltatech.weblinkclient.IClientNotification;
import com.abaltatech.weblinkclient.WebLinkClientCore;
import com.abaltatech.weblinkclient.appcatalog.WLAppCatalogManager;
import com.abaltatech.weblinkclient.hid.HIDInputManager;

public class CustomWLClientCore extends WebLinkClientCore {
    private static final String TAG = "DEBUG";

    public CustomWLClientCore(Context context, WebLinkClient notification, DeviceIdentity myIdentity, WLAppCatalogManager appCatalogManager, ConnectionManager mConnectionManager, HIDInputManager mInputManager, WLConnectionManager mWlConnectionManager) {
        super(context, notification, myIdentity, appCatalogManager, mConnectionManager, mInputManager, mWlConnectionManager);
    }

    @Override
    protected void onPingResponseTimeout() {
        super.onPingResponseTimeout();
    }

    @Override
    protected void onPingResponseReceived(boolean isSenderInactive) {
        super.onPingResponseReceived(isSenderInactive);
        MCSLogger.log(TAG, "Ping timeout occurred - connection may be lost");        // ...
    }
}
