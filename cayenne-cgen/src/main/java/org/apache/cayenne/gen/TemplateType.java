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

/**
 * Defines class generation template types.
 * 
 * @since 3.0
 */
public enum TemplateType {

    ENTITY_SINGLE_CLASS(false,"Single Entity Class","singleclass"),

    ENTITY_SUPERCLASS(true,"Entity Superclass","superclass"),

    ENTITY_SUBCLASS(false,"Entity Subclass","subclass"),

    EMBEDDABLE_SINGLE_CLASS(false,"Single Embeddable Class","embeddable-singleclass"),

    EMBEDDABLE_SUPERCLASS(true,"Embeddable Superclass","embeddable-superclass"),

    EMBEDDABLE_SUBCLASS(false,"Embeddable Subclass","embeddable-subclass"),

    DATAMAP_SINGLE_CLASS(false,"Single DataMap Class","datamap-singleclass"),

    DATAMAP_SUPERCLASS(true,"DataMap Superclass","datamap-superclass"),

    DATAMAP_SUBCLASS(false,"DataMap Subclass","datamap-subclass");

    private final boolean superclass;
    private final String readableName;
    private final String fileName;
    private static final String EXTENSION = ".vm";
    private static final String TEMPLATES_DIR = "templates/v4_1/";

    TemplateType(boolean superclass, String readableName, String fileName) {
        this.superclass = superclass;
        this.readableName = readableName;
        this.fileName = fileName;
    }

    public boolean isSuperclass() {
        return superclass;
    }

    public String readableName() {
        return readableName;
    }

    public String fileName(){
        return fileName;
    }

    public String fullFileName(){
        return fileName+EXTENSION;
    }

    public String pathFromSourceRoot() {return TEMPLATES_DIR+fileName+EXTENSION;}

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
}
