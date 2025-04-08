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
                m_wlConnectionManager);

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

    @Override
    public void onServerListUpdated(ServerInfo[] serverInfos) {
        var info = serverInfos[0]; // TODO: fix
        if (m_clientCore.connect(info.m_peerDevice, IClientNotification.EProtocolType.ePT_WL, -1)) {
            // Request accepted. Wait for either onConnectionEstablished or onConnectionFailed notifications
            // before proceeding.
        } else {
            // The request was not accepted. Probably an invalid ServerInfo.
            MCSLogger.log(MCSLogger.ELogType.eError, "The connect request to a peer was not accepted. Probably an invalid ServerInfo.");
        }
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

    @Override
    public void onConnectionEstablished(PeerDevice peerDevice) {

    }

    @Override
    public void onConnectionFailed(PeerDevice peerDevice, EConnectionResult eConnectionResult) {

    }

    @Override
    public void onConnectionClosed(PeerDevice peerDevice) {

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

    }

    @Override
    public void onConnectionResumed() {

    }

    @Override
    public boolean onCommandReceived(Command command) {
        return false;
    }

    @Override
    public void onAudioChannelStarted(int i) {

    }

    @Override
    public void onAudioChannelStopped(int i) {

    }

    @Override
    public boolean onDeviceScanningBegin(String s) {
        return false;
    }

    @Override
    public boolean onDeviceFound(PeerDevice peerDevice) {
        return false;
    }

    @Override
    public void onDeviceLost(PeerDevice peerDevice) {

    }

    @Override
    public void onDeviceScanningEnd(String s) {

    }

    @Override
    public boolean onFavoriteDeviceAvailable(PeerDevice peerDevice) {
        return false;
    }

    @Override
    public boolean onDeviceConnected(PeerDevice peerDevice, IMCSDataLayer imcsDataLayer) {
        return false;
    }

    @Override
    public void onDeviceConnectFailed(PeerDevice peerDevice, EConnectionResult eConnectionResult) {

    }

    @Override
    public void onDeviceDisconnected(PeerDevice peerDevice) {

    }

    @Override
    public void onAutoconnectFailed(EConnectionResult eConnectionResult) {

    }

    @Override
    public void onConnectionPartialStateChanged(WLConnectionManager wlConnectionManager, ConnectionScenario connectionScenario, PeerDevice peerDevice, int i) {

    }

    @Override
    public void onConnectionStateChanged(WLConnectionManager wlConnectionManager, IMCSDataLayer imcsDataLayer) {

    }
}

