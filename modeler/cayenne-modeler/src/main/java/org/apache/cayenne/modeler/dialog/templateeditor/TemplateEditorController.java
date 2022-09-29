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

package org.apache.cayenne.modeler.dialog.templateeditor;

import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.ClassGenerationActionFactory;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.dialog.pref.TemplatePreferences;
import org.apache.cayenne.modeler.dialog.pref.TemplatePreferencesView;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.cayenne.swing.BindingBuilder;
import org.apache.cayenne.tools.ToolsInjectorBuilder;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.apache.velocity.runtime.resource.util.StringResourceRepositoryImpl;

import javax.swing.text.BadLocationException;
import java.awt.Component;
import java.io.File;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * since 4.3
 */
public class TemplateEditorController extends CayenneController {

    public static final String TEMPLATE_EDITOR_REPO = "templateEditorRepo";
    public static final String TEMPLATE_EDITOR_WRITER = "tplEditorWriter";
    private static final String EDITED_TEMPLATE = "editedTemplate";
    private static final Key<StringWriter> TPL_EDITOR_WRITER = Key.get(StringWriter.class, TEMPLATE_EDITOR_WRITER);

    private final Map<String, String> customTemplates;
    private final DataMap currentDataMap;

    protected TemplatePreferencesView preferencesView;
    protected TemplateEditorView view;
    protected boolean canceled;

    public TemplateEditorController(TemplatePreferences preferences) {
        super(preferences);
        this.preferencesView = preferences.getView();
        this.customTemplates = application.getCodeTemplateManager().getCustomTemplates();
        this.currentDataMap = application.getFrameController().getProjectController().getCurrentDataMap();
        this.view = new TemplateEditorView(getEntityNames());
        initBindings();
    }

    public void startupAction() {
        this.view.setModal(true);
        this.view.getEditingTemplatePane().setText(loadSelectedTemplateText());
        this.view.editingTemplatePane.setCaretPosition(0);
        this.view.pack();
        makeCloseableOnEscape();
        centerView();
        this.view.setVisible(true);
    }

    private String loadSelectedTemplateText() {
        TemplateLoader templateLoader = new TemplateLoader();
        return templateLoader.load(view, getSelectedTemplate());
    }

    public Component getView() {
        return view;
    }

    protected void initBindings() {
        BindingBuilder builder = new BindingBuilder(getApplication().getBindingFactory(), this);
        builder.bindToAction(view.getSaveButton(), "saveAction()");
        builder.bindToAction(view.getPreviewButton(), "generateAction()");
        builder.bindToAction(view.getFindButton(), "findAction()");
        builder.bindToAction(view.getFindAndReplaceButton(), "findAndReplaceAction()");
    }

    @SuppressWarnings("unused")
    public void saveAction() {
        if (getSelectedTemplate() != null) {
            File dest = new File(getSelectedTemplate());
            FileTemplateSaver templateSaver = new FileTemplateSaver();
            templateSaver.save(view.getTemplateText(),dest,view);
        }
    }

    public String getSelectedTemplate() {
        int selectedRow = preferencesView.getTable().getSelectedRow();
        if (selectedRow != -1) {
            Object key = preferencesView.getTable().getModel().getValueAt(selectedRow, 0);
            return customTemplates.get(key.toString());
        }
        return null;
    }

    @SuppressWarnings("unused")
    public void generateAction() throws Exception {
        putTemplateTextInRepository();
        Injector injector = getInjector();
        ClassGenerationAction action = injector
                .getInstance(ClassGenerationActionFactory.class)
                .createAction(getCgenConfiguration());
        StringWriter writer = injector.getInstance(TPL_EDITOR_WRITER);
        action.addEntities(Collections.singleton(getSelectedEntity()));
        int caretPosition = view.getEditingTemplatePane().getCaretPosition();
        try {
            action.execute();
        } catch (ParseErrorException pe) {
            caretPosition = getErrorCaretPosition(pe);
            writer.write(pe.getMessage());
        } catch (Exception e) {
            writer.write(e.getMessage());
        }
        view.getEditingTemplatePane().setCaretPosition(caretPosition);
        view.getClassPreviewPane().setText(writer.toString());
        view.getClassPreviewPane().setCaretPosition(0);
    }

    private void putTemplateTextInRepository() {
        StringResourceLoader.setRepository(TEMPLATE_EDITOR_REPO, new StringResourceRepositoryImpl());
        StringResourceRepository repo = StringResourceLoader.getRepository(TEMPLATE_EDITOR_REPO);
        repo.putStringResource(EDITED_TEMPLATE, view.getTemplateText());
    }

    private Injector getInjector() {
        DataChannelMetaData metaData = getApplication().getMetaData();
        return new ToolsInjectorBuilder()
                .addModule(binder -> binder.bind(DataChannelMetaData.class).toInstance(metaData))
                .addModule(binder -> binder.bind(ClassGenerationActionFactory.class).to(PreviewClassGenerationFactory.class))
                .addModule(binder -> binder.bind(TPL_EDITOR_WRITER).to(StringWriter.class))
                .create();
    }

    private CgenConfiguration getCgenConfiguration() {
        CgenConfiguration cgenConfiguration = new CgenConfiguration();
        cgenConfiguration.setMakePairs(false);
        cgenConfiguration.setTemplate(EDITED_TEMPLATE);
        cgenConfiguration.setDataMap(currentDataMap);
        return cgenConfiguration;
    }

    private int getErrorCaretPosition(ParseErrorException e) throws BadLocationException {
        int errorLineNumber = e.getLineNumber();
        return view.getEditingTemplatePane().getLineStartOffset(errorLineNumber - 1);
    }

    @SuppressWarnings("unused")
    public void findAction() {
        new FindController(this).startupAction();
    }

    @SuppressWarnings("unused")
    public void findAndReplaceAction() {
        new FindAndReplaceController(this).startupAction();
    }

    private ObjEntity getSelectedEntity() {
        String selectedEntityName = view.getSelectedEntityName();
        if(currentDataMap == null) {
            return null;
        }
        return currentDataMap.getObjEntity(selectedEntityName);
    }

    private List<String> getEntityNames() {
        if(currentDataMap == null) {
            return Collections.emptyList();
        }
        return currentDataMap.getObjEntities().stream()
                .map(ObjEntity::getName)
                .collect(Collectors.toList());
    }

}
