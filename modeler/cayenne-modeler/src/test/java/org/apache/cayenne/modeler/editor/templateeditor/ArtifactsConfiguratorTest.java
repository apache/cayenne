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

package org.apache.cayenne.modeler.editor.templateeditor;

import org.apache.cayenne.gen.Artifact;
import org.apache.cayenne.gen.ArtifactsGenerationMode;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.modeler.editor.cgen.templateeditor.DataMapArtefactsConfigurator;
import org.apache.cayenne.modeler.editor.cgen.templateeditor.EmbeddableArtefactsConfigurator;
import org.apache.cayenne.modeler.editor.cgen.templateeditor.EntityArtefactsConfigurator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class ArtifactsConfiguratorTest {
    private DataMap dataMap;
    private ClassGenerationAction action;
    private EntityArtefactsConfigurator entityArtefactsConfigurator;
    private EmbeddableArtefactsConfigurator embeddableArtefactsConfigurator;
    private DataMapArtefactsConfigurator dataMapArtefactsConfigurator;

    @Before
    public void config(){
        this.dataMap = configureDataMap();
        this.action = new ClassGenerationAction(createCgenConfiguration());
        this.entityArtefactsConfigurator = new EntityArtefactsConfigurator();
        this.embeddableArtefactsConfigurator = new EmbeddableArtefactsConfigurator();
        this.dataMapArtefactsConfigurator = new DataMapArtefactsConfigurator();

    }


    @Test
    public void configTest() throws NoSuchFieldException, IllegalAccessException {
        entityArtefactsConfigurator.config(action,"objEntity");
        embeddableArtefactsConfigurator.config(action,"embeddable");
        dataMapArtefactsConfigurator.config(action,"queryDescriptor");

        CgenConfiguration configuration = action.getCgenConfiguration();

        Field artifactsField = configuration.getClass().getDeclaredField("artifacts");
        artifactsField.setAccessible(true);
        Collection<Artifact> artifacts = (Collection<Artifact>) artifactsField.get(configuration);

        Assert.assertEquals(3, artifacts.size());

    }

    @Test
    public void getArtifactsNamesTest(){
        List<String> entityArtifactsNames = entityArtefactsConfigurator.getArtifactsNames(dataMap);
        List<String> embeddableArtifactsNames = embeddableArtefactsConfigurator.getArtifactsNames(dataMap);
        List<String> dataMapArtifactsNames = dataMapArtefactsConfigurator.getArtifactsNames(dataMap);

        assertTrue(entityArtifactsNames.contains("objEntity"));
        assertTrue(embeddableArtifactsNames.contains("embeddable"));
        assertTrue(dataMapArtifactsNames.contains("dataMap"));
    }

    private DataMap configureDataMap() {
        DataMap dataMap = new DataMap();
        dataMap.setName("dataMap");

        ObjEntity objEntity = new ObjEntity("objEntity");
        dataMap.addObjEntity(objEntity);

        Embeddable embeddable = new Embeddable("embeddable");
        dataMap.addEmbeddable(embeddable);

        QueryDescriptor descriptor = QueryDescriptor.descriptor(QueryDescriptor.SELECT_QUERY);
        descriptor.setName("queryDescriptor");
        dataMap.addQueryDescriptor(descriptor);
        return dataMap;
    }

    private CgenConfiguration createCgenConfiguration() {
        CgenConfiguration cgenConfiguration = new CgenConfiguration();
        cgenConfiguration.setDataMap(dataMap);
        cgenConfiguration.setMakePairs(false);
        cgenConfiguration.setArtifactsGenerationMode(ArtifactsGenerationMode.ALL.getLabel());
        return cgenConfiguration;
    }

}
