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

package org.apache.cayenne.modeler.util;

import java.lang.reflect.Method;

/**
 * Opens a URL in the system default browser.
 * 
 */
public class BrowserControl {

    // The default system browser under windows.
    private static final String WIN_PATH = "rundll32";
    // The flag to display a url.
    private static final String WIN_FLAG = "url.dll,FileProtocolHandler";

    // The default browser under unix.
    // private static final String UNIX_PATH = "netscape";
    // The flag to display a url.
    // private static final String UNIX_FLAG = "-remote openURL";

    /**
     * Display a file in the system browser. If you want to display a file, you must
     * include the absolute path name.
     * 
     * @param url the file's url (the url must start with either "http://" or "file://").
     */
    // see public domain code at
    // http://www.centerkey.com/java/browser/myapp/BareBonesBrowserLaunch.java
    public static void displayURL(String url) {
        try {
            if (OperatingSystem.getOS() == OperatingSystem.WINDOWS) {
                // cmd = 'rundll32 url.dll,FileProtocolHandler http://...'
                String cmd = WIN_PATH + " " + WIN_FLAG + " " + url;
                Runtime.getRuntime().exec(cmd);
            }
            else if (OperatingSystem.getOS() == OperatingSystem.MAC_OS_X) {
                Class<?> fileManager = Class.forName("com.apple.eio.FileManager");
                Method openURL = fileManager.getDeclaredMethod("openURL", String.class);
                openURL.invoke(null, url);
            }
            else { // assume Unix or Linux
                String[] browsers = {
                        "firefox", "opera", "konqueror", "epiphany", "mozilla",
                        "netscape"
                };
                for (String browser : browsers) {
                    if (Runtime.getRuntime().exec(new String[] {"which", browser}).waitFor() == 0) {
                        Runtime.getRuntime().exec(new String[] {browser, url});
                        break;
                    }
                }
            }
        }
        catch (Exception ex) {
            // could not open browser. Fail silently.
        }
    }
}
