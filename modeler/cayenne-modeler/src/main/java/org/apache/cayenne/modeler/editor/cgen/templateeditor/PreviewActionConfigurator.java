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

package org.apache.cayenne.modeler.editor.cgen.templateeditor;

import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.gen.ArtifactsGenerationMode;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.ClassGenerationActionFactory;
import org.apache.cayenne.gen.TemplateType;
import org.apache.cayenne.tools.ToolsInjectorBuilder;

import java.io.StringWriter;
import java.io.Writer;


/**
 * @since 4.3
 */
public class PreviewActionConfigurator {

    public static final String TEMPLATE_EDITOR_WRITER = "tplEditorWriter";
    private static final Key<StringWriter> TPL_EDITOR_WRITER = Key.get(StringWriter.class, TEMPLATE_EDITOR_WRITER);
    private final TemplateEditorController editorController;
    private final TemplateType templateType;
    private final ArtefactsConfigurator artefactsConfigurator;
    private final Injector injector;

    public PreviewActionConfigurator(TemplateEditorController editorController) {
        this.editorController = editorController;
        this.templateType = editorController.getTemplateType();
        this.artefactsConfigurator = editorController.getArtefactsConfigurator();
        this.injector = getInjector();
    }

    private Injector getInjector() {
        DataChannelMetaData metaData = editorController.getApplication().getMetaData();
        return new ToolsInjectorBuilder()
                .addModule(binder -> binder.bind(DataChannelMetaData.class).toInstance(metaData))
                .addModule(binder -> binder.bind(ClassGenerationActionFactory.class).to(PreviewClassGenerationFactory.class))
                .addModule(binder -> binder.bind(TPL_EDITOR_WRITER).to(StringWriter.class))
                .create();
    }

    public ClassGenerationAction preparePreviewAction(String templateText) {
        CgenConfiguration previewCgenConfiguration = createPreviewCgenConfiguration();
        setTemplateTextInCgenConfig(templateText,previewCgenConfiguration);

        ClassGenerationAction action = injector
                .getInstance(ClassGenerationActionFactory.class)
                .createAction(previewCgenConfiguration);
        artefactsConfigurator.config(action, editorController.getSelectedArtifactName());
        return action;
    }

    private void setTemplateTextInCgenConfig(String templateText, CgenConfiguration previewCgenConfiguration) {
        switch (templateType) {
            case ENTITY_SUPERCLASS:
            case ENTITY_SUBCLASS: {
                previewCgenConfiguration.setTemplate(templateText);
                break;
            }
            case EMBEDDABLE_SUPERCLASS:
            case EMBEDDABLE_SUBCLASS: {
                previewCgenConfiguration.setEmbeddableTemplate(templateText);
                break;
            }
            case DATAMAP_SUPERCLASS:
            case DATAMAP_SUBCLASS: {
                previewCgenConfiguration.setDataMapTemplate(templateText);
                break;
            }
            default:
                throw new IllegalStateException("Illegal template type " + templateType);
        }
    }

    private CgenConfiguration createPreviewCgenConfiguration() {
        CgenConfiguration cgenConfiguration = new CgenConfiguration();
        cgenConfiguration.setDataMap(editorController.getCurrentDataMap());
        cgenConfiguration.setMakePairs(false);
        cgenConfiguration.setArtifactsGenerationMode(ArtifactsGenerationMode.ALL.getLabel());
        return cgenConfiguration;
    }

    public Writer getWriter() {
        return injector.getInstance(TPL_EDITOR_WRITER);
    }

}
