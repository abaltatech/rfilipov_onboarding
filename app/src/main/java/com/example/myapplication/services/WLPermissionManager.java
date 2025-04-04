/****************************************************************************
 *
 * @file WLPermissionManager.java
 * @brief
 *
 * Contains declaration of the WLPermissionManager class.
 *
 * @author Abalta Technologies, Inc.
 * @date April/2019
 *
 * @cond Copyright
 *
 * COPYRIGHT 2019 ABALTA TECHNOLOGIES ALL RIGHTS RESERVED.<br>
 * This program may not be reproduced, in whole or in part in any form or any means whatsoever
 * without the written permission of ABALTA TECHNOLOGIES.
 *
 * @endcond
 *****************************************************************************/
package com.example.myapplication.services;

import com.abaltatech.weblink.utils.WLPermissionManagerBase;

/**
 * The permission manager allows the application to request permission and track whether they have
 * been granted or not.
 */
public class WLPermissionManager extends WLPermissionManagerBase {
    private static WLPermissionManager s_Instance = new WLPermissionManager();

    /**
     * Returns the singleton instance of the permission manager.
     *
     * @return Singleton instance
     */
    public static WLPermissionManager getInstance() {
        return s_Instance;
    }
}
