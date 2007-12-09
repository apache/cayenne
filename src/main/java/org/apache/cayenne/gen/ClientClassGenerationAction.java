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
package org.apache.cayenne.gen;

import java.util.Collection;

import org.apache.cayenne.map.ObjEntity;

/**
 * @since 3.0
 * @author Andrus Adamchik
 */
public class ClientClassGenerationAction extends ClassGenerationAction {

    public static final String SUBCLASS_TEMPLATE = "dotemplates/v1_2/client-subclass.vm";
    public static final String SUPERCLASS_TEMPLATE = "dotemplates/v1_2/client-superclass.vm";

    @Override
    protected String defaultTemplateName(TemplateType type) {
        switch (type) {
            case ENTITY_SUBCLASS:
                return ClientClassGenerationAction.SUBCLASS_TEMPLATE;
            case ENTITY_SUPERCLASS:
                return ClientClassGenerationAction.SUPERCLASS_TEMPLATE;
            case EMBEDDABLE_SUBCLASS:
                return ClassGenerationAction.EMBEDDABLE_SUBCLASS_TEMPLATE;
            case EMBEDDABLE_SUPERCLASS:
                return ClassGenerationAction.EMBEDDABLE_SUPERCLASS_TEMPLATE;
            default:
                throw new IllegalArgumentException("Unsupported template type: " + type);
        }
    }

    @Override
    public void addEntities(Collection<ObjEntity> entities) {
        if (entities != null) {
            for (ObjEntity entity : entities) {
                artifacts.add(new ClientEntityArtifact(entity));
            }
        }
    }
}
