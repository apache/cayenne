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

import org.apache.cayenne.configuration.xml.DefaultDataChannelMetaData;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.TemplateType;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.QueryDescriptor;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.editor.cgen.templateeditor.ArtefactsConfigurator;
import org.apache.cayenne.modeler.editor.cgen.templateeditor.DataMapArtefactsConfigurator;
import org.apache.cayenne.modeler.editor.cgen.templateeditor.EmbeddableArtefactsConfigurator;
import org.apache.cayenne.modeler.editor.cgen.templateeditor.EntityArtefactsConfigurator;
import org.apache.cayenne.modeler.editor.cgen.templateeditor.PreviewActionConfigurator;
import org.apache.cayenne.modeler.editor.cgen.templateeditor.TemplateEditorController;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.Writer;

import static org.mockito.Mockito.mock;

public class PreviewActionConfiguratorTest {

    private static final String TEST_TEMPLATE_TEXT = "TestTemplate";
    private TemplateEditorController editorController;


    @Before
    public void config() {
        DataMap dataMap = configureDataMap();

        Application application = mock(Application.class);

        this.editorController = mock(TemplateEditorController.class);

        Mockito.when(editorController.getApplication()).thenReturn(application);
        Mockito.when(application.getMetaData()).thenReturn(new DefaultDataChannelMetaData());
        Mockito.when(editorController.getCurrentDataMap()).thenReturn(dataMap);

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
        actionTest("queryDescriptor",TemplateType.DATAMAP_SUBCLASS,new DataMapArtefactsConfigurator());
    }

    private void actionTest(String artifactName, TemplateType type, ArtefactsConfigurator configurator) throws Exception {

        Mockito.when(editorController.getSelectedArtifactName()).thenReturn(artifactName);
        Mockito.when(editorController.getTemplateType()).thenReturn(type);
        Mockito.when(editorController.getArtefactsConfigurator()).thenReturn(configurator);

        PreviewActionConfigurator actionConfigurator = new PreviewActionConfigurator(editorController);
        ClassGenerationAction action = actionConfigurator.preparePreviewAction(TEST_TEMPLATE_TEXT);
        action.execute();
        Writer writer = actionConfigurator.getWriter();
        Assert.assertEquals(TEST_TEMPLATE_TEXT, writer.toString());
    }

}
