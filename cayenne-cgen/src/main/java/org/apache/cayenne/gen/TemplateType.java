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

import java.util.Arrays;

/**
 * Defines class generation template types.
 * 
 * @since 3.0
 */
public enum TemplateType {

    ENTITY_SINGLE_CLASS(false,"Single Entity Class","templates/v4_1/singleclass.vm"),

    ENTITY_SUPERCLASS(true,"Entity Superclass","templates/v4_1/superclass.vm"),

    ENTITY_SUBCLASS(false,"Entity Subclass","templates/v4_1/subclass.vm"),
    EMBEDDABLE_SINGLE_CLASS(false,"Single Embeddable Class","templates/v4_1/embeddable-singleclass.vm"),

    EMBEDDABLE_SUPERCLASS(true,"Embeddable Superclass","templates/v4_1/embeddable-superclass.vm"),

    EMBEDDABLE_SUBCLASS(false,"Embeddable Subclass", "templates/v4_1/embeddable-subclass.vm"),

    DATAMAP_SINGLE_CLASS(false,"Single DataMap Class","templates/v4_1/datamap-singleclass.vm"),

    DATAMAP_SUPERCLASS(true,"DataMap Superclass", "templates/v4_1/datamap-superclass.vm"),

    DATAMAP_SUBCLASS(false,"DataMap Subclass",   "templates/v4_1/datamap-subclass.vm");

    private final boolean superclass;
    private final String readableName;
    private final CgenTemplate defaultTemplate;

    TemplateType(boolean superclass, String readableName,  String defaultTemplate) {
        this.superclass = superclass;
        this.readableName = readableName;
        this.defaultTemplate = new CgenTemplate(defaultTemplate,true,this);
    }

    public boolean isSuperclass() {
        return superclass;
    }

    public String readableName() {
        return readableName;
    }

    public String pathFromSourceRoot() {return defaultTemplate.getData();}

    public CgenTemplate defaultTemplate() {
        return defaultTemplate;
    }

    public static TemplateType byName(String name){
        for (TemplateType templateType : TemplateType.values()) {
            if (templateType.readableName.equals(name)){
                return templateType;
            }
        }
      return null;
    }

    public static TemplateType byPath(String path){
        for (TemplateType templateType : TemplateType.values()) {
            if (templateType.pathFromSourceRoot().equals(path)){
                return templateType;
            }
        }
        return null;
    }

    public static boolean isDefault(String templateText) {
        return Arrays.stream(values()).anyMatch(t -> t.pathFromSourceRoot().equals(templateText));
    }
}
