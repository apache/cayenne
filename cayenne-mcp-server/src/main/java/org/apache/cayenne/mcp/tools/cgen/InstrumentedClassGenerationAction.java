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
package org.apache.cayenne.mcp.tools.cgen;

import org.apache.cayenne.gen.Artifact;
import org.apache.cayenne.gen.ArtifactGenerationMode;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.ClassGenerationAction;
import org.apache.cayenne.gen.StringUtils;
import org.apache.cayenne.gen.TemplateType;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.mcp.tools.cgen.protocol.CgenFileEntry;
import org.apache.cayenne.mcp.tools.cgen.protocol.CgenFileKind;

import java.io.File;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Extends {@link ClassGenerationAction} to record which files cgen wrote and how many
 * were considered. Placed in {@code org.apache.cayenne.gen} so it shares the same Java
 * package as {@link ClassGenerationAction} and can read its protected fields.
 *
 * @since 5.0
 */
class InstrumentedClassGenerationAction extends ClassGenerationAction {

    private final List<CgenFileEntry> writtenFiles;
    private Artifact currentArtifact;

    public InstrumentedClassGenerationAction(CgenConfiguration cgenConfig) {
        super(cgenConfig);
        this.writtenFiles = new ArrayList<>();
    }

    @Override
    protected void execute(Artifact artifact) throws Exception {
        currentArtifact = artifact;
        try {
            super.execute(artifact);
        } finally {
            currentArtifact = null;
        }
    }

    @Override
    protected Writer openWriter(TemplateType templateType) throws Exception {
        Writer w = super.openWriter(templateType);
        if (w != null && currentArtifact != null) {
            writtenFiles.add(buildFileEntry(currentArtifact, templateType));
        }
        return w;
    }

    public List<CgenFileEntry> getWrittenFiles() {
        return List.copyOf(writtenFiles);
    }

    /**
     * Counts the total number of files cgen will evaluate for this DataMap.
     * Must be called after {@link #prepareArtifacts()}.
     */
    public int countFilesConsidered() {
        ArtifactGenerationMode mode = cgenConfiguration.isMakePairs()
                ? ArtifactGenerationMode.GENERATION_GAP
                : ArtifactGenerationMode.SINGLE_CLASS;
        int count = 0;
        for (Artifact artifact : cgenConfiguration.getArtifacts()) {
            count += artifact.getTemplateTypes(mode).length;
        }
        return count;
    }

    private CgenFileEntry buildFileEntry(Artifact artifact, TemplateType templateType) {
        String packageName;
        String className;
        if (templateType.isSuperclass()) {
            packageName = (String) context.get(Artifact.SUPER_PACKAGE_KEY);
            className = (String) context.get(Artifact.SUPER_CLASS_KEY);
        } else {
            packageName = (String) context.get(Artifact.SUB_PACKAGE_KEY);
            className = (String) context.get(Artifact.SUB_CLASS_KEY);
        }

        String filename = StringUtils.getInstance().replaceWildcardInStringWithString(
                "*", cgenConfiguration.getOutputPattern(), className);
        Path dir = cgenConfiguration.buildOutputPath();
        if (cgenConfiguration.isUsePkgPath() && packageName != null) {
            dir = dir.resolve(packageName.replace('.', File.separatorChar));
        }
        Path filePath = dir.resolve(filename).toAbsolutePath();

        return new CgenFileEntry(filePath.toString(), toKind(templateType), sourceName(artifact));
    }

    private static CgenFileKind toKind(TemplateType type) {
        return switch (type) {
            case ENTITY_SUPERCLASS -> CgenFileKind.entity_super;
            case ENTITY_SUBCLASS, ENTITY_SINGLE_CLASS -> CgenFileKind.entity_sub;
            case EMBEDDABLE_SUPERCLASS -> CgenFileKind.embeddable_super;
            case EMBEDDABLE_SUBCLASS, EMBEDDABLE_SINGLE_CLASS -> CgenFileKind.embeddable_sub;
            case DATAMAP_SUPERCLASS, DATAMAP_SUBCLASS, DATAMAP_SINGLE_CLASS -> CgenFileKind.datamap;
        };
    }

    private static String sourceName(Artifact artifact) {
        Object obj = artifact.getObject();
        return switch (obj) {
            case ObjEntity entity -> entity.getName();
            case Embeddable embeddable -> {
                String fq = embeddable.getClassName();
                int dot = fq.lastIndexOf('.');
                yield dot >= 0 ? fq.substring(dot + 1) : fq;
            }
            default -> {
                String fq = artifact.getQualifiedBaseClassName();
                int dot = fq.lastIndexOf('.');
                yield dot >= 0 ? fq.substring(dot + 1) : fq;
            }
        };
    }
}
