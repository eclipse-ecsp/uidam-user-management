/*
 * ******************************************************************************
 *  COPYRIGHT (c) 2023 Harman International Industries, Inc.
 *
 *  All rights reserved
 *
 *  This software embodies materials and concepts which are
 *  confidential to Harman International Industries, Inc.
 *  and is made available solely pursuant to the terms of a
 *  written license agreement with Harman International Industries, Inc.
 *
 *  Designed and Developed by Harman International Industries, Inc.
 *  -----------------------------------------------------------------------------
 *  MODULE OR UNIT: uidam-user-management
 *
 *  DATE       :
 *  VERSION    :
 *  -----------------------------------------------------------------------------
 */

package org.eclipse.ecsp.uidam.usermanagement.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * UserNotFoundException.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
@Getter
public class UserNotFoundException extends Exception {

    private static final long serialVersionUID = -4174360476934760735L;
    private final String errorCode;

    /**
     * UserNotFoundException.
     *
     * @param message   error message.
     * @param errorCode error code.
     */
    public UserNotFoundException(String message, String errorCode) {
        super("{ Error ='" + message + '\'' + "}");
        this.errorCode = errorCode;
    }
}