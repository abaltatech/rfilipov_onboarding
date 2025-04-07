package com.example.myapplication;

import java.util.HashSet;
import java.util.Set;

public class CustomListener {

    public interface INotification {
        void onDeviceAttached(String device);
        void onDeviceDetached(String device);
    }

    public native Set<String> get_attached_devices_api_here();

    private class ListenThread extends Thread {

        private Set<String> m_attachedDevices = new HashSet<String>();

        @Override
        public void run() {
            while (!isInterrupted()) {
                newDevices.clear();
                Set<String> devices = get_attached_devices_api_here();
                for (String device : devices) {
                    if (!m_attachedDevices.contains(device)) {
                        m_notification.onDeviceAttached(device);
                    }
                }
                for (String device : m_attachedDevices) {
                    if (!devices.contains(device)) {
                        removedDevices.add(device);
                        m_notification.onDeviceDetached(device);
                    }
                }
                m_attachedDevices = devices;
            }
        }
    }

    private INotification m_notification;
    private ListenThread m_listenThread;

    public CustomListener() { }

    public boolean startListener() {
        if (m_listenThread == null || m_listenThread.isAlive()) {
            m_listenThread = new ListenThread();
            m_listenThread.start();
        }
    }

    public void stopListener() {
        if (m_listenThread != null) {
            m_listenThread.interrupt();
            m_listenThread = null;
        }
    }

    public void registerNotification(INotification notification) {
        m_notification = notification;
    }

    public void unregisterNotification(INotification notification) {
        if (m_notification == notification) {
            m_notification = null;
        }
    }
}
