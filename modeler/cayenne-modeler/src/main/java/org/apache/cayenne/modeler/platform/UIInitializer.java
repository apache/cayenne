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
package org.apache.cayenne.modeler.platform;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.toolkit.filechooser.CMFileChooserFactory;
import org.apache.cayenne.modeler.toolkit.filechooser.SwingFileChooser;

/**
 * A base callback for platform-specific Modeler initialization.
 */
public interface UIInitializer {

    /**
     * Sets platform-specific system properties and initializes the application look and feel.
     * Called before AWT/Swing is touched, so implementations may set properties that AWT reads
     * only during its own init (e.g. {@code apple.awt.application.name}) before installing the L&F.
     */
    default void beforeSwingLaunch() {
    }

    /**
     * Updates default frame menus according to the platform specifics.
     */
    default void afterFrameCreated(Application app) {
    }

    /**
     * Returns the platform-appropriate {@link CMFileChooserFactory}. Defaults to Swing's
     * {@code JFileChooser}; overridden on macOS to use the native {@code FileDialog}.
     */
    default CMFileChooserFactory fileChooserFactory() {
        return SwingFileChooser::new;
    }
}
