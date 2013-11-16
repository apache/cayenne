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

package org.apache.cayenne.util;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Provides access to various modeler resources (mainly strings) obtained via a
 * ResourceBundle.
 */
public class LocalizedStringsHandler {

    public static final String DEFAULT_MESSAGE_BUNDLE = "org.apache.cayenne.cayenne-strings";

    protected static ResourceBundle bundle;

    /**
     * Returns localized string for the given key.
     */
    public static String getString(String key) {
        if (getBundle() == null) {
            return "";
        }

        try {
            return getBundle().getString(key);
        }
        catch (Throwable e) {
            return "";
        }
    }

    protected synchronized static ResourceBundle getBundle() {
        if (bundle == null) {
            try {
                bundle = ResourceBundle.getBundle(DEFAULT_MESSAGE_BUNDLE);
            }
            catch (MissingResourceException e) {
                // do not throw Cayenne exceptions, as they rely on
                // LocalizedStringsHandler, and we can get into infinite loop
                throw new RuntimeException("Can't load properties: "
                        + DEFAULT_MESSAGE_BUNDLE, e);
            }
        }
        return bundle;
    }
}
