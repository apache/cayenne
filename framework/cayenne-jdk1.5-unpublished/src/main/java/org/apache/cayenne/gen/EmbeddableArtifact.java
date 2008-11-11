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
package org.apache.cayenne.gen;

import org.apache.cayenne.map.Embeddable;
import org.apache.velocity.VelocityContext;

/**
 * {@link Artifact} facade for an {@link Embeddable}.
 * 
 * @since 3.0
 */
public class EmbeddableArtifact implements Artifact {

    protected Embeddable embeddable;

    public EmbeddableArtifact(Embeddable embeddable) {
        this.embeddable = embeddable;
    }

    public Object getObject() {
        return embeddable;
    }

    public String getQualifiedBaseClassName() {
        return Object.class.getName();
    }

    public String getQualifiedClassName() {
        return embeddable.getClassName();
    }

    public TemplateType[] getTemplateTypes(ArtifactGenerationMode mode) {
        switch (mode) {
            case SINGLE_CLASS:
                return new TemplateType[] {
                    TemplateType.EMBEDDABLE_SINGLE_CLASS
                };
            case GENERATION_GAP:
                return new TemplateType[] {
                        TemplateType.EMBEDDABLE_SUPERCLASS,
                        TemplateType.EMBEDDABLE_SUBCLASS
                };
            default:
                return new TemplateType[0];
        }
    }

    public void postInitContext(VelocityContext context) {
        // noop - no special keys...
    }
}
