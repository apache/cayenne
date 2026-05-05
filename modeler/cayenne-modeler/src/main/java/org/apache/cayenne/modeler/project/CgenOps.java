package org.apache.cayenne.modeler.project;

import org.apache.cayenne.gen.internal.Utils;
import org.apache.cayenne.project.Project;
import org.apache.cayenne.resource.Resource;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CgenOps {

    public static Path baseDir(ProjectSession session) {
        Path projectRoot = projectRoot(session);
        if (projectRoot == null) {
            return Paths.get(".");
        }

        return Utils.getMavenSrcPathForPath(projectRoot).map(Paths::get).orElse(projectRoot);
    }

    private static Path projectRoot(ProjectSession session) {
        Project project = session.project();
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
