package com.example.myapplication;

import com.abaltatech.mcs.common.MCSDataLayerBase;

public class CustomDataLayer extends MCSDataLayerBase {
    private ReadThread m_readThread = null;

    @Override
    protected void writeDataInternal(byte[] bytes, int i) {

    }

    @Override
    public int readData(byte[] bytes, int i) {
        return 0;
    }

    @Override
    public void closeConnection() {

    }
}
