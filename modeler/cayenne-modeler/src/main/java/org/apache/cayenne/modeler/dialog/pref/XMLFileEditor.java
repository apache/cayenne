/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.modeler.dialog.pref;

import org.apache.cayenne.dbimport.DefaultReverseEngineeringLoader;
import org.apache.cayenne.dbimport.DefaultReverseEngineeringWriter;
import org.apache.cayenne.dbimport.ReverseEngineering;
import org.apache.cayenne.dbimport.ReverseEngineeringLoaderException;
import org.apache.cayenne.modeler.util.CayenneController;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

/**
 * @since 4.0
 */
public class XMLFileEditor extends CayenneController {
    private static final Log LOGGER = LogFactory.getLog(XMLFileEditor.class);

    protected XMLView XMLview;


    public XMLFileEditor(CayenneController parent) {
        super(parent);

        this.XMLview = new XMLView();
    }

    public ReverseEngineering convertTextIntoReverseEngineering() throws ReverseEngineeringLoaderException {
        String text = XMLview.getEditorPane().getText();
        try(InputStream inputStream = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))) {
            ReverseEngineering reverseEngineering = (new DefaultReverseEngineeringLoader()).load(inputStream);
            return reverseEngineering;
        } catch (IOException e) {
            addAlertMessage(e.getMessage());
        }
        return null;
    }

    public void convertReverseEngineeringIntoText(ReverseEngineering reverseEngineering) {
        StringWriter buffer = new StringWriter();
        PrintWriter writer = new PrintWriter(buffer);
        DefaultReverseEngineeringWriter reverseEngineeringWriter = new DefaultReverseEngineeringWriter();
        reverseEngineeringWriter.write(reverseEngineering, writer);
        XMLview.getEditorPane().setText(buffer.toString());
    }

    @Override
    public XMLView getView() {
        return XMLview;
    }

    public void addAlertMessage(String message) {
        XMLview.addAlertMessage(message);
    }

    public void removeAlertMessage() {
        XMLview.removeAlertMessage();
    }
}
