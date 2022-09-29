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

import org.apache.velocity.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * since 4.3
 */
public class TemplateLoader {

    private static final Logger logger = LoggerFactory.getLogger(TemplateLoader.class);

    public String load(TemplateEditorView view, String selectedTemplate) {
        StringBuilder stringBuilder = new StringBuilder();
        if (selectedTemplate != null) {
            try {
                List<String> strings = Files.readAllLines(Paths.get(selectedTemplate));
                for (String string : strings) {
                    stringBuilder.append(string).append("\n");
                }
            } catch (IOException | ResourceNotFoundException e) {
                JOptionPane.showMessageDialog(
                        view,
                        "File reading error \n" + selectedTemplate,
                        "Error",
                        JOptionPane.WARNING_MESSAGE);
                logger.warn("File reading error {}", selectedTemplate);
            }
        }
        return stringBuilder.toString();
    }
}
