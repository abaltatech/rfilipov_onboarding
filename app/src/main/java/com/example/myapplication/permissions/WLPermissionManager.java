package com.example.myapplication.permissions;

import com.abaltatech.weblink.utils.WLPermissionManagerBase;

import javax.inject.Inject;
import javax.inject.Singleton;


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
