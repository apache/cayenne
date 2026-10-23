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

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.map.SQLTemplateDescriptor;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.helpers.NOPLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BaseTemplatesGenerationTest extends CgenCase {

    @TempDir
    public File folder;

    protected CgenConfiguration cgenConfiguration;
    protected ClassGenerationAction action;
    protected DataMap dataMap;
    protected ObjEntity objEntity;

    @BeforeEach
    public void setUp() {
        cgenConfiguration = new CgenConfiguration();
        action = new ClassGenerationAction(
                cgenConfiguration,
                getUnitTestInjector().getInstance(ToolsUtilsFactory.class),
                getUnitTestInjector().getInstance(MetadataUtils.class),
                NOPLogger.NOP_LOGGER);
        dataMap = new DataMap();
        dataMap.setDefaultPackage("test");
        objEntity = new ObjEntity();
    }

    @Test
    public void selectQuery() throws Exception {
        dataMap.setName("SelectQuery");
        createTestEntity();

        execute(new DataMapArtifact(dataMap, List.of(selectQuery("select", "from Test where name = $param"))));
    }

    @Test
    public void selectQueryResultTypes() throws Exception {
        dataMap.setName("SelectQueryResultTypes");
        createTestEntity();

        execute(new DataMapArtifact(dataMap, List.of(
                selectQuery("names", "select name from Test where name like $name"),
                selectQuery("counts", "select name, count(*) from Test"),
                selectQuery("rows", "from db:TEST where testKey = $key"))));
    }

    private void createTestEntity() {
        DbEntity dbEntity = new DbEntity("TEST");
        dbEntity.addAttribute(new DbAttribute("testKey"));
        dataMap.addDbEntity(dbEntity);

        ObjAttribute attribute = new ObjAttribute("name");
        attribute.setDbAttributePath("testKey");
        attribute.setType("java.lang.String");
        objEntity.setName("Test");
        objEntity.addAttribute(attribute);
        objEntity.setDbEntity(dbEntity);
        objEntity.setClassName("Test");
        dataMap.addObjEntity(objEntity);
    }

    private SelectQueryDescriptor selectQuery(String name, String ql) {
        SelectQueryDescriptor descriptor = new SelectQueryDescriptor();
        descriptor.setName(name);
        descriptor.setQl(ql);
        dataMap.addQueryDescriptor(descriptor);
        return descriptor;
    }

    @Test
    public void sqlTemplate() throws Exception {
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
    public void genClass() throws Exception {
        dataMap.setName("ObjEntity");

        DbEntity dbEntity = new DbEntity();
        DbAttribute dbAttribute = new DbAttribute("id");
        dbAttribute.setMandatory(true);
        dbAttribute.setPrimaryKey(true);
        dbAttribute.setType(Types.INTEGER);
        dbEntity.addAttribute(dbAttribute);
        dbEntity.setName("EntityTest");
        dbEntity.setDataMap(dataMap);
        dataMap.addDbEntity(dbEntity);

        objEntity.setDbEntity(dbEntity);
        objEntity.setClassName("test.ObjEntity");
        objEntity.setDataMap(dataMap);

        EntityArtifact entityArtifact = new EntityArtifact(objEntity);

        execute(entityArtifact);
    }

    public void execute(Artifact artifact) throws Exception{
        cgenConfiguration.addArtifact(artifact);

        cgenConfiguration.setRootPath(folder.toPath());
        cgenConfiguration.setOutputDir(Paths.get("."));
        cgenConfiguration.loadEntity(objEntity);
        cgenConfiguration.setDataMap(dataMap);

        action.execute();

        String targetName = dataMap.getName();

        fileComparison(targetName);
        fileComparison("auto/_" + targetName);
    }

    private void fileComparison(String fileName) throws IOException {
        String expected = readResource(fileName);

        StringBuilder generated = new StringBuilder();
        Files.readAllLines(new File(folder + "/test/" + fileName + ".java").toPath())
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
