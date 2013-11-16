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
package org.apache.cayenne.project.unit;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.TestCase;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.test.file.FileUtil;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class Project2Case extends TestCase {

    /**
     * A helper method returning the contents of an XML source as a DOM Document.
     * 
     * @throws IOException
     * @throws SAXException
     */
    protected Document toDOMTree(File file) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder domParser;
        try {
            domParser = dbf.newDocumentBuilder();
        }
        catch (ParserConfigurationException e) {
            fail("ParserConfigurationException: " + e.getMessage());
            throw new RuntimeException();
        }

        try {
            return domParser.parse(file);
        }
        catch (Exception e) {
            fail("DOM parsing exception: " + e.getMessage());
            throw new RuntimeException();
        }
    }

    protected File setupTestDirectory(String subfolder) {
        String classPath = getClass().getName().replace('.', '/');
        String location = "target/testrun/" + classPath + "/" + subfolder;
        File testDirectory = new File(location);

        // delete old tests
        if (testDirectory.exists()) {
            if (!FileUtil.delete(location, true)) {
                throw new CayenneRuntimeException(
                        "Error deleting test directory '%s'",
                        location);
            }
        }

        if (!testDirectory.mkdirs()) {
            throw new CayenneRuntimeException(
                    "Error creating test directory '%s'",
                    location);
        }

        return testDirectory;
    }
}
