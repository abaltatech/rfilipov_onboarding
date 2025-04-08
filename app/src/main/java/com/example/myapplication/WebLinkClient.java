package com.example.myapplication;

import static com.example.myapplication.WLConstants.SOCKET_PORT;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.abaltatech.mcs.common.IMCSDataLayer;
import com.abaltatech.mcs.connectionmanager.ConnectionManager;
import com.abaltatech.mcs.connectionmanager.ConnectionMethod;
import com.abaltatech.mcs.connectionmanager.ConnectionScenario;
import com.abaltatech.mcs.connectionmanager.EConnectionResult;
import com.abaltatech.mcs.connectionmanager.IDeviceScanningNotification;
import com.abaltatech.mcs.connectionmanager.IDeviceStatusNotification;
import com.abaltatech.mcs.connectionmanager.PeerDevice;
import com.abaltatech.mcs.logger.MCSLogger;
import com.abaltatech.mcs.socket.android.AndroidSocketConnectionMethod;
import com.abaltatech.weblink.core.WLConnectionManager;
import com.abaltatech.weblink.core.WLScenarioConnection;
import com.abaltatech.weblink.core.WLTypes;
import com.abaltatech.weblink.core.WebLinkConnection;
import com.abaltatech.weblink.core.authentication.DeviceIdentity;
import com.abaltatech.weblink.core.commandhandling.Command;
import com.abaltatech.weblinkclient.IClientNotification;
import com.abaltatech.weblinkclient.WebLinkClientCore;

import java.util.Map;

public class WebLinkClient implements IClientNotification,
        IDeviceScanningNotification,
        IDeviceStatusNotification,
        WLConnectionManager.ConnectionStateChangedDelegate,
        WLConnectionManager.ConnectionPartialStateChangedDelegate {


    private String TAG = "WebLinkClient";
    private WebLinkClientCore m_clientCore;
    private ConnectionManager m_connectionManager = null;
    private WLConnectionManager m_wlConnectionManager = null;
    private DeviceIdentity m_deviceIdentity = null;

    WebLinkClient(Context context, Map<String, String> properties){
        initDeviceIdentity();
        createWebLinkConnectionManager();
        m_clientCore = new WebLinkClientCore( context, this, m_deviceIdentity, m_connectionManager);
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
        WLConnectionManager wlConnectionManager = new WLConnectionManager(m_connectionManager, WebLinkConnection.MAX_COMMANDS_SERVER, true);
        wlConnectionManager.setStateChangeDelegate(this);
        wlConnectionManager.setConnectionPartialStateChangedDelegate(this);

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

