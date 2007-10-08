package org.objectstyle.cayenne.project;

import java.io.File;

import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class ProjectConfigInfoTst extends CayenneTestCase {
    protected ProjectConfigInfo config;

    protected void setUp() throws Exception {
        super.setUp();
        config = new ProjectConfigInfo();
    }

    public void testSourceJar() throws Exception {
        File f = new File("xyz");
        config.setSourceJar(f);
        assertSame(f, config.getSourceJar());
    }

    public void testDestJar() throws Exception {
        File f = new File("xyz");
        config.setDestJar(f);
        assertSame(f, config.getDestJar());
    }

    public void testAltProjectFile() throws Exception {
        File f = new File("xyz");
        config.setAltProjectFile(f);
        assertSame(f, config.getAltProjectFile());
    }
}
