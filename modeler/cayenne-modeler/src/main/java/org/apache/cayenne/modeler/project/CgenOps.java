package org.apache.cayenne.modeler.project;

import org.apache.cayenne.gen.internal.Utils;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.pref.FSPath;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CgenOps {

    // TODO:
    //  1. "cayenne.cgen.destdir" is a Maven Modeler startup artifact. Should go away with CAY-2925 (or should be
    //  reimplemented as an app arg).
    //  2. "getLastDirectory()" is likely the wrong directory for the base

    public static Path baseDir(Application application) {
        String propDir = System.getProperty("cayenne.cgen.destdir");
        if (propDir != null) {
            return Paths.get(propDir);
        }

        FSPath lastPath = application.getFrameController().getLastDirectory();
        File lastDir = lastPath.getExistingDirectory(false);

        return Utils.getMavenSrcPathForPath(lastPath.getPath()).map(Paths::get)
                .orElseGet(() -> Paths.get(lastDir != null ? lastDir.getAbsolutePath() : "."));
    }
}
