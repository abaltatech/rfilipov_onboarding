package com.example.myapplication.Connection;

import android.app.PendingIntent;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Handler;

import com.abaltatech.mcs.common.MCSDataLayerBase;
import com.abaltatech.mcs.logger.MCSLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CustomListener {
    private static final String TAG = CustomListener.class.getSimpleName();

    protected static final String ACTION_USB_PERMISSION = "com.abaltatech.mcs.USB_PERMISSION";

    /**
     * Notification that is used to notify for modifications of custom devices.
     *
     * @author Abalta Technologies, Inc.
     */
    public interface ICustomNotification {
        /**
         * Called when the next
         */
        void onCustomDetected(CustomAddress address);
    }

    private ICustomNotification m_updateListener = null;
    private Handler m_handler = new Handler();

    private ScanThread m_scanThread = null;

    private Map<String, UsbDevice> m_attachedDevices = null;
    private Map<String, PendingIntent> m_pendingIntents = null;
    private List<UsbDevice> m_ignoredDevices = null;

    /**
     * Creates a new instance of the {@link CustomListener} class.
     * <p>
     * This class will make the device act as a USB host. The strings used to
     * identify this host can be set through this constructor.
     *
     * @param ctx application context
     */
    public CustomListener(Context ctx) {
        m_pendingIntents = new HashMap<String, PendingIntent>();
        m_attachedDevices = new HashMap<String, UsbDevice>();
        m_ignoredDevices = new ArrayList<UsbDevice>();
    }


    /**
     * Register a listener that will receive notifications when the list of
     * attached USB device has changed.
     *
     * @param listener reference to the listener interface to start receiving updates.
     */
    public void registerUpdateListener(ICustomNotification listener) {
        m_updateListener = listener;
    }

    /**
     * Unregister a listener from notifications when the list of attached USB
     * device has changed.
     * <p>
     * After calling this method the listener will no longer receive
     * notifications.
     *
     * @param listener reference to the listener to unregister
     */
    public void unregisterUpdateListener(ICustomNotification listener) {
        if (m_updateListener == listener) {
            m_updateListener = null;
        }
    }

    /**
     * Start listening for USB devices.
     * <p>
     * This thread creates a thread which will monitor for USB devices being
     * attached or detached from this host.
     *
     * @return true if successful, false otherwise
     */
    public boolean startServerScan() {
        if (m_scanThread == null || !m_scanThread.isAlive()) {
            m_scanThread = new ScanThread();
            m_scanThread.start();
            return true;
        } else {
            MCSLogger.log(TAG, "startServerScan: thread already running, taking no action.");
        }

        return false;
    }

    /**
     * Stop listening for USB devices.
     * <p>
     * This thread will stop the listening thread.
     */
    public void stopServerScan() {
        if (m_scanThread != null) {
            m_scanThread.interrupt();
            m_scanThread = null;
        } else {
            MCSLogger.log(TAG, "stopServerScan: thread already stopped, taking no action.");
        }
    }

    /**
     * Simple thread subclass to poll the UsbDevices and send an update
     * notification if the list changes.
     *
     * @author zkimball
     */
    public class ScanThread extends Thread {

        private volatile boolean m_stopRequested = false;

        public ScanThread() {
            setName(TAG + "_" + ScanThread.class.getSimpleName());
            m_stopRequested = false;
        }

        @Override
        public void run() {
            m_attachedDevices.clear();
            m_pendingIntents.clear();
            m_ignoredDevices.clear();

            MCSLogger.log(TAG, "USB scan thread started");

            while (!m_stopRequested) {

                //TODO - scan for available custom devices.

                if (m_updateListener != null) {
                    m_updateListener.onCustomDetected(null);
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    m_stopRequested = true;
                }
            }

            MCSLogger.log(TAG, "Thread FINISH!");
        }

        @Override
        public void interrupt() {
            m_stopRequested = true;
            super.interrupt();
        }
    }
}