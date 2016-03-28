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

package org.apache.cayenne.map.template;

import java.io.Serializable;
import java.util.*;

/**
 * @since 4.0
 */
public class ClassGenerationDescriptor implements Serializable {
    protected ArtifactsGenerationMode artifactsGenerationMode;

    protected Map<String, ClassTemplate>  templates;

    public ClassGenerationDescriptor() {
        this.templates = new HashMap<>(6);
    }


    public ArtifactsGenerationMode getArtifactsGenerationMode() {
        return artifactsGenerationMode;
    }

    public Map<String, ClassTemplate> getTemplates() {
        return templates;
    }

    public void setTemplates(Map<String, ClassTemplate> templates) {
        this.templates = templates;
    }

    public void setArtifactsGenerationMode(String mode) {
        if (ArtifactsGenerationMode.ENTITY.getLabel().equalsIgnoreCase(mode)) {
            this.artifactsGenerationMode = ArtifactsGenerationMode.ENTITY;
        } else if (ArtifactsGenerationMode.DATAMAP.getLabel().equalsIgnoreCase(mode)) {
            this.artifactsGenerationMode = ArtifactsGenerationMode.DATAMAP;
        } else {
            this.artifactsGenerationMode = ArtifactsGenerationMode.ALL;
        }
    }
}
