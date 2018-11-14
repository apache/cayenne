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
package org.apache.cayenne.gen.xml;

import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.project.extension.BaseSaverDelegate;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @since 4.1
 */
public class CgenSaverDelegate extends BaseSaverDelegate{

    private DataChannelMetaData metaData;

    CgenSaverDelegate(DataChannelMetaData metaData){
        this.metaData = metaData;
    }

    @Override
    public Void visitDataMap(DataMap dataMap) {
        CgenConfiguration cgen = metaData.get(dataMap, CgenConfiguration.class);
        if(cgen != null){
            resolveOutputDir(cgen);
            encoder.nested(cgen, getParentDelegate());
        }
        return null;
    }

    private void resolveOutputDir(CgenConfiguration cgenConfiguration) {
        if(cgenConfiguration.getRootPath() == null) {
            return;
        }
        Path prevPath = cgenConfiguration.buildPath();
        URL url = getBaseDirectory().getURL();
        if(url != null) {
            Path resourcePath = Paths.get(url.getPath());
            if(Files.isRegularFile(resourcePath)) {
                resourcePath = resourcePath.getParent();
            }
            cgenConfiguration.setRootPath(resourcePath);
            if(prevPath != null) {
                Path relPath = resourcePath.relativize(prevPath).normalize();
                cgenConfiguration.setRelPath(relPath);
                Path templatePath = Paths.get(cgenConfiguration.getTemplate());
                if(templatePath.isAbsolute()) {
                    cgenConfiguration.setTemplate(resourcePath.relativize(templatePath).normalize().toString());
                }
                Path superTemplatePath = Paths.get(cgenConfiguration.getSuperTemplate());
                if(superTemplatePath.isAbsolute()) {
                    cgenConfiguration.setSuperTemplate(resourcePath.relativize(superTemplatePath).normalize().toString());
                }
            }
        }
    }
}
