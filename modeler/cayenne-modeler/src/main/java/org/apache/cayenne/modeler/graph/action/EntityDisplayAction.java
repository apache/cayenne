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
package org.apache.cayenne.modeler.graph.action;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.Icon;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.action.FindAction;
import org.apache.cayenne.modeler.graph.GraphBuilder;
import org.apache.cayenne.modeler.util.CayenneAction;

/**
 * Action that displays one of the objects in main tree, and then fires another action (if specified)
 */
public class EntityDisplayAction extends CayenneAction {

    /**
     * Action that will be performed after selection
     */
    private CayenneAction delegate;

    private GraphBuilder builder;

    public EntityDisplayAction(GraphBuilder builder) {
        super("Show", Application.getInstance());
        this.builder = builder;
        init();
    }

    public EntityDisplayAction(GraphBuilder builder, CayenneAction delegate) {
        super((String) delegate.getValue(Action.NAME), Application.getInstance());
        this.delegate = delegate;
        this.builder = builder;
        init();
    }

    private void init() {
        setEnabled(true);

        // Create icon manually, because at creation of super object delegate is null
        Icon icon = createIcon();
        if (icon != null) {
            super.putValue(Action.SMALL_ICON, icon);
        }
    }

    @Override
    public void performAction(ActionEvent e) {
        if (display()) {
            if (delegate != null) {
                delegate.performAction(e);
            }
        }
    }

    private boolean display() {
        Entity entity = builder.getSelectedEntity();
        if (entity == null) {
            return false;
        }

        // reusing logic from FindAction
        FindAction.jumpToResult(new FindAction.SearchResultEntry(entity, entity.getName()));
        return true;
    }

    @Override
    public String getIconName() {
        if (delegate != null) {
            return delegate.getIconName();
        }
        return null;
    }

    @Override
    public boolean enableForPath(ConfigurationNode object) {
        return builder.getSelectedEntity() != null;
    }
}
