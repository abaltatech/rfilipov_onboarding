package com.example.myapplication;

import static android.content.ContentValues.TAG;

import static com.abaltatech.mcs.logger.MCSLogger.ELogType.eError;
import static com.abaltatech.mcs.logger.MCSLogger.ELogType.eInfo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.abaltatech.mcs.BuildConfig;
import com.abaltatech.mcs.bluetooth.android.BluetoothConnectionMethod;
import com.abaltatech.mcs.common.IMCSConnectionClosedNotification;
import com.abaltatech.mcs.common.IMCSDataLayer;
import com.abaltatech.mcs.connectionmanager.ConnectionManager;
import com.abaltatech.mcs.connectionmanager.ConnectionMethod;
import com.abaltatech.mcs.connectionmanager.ConnectionScenario;
import com.abaltatech.mcs.connectionmanager.EConnectionResult;
import com.abaltatech.mcs.connectionmanager.IDeviceScanningNotification;
import com.abaltatech.mcs.connectionmanager.IDeviceStatusNotification;
import com.abaltatech.mcs.connectionmanager.PeerDevice;
import com.abaltatech.mcs.logger.MCSLogger;
import com.abaltatech.mcs.logger.android.EventLogger;
import com.abaltatech.mcs.socket.android.AndroidSocketConnectionMethod;
import com.abaltatech.mcs.usbhost.android.ConnectionMethodAOA;
import com.abaltatech.mcs.usbhost.android.AOALayer;
import com.abaltatech.mcs.utils.android.WLSerializer;
import com.abaltatech.weblink.core.DataBuffer;
import com.abaltatech.weblink.core.IWebLinkConnection;
import com.abaltatech.weblink.core.WLConnectionManager;
import com.abaltatech.weblink.core.WLScenarioConnection;
import com.abaltatech.weblink.core.WLTypes;
import com.abaltatech.weblink.core.WebLinkConnection;
import com.abaltatech.weblink.core.audioconfig.WLAudioChannelMapping;
import com.abaltatech.weblink.core.authentication.DeviceIdentity;
import com.abaltatech.weblink.core.commandhandling.Command;
import com.abaltatech.weblink.core.commandhandling.hid.HIDRequestProperties;
import com.abaltatech.weblinkclient.IClientNotification;
import com.abaltatech.weblinkclient.WebLinkClientCore;
import com.abaltatech.weblinkclient.appcatalog.WLAppCatalogManager;
import com.abaltatech.weblinkclient.audiodecoding.AudioDecoder_MediaCodec;
import com.abaltatech.weblinkclient.audiodecoding.AudioOutput;
import com.abaltatech.weblinkclient.audiodecoding.IAudioDecoder;
import com.abaltatech.weblinkclient.audiodecoding.IAudioOutput;
import com.abaltatech.weblinkclient.hid.EHIDCapability;
import com.abaltatech.weblinkclient.hid.HIDController_AOA;
import com.abaltatech.weblinkclient.hid.HIDController_TCPIP;
import com.abaltatech.weblinkclient.hid.HIDController_USB;
import com.abaltatech.weblinkclient.hid.HIDInputManager;
import com.abaltatech.weblinkclient.hid.IHIDController;
import com.abaltatech.weblinkclient.hid.IHIDUSBConnection;
import com.abaltatech.weblinkclient.hid.IUSBDeviceConnection;
import com.abaltatech.weblinkclient.hid.TCPIPHIDUtils;
import com.weblink.androidext.wifi.ConnectionScenario_WiFiAP;
import com.weblink.androidext.wifi.IWiFiControlHandlerAP;
import com.weblink.androidext.wifi.IWiFiControlHandlerCallbackAP;
import com.weblink.androidext.wifi.WLScenarioConnection_WiFiAP;
import com.weblink.androidext.wifi.WiFiManager;
import com.weblink.androidext.wifi.ap_capabilities.APCapabilities;
import com.weblink.androidext.wifi.ap_capabilities.APSettingsProvider;
import com.example.myapplication.presentation.AndroidFileManager;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.example.myapplication.WLConstants.DEFAULT_APP_IMAGE_HEIGHT;
import static com.example.myapplication.WLConstants.DEFAULT_APP_IMAGE_WIDTH;
import static com.example.myapplication.WLConstants.DEFAULT_APP_NAME_LANGUAGE;
import static com.example.myapplication.WLConstants.SOCKET_PORT;


