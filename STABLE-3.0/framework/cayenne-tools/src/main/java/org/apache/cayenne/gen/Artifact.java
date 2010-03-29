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

import org.apache.velocity.VelocityContext;

/**
 * Represents a class generation "artifact" which is a facade to a metadata object used
 * for a given single generation template run.
 * 
 * @since 3.0
 */
public interface Artifact {

    public static String STRING_UTILS_KEY = "stringUtils";
    public static String IMPORT_UTILS_KEY = "importUtils";

    /**
     * Root object, such as ObjEntity or Embeddable, etc.
     */
    public static String OBJECT_KEY = "object";
    public static String SUPER_CLASS_KEY = "superClassName";
    public static String SUPER_PACKAGE_KEY = "superPackageName";
    public static String SUB_CLASS_KEY = "subClassName";
    public static String SUB_PACKAGE_KEY = "subPackageName";
    public static String BASE_CLASS_KEY = "baseClassName";
    public static String BASE_PACKAGE_KEY = "basePackageName";

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
     * {@link ClassGenerationAction#resetContextForArtifactTemplate(Artifact, TemplateType)},
     * after the context is initialized by code generator, so this method can use
     * predefined keys from the context.
     */
    void postInitContext(VelocityContext context);
}
