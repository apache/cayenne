package org.apache.cayenne.modeler.project;

import org.apache.cayenne.gen.internal.Utils;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.resource.Resource;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CgenOps {

    // TODO: "cayenne.cgen.destdir" is a Maven Modeler startup artifact. Should go away with CAY-2925 (or should be
    // reimplemented as an app arg).

    public static Path baseDir(Application application) {
        String propDir = System.getProperty("cayenne.cgen.destdir");
        if (propDir != null) {
            return Paths.get(propDir);
        }

        Path projectRoot = projectRoot(application);
        if (projectRoot == null) {
            return Paths.get(".");
        }

        return Utils.getMavenSrcPathForPath(projectRoot).map(Paths::get).orElse(projectRoot);
    }

    private static Path projectRoot(Application application) {
        Project project = application.getProject();
        if (project == null) {
            return null;
        }
        Resource resource = project.getConfigurationResource();
        if (resource == null) {
            return null;
        }
        try {
            Path path = Path.of(resource.getURL().toURI());
            return Files.isRegularFile(path) ? path.getParent() : path;
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
