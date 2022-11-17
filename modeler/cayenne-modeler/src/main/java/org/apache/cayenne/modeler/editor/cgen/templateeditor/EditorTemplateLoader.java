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

import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.TemplateType;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @since 5.0
 */
public class EditorTemplateLoader {

    private static final Logger logger = LoggerFactory.getLogger(EditorTemplateLoader.class);
    private final CgenConfiguration cgenConfiguration;
    private final JFrame view;

    public EditorTemplateLoader(CgenConfiguration configuration, TemplateEditorView view) {
        this.cgenConfiguration = configuration;
        this.view = view;
    }

    public String load(TemplateType type, Boolean isTemplateDefault) {
        if (isTemplateDefault) {
            return getDefaultTemplateText(type);
        } else {
            return cgenConfiguration.getTemplateByType(type).getData();
        }
    }

    private String getDefaultTemplateText(TemplateType type) {
        TemplateType templateType = cgenConfiguration.isMakePairs() ? type : switchTypeToSingle(type);
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(templateType.pathFromSourceRoot())) {
            if (stream != null) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException | ResourceNotFoundException e) {
            JOptionPane.showMessageDialog(
                    view,
                    "File reading error \n" + templateType.pathFromSourceRoot(),
                    "Error",
                    JOptionPane.WARNING_MESSAGE);
            logger.warn("File reading error {}", templateType.pathFromSourceRoot());
        }
        return null;
    }

    private TemplateType switchTypeToSingle(TemplateType type) {
            switch (type) {
                case ENTITY_SUBCLASS: {
                    return TemplateType.ENTITY_SINGLE_CLASS;
                }
                case EMBEDDABLE_SUBCLASS: {
                    return TemplateType.EMBEDDABLE_SINGLE_CLASS;
                }
                case DATAMAP_SUBCLASS: {
                    return TemplateType.DATAMAP_SINGLE_CLASS;
                }
                default:
                    throw new IllegalStateException("Illegal template type for singleClass " + type);
            }
    }

}
