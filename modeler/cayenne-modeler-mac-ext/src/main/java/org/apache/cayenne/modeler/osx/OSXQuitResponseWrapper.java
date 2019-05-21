/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.osx;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Small wrapper around QuitResponse class that can reside in different packages:
 * com.apple.eawt.QuitResponse in JDK 8 and java.awt.desktop.QuitResponse in JDK 9.
 * Luckily it has same signature so we can dynamically resolve it's methods.
 *
 * @since 4.1
 */
public class OSXQuitResponseWrapper {

    private static final Logger logger = LoggerFactory.getLogger(OSXQuitResponseWrapper.class);

    private Method performQuit;

    private Method cancelQuit;

    private final Object quitResponse;

    public OSXQuitResponseWrapper(Object quitResponse) {
        this.quitResponse = quitResponse;
        try {
            performQuit = quitResponse.getClass().getMethod("performQuit");
            cancelQuit = quitResponse.getClass().getMethod("cancelQuit");
        } catch (NoSuchMethodException ex) {
            logger.warn("Unable to find methods for quit response", ex);
        }
    }

    public void performQuit() {
        safePerform(performQuit);
    }

    public void cancelQuit() {
        safePerform(cancelQuit);
    }

    private void safePerform(Method method) {
        if(method == null) {
            return;
        }
        try {
            method.invoke(quitResponse);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            logger.warn("Unable to call " + method.getName(), ex);
        }
    }
}
