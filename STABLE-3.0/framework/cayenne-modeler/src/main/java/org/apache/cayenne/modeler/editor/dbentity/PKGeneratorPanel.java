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

package org.apache.cayenne.modeler.editor.dbentity;

import java.util.Iterator;

import javax.swing.JPanel;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.modeler.ProjectController;

public abstract class PKGeneratorPanel extends JPanel {

    protected ProjectController mediator;

    public PKGeneratorPanel(ProjectController mediator) {
        this.mediator = mediator;
    }

    /**
     * Called by parent when DbEntity changes, regardless of whether this panel is visible
     * or not. Another case when this method is invoked is when entity tab changes and
     * this panel may need a refresh.
     */
    public abstract void setDbEntity(DbEntity entity);

    /**
     * Called by parent when the panel becomes visible.
     */
    public abstract void onInit(DbEntity entity);

    protected void resetStrategy(
            DbEntity entity,
            boolean resetCustomSequence,
            boolean resetDBGenerated) {

        boolean hasChanges = false;

        if (resetCustomSequence && entity.getPrimaryKeyGenerator() != null) {
            entity.setPrimaryKeyGenerator(null);
            hasChanges = true;
        }

        if (resetDBGenerated) {
            for (DbAttribute a : entity.getPrimaryKeys()) {
                if (a.isGenerated()) {
                    a.setGenerated(false);
                    hasChanges = true;
                }
            }
        }

        if (hasChanges) {
            mediator.fireDbEntityEvent(new EntityEvent(this, entity));
        }
    }
}
