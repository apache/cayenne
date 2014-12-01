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
package org.apache.cayenne.tools;

import static org.apache.cayenne.tools.dbimport.config.DefaultReverseEngineeringLoaderTest.*;
import static org.junit.Assert.assertEquals;

import org.apache.cayenne.tools.dbimport.config.Schema;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.junit.Test;

/**
 * @since 3.2.
 */
public class DbImporterMojoConfigurationTest extends AbstractMojoTestCase {

    @Test
    public void testLoadCatalog() throws Exception {
        assertCatalog(getCdbImport("pom-catalog.xml").getReverseEngineering().getCatalogs().iterator());
    }

    @Test
    public void testLoadSchema() throws Exception {
        Schema schema = getCdbImport("pom-schema.xml").getReverseEngineering().getSchemas().iterator().next();
        assertEquals("schema-name-03", schema.getName());

        assertSchemaContent(schema);
    }

    @Test
    public void testLoadCatalogAndSchema() throws Exception {
        assertCatalogAndSchema(getCdbImport("pom-catalog-and-schema.xml").getReverseEngineering());
    }

    @Test
    public void testLoadFlat() throws Exception {
        assertFlat(getCdbImport("pom-flat.xml").getReverseEngineering());

    }

    private DbImporterMojo getCdbImport(String pomFileName) throws Exception {
        return (DbImporterMojo) lookupMojo("cdbimport",
                getTestFile("src/test/resources/org/apache/cayenne/tools/config/" + pomFileName));
    }

}
