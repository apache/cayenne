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

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.map.SQLTemplateDescriptor;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class BaseTemplatesGenerationTest {

    @Rule
    public TemporaryFolder folder= new TemporaryFolder();

    protected CgenConfiguration cgenConfiguration;
    protected ClassGenerationAction action;
    protected DataMap dataMap;
    protected ObjEntity objEntity;

    @Before
    public void setUp() {
        cgenConfiguration = new CgenConfiguration(false);
        action = new ClassGenerationAction(cgenConfiguration);
        dataMap = new DataMap();
        dataMap.setDefaultPackage("test");
        objEntity = new ObjEntity();
    }

    @Test
    public void testSelectQuery() throws Exception {
        dataMap.setName("SelectQuery");

        String param = "param";
        String qualifierString = "name = $" + param;

        DbEntity dbEntity = new DbEntity();
        ObjAttribute attribute = new ObjAttribute("name");
        attribute.setDbAttributePath("testKey");
        attribute.setType("java.lang.String");
        objEntity.addAttribute(attribute);
        objEntity.setDbEntity(dbEntity);
        objEntity.setClassName("Test");

        SelectQueryDescriptor selectQueryDescriptor = new SelectQueryDescriptor();
        Expression exp = ExpressionFactory.exp(qualifierString);
        selectQueryDescriptor.setQualifier(exp);
        selectQueryDescriptor.setName("select");
        selectQueryDescriptor.setRoot(objEntity);

        Collection<QueryDescriptor> descriptors = new ArrayList<>();
        descriptors.add(selectQueryDescriptor);

        DataMapArtifact dataMapArtifact = new DataMapArtifact(dataMap, descriptors);

        execute(dataMapArtifact);
    }

    @Test
    public void testSQLTemplate() throws Exception {
        dataMap.setName("SQLTemplate");

        DbEntity dbEntity = new DbEntity();
        objEntity.setDbEntity(dbEntity);
        objEntity.setClassName("Test");

        SQLTemplateDescriptor sqlTemplateDescriptor = new SQLTemplateDescriptor();
        sqlTemplateDescriptor.setSql("SELECT * FROM table");
        sqlTemplateDescriptor.setRoot(objEntity);
        sqlTemplateDescriptor.setName("select");
        sqlTemplateDescriptor.setRoot(objEntity);

        Collection<QueryDescriptor> descriptors = new ArrayList<>();
        descriptors.add(sqlTemplateDescriptor);

        DataMapArtifact dataMapArtifact = new DataMapArtifact(dataMap, descriptors);

        execute(dataMapArtifact);
    }

    @Test
    public void testGenClass() throws Exception {
        dataMap.setName("ObjEntity");

        DbEntity dbEntity = new DbEntity();
        dbEntity.setName("EntityTest");
        objEntity.setDbEntity(dbEntity);
        objEntity.setClassName("test.ObjEntity");
        objEntity.setDataMap(dataMap);

        EntityArtifact entityArtifact = new EntityArtifact(objEntity);

        execute(entityArtifact);
    }

    public void execute(Artifact artifact) throws Exception{
        cgenConfiguration.addArtifact(artifact);

        cgenConfiguration.setRootPath(folder.getRoot().toPath());
        cgenConfiguration.setRelPath(Paths.get("."));
        cgenConfiguration.loadEntity(objEntity);
        cgenConfiguration.setDataMap(dataMap);

        action.setUtilsFactory(new DefaultToolsUtilsFactory());
        action.execute();

        String targetName = dataMap.getName();

        fileComparison(targetName);
        fileComparison("auto/_" + targetName);
    }

    private void fileComparison(String fileName) throws IOException {
        String expected = readResource(fileName);

        StringBuilder generated = new StringBuilder();
        Files.readAllLines(new File(folder.getRoot() + "/test/" + fileName + ".java").toPath())
                .forEach(generated::append);

        assertEquals(expected, generated.toString());
    }

    private String readResource(String name) throws IOException {
        String resourceName = "templateTest/_" + name + ".java";
        InputStream stream = getClass().getClassLoader().getResourceAsStream(resourceName);
        if(stream == null) {
            throw new FileNotFoundException("Resource not found: " + resourceName);
        }
        StringBuilder expected = new StringBuilder();
        try(BufferedReader resource = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = resource.readLine()) != null) {
                expected.append(line);
            }
        }

        return expected.toString();
    }
}
