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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * since 4.3
 */
public class FileTemplateSaver  {

    private static final Logger logger = LoggerFactory.getLogger(FileTemplateSaver.class);

    public void save(String templateText, File dest, JDialog view) {
        try {
            Files.write(dest.toPath(), templateText.getBytes());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    view,
                    "File writing error \n" + dest,
                    "Error",
                    JOptionPane.WARNING_MESSAGE);
            logger.warn("File writing error {}", dest);
        }
        JOptionPane.showMessageDialog(
                view,
                new JLabel("The changes in the \n"
                        + dest
                        + "\n have been saved", SwingConstants.CENTER),
                "Message",
                JOptionPane.PLAIN_MESSAGE);
        logger.info("Change the template {}", dest);
    }
}
