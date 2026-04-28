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

package org.apache.cayenne.modeler.ui.project.editor.dbentity.main;

import org.apache.cayenne.modeler.event.model.DbEntityEvent;
import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.JLabel;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbKeyGenerator;
import org.apache.cayenne.modeler.toolkit.text.CMUndoableTextField;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationException;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class PKCustomSequenceGeneratorPanel extends PKGeneratorPanel {

    protected CMUndoableTextField customPKName;
    protected CMUndoableTextField customPKSize;

    public PKCustomSequenceGeneratorPanel(ProjectController mediator) {
        super(mediator);
        initView();
    }

    private void initView() {

        JLabel note = new JLabel(
                "* Custom sequences are supported on Oracle and Postgres");
        note.setFont(note.getFont().deriveFont(Font.ITALIC).deriveFont(11f));

        customPKName = new CMUndoableTextField(mediator.getApplication().getUndoManager());
        customPKName.addCommitListener(this::setPKName);
        customPKSize = new CMUndoableTextField(mediator.getApplication().getUndoManager());
        customPKSize.addCommitListener(this::setPKSize);

        // assemble

        DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout(
                "right:70dlu, 3dlu, 20dlu, 3dlu, fill:177dlu",
                ""));

        builder.setDefaultDialogBorder();

        builder.append("Sequence Name:", customPKName, 3);
        builder.append("Cached PK Size:", customPKSize);
        builder.nextLine();
        builder.append("", note, 3);

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);
    }

    protected void onInitInternal(DbEntity entity) {
        resetStrategy(entity, false, true);

        if (entity.getPrimaryKeyGenerator() == null) {
            DbKeyGenerator generator = new DbKeyGenerator();
            generator.setGeneratorType(DbKeyGenerator.ORACLE_TYPE);
            entity.setPrimaryKeyGenerator(generator);
        } else {
            setDbEntity(entity);
        }
    }

    public void setDbEntity(DbEntity entity) {
        DbKeyGenerator generator = entity.getPrimaryKeyGenerator();

        if (generator != null) {
            customPKName.setText(generator.getGeneratorName());
            customPKSize.setText(generator.getKeyCacheSize() != null ?
                    generator.getKeyCacheSize().toString() : "0");
        } else {
            customPKName.setText(null);
            customPKSize.setText(null);
        }
    }

    protected void setPKSize(String text) {

        if (mediator.getSelectedDbEntity() == null
                || mediator.getSelectedDbEntity().getPrimaryKeyGenerator() == null) {
            return;
        }

        int cacheSize = 0;

        if (text != null && text.trim().length() > 0) {
            try {
                cacheSize = Integer.parseInt(text);
            }
            catch (NumberFormatException nfex) {
                throw new ValidationException("Invalid number");
            }
        }

        DbKeyGenerator generator = mediator.getSelectedDbEntity().getPrimaryKeyGenerator();
        if (!Util.nullSafeEquals(generator.getKeyCacheSize(), cacheSize)) {
            generator.setKeyCacheSize(cacheSize);
            mediator.fireDbEntityEvent(DbEntityEvent.ofChange(this, generator.getDbEntity()));
        }
    }

    protected void setPKName(String text) {

        if (mediator.getSelectedDbEntity() == null
                || mediator.getSelectedDbEntity().getPrimaryKeyGenerator() == null) {
            return;
        }

        if (text != null && text.trim().length() == 0) {
            text = null;
        }

        DbKeyGenerator generator = mediator.getSelectedDbEntity().getPrimaryKeyGenerator();
        if (!Util.nullSafeEquals(text, generator.getName())) {
            generator.setGeneratorName(text);
            mediator.fireDbEntityEvent(DbEntityEvent.ofChange(this, generator.getDbEntity()));
        }
    }
}
