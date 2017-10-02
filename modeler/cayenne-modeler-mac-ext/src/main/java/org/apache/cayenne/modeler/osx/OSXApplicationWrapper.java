/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.osx;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;

import com.apple.eawt.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class wraps apple {@link com.apple.eawt.Application} class and dynamically
 * proxying it's lifecycle handlers setup.
 * <p>
 * This code exists to support both Java 8 and 9, as handler interfaces where incompatibly moved
 * to other package between these versions.
 * <p>
 * See <a href="https://bugs.openjdk.java.net/browse/JDK-8160437">JDK-8160437 issue</a> for details.
 *
 * @see #setAboutHandler(Runnable) run action on "About App" menu item select
 * @see #setPreferencesHandler(Runnable) run action on "Preferences..." menu item select
 * @see #setQuitHandler(Consumer) run action on "Quit App" menu item select
 *
 * @see OSXQuitResponseWrapper
 *
 * @since 4.1
 */
public class OSXApplicationWrapper {

    private static final Logger logger = LoggerFactory.getLogger(OSXApplicationWrapper.class);

    // package for handler classes for Java 8 and older
    private static final String JAVA8_PACKAGE = "com.apple.eawt.";

    // package for handler classes for Java 9 and newer
    private static final String JAVA9_PACKAGE = "java.awt.desktop.";

    private final Application application;

    private Class<?> aboutHandlerClass;
    private Method setAboutHandler;

    private Class<?> preferencesHandlerClass;
    private Method setPreferencesHandler;

    private Class<?> quitHandlerClass;
    private Method setQuitHandler;

    public OSXApplicationWrapper(Application application) {
        this.application = application;
        initMethods();
    }

    public void setPreferencesHandler(Runnable action) {
        setHandler(setPreferencesHandler, preferencesHandlerClass, action);
    }

    public void setAboutHandler(Runnable action) {
        setHandler(setAboutHandler, aboutHandlerClass, action);
    }

    public void setQuitHandler(Consumer<OSXQuitResponseWrapper> action) {
        InvocationHandler handler = (proxy, method, args) -> {
            // args: 0 - event, 1 - quitResponse
            action.accept(new OSXQuitResponseWrapper(args[1]));
            return null;
        };
        Object proxy = createProxy(quitHandlerClass, handler);
        try {
            setQuitHandler.invoke(application, proxy);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            logger.warn("Unable to call " + setQuitHandler.getName(), ex);
        }
    }

    /**
     * Find required handlers' methods and classes
     */
    private void initMethods() {
        aboutHandlerClass = getHandlerClass("AboutHandler");
        setAboutHandler = getMethod("setAboutHandler", aboutHandlerClass);

        preferencesHandlerClass = getHandlerClass("PreferencesHandler");
        setPreferencesHandler = getMethod("setPreferencesHandler", preferencesHandlerClass);

        quitHandlerClass = getHandlerClass("QuitHandler");
        setQuitHandler = getMethod("setQuitHandler", quitHandlerClass);
    }

    private void setHandler(Method setMethod, Class<?> handlerClass, Runnable action) {
        InvocationHandler handler = (proxy, method, args) -> {
            action.run();
            return null;
        };
        Object proxy = createProxy(handlerClass, handler);
        try {
            setMethod.invoke(application, proxy);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            logger.warn("Unable to call " + setMethod.getName(), ex);
        }
    }

    private Object createProxy(Class<?> handlerClass, InvocationHandler handler) {
        return Proxy.newProxyInstance(OSXApplicationWrapper.class.getClassLoader(), new Class<?>[]{handlerClass}, handler);
    }

    private Method getMethod(String name, Class<?> ... parameters) {
        try {
            return application.getClass().getMethod(name, parameters);
        } catch (NoSuchMethodException ex) {
            logger.warn("Unable to find method " + name, ex);
            return null;
        }
    }

    private Class<?> getHandlerClass(String className) {
        try {
            return Class.forName(JAVA8_PACKAGE + className);
        } catch (ClassNotFoundException ex) {
            try {
                return Class.forName(JAVA9_PACKAGE + className);
            } catch (ClassNotFoundException ex2) {
                return null;
            }
        }
    }

}
