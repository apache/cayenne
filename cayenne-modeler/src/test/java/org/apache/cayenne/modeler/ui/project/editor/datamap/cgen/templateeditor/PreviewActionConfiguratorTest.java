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

package org.apache.cayenne.modeler.ui.project.editor.datamap.cgen.templateeditor;

import org.apache.cayenne.project.xml.DefaultProjectMetaData;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.TemplateType;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.NamedQuery;
import org.apache.cayenne.map.NamedSelectQuery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Writer;

public class PreviewActionConfiguratorTest {

    private static final String TEST_TEMPLATE_TEXT = "TestTemplate";
    private DataMap dataMap;

    @BeforeEach
    public void config() {
        this.dataMap = configureDataMap();
    }

    private DataMap configureDataMap() {
        DataMap dataMap = new DataMap();
        dataMap.setName("dataMap");

        ObjEntity objEntity = new ObjEntity("objEntity");
        dataMap.addObjEntity(objEntity);

        Embeddable embeddable = new Embeddable("embeddable");
        dataMap.addEmbeddable(embeddable);

        NamedQuery namedQuery = new NamedSelectQuery();
        namedQuery.setName("namedQuery");
        dataMap.addNamedQuery(namedQuery);
        return dataMap;
    }

    @Test
    public void previewActionEntityTest() throws Exception {
        actionTest("objEntity",TemplateType.ENTITY_SUBCLASS,new EntityArtefactsConfigurator());
    }

    @Test
    public void previewActionEmbeddableTest() throws Exception {
        actionTest("embeddable",TemplateType.EMBEDDABLE_SUBCLASS,new EmbeddableArtefactsConfigurator());
    }

    @Test
    public void previewActionDataMapTest() throws Exception {
        actionTest("namedQuery",TemplateType.DATAMAP_SUBCLASS,new DataMapArtefactsConfigurator());
    }

    private void actionTest(String artifactName, TemplateType type, ArtefactsConfigurator configurator) throws Exception {

        PreviewActionConfigurator actionConfigurator = new PreviewActionConfigurator(
                type,
                configurator,
                new DefaultProjectMetaData(),
                () -> dataMap,
                () -> artifactName);
        ClassGenerationAction action = actionConfigurator.preparePreviewAction(TEST_TEMPLATE_TEXT);
        action.execute();
        Writer writer = actionConfigurator.getWriter();
        Assertions.assertEquals(TEST_TEMPLATE_TEXT, writer.toString());
    }

}
