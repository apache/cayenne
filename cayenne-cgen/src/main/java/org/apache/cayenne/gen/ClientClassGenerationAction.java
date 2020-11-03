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

package org.apache.cayenne.gen;

import java.util.Collection;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.QueryDescriptor;

/**
 * @since 3.0
 */
public class ClientClassGenerationAction extends ClassGenerationAction {

    private static final String TEMPLATES_DIR_NAME = "templates/v4_1/";
    public static final String SUBCLASS_TEMPLATE = TEMPLATES_DIR_NAME + "client-subclass.vm";
    public static final String SUPERCLASS_TEMPLATE = TEMPLATES_DIR_NAME + "client-superclass.vm";
    public static final String SINGLE_CLASS_TEMPLATE = TEMPLATES_DIR_NAME + "client-singleclass.vm";

    public static final String DMAP_SUBCLASS_TEMPLATE = TEMPLATES_DIR_NAME + "client-datamap-subclass.vm";
    public static final String DMAP_SUPERCLASS_TEMPLATE = TEMPLATES_DIR_NAME + "client-datamap-superclass.vm";
    public static final String DATAMAP_SINGLE_CLASS_TEMPLATE = TEMPLATES_DIR_NAME + "client-datamap-singleclass.vm";

    public static final String CLIENT_SUPERCLASS_PREFIX = "_Client";

    public ClientClassGenerationAction(CgenConfiguration config) {
        super(config);
    }

    @Override
    public String defaultTemplateName(TemplateType type) {
        switch (type) {
            case ENTITY_SUBCLASS:
                return SUBCLASS_TEMPLATE;
            case ENTITY_SUPERCLASS:
                return SUPERCLASS_TEMPLATE;
            case ENTITY_SINGLE_CLASS:
                return SINGLE_CLASS_TEMPLATE;

            case EMBEDDABLE_SUBCLASS:
                return ClassGenerationAction.EMBEDDABLE_SUBCLASS_TEMPLATE;
            case EMBEDDABLE_SUPERCLASS:
                return ClassGenerationAction.EMBEDDABLE_SUPERCLASS_TEMPLATE;
            case EMBEDDABLE_SINGLE_CLASS:
                return ClassGenerationAction.EMBEDDABLE_SINGLE_CLASS_TEMPLATE;

            case DATAMAP_SUPERCLASS:
                return DMAP_SUPERCLASS_TEMPLATE;
            case DATAMAP_SUBCLASS:
                return DMAP_SUBCLASS_TEMPLATE;
            case DATAMAP_SINGLE_CLASS:
                return DATAMAP_SINGLE_CLASS_TEMPLATE;

            default:
                throw new IllegalArgumentException("Unsupported template type: " + type);
        }
    }

    /**
     * @since 4.0 throws exception
     */
    @Override
    public void addEntities(Collection<ObjEntity> entities) {
        if (!cgenConfiguration.getDataMap().isClientSupported()) {
            throw new CayenneRuntimeException("Can't create client classes. Check client supported option on DataMap configuration.");
        }
        if (entities != null) {
            for (ObjEntity entity : entities) {
                if (!entity.isServerOnly()) {
                    cgenConfiguration.addArtifact(new ClientEntityArtifact(entity));
                }
            }
        }
    }

    @Override
    public void addQueries(Collection<QueryDescriptor> queries) {
        if (cgenConfiguration.getArtifactsGenerationMode().equals(ArtifactsGenerationMode.ALL.getLabel())) {
            if (queries != null) {
                cgenConfiguration.addArtifact(new ClientDataMapArtifact(cgenConfiguration.getDataMap(), queries));
            }
        }
    }

    public void setCgenConfiguration(CgenConfiguration cgenConfiguration) {
        super.setCgenConfiguration(cgenConfiguration);
        cgenConfiguration.setQueryTemplate(DMAP_SUBCLASS_TEMPLATE);
        cgenConfiguration.setQuerySuperTemplate(DMAP_SUPERCLASS_TEMPLATE);
    }
}
