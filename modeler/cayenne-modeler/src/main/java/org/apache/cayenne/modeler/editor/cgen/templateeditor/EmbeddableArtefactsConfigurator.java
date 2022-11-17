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

import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Embeddable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 5.0
 */
public class EmbeddableArtefactsConfigurator implements ArtefactsConfigurator {

    @Override
    public void config(ClassGenerationAction action, String artifactName) {
        action.addEmbeddables(Collections.singleton(getSelectedEntity(artifactName, action)));
    }

    public List<String> getArtifactsNames(DataMap dataMap) {
        if (dataMap != null) {
            return dataMap.getEmbeddables().stream()
                    .map(Embeddable::getClassName)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private Embeddable getSelectedEntity(String artifactName, ClassGenerationAction action) {
        DataMap dataMap = action.getCgenConfiguration().getDataMap();
        if (dataMap != null) {
            return dataMap.getEmbeddable(artifactName);
        }
        return null;
    }
}
