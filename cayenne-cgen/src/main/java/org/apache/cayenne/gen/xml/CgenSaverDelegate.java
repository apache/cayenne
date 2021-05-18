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
package org.apache.cayenne.gen.xml;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.project.extension.BaseSaverDelegate;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @since 4.1
 */
public class CgenSaverDelegate extends BaseSaverDelegate {

    private DataChannelMetaData metaData;

    CgenSaverDelegate(DataChannelMetaData metaData){
        this.metaData = metaData;
    }

    @Override
    public Void visitDataMap(DataMap dataMap) {
        CgenConfiguration cgen = metaData.get(dataMap, CgenConfiguration.class);
        if(cgen != null){
            resolveOutputDir(getBaseDirectory().getURL(), cgen);
            encoder.nested(cgen, getParentDelegate());
        }
        return null;
    }

    static void resolveOutputDir(URL baseURL, CgenConfiguration cgenConfiguration) {
        if(baseURL == null) {
            return;
        }

        Path resourcePath;
        try {
            resourcePath = Paths.get(baseURL.toURI());
        } catch (URISyntaxException e) {
            throw new CayenneRuntimeException("Unable to resolve output path", e);
        }
        if(Files.isRegularFile(resourcePath)) {
            resourcePath = resourcePath.getParent();
        }
        Path oldRoot = cgenConfiguration.getRootPath();
        if(oldRoot == null) {
            cgenConfiguration.setRootPath(resourcePath);
        }
        Path prevPath = cgenConfiguration.buildPath();
        if(prevPath != null) {
            if(prevPath.isAbsolute()) {

                Path relPath = prevPath;

                if (resourcePath.getRoot().equals(prevPath.getRoot())) {
                    relPath = resourcePath.relativize(prevPath).normalize();
                }
                cgenConfiguration.setRelPath(relPath);
            }

            Path templatePath = Paths.get(cgenConfiguration.getTemplate());
            if(templatePath.isAbsolute()) {
                cgenConfiguration.setTemplate(resourcePath.relativize(templatePath).normalize().toString());
            }
            Path superTemplatePath = Paths.get(cgenConfiguration.getSuperTemplate());
            if(superTemplatePath.isAbsolute()) {
                cgenConfiguration.setSuperTemplate(resourcePath.relativize(superTemplatePath).normalize().toString());
            }
        }
        cgenConfiguration.setRootPath(resourcePath);
    }
}
