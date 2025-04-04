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
import com.example.myapplication.services.ServiceClient;
import com.example.myapplication.services.Services;
import com.weblink.androidext.wifi.ConnectionScenario_WiFiAP;
import com.weblink.androidext.wifi.IWiFiControlHandlerAP;
import com.weblink.androidext.wifi.IWiFiControlHandlerCallbackAP;
import com.weblink.androidext.wifi.WLScenarioConnection_WiFiAP;
import com.weblink.androidext.wifi.WiFiManager;
import com.weblink.androidext.wifi.ap_capabilities.APCapabilities;
import com.weblink.androidext.wifi.ap_capabilities.APSettingsProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Wrapper class for the notification interfaces to be able to register multiple listeners to certain
 * callbacks.
 */

class WebLinkClient implements IClientNotification,
        WLConnectionManager.ConnectionStateChangedDelegate,
        WLConnectionManager.ConnectionPartialStateChangedDelegate,
        IDeviceScanningNotification {
    private static final java.lang.String TAG = "WebLinkClientTag";

    private static final String DEFAULT_APP_NAME_LANGUAGE = "en";
    private static final short DEFAULT_APP_IMAGE_WIDTH = 96;
    private static final short DEFAULT_APP_IMAGE_HEIGHT = 96;

    private static final int WIFI_AP_PRIORITY = 7;
    //private static final int SOCK_ALT_PRIORITY = 6; //special high priority socket
    private static final int SOCKET_PRIORITY = 5;
    private static final int USB_PRIORITY = 4;
    private static final int PING_INTERVAL_MS = 500;
    private static final int RECONFIGURE_DELAY = 2000;

    /**
     * Predefined consumer controls in the order they are defined in the HID Report Descriptor.
     * These should match the order and presence in the platform's Consumer Report.
     */
    private static final short[] PREDEFINED_CONSUMER_CONTROLS = new short[]{
            HIDRequestProperties.CU_Menu_Home, //first bit
            HIDRequestProperties.CU_Snapshot_Screenshot,
            HIDRequestProperties.CU_AC_Search_Spotlight,
            HIDRequestProperties.CU_AL_Keyboard_Layout_Toggle_Onscreen_Keyboard,
            HIDRequestProperties.CU_Scan_Previous_Track_Transport_Left,
            HIDRequestProperties.CU_Play_Pause_Play_Pause,
            HIDRequestProperties.CU_Scan_Next_Track_Transport_Right,
            HIDRequestProperties.CU_Mute_Mute,
            HIDRequestProperties.CU_Volume_Increment_Louder,
            HIDRequestProperties.CU_Volume_Decrement_Softer,
            HIDRequestProperties.CU_Power_Lock, //final bit
    };

    private final Context m_context;
    private ConnectionManager m_connectionManager;
    private WLConnectionManager m_wlConnectionManager;
    private CustomWLClientCore m_client;
    private ServiceClient m_serviceClient;
    private Services m_services;

    // HID support
    private HIDInputManager m_inputManager;
    private HIDController_AOA m_aoaController;
    private HIDController_USB m_usbController;
    private HIDController_USB m_consumerController;
    private HIDController_TCPIP m_tcpController;

    private AOALayer m_aoaLayer;

    private IClientNotification m_listener = null;
    private final List<IConnectionStatusNotification> m_connListeners = new ArrayList<IConnectionStatusNotification>();
    private final List<IServerUpdateNotification> m_serverListeners = new ArrayList<IServerUpdateNotification>();
    private IPingHandler m_pingHandler;
    private PreferenceHelper m_sharedPref = null;

    private ConnectionScenario_WiFiAP m_connectionScenarioAP = null;
    private ConnectionMethod m_connectionMethodSocket;
    private boolean m_canCreateAP = false;
    private List<PeerDevice> m_connectedDevices = new ArrayList<PeerDevice>();
    private boolean m_isConnected = false;
    private int m_numberOfConnects = 0;
    private String m_lastScenarioID;
    private final Map<String, String> m_properties;
    private BluetoothConnectionMethod m_btConnectionMethod;

    WebLinkClient(final Context context, final Map<String, String> properties) {
        MCSLogger.log(TAG, "Constructor");
        m_sharedPref = new PreferenceHelper(context);

        m_context = context;
        m_properties = properties;

        // Create a custom connection manager that is easier to configure than accessing the internal version.
        createDefaultConnectionManager(context, properties);

        DeviceIdentity myIdentity = new DeviceIdentity();
        myIdentity.setDisplayNameEn(android.os.Build.DISPLAY);
        myIdentity.setManufacturer(Build.MANUFACTURER);
        myIdentity.setModel(Build.MODEL);
        myIdentity.setApplication("WebLink Android Reference Client");
        myIdentity.setApplicationVendor("com.abaltatech");
        String versionName;

        final PackageManager packageManager = context.getPackageManager();
        if (packageManager != null) {
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
                versionName = packageInfo.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                versionName = null;
            }
            myIdentity.setAppVersion(versionName);
        }
        myIdentity.setCountryCodes("us");
        myIdentity.setDisplayNameMultiLanguage("{\"en\":\"" + android.os.Build.DISPLAY + "\"}");
        myIdentity.setOs("Android");
        myIdentity.setOsVersion(String.valueOf(android.os.Build.VERSION.SDK_INT));
        myIdentity.setSerialNumber(Build.SERIAL);
        myIdentity.setSystemId(Build.MANUFACTURER + Build.MODEL);

        // Create the input manager and register all hid controllers
        m_inputManager = new HIDInputManager(context);
        m_aoaController = new HIDController_AOA(m_usbDeviceConnection);
        m_usbController = new HIDController_USB(m_hidUSBConnection);
        //Specify the capabilities for the two USB controllers, allowing each to focus on a single feature.
        //for the usbController (iOS support) we support AssistiveTouch mouse pointer and Stylus.
        m_usbController.setCapabilities(EHIDCapability.CAP_MOUSE, EHIDCapability.CAP_STYLUS);

        //The secondary HIDController_USB will only be used for the Consumer Controls support. This
        //provides WebLink with a way to trigger select actions on the phone through HID controls.
        m_consumerController = new HIDController_USB(m_consumerControlHID);
        m_consumerController.setCapabilities(EHIDCapability.CAP_CONSUMER);
        //Predefine the consumer controls that the platform supports.
        m_consumerController.setSupportedConsumerControls(PREDEFINED_CONSUMER_CONTROLS, PREDEFINED_CONSUMER_CONTROLS.length, 2);

        m_tcpController = new HIDController_TCPIP(); //this is demo only.

        m_inputManager.registerHIDController(m_aoaController);
        m_inputManager.registerHIDController(m_usbController);
        m_inputManager.registerHIDController(m_consumerController);
        //
        m_inputManager.registerHIDController(m_tcpController); //this is demo only.

        // Create and initialize the WebLink Application Catalog Manager
        WLAppCatalogManager appCatalogManager = new WLAppCatalogManager();
        appCatalogManager.init(context.getFilesDir().getAbsolutePath(),
                DEFAULT_APP_IMAGE_WIDTH,
                DEFAULT_APP_IMAGE_HEIGHT,
                DEFAULT_APP_NAME_LANGUAGE,
                true,
                new AndroidFileManager(context));

        // Initialize the WiFi Manager
        WiFiManager wifiManager = WiFiManager.instance();
        wifiManager.setContext(context.getApplicationContext());
        wifiManager.getWiFiControlHandlerSTA().setContext(context);

        // Create the client core
        m_client = new CustomWLClientCore(
                context,
                this,
                myIdentity,
                appCatalogManager,
                m_connectionManager,
                m_inputManager,
                m_wlConnectionManager) {
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
                else
                {
                    byte[] pongPayload = new byte[2];
                    short pongFlags = 0x0001; // Set "Is Response Message" flag
                    pongPayload[0] = (byte)(pongFlags & 0xFF);        // Low byte
                    pongPayload[1] = (byte)((pongFlags >> 8) & 0xFF); // High byte

                    // Step 9: Create the Pong command
                    Command pongCommand = new Command((short)0x4C, pongPayload, 0, 2);

                    IWebLinkConnection conn = m_client.getConnection();
                    conn.sendCommand(pongCommand);
                }

                if (m_pingHandler != null) {
                    m_pingHandler.onPingResponseReceived(isSenderInactive);
                }
            }
        };

        //Set the periodic ping parameters. Ping helps test the server for responsiveness.
        m_client.setPeriodicPingParams(PING_INTERVAL_MS, m_sharedPref.getConnectionTimeout());
        //set the reconfiguration delay. this happens if the phone app loses state (killed).
        m_client.setReconfigureDelay(RECONFIGURE_DELAY);

        //optional features: client services.
        m_serviceClient = new ServiceClient();
        m_services = new Services();

        //setupAudio();
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
            m_client.onConnectionEstablished(manager);
            // TODO: Notify app about onConnectionStateChanged changed if needed
            EventLogger.logEventStart(EventLogger.EWLLogEvents.WL_CLIENT_CONNECTED);
        } else if (wasConnected && !m_isConnected) {
            if (BuildConfig.DEBUG) {
                MCSLogger.log(TAG, "[ConnectivityTesting] Disconnect number[" + m_numberOfConnects + "]");
            }
            m_client.onConnectionClosed(manager);
            // TODO: Notify app about onConnectionStateChanged changed if needed
            EventLogger.logEventEnd(EventLogger.EWLLogEvents.WL_CLIENT_CONNECTED);
        } else {
            MCSLogger.log(TAG, "onConnectionStateChanged: Other event");
            String lastActiveScenario = manager.getActiveScenarioID();
            if (lastActiveScenario == null) {
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

    public WebLinkClientCore getWebLinkClientCore() {
        return m_client;
    }

    public boolean onConnectionAttempt(PeerDevice device) {
        if (device == null) {
            MCSLogger.log(MCSLogger.eWarning, TAG, "Fail to connect to device, PeerDevice is null");
            return false;
        }

        MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "Connecting to device " + device.toString());
        return m_client.connect(device, IClientNotification.EProtocolType.ePT_WL, -1);
    }

    public void terminate() {
        return;
    }

    /**
     * Create a custom configuration for the WEBLINK connection manager, which can be passed to
     * the client core to override the default internal version.
     *
     * @param context    android context
     * @param properties settings for the connection manager
     * @return the default connection manager.
     */
    protected void createDefaultConnectionManager(final Context context, final Map<String, String> properties) {
        MCSLogger.log(TAG, "createDefaultConnectionManager, properties: " + properties);
        m_connectionManager = new ConnectionManager();
        m_connectionManager.registerScanningNotification(this);
        m_wlConnectionManager = new WLConnectionManager(m_connectionManager, WebLinkConnection.MAX_COMMANDS_SERVER, true);
        m_wlConnectionManager.setStateChangeDelegate(this);
        m_wlConnectionManager.setConnectionPartialStateChangedDelegate(this);

        m_connectionManager.registerNotification(new IDeviceStatusNotification() {
            @Override
            public boolean onFavoriteDeviceAvailable(PeerDevice favoriteDevice) {
                //nothing to do
                MCSLogger.log(MCSLogger.eDebug, TAG, "onFavoriteDeviceAvailable");
                return false;
            }

            @Override
            public boolean onDeviceConnected(PeerDevice device, IMCSDataLayer dataLayer) {
                MCSLogger.log(MCSLogger.eDebug, TAG, "onDeviceConnected");
                return true; //should return true if we want to keep the device.
            }

            @Override
            public void onDeviceConnectFailed(PeerDevice device, EConnectionResult result) {
                //nothing to do
                MCSLogger.log(MCSLogger.eDebug, TAG, "onDeviceConnectFailed");
            }

            @Override
            public void onDeviceDisconnected(PeerDevice device) {
                //nothing to do
                MCSLogger.log(MCSLogger.eDebug, TAG, "onDeviceDisconnected");
            }

            @Override
            public void onAutoconnectFailed(EConnectionResult result) {
                //nothing to do
                MCSLogger.log(MCSLogger.eDebug, TAG, "onAutoconnectFailed");
            }
        });

        ConnectionMethod connMethodAOA = new ConnectionMethodAOA(context,
                context.getString(R.string.aoa_manufacturer),
                context.getString(R.string.aoa_model),
                context.getString(R.string.aoa_version),
                context.getString(R.string.aoa_description),
                context.getString(R.string.aoa_url),
                context.getString(R.string.aoa_serial));
        connMethodAOA.setPriority(USB_PRIORITY);
        registerMethod(connMethodAOA);

        ConnectionMethod connectionMethodSocket = new AndroidSocketConnectionMethod(12345, WLTypes.SERVER_DEFAULT_BROADCAST_PORT);
        connectionMethodSocket.setPriority(SOCKET_PRIORITY);
        registerMethod(connectionMethodSocket);
        m_connectionMethodSocket = connectionMethodSocket;

        m_connectionManager.setSerializer(new WLSerializer(context, "ConnectionManager"));
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

    private final IHIDUSBConnection m_consumerControlHID = new IHIDUSBConnection() {
        @Override
        public boolean prepare(IHIDController controller) {
            //TODO prepare the native platform references
            MCSLogger.log(TAG, "prepare: " + controller);
            return true; //for now is a placeholder.  returning false here will prevent this controller from activating.
        }

        @Override
        public boolean release(IHIDController controller) {
            //TODO free up native platform references (if required)
            MCSLogger.log(TAG, "release: " + controller);
            return false; //for now is a placeholder.
        }

        @Override
        public boolean isAvailable(IHIDController controller) {
            //TODO query the native platform's device state to see if connection.
            return m_client.isConnected(); //for now is a placeholder.
        }

        @Override
        public boolean startHIDDescriptor(int deviceId, EHIDCapability capability, byte[] descriptorData, int descriptorLength) {
            //TODO do native platform's actions to start a HID descriptor.
            // The platform may want to declare the USB HID report descriptor at start of the USB setup. In that case this value can be ignored.
            MCSLogger.log(TAG, "[HID CC]startHIDDescriptor: " + deviceId + "cap=" + capability + " d=" + Arrays.toString(descriptorData));
            return m_client.isConnected(); //TODO - return the actual state.
        }

        @Override
        public boolean stopHIDDescriptor(int deviceId, EHIDCapability capability) {
            //TODO do native platform's actions to stop a HID descriptor (if required).
            MCSLogger.log(TAG, "[HID CC]stopHIDDescriptor: " + deviceId + "cap=" + capability);
            return m_client.isConnected(); //TODO - return the actual result.
        }

        @Override
        public boolean sendHIDReport(int deviceId, EHIDCapability capability, byte[] reportData, int reportLength) {
            //TODO do native platform's actions to send a HID input report.
            MCSLogger.log(TAG, "[HID CC]sendHIDReport: " + deviceId + "cap=" + capability + " d=" + Arrays.toString(reportData));
            return m_client.isConnected();
        }
    };
    private final IUSBDeviceConnection m_usbDeviceConnection = new IUSBDeviceConnection() {
        @Override
        public boolean prepare(IHIDController controller) {
            // we are prepared if the aoaLayer is not-null!
            return m_aoaLayer != null;
        }

        @Override
        public boolean release(IHIDController controller) {
            //TODO free up native platform references (if needed)
            return true;
        }

        @Override
        public boolean isAvailable(IHIDController controller) {
            return m_aoaLayer != null;
        }

        @Override
        public boolean sendControlTransfer(int requestType, int requestId, int value, byte[] payload, int payloadLength, int timeOutMs) {
            if (m_aoaLayer != null) {
                return m_aoaLayer.sendControlTransfer(requestType, requestId, value, payload, payloadLength, timeOutMs);
            }
            return false;
        }

        @Override
        public boolean sendControlTransfer(int requestType, int requestId, int value, int index, int timeOutMs) {
            if (m_aoaLayer != null) {
                return m_aoaLayer.sendControlTransfer(requestType, requestId, value, index, timeOutMs);
            }
            return false;
        }
    };

    private final IHIDUSBConnection m_hidUSBConnection = new IHIDUSBConnection() {
        private IMCSDataLayer m_dataLayer;

        @Override
        public boolean prepare(IHIDController controller) {
            MCSLogger.log(TAG, "IHIDUSBConnection prepare");
            //TODO prepare the native platform references
            m_dataLayer = TCPIPHIDUtils.getInstance().getLayer();
            return m_dataLayer != null; //for now is a placeholder. returning false here will prevent this controller from activating.
        }

        @Override
        public boolean release(IHIDController controller) {
            MCSLogger.log(TAG, "IHIDUSBConnection release");
            //TODO free up native platform references (if required)
            m_dataLayer = null;
            return false; //for now is a placeholder.
        }

        @Override
        public boolean isAvailable(IHIDController controller) {
            MCSLogger.log(TAG, "IHIDUSBConnection isAvailable");
            //TODO query the native platform's device state to see if connection.
            return m_connectionManager.getConnectedDevices().size() != 0; //for now is a placeholder.
        }

        /**
         *
         * @param deviceId the HID session unique identifier specific to this HID descriptor
         * @param capability the specific HID feature type being started with this deviceId.
         * @param descriptorData the array containing the HID class descriptor for this HID session
         * @param descriptorLength number of bytes to use for the descriptor.
         *
         * @return
         */
        @Override
        public boolean startHIDDescriptor(int deviceId, EHIDCapability capability, byte[] descriptorData, int descriptorLength) {
            MCSLogger.log(TAG, "startHIDDescriptor");
            //TODO do native platform's actions to start a HID descriptor.
            return m_dataLayer != null; //for now is a placeholder.
        }

        @Override
        public boolean stopHIDDescriptor(int deviceId, EHIDCapability capability) {
            //TODO do native platform's actions to stop a HID descriptor (if required).
            MCSLogger.log(TAG, "[HID USB]stopHIDDescriptor: " + deviceId + "cap=" + capability);
            return true; //for now is a placeholder.
        }

        @Override
        public boolean sendHIDReport(int deviceId, EHIDCapability capability, byte[] reportData, int reportLength) {
            //TODO do native platform's actions to send a HID input report.
            MCSLogger.log(TAG, "[HID USB]sendHIDReport: " + deviceId + "cap=" + capability + " d=" + Arrays.toString(reportData));
            TCPIPHIDUtils.getInstance().sendBufferEvent(reportData, reportLength);
            return m_dataLayer != null; //for now is a placeholder.
        }
    };

    public boolean isConnected() {
        return m_isConnected;
    }

    @Override
    public void onServerListUpdated(ServerInfo[] servers) {

    }

    @Override
    public void onConnectionEstablished(PeerDevice peerDevice) {
        MCSLogger.log(eInfo, TAG, "Connection established with " + peerDevice.getName());
        if (m_listener != null) {
            m_listener.onConnectionEstablished(peerDevice);
        }
        synchronized (m_connListeners) {
            for (IConnectionStatusNotification listener : m_connListeners) {
                listener.onConnectionEstablished(peerDevice);
            }
        }
    }

    @Override
    public void onConnectionFailed(PeerDevice peerDevice, EConnectionResult result) {
        MCSLogger.log(eError, TAG, "Connection failed with " + peerDevice.getName() + ". Result: " + result);
        if (m_listener != null) {
            m_listener.onConnectionFailed(peerDevice, result);
        }
        synchronized (m_connListeners) {
            for (IConnectionStatusNotification listener : m_connListeners) {
                listener.onConnectionFailed(peerDevice, result);
            }
        }
    }

    @Override
    public void onConnectionClosed(PeerDevice peerDevice) {
        MCSLogger.log(eInfo, TAG, "Connection closed with " + peerDevice.getName());
        if (m_listener != null) {
            m_listener.onConnectionClosed(peerDevice);
        }
        synchronized (m_connListeners) {
            for (IConnectionStatusNotification listener : m_connListeners) {
                listener.onConnectionClosed(peerDevice);
            }
        }
    }

    @Override
    public void onApplicationChanged(int i) {

    }

    @Override
    public void onFrameRendered() {

    }

    @Override
    public boolean canProcessFrame() {
        return false;
    }

    @Override
    public void onShowKeyboard(short i) {

    }

    @Override
    public void onHideKeyboard() {

    }

    @Override
    public void onWaitIndicator(boolean b) {

    }

    @Override
    public void onAppImageChanged(int i, Bitmap bitmap) {

    }

    @Override
    public void onConnectionLost() {
        MCSLogger.log(eInfo, TAG, "Connection  lost");
        if(m_listener != null) {
            m_listener.onConnectionLost();
        }
    }

    @Override
    public void onConnectionResumed() {
        MCSLogger.log(eInfo, TAG, "Connection  resumed");
        if(m_listener != null) {
            m_listener.onConnectionResumed();
        }
    }

    @Override
    public boolean onCommandReceived(Command command) {
        if (command == null || command.getCommandID() != 0x4C || !command.isValid()) {
            MCSLogger.log(TAG, "Received invalid or non-ping command");
            return false;
        }

        try {
            MCSLogger.log(TAG, "Received Ping command: " + command.getCommandDataString());

            DataBuffer payload = new DataBuffer();
            if (!command.getPayload(payload)) {
                MCSLogger.log(TAG, "Failed to extract ping payload");
                return false;
            }

            if (payload.getSize() < 2) {
                MCSLogger.log(TAG, "Ping payload too small");
                return false;
            }

            short receivedFlags = payload.getShort(0);
            boolean isResponse = (receivedFlags & 0x0001) != 0;
            boolean isRegularPing = (receivedFlags & 0x0002) != 0;

            MCSLogger.log(TAG, "Ping details - IsResponse: " + isResponse + ", IsRegularPing: " + isRegularPing);

            if (isResponse) {
                MCSLogger.log(TAG, "Received ping response, not sending pong");
                return true;
            }

            // Prepare and send pong
            byte[] pongPayload = new byte[2];
            short pongFlags = 0x0001; // Set "Is Response Message" flag
            pongPayload[0] = (byte)(pongFlags & 0xFF);
            pongPayload[1] = (byte)((pongFlags >> 8) & 0xFF);

            Command pongCommand = new Command((short)0x4C, pongPayload, 0, 2);

            IWebLinkConnection conn = m_client.getConnection();
            conn.sendCommand(pongCommand);

            MCSLogger.log(TAG, "Sent Pong command");

            return true;

        } catch (Exception e) {
            MCSLogger.log(TAG, "Error handling ping command", e);
            return false;
        }
    }

    @Override
    public void onAudioChannelStarted(int i) {

    }

    @Override
    public void onAudioChannelStopped(int i) {

    }

    @Override
    public boolean onDeviceScanningBegin(String s) {
        return true;
    }

    @Override
    public boolean onDeviceFound(PeerDevice peerDevice) {
        return false;
    }

    @Override
    public void onDeviceLost(PeerDevice peerDevice) {
        MCSLogger.log(TAG, "onDeviceLost: %s", peerDevice.toString());
        for (PeerDevice it : m_connectedDevices) {
            if (it.equals(peerDevice)) {
                m_connectedDevices.remove(it);
                m_connectionManager.disconnectDevice(peerDevice);
                break;
            }
        }
    }

    @Override
    public void onDeviceScanningEnd(String s) {

    }

    @Override
    public void onConnectionPartialStateChanged(WLConnectionManager connectionManager, ConnectionScenario scenario, PeerDevice peerDevice, int step) {
        if (connectionManager.isPartialConnected() && !connectionManager.isConnected()){
            m_client.onConnectionPartiallyConnected(connectionManager);
        }
    }
} 