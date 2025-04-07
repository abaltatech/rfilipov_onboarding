package com.example.myapplication;

import com.abaltatech.mcs.common.IMCSConnectionAddress;
import com.abaltatech.mcs.connectionmanager.ConnectionMethod;
import com.abaltatech.mcs.connectionmanager.IDeviceScanningNotification;
import com.abaltatech.mcs.connectionmanager.PeerDevice;


/// find constants some way
public class CustomConnectionMethod extends ConnectionMethod {
    @Override
    protected String getSystemName() {
        return "";
    }

    @Override
    public String getConnectionMethodID() {
        return "";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public boolean start() {
        return false;
    }

    @Override
    public void stop() {

    }

    @Override
    public boolean scanDevices(IDeviceScanningNotification iDeviceScanningNotification, boolean b) {
        return false;
    }

    @Override
    protected boolean stopScan() {
        return false;
    }

    @Override
    public boolean connectDevice(PeerDevice peerDevice) {
        return false;
    }

    @Override
    protected boolean disconnectDevice(PeerDevice peerDevice) {
        return false;
    }

    @Override
    protected boolean authorizeDevice(PeerDevice peerDevice, String s) {
        return false;
    }

    @Override
    protected boolean deauthorizeDevice(PeerDevice peerDevice) {
        return false;
    }

    @Override
    public PeerDevice getDeviceForAddress(IMCSConnectionAddress imcsConnectionAddress) {
        return null;
    }
}
