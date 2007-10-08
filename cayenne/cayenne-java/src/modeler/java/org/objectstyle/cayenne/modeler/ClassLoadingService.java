package org.objectstyle.cayenne.modeler;

/**
 * An interface defining a service for locating external Java resources.
 * 
 * @author Andrei Adamchik
 */
public interface ClassLoadingService {

    /**
     * Returns Java ClassLoader that knows how to load all classes configured for this
     * service.
     */
    public ClassLoader getClassLoader();

    /**
     * Returns a class for given class name.
     */
    public Class loadClass(String className) throws ClassNotFoundException;
}