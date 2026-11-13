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

import org.apache.cayenne.project.cgen.CgenDescriptor;

/**
 * Defines class generation template types.
 *
 * @since 3.0
 */
public enum TemplateType {

    ENTITY_SINGLE_CLASS(false, "Single Entity Class", CgenDescriptor.DEFAULT_SINGLE_CLASS_TEMPLATE),

    ENTITY_SUPERCLASS(true, "Entity Superclass", CgenDescriptor.DEFAULT_SUPER_TEMPLATE),

    ENTITY_SUBCLASS(false, "Entity Subclass", CgenDescriptor.DEFAULT_TEMPLATE),

    EMBEDDABLE_SINGLE_CLASS(false, "Single Embeddable Class", CgenDescriptor.DEFAULT_EMBEDDABLE_SINGLE_CLASS_TEMPLATE),

    EMBEDDABLE_SUPERCLASS(true, "Embeddable Superclass", CgenDescriptor.DEFAULT_EMBEDDABLE_SUPER_TEMPLATE),

    EMBEDDABLE_SUBCLASS(false, "Embeddable Subclass", CgenDescriptor.DEFAULT_EMBEDDABLE_TEMPLATE),

    DATAMAP_SINGLE_CLASS(false, "Single DataMap Class", CgenDescriptor.DEFAULT_DATA_MAP_SINGLE_CLASS_TEMPLATE),

    DATAMAP_SUPERCLASS(true, "DataMap Superclass", CgenDescriptor.DEFAULT_DATA_MAP_SUPER_TEMPLATE),

    DATAMAP_SUBCLASS(false, "DataMap Subclass", CgenDescriptor.DEFAULT_DATA_MAP_TEMPLATE);

    private final boolean superclass;
    private final String readableName;
    private final CgenTemplate defaultTemplate;

    TemplateType(boolean superclass, String readableName, String defaultTemplate) {
        this.superclass = superclass;
        this.readableName = readableName;
        this.defaultTemplate = new CgenTemplate(defaultTemplate, true, this);
    }

    public boolean isSuperclass() {
        return superclass;
    }

    public String readableName() {
        return readableName;
    }

    public String pathFromSourceRoot() {
        return defaultTemplate.getData();
    }

    public CgenTemplate defaultTemplate() {
        return defaultTemplate;
    }

    public static boolean isDefault(String templateText) {
        for (TemplateType type : values()) {
            if (type.pathFromSourceRoot().equals(templateText)) {
                return true;
            }
        }
        return false;
    }
}
