package de.jexp.jequel.transform;

import de.jexp.jequel.util.FileUtils;

import java.io.File;

public class FileTransformer {
    private final boolean useSchema;

    public FileTransformer(final boolean useSchema) {
        this.useSchema = useSchema;
    }

    public FileTransformer() {
        this(false);
    }

    public void transformClass(final Class<?> javaClass, final Class schemaClass) {
        final File javaFile = FileUtils.getJavaFile(javaClass);
        transformFile(javaFile, schemaClass);
    }

    public void transformFile(final File javaFile, final Class schemaClass) {
        final LineTransformer lineTransformer = new LineTransformer(schemaClass, useSchema);
        final String newJavaSource = FileUtils.processFile(javaFile, new FileUtils.LineProcessor() {
            public String processLine(final String line) {
                if (line.startsWith("package")) {
                    return line + lineTransformer.getImports();
                }
                if (lineTransformer.ignoreLine(line)) return line;
                return lineTransformer.transformLine(line);
            }
        });
        final File newJavaFile = new File(javaFile.getParent(), javaFile.getName().replace(".java", ".jequel"));
        FileUtils.writeFile(newJavaFile, newJavaSource);
    }
}