public class WebLinkClient implements IClientNotification,
        IDeviceScanningNotification,
        IDeviceStatusNotification,
        WLConnectionManager.ConnectionStateChangedDelegate,
        WLConnectionManager.ConnectionPartialStateChangedDelegate {


    private String TAG = "WebLinkClient";
    private WebLinkClientCore m_clientCore;
    private ConnectionManager m_connectionManager;
    private WLConnectionManager m_wlConnectionManager;
    private DeviceIdentity m_deviceIdentity;
    private HIDInputManager m_inputManager;
    private IClientNotification m_listener = null;
    private final List<IConnectionStatusNotification> m_connListeners = new ArrayList<IConnectionStatusNotification>();
    private final List<IServerUpdateNotification> m_serverListeners = new ArrayList<IServerUpdateNotification>();
    private AOALayer m_aoaLayer;
    private IPingHandler m_pingHandler;
    private boolean m_isConnected = false;
    private int m_numberOfConnects = 0;
    private String m_lastScenarioID;




    WebLinkClient(Context context, Map<String, String> properties){

        initDeviceIdentity();

        m_inputManager = new HIDInputManager(context);
        HIDController_TCPIP m_tcpController = new HIDController_TCPIP();
        m_inputManager.registerHIDController(m_tcpController);

        WLAppCatalogManager appCatalogManager = new WLAppCatalogManager();
        appCatalogManager.init(context.getFilesDir().getAbsolutePath(),
                DEFAULT_APP_IMAGE_WIDTH,
                DEFAULT_APP_IMAGE_HEIGHT,
                DEFAULT_APP_NAME_LANGUAGE,
                true,
                new AndroidFileManager(context));

        WiFiManager wiFiManager = WiFiManager.instance();
        wiFiManager.setContext(context);
        wiFiManager.getWiFiControlHandlerSTA().setContext(context);

        createWebLinkConnectionManager();

        m_clientCore = new WebLinkClientCore(context,
                this,
                m_deviceIdentity,
                appCatalogManager,
                m_connectionManager,
                m_inputManager,
                m_wlConnectionManager){
            @Override
            protected void onPingResponseTimeout() {
                MCSLogger.log(TAG, "onPingResponseTimeout");
                super.onPingResponseTimeout(); //Always call super for this function.
                MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "onPingResponseTimeout called!");
                //the communication is blocked!  do your own connection shutdown / stopping to reset!

                if (m_pingHandler != null) {
                    m_pingHandler.onPingResponseTimeout();
                }
            }

            @Override
            protected void onPingResponseReceived(boolean isSenderInactive) {
                MCSLogger.log(TAG, "onPingResponseReceived, isSenderInactive: " + isSenderInactive);
                super.onPingResponseReceived(isSenderInactive);//Always call super for this function.
                MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "onPingResponseReceived "+isSenderInactive);
                if (isSenderInactive) {
                    //Do your own restart of the connection here! (if not auto-reconfiguring).
                }

                if (m_pingHandler != null) {
                    m_pingHandler.onPingResponseReceived(isSenderInactive);
                }
            }
        };

    }

    void initDeviceIdentity()
    {
        m_deviceIdentity = new DeviceIdentity();
        m_deviceIdentity.setSystemId(Build.MANUFACTURER + Build.MODEL + Build.SERIAL);
        m_deviceIdentity.setDisplayNameEn(android.os.Build.DISPLAY);
        m_deviceIdentity.setManufacturer(Build.MANUFACTURER);
        m_deviceIdentity.setModel(Build.MODEL);
        m_deviceIdentity.setCountryCodes("US,CA");
        m_deviceIdentity.setSerialNumber(Build.SERIAL);
    }

    void createWebLinkConnectionManager() {
        m_connectionManager = new ConnectionManager();
        m_connectionManager.registerScanningNotification(this);
        m_connectionManager.registerNotification(this);
        m_wlConnectionManager = new WLConnectionManager(m_connectionManager, WebLinkConnection.MAX_COMMANDS_SERVER, true);
        m_wlConnectionManager.setStateChangeDelegate(this);
        m_wlConnectionManager.setConnectionPartialStateChangedDelegate(this);

        final AndroidSocketConnectionMethod androidSocketMethod = new AndroidSocketConnectionMethod(SOCKET_PORT, WLTypes.SERVER_DEFAULT_BROADCAST_PORT);
        androidSocketMethod.setPriority(5);
        registerMethod(androidSocketMethod);
    }

    private void registerMethod(ConnectionMethod method) {
        if (method != null) {
            MCSLogger.log(TAG, "registerMethod(): " + method.getConnectionMethodID());
            m_connectionManager.registerConnectionMethod(method);

            String id = method.getConnectionMethodID();
            ConnectionScenario connScenario = new ConnectionScenario(method.getPriority(), id, m_connectionManager);
            connScenario.addNextStepConnectionMethod(id);
            m_connectionManager.addScenario(connScenario);

            WLScenarioConnection scenarioConnection = new WLScenarioConnection(connScenario, id, id);
            m_wlConnectionManager.addScenarioConnection(scenarioConnection);
        }
    }

    WebLinkClientCore getWebLinkClientCore(){
        return m_clientCore;
    }

    public boolean connectToDevice(PeerDevice device)
    {
        if (device == null) {
            MCSLogger.log(MCSLogger.eWarning, TAG, "ConnectToDevice():: Fail to connect to device, PeerDevice is null");
            return false;
        }

        MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "ConnectToDevice():: Connecting to device " + device.toString());
        return getWebLinkClientCore().connect(device, IClientNotification.EProtocolType.ePT_WL, -1);
    }

    /**
     * add the listener from the list of listeners to server list updates.
     *
     * @param listener
     */
    public void registerServerUpdateListener(IServerUpdateNotification listener) {
        synchronized (m_serverListeners) {
            if (!m_serverListeners.contains(listener)) {
                m_serverListeners.add(listener);
            }
        }
    }


    /**
     * add the listener from the list of listeners to connection updates.
     *
     * @param listener
     */
    public void registerConnectionListener(IConnectionStatusNotification listener) {
        synchronized (m_connListeners) {
            if (!m_connListeners.contains(listener)) {
                m_connListeners.add(listener);
            }
        }
    }

    /**
     * Remove the listener from the list of listeners to connection updates.
     *
     * @param listener
     */
    public void unregisterConnectionListener(IConnectionStatusNotification listener) {
        synchronized (m_connListeners) {
            m_connListeners.remove(listener);
        }
    }

    public void unregisterServerUpdate(IServerUpdateNotification listener) {
        synchronized (m_serverListeners) {
            m_serverListeners.remove(listener);
        }
    }

    /**
     * @param servers
     */
    @Override
    public void onServerListUpdated(ServerInfo[] servers) {
        for (ServerInfo server : servers) {
            MCSLogger.log(TAG, "Server: " + server.toString());
        }

        if (m_listener != null) {
            m_listener.onServerListUpdated(servers);
        }

        synchronized (m_serverListeners) {
            for (IServerUpdateNotification listener : m_serverListeners) {
                listener.onServerListUpdated(servers);
            }
        }
    }

    /**
     * @param peerDevice
     */
    @Override
    public void onConnectionEstablished(PeerDevice peerDevice) {
        MCSLogger.log(TAG, "onConnectionEstablished(): ");

        if (m_listener != null) {
            m_listener.onConnectionEstablished(peerDevice);
        }

        synchronized (m_connListeners) {
            for (IConnectionStatusNotification listener : m_connListeners) {
                listener.onConnectionEstablished(peerDevice);
            }
        }

        //startAudio() ??

    }

    /**
     * @param peerDevice
     * @param eConnectionResult
     */
    @Override
    public void onConnectionFailed(PeerDevice peerDevice, EConnectionResult eConnectionResult) {
        MCSLogger.log(TAG, "onConnectionFailed(): ");
    }

    /**
     * @param peerDevice
     */
    @Override
    public void onConnectionClosed(PeerDevice peerDevice) {
        MCSLogger.log(TAG, "onConnectionClosed(): ");
        if (m_listener != null) {
            m_listener.onConnectionClosed(peerDevice);
        }
    }

    /**
     * @param appId
     */
    @Override
    public void onApplicationChanged(int appId) {
        if (m_listener != null) {
            m_listener.onApplicationChanged(appId);
        }
    }

    /**
     *
     */
    @Override
    public void onFrameRendered() {
        if (m_listener != null) {
            m_listener.onFrameRendered();
        }
    }

    /**
     * @return
     */
    @Override
    public boolean canProcessFrame() {
        if (m_listener != null) {
            return m_listener.canProcessFrame();
        }
        return true;
    }

    /**
     * @param i
     */
    @Override
    public void onShowKeyboard(short i) {

    }

    /**
     *
     */
    @Override
    public void onHideKeyboard() {

    }

    /**
     * @param isLoading
     */
    @Override
    public void onWaitIndicator(boolean isLoading) {
        if (m_listener != null) {
            m_listener.onWaitIndicator(isLoading);
        }
    }

    /**
     * @param appId
     * @param bitmap
     */
    @Override
    public void onAppImageChanged(int appId, Bitmap bitmap) {
        if (m_listener != null) {
            m_listener.onAppImageChanged(appId, bitmap);
        }
    }

    /**
     *
     */
    @Override
    public void onConnectionLost() {

    }

    /**
     *
     */
    @Override
    public void onConnectionResumed() {

    }

    /**
     * @param command
     * @return
     */
    @Override
    public boolean onCommandReceived(Command command) {
        return true;
    }

    /**
     * @param i
     */
    @Override
    public void onAudioChannelStarted(int i) {

    }

    /**
     * @param i
     */
    @Override
    public void onAudioChannelStopped(int i) {

    }


    /**
     * @param s
     * @return
     */
    @Override
    public boolean onDeviceScanningBegin(String s) {
        return true;
    }

    /**
     * @param peerDevice
     * @return
     */
    @Override
    public boolean onDeviceFound(PeerDevice peerDevice) {
        return false;
    }

    /**
     * @param peerDevice
     */
    @Override
    public void onDeviceLost(PeerDevice peerDevice) {

    }

    /**
     * @param s
     */
    @Override
    public void onDeviceScanningEnd(String s) {

    }

    /**
     * @param peerDevice
     * @return
     */
    @Override
    public boolean onFavoriteDeviceAvailable(PeerDevice peerDevice) {
        return false;
    }

    /**
     * @param peerDevice
     * @param dataLayer
     * @return
     */
    @Override
    public boolean onDeviceConnected(PeerDevice peerDevice, IMCSDataLayer dataLayer) {
        if (dataLayer instanceof AOALayer) {
            m_aoaLayer = (AOALayer) dataLayer;
            //detect when the data layer is closed to remove it.
            m_aoaLayer.registerCloseNotification(new IMCSConnectionClosedNotification() {
                @Override
                public void onConnectionClosed(IMCSDataLayer connection) {
                    connection.unregisterCloseNotification(this);
                    m_aoaLayer = null;
                }
            });
        }

        return true;
    }

    /**
     * @param peerDevice
     * @param result
     */
    @Override
    public void onDeviceConnectFailed(PeerDevice peerDevice, EConnectionResult result) {
        if (m_listener != null) {
            m_listener.onConnectionFailed(peerDevice, result);
        }

        //TODO: call connection fail m_connListListeners
    }

    /**
     * @param peerDevice
     */
    @Override
    public void onDeviceDisconnected(PeerDevice peerDevice) {
        MCSLogger.log(MCSLogger.eDebug, "onDeviceDisconnected()");
    }

    /**
     * @param eConnectionResult
     */
    @Override
    public void onAutoconnectFailed(EConnectionResult eConnectionResult) {

    }

    /**
     * @param wlConnectionManager
     * @param connectionScenario
     * @param peerDevice
     * @param i
     */
    @Override
    public void onConnectionPartialStateChanged(WLConnectionManager wlConnectionManager, ConnectionScenario connectionScenario, PeerDevice peerDevice, int i) {

    }

    @Override
    public void onConnectionStateChanged(WLConnectionManager manager, IMCSDataLayer dataLayer) {
        boolean wasConnected = m_isConnected;
        m_isConnected = m_wlConnectionManager.isConnected();
        MCSLogger.log(TAG, "onConnectionStateChanged(): isConnected=" + m_isConnected);

        if (!manager.isPartialConnected() && !manager.isConnected()) {
            // TODO: Notify app about onConnectionStateChanged changed if needed
        }
        if (!wasConnected && m_isConnected) {
            if (BuildConfig.DEBUG) {
                m_numberOfConnects++;
                MCSLogger.log(TAG, "[ConnectivityTesting] Connect    number[" + m_numberOfConnects + "]");
            }
            if(dataLayer instanceof AOALayer) {
                m_aoaLayer = (AOALayer) dataLayer;
                //detect when the data layer is closed to remove it.
                m_aoaLayer.registerCloseNotification(new IMCSConnectionClosedNotification(){
                    @Override
                    public void onConnectionClosed(IMCSDataLayer connection) {
                        connection.unregisterCloseNotification(this);
                        m_aoaLayer = null;
                    }
                });
            }
            m_clientCore.onConnectionEstablished(manager);
            // TODO: Notify app about onConnectionStateChanged changed if needed
            EventLogger.logEventStart(EventLogger.EWLLogEvents.WL_CLIENT_CONNECTED);
        } else if (wasConnected && !m_isConnected) {
            if (BuildConfig.DEBUG) {
                MCSLogger.log(TAG, "[ConnectivityTesting] Disconnect number[" + m_numberOfConnects + "]");
            }
            m_clientCore.onConnectionClosed(manager);
            // TODO: Notify app about onConnectionStateChanged changed if needed
            EventLogger.logEventEnd(EventLogger.EWLLogEvents.WL_CLIENT_CONNECTED);
        } else {
            MCSLogger.log(TAG, "onConnectionStateChanged: Other event");
            String lastActiveScenario = manager.getActiveScenarioID();
            if(lastActiveScenario == null) {
                return;
            }
            String lastScenarioID = m_lastScenarioID;
            if (lastScenarioID != null && !lastScenarioID.equals(lastActiveScenario)) {
                MCSLogger.log(TAG, "Active Scenario is changed: " + lastScenarioID + " >> " + lastActiveScenario);
            }
            // TODO: Notify app about onConnectionStateChanged if needed
        }
        m_lastScenarioID = manager.getActiveScenarioID();
    }

    boolean isM_isConnected(){
        return  m_isConnected;
    }
}

