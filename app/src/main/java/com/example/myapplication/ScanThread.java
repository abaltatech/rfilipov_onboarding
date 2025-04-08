package com.example.myapplication;

import com.abaltatech.weblinkclient.WebLinkClientCore;

class ScanThread extends Thread {

    private final WebLinkClientCore m_client;
    private final int m_scanIntervalMs;


    public ScanThread(WebLinkClientCore client, int scanIntervalMs) {
        m_client = client;
        m_scanIntervalMs = scanIntervalMs;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            if (!m_client.isConnected()) {
                m_client.scanDeviceList();
            }
            try {
                sleep( m_scanIntervalMs );
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}