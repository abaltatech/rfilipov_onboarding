/****************************************************************************
 *
 * @file IServerUpdateNotification.java
 * @brief
 *
 * Contains the IServerUpdateNotification interface.
 *
 * @author Abalta Technologies, Inc.
 * @date Jan, 2014
 *
 * @cond Copyright
 *
 * COPYRIGHT 2014 ABALTA TECHNOLOGIES ALL RIGHTS RESERVED.<br>
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

import com.abaltatech.weblinkclient.IClientNotification.ServerInfo;

/**
 * The server list update notification interface.
 */
public interface IServerUpdateNotification {
    
    /**
     * Called when the list of active WL servers has changed
     * @param servers the list of active WL servers
     */
    void onServerListUpdated(ServerInfo[] servers);
}
