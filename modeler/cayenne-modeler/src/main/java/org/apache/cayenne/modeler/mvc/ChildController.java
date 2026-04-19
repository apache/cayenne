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

package org.apache.cayenne.modeler.mvc;

import org.apache.cayenne.modeler.pref.FSPath;

import java.awt.*;
import java.util.Objects;

/**
 * A superclass of non-root controllers.
 */
public abstract class ChildController<P extends RootController> extends RootController {

    protected final P parent;

    protected ChildController(P parent) {
        super(Objects.requireNonNull(parent).getApplication());
        this.parent = parent;
    }

    @Override
    public FSPath getLastDirectory() {
        // find start directory in preferences
        FSPath path = (FSPath) application
                .getCayenneProjectPreferences()
                .getProjectDetailObject(
                        FSPath.class,
                        getViewPreferences().node("lastDir"));

        if (path.getPath() == null) {
            path.setPath(parent.getLastDirectory().getPath());
        }

        return path;
    }

    /**
     * Centers view on parent window.
     */
    protected void centerView() {
        Window parentWindow = parent.getWindow();

        Dimension parentSize = parentWindow.getSize();
        Dimension windowSize = getView().getSize();
        Point parentLocation = new Point(0, 0);
        if (parentWindow.isShowing()) {
            parentLocation = parentWindow.getLocationOnScreen();
        }

        int x = parentLocation.x + parentSize.width / 2 - windowSize.width / 2;
        int y = parentLocation.y + parentSize.height / 2 - windowSize.height / 2;

        getView().setLocation(x, y);
    }
}
