package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.abaltatech.mcs.connectionmanager.ConnectionManager;
import com.abaltatech.mcs.connectionmanager.EConnectionResult;
import com.abaltatech.mcs.connectionmanager.PeerDevice;
import com.abaltatech.weblink.core.authentication.DeviceIdentity;
import com.abaltatech.weblink.core.commandhandling.Command;
import com.abaltatech.weblinkclient.IClientNotification;
import com.abaltatech.weblinkclient.WebLinkClientCore;

class WebLinkClient implements IClientNotification {

    WebLinkClientCore m_clientCore;
    private ConnectionManager m_connManager = null;

    WebLinkClient(Context context){
        DeviceIdentity m_deviceIdentity = getDeviceIdentity();

        /// need to se the connectionManager

        m_clientCore = new WebLinkClientCore( context, this, m_deviceIdentity, m_connManager);
    }

    DeviceIdentity getDeviceIdentity()
    {
        DeviceIdentity myIdentity = new DeviceIdentity();

        myIdentity.setSystemId(Build.MANUFACTURER + Build.MODEL + Build.SERIAL);
        myIdentity.setDisplayNameEn(android.os.Build.DISPLAY);
        myIdentity.setManufacturer(Build.MANUFACTURER);
        myIdentity.setModel(Build.MODEL);
        myIdentity.setCountryCodes("US,CA");
        myIdentity.setSerialNumber(Build.SERIAL);
        myIdentity.setDisplayNameMultiLanguage( "{\"en\":\"" +
                android.os.Build.DISPLAY + "\"}" );
        myIdentity.setApplication("Integrator WebLink Client");
        myIdentity.setApplicationVendor("com.integrator");
        myIdentity.setOs("Android");
        myIdentity.setOsVersion(String.valueOf(android.os.Build.VERSION.SDK_INT));

        return myIdentity;
    }


    @Override
    public void onServerListUpdated(ServerInfo[] serverInfos) {

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

}