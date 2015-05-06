//------------------------------------------------------------------------------
//  (c) Copyright 2013 Hewlett-Packard Development Company, L.P.
//
//  Confidential computer software. Valid license from HP required for 
//  possession, use or copying. 
//
//  Consistent with FAR 12.211 and 12.212, Commercial Computer Software,
//  Computer Software Documentation, and Technical Data for Commercial Items
//  are licensed to the U.S. Government under vendor's standard commercial 
//  license.
//------------------------------------------------------------------------------
package com.tallac.blacklist.api;

/**
 * Blacklist
 */
public interface IBlacklist {

    /**
     * Starts the Blacklist going.
     */
    public void enableBlacklist();

    /**
     * Stops the Blacklist going.
     */
    public void disableBlacklist();
    /**
     * Verifies whether Blacklist is up and running
     * 
     * @return {@code true} if Blacklist is operating, {@code false} otherwise.
     */
    public boolean isBlacklistEnabled();

}
