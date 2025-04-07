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
        IDeviceScanningNotification  {

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
    private static final short[] PREDEFINED_CONSUMER_CONTROLS = new short[] {
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
    private WebLinkClientCore m_client;
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
    private final Map<String, String>          m_properties;
    private IWiFiControlHandlerAP m_controlHandlerAP = WiFiManager.instance().getWiFiControlHandlerAP();
    private BluetoothConnectionMethod m_btConnectionMethod;


    public boolean onConnectionAttempt(PeerDevice device) {
        if (device == null) {
            MCSLogger.log(MCSLogger.eWarning, TAG, "Fail to connect to device, PeerDevice is null");
            return false;
        }

        MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "Connecting to device " + device.toString());
        return m_client.connect(device, IClientNotification.EProtocolType.ePT_WL, -1);
    }

    /**
     * Setup the client wrapper, which acts as the main receiver for WebLinkClientCore notifications.
     * This allows multiple UI classes register/unregister the notifications they care about.
     * @param context Application context
     * @param properties WebLinkClient properties, e.g. WiFi AP settings
     */

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
        myIdentity.setCountryCodes( "us" );
        myIdentity.setDisplayNameMultiLanguage( "{\"en\":\"" + android.os.Build.DISPLAY + "\"}" );
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
        m_consumerController.setSupportedConsumerControls( PREDEFINED_CONSUMER_CONTROLS, PREDEFINED_CONSUMER_CONTROLS.length, 2 );

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
        m_client = new WebLinkClientCore(
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
                //MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "onPingResponseReceived "+isSenderInactive);
                if(isSenderInactive) {
                    //Do your own restart of the connection here! (if not auto-reconfiguring).
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

        setupAudio();
    }

    public void setupAudio() {
        MCSLogger.log(TAG, "setupAudio");
        AudioConfigFileParser parser = null;
        InputStream is;
        try {
            is = m_context.getAssets().open("AudioChannelsConfig.ini");
            parser = new AudioConfigFileParser(is);
        } catch (IOException e) {
            MCSLogger.log(MCSLogger.ELogType.eError, TAG, "Failed to load default configuration file!", e);
            return;
        }

        try {
            if (parser != null) {
                parser.parse();
                for (WLAudioChannelMapping mapping : parser.m_channels) {
                    IAudioDecoder decoder = new AudioDecoder_MediaCodec();
                    IAudioOutput output = new AudioOutput();
                    decoder.setAudioOutput(output);

                    final String audioType = m_sharedPref.getAudioType();
                    if (!audioType.isEmpty()) {
                        List<Integer> audioTypes = new ArrayList<Integer>();
                        audioTypes.add(Integer.parseInt(audioType));
                        mapping.setAudioTypes(audioTypes);
                    }

                    m_client.addAudioChannel(mapping, decoder);
                }
            }
        } catch (Exception e) {
            MCSLogger.printStackTrace(TAG,e);
        }
    }

    void startAudio() {
        m_client.startAudio(0);
    }

    void stopAudio() {
        m_client.stopAudio(0);
    }

    /**
     * Create a custom configuration for the WEBLINK connection manager, which can be passed to
     * the client core to override the default internal version.
     * @param context android context
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

        // Prepare WiFi AP if Android version is 8 or above
        boolean isWiFiPrepared = tryPrepareWiFiAP(true);

        // Add your custom connection methods.
        //registerMethod(new CustomConnectionMethod(context));

        m_connectionManager.setSerializer(new WLSerializer(context, "ConnectionManager"));
        if (!isWiFiPrepared) {
            m_connectionManager.init();
        }
    }

    public boolean tryPrepareWiFiAP(boolean onAppStart) {
        MCSLogger.log(MCSLogger.eDebug, TAG, "tryPrepareWiFiAP");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (m_sharedPref.isWiFiApEnabled()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ActivityCompat.checkSelfPermission(m_context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED) {
                            prepareWiFiAP(m_context, m_properties);
                        }
                    }
                } else if (ActivityCompat.checkSelfPermission(m_context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    prepareWiFiAP(m_context, m_properties);
                }
            } else {
                if (onAppStart) {
                    MCSLogger.log(MCSLogger.eDebug, TAG, "Skipping creating of WiFi AP connectivity");
                } else {
                    MCSLogger.log(MCSLogger.eDebug, TAG, "Disconnecting and destroying WiFi AP");
                    for (PeerDevice connectedDevice : m_connectedDevices) {
                        m_connectionManager.disconnectDevice(connectedDevice);
                    }
                    m_connectionManager.unregisterConnectionMethod(m_btConnectionMethod);
                    m_controlHandlerAP.destroyAP();
                }
                return false;
            }
        } else {
            MCSLogger.log(MCSLogger.eDebug, TAG, "Skipping creating of WiFi AP connectivity, Android version: " + Build.VERSION.CODENAME);
            return false;
        }
        return true;
    }

    /**
     * Prepares the Wi-Fi AP connectivity
     *
     * @param properties settings for the AP connectivity
     */
    private void prepareWiFiAP(final Context context, final Map<String, String> properties) {
        MCSLogger.log(TAG, "prepareWiFiAP");
        // Note that the permissions have to be granted in order to start WiFi AP
        try {
            MCSLogger.log(MCSLogger.eDebug, TAG, "prepare WiFi AP");
            m_connectionManager.terminate();

            // Prepare BT connection method
            if (m_btConnectionMethod == null) {
                prepareBTConnectionMethod(properties);
            }


            // Prepare Socket connection method
            ConnectionMethod connMethodSocket = new AndroidSocketConnectionMethod(12345, WLTypes.SERVER_DEFAULT_BROADCAST_PORT);
            connMethodSocket.setPriority(SOCKET_PRIORITY);

            // Register AP callbacks with ConnectionManager
            m_controlHandlerAP.setContext(context);
            m_controlHandlerAP.start();
            m_controlHandlerAP.registerAPCallback(new IWiFiControlHandlerCallbackAP() {
                @Override
                public void onAPRequest() {
                    // Do nothing for now
                    // We could show a message
                }

                @Override
                public void onAPCreated() {
                    // Do nothing for now
                    // We could show a message
                }

                @Override
                public void onAPCreationFailed() {
                    // Do nothing for now
                    // We could show a message
                }

                @Override
                public void onAPDestroyed() {
                    // Do nothing for now
                    // We could show a message
                }

                @Override
                public void onAPSelected() {}

                @Override
                public void onAPDisabled() {
                    // Do nothing for now
                    // We could show a message
                }

                @Override
                public void onAPConnectionTimeout() {
                    // Do nothing for now
                    // We could show a message
                }
            });

            APSettingsProvider apSettingsProvider = createAPSettingsProvider(properties);
            m_connectionScenarioAP = new ConnectionScenario_WiFiAP(
                    WIFI_AP_PRIORITY,
                    ConnectionScenario_WiFiAP.ID,
                    m_connectionManager,
                    apSettingsProvider);
            m_connectionScenarioAP.addNextStepConnectionMethod(m_btConnectionMethod.getConnectionMethodID());
            m_connectionScenarioAP.addNextStepConnectionMethod(connMethodSocket.getConnectionMethodID());
            m_connectionManager.registerConnectionMethod(m_btConnectionMethod);
            m_connectionManager.registerConnectionMethod(connMethodSocket);
            m_connectionManager.addScenario(m_connectionScenarioAP);

            // Register with WLConnectionManager
            // WLScenarioConnection_WiFiAP scenarioConnection = new WLScenarioConnection_WiFiAP(scenario, socketConnectionMethod.getConnectionMethodID(), socketConnectionMethod.getConnectionMethodID(), m_connectionManager, serverPort, m_context, apSettingsProvider);
            WLScenarioConnection_WiFiAP scenarioConnection = new WLScenarioConnection_WiFiAP(
                    m_connectionScenarioAP,
                    connMethodSocket.getConnectionMethodID(),
                    connMethodSocket.getConnectionMethodID(),
                    m_btConnectionMethod.getConnectionMethodID(),
                    m_connectionManager,
                    (short) 12345,
                    m_context,
                    apSettingsProvider);
            m_wlConnectionManager.addScenarioConnection(scenarioConnection);

            // Start the local-only hotspot
            m_controlHandlerAP.createAP();

            m_connectionManager.init();

            MCSLogger.log(TAG, "prepareWiFiAP() end");
        } catch (Exception e) {
            MCSLogger.log(MCSLogger.ELogType.eError, TAG, "Failed to start WiFi AP!", e);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, R.string.wifiap_failed_to_start, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private APSettingsProvider createAPSettingsProvider(Map<String, String> properties) {
        MCSLogger.log(TAG, "createAPSettingsProvider");
        APSettingsProvider provider = new APSettingsProvider(false);

        // Client app can create AP but it cannot join AP
        provider.setLocalCapabilities(new APCapabilities(true, false));
        m_canCreateAP = true;

        // Host app can join AP but it cannot create AP
        provider.setRemoteCapabilities(new APCapabilities(false, true));

        return provider;
    }

    /**
     * Access the weblink client core object.
     * @return
     */
    public WebLinkClientCore getWLClientCore() {
        return m_client;
    }

    /**
     * Set the client notification listener.
     * @param listener the listener, or null to remove.
     */
    public void setClientListener(IClientNotification listener) {
        m_listener = listener;
    }

    /**
     * add the listener from the list of listeners to connection updates.
     * @param listener
     */
    public void registerConnectionListener(IConnectionStatusNotification listener) {
        MCSLogger.log(TAG, "registerConnectionListener");
        synchronized (m_connListeners) {
            if(!m_connListeners.contains(listener)) {
                m_connListeners.add(listener);
            }
        }
    }

    /**
     * Remove the listener from the list of listeners to connection updates.
     * @param listener
     */
    public void unregisterConnectionListener(IConnectionStatusNotification listener) {
        MCSLogger.log(TAG, "unregisterConnectionListener");
        synchronized (m_connListeners) {
            m_connListeners.remove(listener);
        }
    }

    /**
     * add the listener from the list of listeners to server list updates.
     * @param listener
     */
    public void registerServerUpdateListener(IServerUpdateNotification listener) {
        MCSLogger.log(TAG, "registerServerUpdateListener");
        synchronized (m_serverListeners) {
            if(!m_serverListeners.contains(listener)) {
                m_serverListeners.add(listener);
            }
        }
    }

    /**
     * Remove the listener from the list of listeners to server list updates.
     * @param listener
     */
    public void unregisterServerUpdateListener(IServerUpdateNotification listener) {
        MCSLogger.log(TAG, "unregisterServerUpdateListener");
        synchronized (m_serverListeners) {
            m_serverListeners.remove(listener);
        }
    }
    /**
     * Get the service client.
     */
    public ServiceClient getServiceClient() {
        return m_serviceClient;
    }

    /**
     * Get the service object.
     */
    public Services getServices() {
        return m_services;
    }

    ///

    /**
     * HID USB Connection is used with the HIDController_USB.  This is used to bridge to the native
     * platform's iOS support.
     */
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
            TCPIPHIDUtils.getInstance().sendBufferEvent(reportData,reportLength);
            return m_dataLayer != null; //for now is a placeholder.
        }
    };

    /**
     * USB Device Connection is used with the HIDController_AOA to check the current state
     */
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
            if(m_aoaLayer != null) {
                return m_aoaLayer.sendControlTransfer(requestType, requestId, value, payload, payloadLength, timeOutMs);
            }
            return false;
        }

        @Override
        public boolean sendControlTransfer(int requestType, int requestId, int value, int index, int timeOutMs) {
            if(m_aoaLayer != null) {
                return m_aoaLayer.sendControlTransfer(requestType, requestId, value, index, timeOutMs);
            }
            return false;
        }
    };

    /**
     * HID USB Connection is used with the HIDController_USB.  This is used to bridge to the native
     * platform's interface for Consumer Control actions.
     */
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

    ///

    @Override
    public void onServerListUpdated(ServerInfo[] servers) {
        if(m_listener != null) {
            m_listener.onServerListUpdated(servers);
        }
        synchronized (m_serverListeners) {
            for(IServerUpdateNotification listener : m_serverListeners) {
                listener.onServerListUpdated(servers);
            }
        }
    }

    @Override
    public void onConnectionEstablished(PeerDevice peerDevice) {
        if(m_listener != null) {
            m_listener.onConnectionEstablished(peerDevice);
        }
        synchronized (m_connListeners) {
            for(IConnectionStatusNotification listener : m_connListeners) {
                listener.onConnectionEstablished(peerDevice);
            }
        }
        startAudio();
    }

    @Override
    public void onConnectionFailed(PeerDevice peerDevice, EConnectionResult result) {
        if(m_listener != null) {
            m_listener.onConnectionFailed(peerDevice, result);
        }
        synchronized (m_connListeners) {
            for(IConnectionStatusNotification listener : m_connListeners) {
                listener.onConnectionFailed(peerDevice, result);
            }
        }
    }

    @Override
    public void onConnectionClosed(PeerDevice peerDevice) {
        if(m_listener != null) {
            m_listener.onConnectionClosed(peerDevice);
        }
        synchronized (m_connListeners) {
            for(IConnectionStatusNotification listener : m_connListeners) {
                listener.onConnectionClosed(peerDevice);
            }
        }
        stopAudio();
    }

    @Override
    public void onApplicationChanged(int appID) {
        if(m_listener != null) {
            m_listener.onApplicationChanged(appID);
        }
    }

    @Override
    public void onFrameRendered() {
        if(m_listener != null) {
            m_listener.onFrameRendered();
        }
    }

    @Override
    public boolean canProcessFrame() {
        if(m_listener != null) {
            return m_listener.canProcessFrame();
        }
        return true;
    }

    @Override
    public void onShowKeyboard(short type) {
        if(m_listener != null) {
            m_listener.onShowKeyboard(type);
        }
    }

    @Override
    public void onHideKeyboard() {
        if(m_listener != null) {
            m_listener.onHideKeyboard();
        }
    }

    @Override
    public void onWaitIndicator(boolean showWaitIndicator) {
        if(m_listener != null) {
            m_listener.onWaitIndicator(showWaitIndicator);
        }
    }

    @Override
    public void onAppImageChanged(int appID, Bitmap image) {
        if(m_listener != null) {
            m_listener.onAppImageChanged(appID,image);
        }
    }

    @Override
    public void onConnectionLost() {
        if(m_listener != null) {
            m_listener.onConnectionLost();
        }
    }

    @Override
    public void onConnectionResumed() {
        if(m_listener != null) {
            m_listener.onConnectionResumed();
        }
    }

    public boolean isConnected()
    {
        return m_isConnected;
    }

    @Override
    public boolean onCommandReceived(Command command) {
        return true;
    }

    @Override
    public void onAudioChannelStarted(final int channelID) {
        if (m_listener != null) {
            m_listener.onAudioChannelStarted(channelID);
        }
    }

    @Override
    public void onAudioChannelStopped(final int channelID) {
        if (m_listener != null) {
            m_listener.onAudioChannelStopped(channelID);
        }
    }

    public void setPingHandler(IPingHandler pingHandler) {
        m_pingHandler = pingHandler;
    }

    private void registerMethod(ConnectionMethod method) {
        if(method != null) {
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

    @Override
    public void onConnectionPartialStateChanged(WLConnectionManager connectionManager, ConnectionScenario scenario, PeerDevice peerDevice, int step) {
        if (connectionManager.isPartialConnected() && !connectionManager.isConnected()){
            m_client.onConnectionPartiallyConnected(connectionManager);
        }
    }

    /**
     * Called when the process of scanning has started.
     *
     * @param connectionMethodID (to be removed)
     * @return true if the scanning should continue or false to stop the scanning process.
     */
    @Override
    public boolean onDeviceScanningBegin(String connectionMethodID) {
        return true;
    }

    /**
     * Called when a new device has been found as a result of the scanning process.
     *
     * @param device the found device
     * @return true to continue scanning for more devices or false to stop the scanning process
     */
    @Override
    public boolean onDeviceFound(PeerDevice device) {
        boolean isAutoConnectEnabled = m_sharedPref.isAutoConnectEnabled();
        String favoriteDeviceIP = m_sharedPref.getFavoriteDeviceIP();
        String favoriteDeviceName = m_sharedPref.getFavoriteDeviceName();
        boolean connectToDevice = isAutoConnectEnabled && favoriteDeviceIP.isEmpty() && favoriteDeviceName.isEmpty()
                || (isAutoConnectEnabled && !favoriteDeviceIP.isEmpty() && device.getAddress().contains(favoriteDeviceIP))
                || (isAutoConnectEnabled && !favoriteDeviceName.isEmpty() && device.getName().contains(favoriteDeviceName));
        if (connectToDevice) {
            MCSLogger.log(MCSLogger.ELogType.eDebug, TAG,
                    "onDeviceFound, connectToDevice == true, favoriteDeviceIP: " + favoriteDeviceIP
                            + " favoriteDeviceName: " + favoriteDeviceName
                            + "device: " + device.toString());
            boolean connected = m_client.connect(device);
            MCSLogger.log(MCSLogger.ELogType.eDebug, TAG,
                    "onDeviceFound: %s = %b", device.toString(), connected);
            if (connected) {
                MCSLogger.log(MCSLogger.ELogType.eDebug, TAG,
                        "onDeviceFound connected: " + device.toString());
                m_connectedDevices.add(device);
            }
        } else {
            MCSLogger.log(MCSLogger.ELogType.eDebug, TAG,
                    "onDeviceFound, connectToDevice == false, favoriteDeviceIP: " + favoriteDeviceIP
                            + " favoriteDeviceName: " + favoriteDeviceName
                            + "device: " + device.toString());
        }
        return true;
    }

    /**
     * Called when a device is no longer available.
     *
     * @param device the device that has been lost
     */
    @Override
    public void onDeviceLost(PeerDevice device) {
        MCSLogger.log(TAG, "onDeviceLost: %s", device.toString());
        for (PeerDevice it : m_connectedDevices) {
            if (it.equals(device)) {
                m_connectedDevices.remove(it);
                m_connectionManager.disconnectDevice(device);
                break;
            }
        }
    }

    /**
     * Called when the scanning process is complete.
     *
     * @param connectionMethodID (to be removed)
     */
    @Override
    public void onDeviceScanningEnd(String connectionMethodID) {
        // Do nothing
    }

    /**
     * Returns WebLink Client's capability to create WiFi AP.
     * This is controlled by the properties passed to the constructor.
     *
     * @return WebLink Client's capability to create WiFi AP
     */
    public boolean canCreateAP() {
        return m_canCreateAP;
    }

    private void prepareBTConnectionMethod(final Map<String, String> properties) {
        String listenUUIDString = properties.get(WLConstants.PROP_BT_SERVICE_LISTEN_UUID);
        String connectUUIDString = properties.get(WLConstants.PROP_BT_SERVICE_CONNECT_UUID);
        UUID listenUUID = UUID.fromString(listenUUIDString);
        UUID connectUUID = UUID.fromString(connectUUIDString);
        m_btConnectionMethod = new BluetoothConnectionMethod(m_context, listenUUID, connectUUID);
    }
}