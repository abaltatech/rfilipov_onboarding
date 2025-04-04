package com.example.myapplication;

import com.abaltatech.mcs.common.IMCSDataStats;
import com.abaltatech.mcs.common.MCSDataLayerBase;
import com.abaltatech.mcs.logger.MCSLogger;

import java.util.Arrays;

public class CustomDataLayer extends MCSDataLayerBase {
    // Thread to poll the platform's driver for incoming data
    private ReadThread m_readThread = null;

    // Internal buffer to hold data read from the driver
    private byte[] m_readBuffer = null;

    // Number of bytes currently available in the buffer
    private int m_bytesAvailable = 0;

    // Handle representing the connection, maintained by the driver
    private int m_connection = -1; // -1 indicates not connected

    // Tag for logging (replace with your app's logging mechanism if different)
    private static final String TAG = "CustomDataLayer";

    // Constructor
    public CustomDataLayer() {
        m_readBuffer = new byte[1024]; // Initial buffer size, adjust as needed
    }

    // Native methods (placeholders for your platform's driver APIs)
    private native int openConnectionNative(String address);
    private native int readDataNative(int connection, byte[] buffer, int offset, int size);
    private native int writeDataNative(int connection, byte[] buffer, int offset, int size);
    private native void closeConnectionNative(int connection);

    /**
     * Opens a connection to the device using the provided address.
     * @param address The CustomAddress containing the device address (e.g., "172.20.10.12:12345")
     * @return true if the connection is successfully opened, false otherwise
     */
    public boolean openConnection(CustomAddress address) {
        synchronized (this) {
            if (m_connection == -1) {
                // Attempt to open the connection via the platform's driver
                m_connection = openConnectionNative(address.getAddress());
                if (m_connection != -1) {
                    // Start the read thread to poll for incoming data
                    m_readThread = new ReadThread();
                    m_readThread.start();
                    return true;
                } else {
                    MCSLogger.log(MCSLogger.ELogType.eError, TAG, "Failed to open connection to " + address.getAddress());
                    return false;
                }
            }
            return false; // Already connected or failed
        }
    }

    /**
     * Writes data to the phone, blocking until all data is sent.
     * @param buffer The data to send
     * @param size The number of bytes to send
     */
    @Override
    protected void writeDataInternal(byte[] buffer, int size) {
        int bytesWritten = 0;
        int offset = 0;
        IMCSDataStats stats = getDataStats();

        try {
            while (offset < size) {
                // Write data using the platform's driver
                bytesWritten = writeDataNative(m_connection, buffer, offset, size - offset);
                if (bytesWritten > 0) {
                    offset += bytesWritten;
                    if (stats != null) {
                        stats.onDataSent(bytesWritten);
                    }
                } else if (bytesWritten == 0) {
                    // Driver is busy, wait briefly
                    Thread.sleep(5);
                } else {
                    // Error occurred, close the connection
                    MCSLogger.log(MCSLogger.ELogType.eError, TAG, "Write error, closing connection");
                    closeConnection();
                    break;
                }
            }
        } catch (InterruptedException e) {
            MCSLogger.log(MCSLogger.ELogType.eError, TAG, "Interrupted while writing", e);
            closeConnection();
        }
    }

    /**
     * Reads data from the internal buffer into the provided buffer.
     * @param buffer The buffer to copy data into
     * @param size The maximum number of bytes to read
     * @return The number of bytes read, 0 if no data is available, -1 on error
     */
    @Override
    public int readData(byte[] buffer, int size) {
        int bytesRead = 0;
        IMCSDataStats stats = getDataStats();

        try {
            synchronized (this) {
                if (m_readBuffer != null && m_bytesAvailable > 0 && size > 0) {
                    bytesRead = Math.min(m_bytesAvailable, size);
                    System.arraycopy(m_readBuffer, 0, buffer, 0, bytesRead);

                    // Shift remaining data to the start of the buffer
                    if (m_bytesAvailable > bytesRead) {
                        System.arraycopy(m_readBuffer, bytesRead, m_readBuffer, 0, m_bytesAvailable - bytesRead);
                    }
                    m_bytesAvailable -= bytesRead;

                    if (stats != null) {
                        stats.onDataReceived(bytesRead);
                    }
                }
                return bytesRead;
            }
        } catch (Exception e) {
            MCSLogger.log(MCSLogger.ELogType.eError, TAG, "Error reading data", e);
            closeConnection();
            return -1;
        }
    }

    /**
     * Closes the connection and releases resources.
     */
    @Override
    public void closeConnection() {
        synchronized (this) {
            if (m_readThread != null && m_readThread.isAlive()) {
                m_readThread.interrupt();
            }
            m_readThread = null;

            if (m_connection != -1) {
                closeConnectionNative(m_connection);
                m_connection = -1;
                m_bytesAvailable = 0;
                m_readBuffer = new byte[1024]; // Reset buffer
                notifyForConnectionClosed();
            }
        }
    }

    /**
     * Checks if the connection is active and ready for data transfer.
     * @return true if the connection is open, false otherwise
     */
    @Override
    public boolean isReady() { // Note: Query specifies isReady(), though examples use isOpened()
        synchronized (this) {
            return m_connection != -1;
        }
    }

    /**
     * Inner thread class to continuously poll the driver for incoming data.
     */
    private class ReadThread extends Thread {
        @Override
        public void run() {
            byte[] tempBuffer = new byte[1024]; // Temporary buffer for reading
            try {
                while (!isInterrupted()) {
                    synchronized (CustomDataLayer.this) {
                        int bytesRemaining = m_readBuffer.length - m_bytesAvailable;
                        if (bytesRemaining < 512) { // Resize if less than half buffer remains
                            m_readBuffer = Arrays.copyOf(m_readBuffer, m_readBuffer.length + 1024);
                            bytesRemaining = m_readBuffer.length - m_bytesAvailable;
                        }

                        // Read data from the driver
                        int bytesRead = readDataNative(m_connection, m_readBuffer, m_bytesAvailable, bytesRemaining);
                        if (bytesRead > 0) {
                            m_bytesAvailable += bytesRead;
                            notifyForData(); // Notify Client SDK of available data
                        } else if (bytesRead < 0) {
                            // Error occurred
                            MCSLogger.log(MCSLogger.ELogType.eError, TAG, "Read error, closing connection");
                            closeConnection();
                            break;
                        }
                    }
                    // Brief sleep to prevent tight looping if no data
                    Thread.sleep(10);
                }
            } catch (Exception e) {
                MCSLogger.log(MCSLogger.ELogType.eError, TAG, "Error in ReadThread", e);
            } finally {
                closeConnection();
                notifyForConnectionClosed();
            }
        }
    }
}