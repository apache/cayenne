package org.apache.cayenne.configuration.xml;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ProjectVersionTest {

    @Test
    public void decodeVersion() {
        assertEquals(1.2340, ProjectVersion.fromString("1.2.3.4").getAsDouble(), 0.000001);
        assertEquals(1.0004, ProjectVersion.fromString("1.0.0.0.4").getAsDouble(), 0.000001);
        assertEquals(10.0, ProjectVersion.fromString("10").getAsDouble(), 0.000001);
    }
    
    @Test
    public void knownVersions() {
        assertEquals(ProjectVersion.V7, ProjectVersion.fromString("7"));
        assertEquals(ProjectVersion.V10, ProjectVersion.fromString("10"));
        assertEquals(ProjectVersion.V11, ProjectVersion.fromString("11"));
    }
    
    @Test
    public void comparison() {
        assertEquals(0, ProjectVersion.V10.compareTo(ProjectVersion.V10));
        assertEquals(-1, ProjectVersion.V7.compareTo(ProjectVersion.V10));
        assertEquals(1, ProjectVersion.V11.compareTo(ProjectVersion.V9));
    }
}
