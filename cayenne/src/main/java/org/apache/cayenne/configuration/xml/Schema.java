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
package org.apache.cayenne.configuration.xml;

/**
 * Utility class for generating XSD namespace URLs for Cayenne project XML files.
 *
 * @since 5.0
 */
public final class Schema {

    private static final String NAMESPACE_BASE = "http://cayenne.apache.org/schema/";
    private static final String SCHEMA_LOCATION_BASE = "https://cayenne.apache.org/schema/";

    private Schema() {
    }

    /**
     * Generates XSD namespace for the given schema type and version.
     *
     * @param version project version
     * @param schemaType schema type (e.g., "domain", "modelMap", "info", "cgen")
     * @return XSD namespace URL
     */
    public static String buildNamespace(ProjectVersion version, String schemaType) {
        return NAMESPACE_BASE + version.getAsString() + "/" + schemaType;
    }

    /**
     * Generates XSD schema location for the given schema type and version.
     *
     * @param version project version
     * @param schemaType schema type (e.g., "domain", "modelMap")
     * @return XSD schema location URL
     */
    public static String buildSchemaLocation(ProjectVersion version, String schemaType) {
        return SCHEMA_LOCATION_BASE + version.getAsString() + "/" + schemaType + ".xsd";
    }
}
