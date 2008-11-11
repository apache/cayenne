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


package org.apache.cayenne.modeler;

import org.apache.cayenne.modeler.action.AboutAction;
import org.apache.cayenne.modeler.action.ConfigurePreferencesAction;
import org.apache.cayenne.modeler.action.ExitAction;
import org.apache.cayenne.modeler.util.CayenneAction;

import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

/**
 * Configures Modeler to better integrate into Mac OS X environment. Has no effect on
 * other platforms.
 * 
 * @since 1.1
 */
class MacOSXSetup {

    static void configureMacOSX() {
        com.apple.eawt.Application.getApplication().addAboutMenuItem();
        com.apple.eawt.Application.getApplication().addPreferencesMenuItem();
        com.apple.eawt.Application.getApplication().setEnabledAboutMenu(true);
        com.apple.eawt.Application.getApplication().setEnabledPreferencesMenu(true);

        com.apple.eawt.Application.getApplication().addApplicationListener(new MacEventsAdapter());
    }

    static class MacEventsAdapter extends ApplicationAdapter {

        public void handleAbout(ApplicationEvent e) {
            if (!e.isHandled()) {
                ((AboutAction) getAction(AboutAction.getActionName())).showAboutDialog();
                e.setHandled(true);
            }
        }

        public void handlePreferences(ApplicationEvent e) {
            ((ConfigurePreferencesAction) getAction(ConfigurePreferencesAction.getActionName())).showPreferencesDialog();
            e.setHandled(true);
        }

        public void handleQuit(ApplicationEvent e) {
            if (!e.isHandled()) {
                Application.getInstance().setQuittingApplication(true);
                ((ExitAction) getAction(ExitAction.getActionName())).exit();
                if (Application.getInstance().isQuittingApplication())
                    e.setHandled(true);
                else
                    Application.getInstance().setQuittingApplication(false);
            }
        }

        CayenneAction getAction(String name) {
            return Application.getInstance().getAction(name);
        }
    }
}
