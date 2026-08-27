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

package org.apache.cayenne.modeler.toolkit;

import org.apache.cayenne.modeler.Application;

import javax.swing.JPanel;

/**
 * Base for modeler panels that merge presentation and logic into a single component.
 * Holds the {@link Application} reference for code that needs access to application-wide
 * services (preferences, action manager, frame controller, etc.).
 */
public abstract class AppPanel extends JPanel {

    protected final Application app;

    protected AppPanel(Application app) {
        this.app = app;
    }
}
