/****************************************************************************
 *
 * @file WiFiControlHandlerAP_Custom.java
 * @brief
 *
 * Contains the WiFiControlHandlerAP_Custom class.
 *
 * @author Abalta Technologies, Inc.
 * @date Feb, 2024
 *
 * @cond Copyright
 *
 * COPYRIGHT 2024 ABALTA TECHNOLOGIES ALL RIGHTS RESERVED.<br>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @endcond
 *****************************************************************************/
package com.example.myapplication;

import android.content.Context;

import com.weblink.androidext.wifi.IWiFiControlHandlerAP;
import com.weblink.androidext.wifi.IWiFiControlHandlerCallbackAP;
import com.weblink.androidext.wifi.NetworkConfigAP;

/**
 * This class implements handling AP WiFi control-related requests coming from
 * WiFi Connection Scenarios / WiFi WL Scenario Connections
 * that need to be dispatched to the WiFi management
 * modules on the device.
 *
 * It takes advantage of device hardware specifics, device API specifics and
 */
public class WiFiControlHandlerAP_Custom implements IWiFiControlHandlerAP {
    /**
     * Sets the {@link Context} to be used by the IWiFiControlHandlerAP.
     *
     * @param context the {@link Context} to be used by the IWiFiControlHandlerAP
     */
    @Override
    public void setContext(Context context) {
        // TODO
    }

    /**
     * Starts the IWiFiControlHandlerAP.
     */
    @Override
    public void start() {
        // TODO
    }

    /**
     * Stops the IWiFiControlHandlerAP.
     */
    @Override
    public void stop() {
        // TODO
    }

    /**
     * Create AP network
     *
     * @return true on success, false on failure
     */
    @Override
    public boolean createAP() {
        // TODO
        return false;
    }

    /**
     * Destroy AP network
     *
     * @return true on success, false on failure
     */
    @Override
    public boolean destroyAP() {
        // TODO
        return false;
    }

    /**
     * Notify when WiFi connection attempt timeout occurs
     **/
    @Override
    public void notifyAPConnectionTimeout() {
        // TODO
    }

    /**
     * Register a callback to receive AP responses.
     *
     * @param callback the callback to register
     */
    @Override
    public void registerAPCallback(IWiFiControlHandlerCallbackAP callback) {
        // TODO
    }

    /**
     * Unregister a callback receiving AP responses.
     *
     * @param callback the callback to unregister
     */
    @Override
    public void unregisterAPCallback(IWiFiControlHandlerCallbackAP callback) {
        // TODO
    }

    /**
     * Unregister all callbacks receiving AP responses.
     */
    @Override
    public void unregisterAllAPCallbacks() {
        // TODO
    }

    /**
     * Get AP network configuration.
     *
     * @return the AP network configuration
     */
    @Override
    public NetworkConfigAP getNetworkConfigAP() {
        // TODO
        return null;
    }

    /**
     * Sets AP network configuration.
     *
     * @param configuration the AP network configuration
     */
    @Override
    public void setNetworkConfigAP(NetworkConfigAP configuration) {
        // TODO
    }

    /**
     * Check if AP is ready for use.
     *
     * @return true if the AP is ready
     */
    @Override
    public boolean isAPReady() {
        // TODO
        return false;
    }
}
