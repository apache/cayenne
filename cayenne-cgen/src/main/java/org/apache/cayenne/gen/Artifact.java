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

import org.apache.velocity.context.Context;

/**
 * Represents a class generation "artifact" which is a facade to a metadata object used
 * for a given single generation template run.
 * 
 * @since 3.0
 */
public interface Artifact {

    String STRING_UTILS_KEY = "stringUtils";
    String IMPORT_UTILS_KEY = "importUtils";

    /**
     * Root object, such as ObjEntity or Embeddable, etc.
     */
    String OBJECT_KEY = "object";
    String SUPER_CLASS_KEY = "superClassName";
    String SUPER_PACKAGE_KEY = "superPackageName";
    String SUB_CLASS_KEY = "subClassName";
    String SUB_PACKAGE_KEY = "subPackageName";
    String BASE_CLASS_KEY = "baseClassName";
    String BASE_PACKAGE_KEY = "basePackageName";
    String CREATE_PROPERTY_NAMES = "createPropertyNames";
    String CREATE_PK_PROPERTIES = "createPKProperties";
    String PROPERTY_UTILS_KEY = "propertyUtils";
    String METADATA_UTILS_KEY = "metadataUtils";

    TemplateType[] getTemplateTypes(ArtifactGenerationMode mode);

    String getQualifiedBaseClassName();

    String getQualifiedClassName();

    /**
     * Returns a mapping metadata object for this artifact.
     */
    Object getObject();

    /**
     * A callback method that allows each artifact to add its own special keys to the
     * context. Invoked from
     * {@link ClassGenerationAction#resetContextForArtifactTemplate(Artifact)},
     * after the context is initialized by code generator, so this method can use
     * predefined keys from the context.
     */
    void postInitContext(Context context);
}
