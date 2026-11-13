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
package org.apache.cayenne.modeler.undo;

import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.CliArgs;
import org.apache.cayenne.modeler.event.model.ProjectEvent;
import org.apache.cayenne.modeler.platform.UIInitializer;
import org.apache.cayenne.modeler.project.ProjectSession;
import org.apache.cayenne.modeler.ui.action.UpdateValidationConfigAction;
import org.apache.cayenne.project.ConfigurationNameMapper;
import org.apache.cayenne.project.DefaultConfigurationNameMapper;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.projecttools.validation.Inspection;
import org.apache.cayenne.projecttools.validation.ValidationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.undo.UndoableEdit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UpdateValidationConfigUndoableEditTest {

    private Project project;
    private TestSession session;
    private RecordingUndoManager undoManager;
    private UpdateValidationConfigAction action;

    @BeforeEach
    public void setUp() {
        project = new Project();
        project.setDisabledInspections(List.of("NO_SUCH_INSPECTION", "PROJECT_NO_NAME"));

        TestApplication application = new TestApplication();
        undoManager = application.undoManager;
        session = new TestSession(application, project);
        action = new UpdateValidationConfigAction(application) {
            @Override
            public ProjectSession getProjectSession() {
                return session;
            }
        };
    }

    @Test
    public void undo() {
        UndoableEdit edit = updateConfig();

        // including the names unknown to the config
        edit.undo();
        assertEquals(List.of("NO_SUCH_INSPECTION", "PROJECT_NO_NAME"), List.copyOf(project.getDisabledInspections()));
        assertEquals(2, session.events);
    }

    @Test
    public void redo() {
        UndoableEdit edit = updateConfig();
        edit.undo();

        edit.redo();
        assertEquals(List.of("DB_ENTITY_NO_PK"), List.copyOf(project.getDisabledInspections()));
        assertEquals(3, session.events);

        // redo is not a new edit
        assertEquals(1, undoManager.edits.size());
    }

    private UndoableEdit updateConfig() {
        action.putConfig(new ValidationConfig(EnumSet.complementOf(EnumSet.of(Inspection.DB_ENTITY_NO_PK))))
                .setUndoable(true)
                .performAction(this);
        assertEquals(List.of("DB_ENTITY_NO_PK"), List.copyOf(project.getDisabledInspections()));

        assertEquals(1, undoManager.edits.size());
        return undoManager.edits.get(0);
    }

    /**
     * An Application that is not launched (so it has no frame and no actions), with a recording undo manager.
     */
    static class TestApplication extends Application {

        final RecordingUndoManager undoManager;

        TestApplication() {
            super(DIBootstrap.createInjector(
                            binder -> binder.bind(ConfigurationNameMapper.class).to(DefaultConfigurationNameMapper.class)),
                    new UIInitializer() {
                    },
                    CliArgs.parse(new String[0]));
            this.undoManager = new RecordingUndoManager(this);
        }

        @Override
        public CayenneUndoManager getUndoManager() {
            return undoManager;
        }
    }

    /**
     * Keeps the added edits, instead of updating the undo and redo actions.
     */
    static class RecordingUndoManager extends CayenneUndoManager {

        final List<UndoableEdit> edits = new ArrayList<>();

        RecordingUndoManager(Application application) {
            super(application);
        }

        @Override
        public boolean addEdit(UndoableEdit edit) {
            edits.add(edit);
            return true;
        }
    }

    /**
     * A session over a given project, that counts the project events instead of firing them.
     */
    static class TestSession extends ProjectSession {

        private final Project project;
        int events;

        TestSession(Application app, Project project) {
            super(app);
            this.project = project;
        }

        @Override
        public Project project() {
            return project;
        }

        @Override
        public void fireProjectEvent(ProjectEvent e) {
            events++;
        }
    }
}
