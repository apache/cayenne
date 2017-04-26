package org.slf4j.impl;

import org.apache.cayenne.modeler.util.ModelerLogFactory;
import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

/*
*   StaticLoggerBinder bind and replace standard SLF4J LoggerFactory to ModelerLogFactory.
*   This is used because in Cayenne Modeler we use custom logger implementation (ModelerLogger)
*   for logging in System.err and modeler logger.
* */
public class StaticLoggerBinder implements LoggerFactoryBinder {

    private final static StaticLoggerBinder singleton = new StaticLoggerBinder();
    private final ILoggerFactory loggerFactory = new ModelerLogFactory();
    private static final String loggerFactoryClassStr = ModelerLogFactory.class.getName();

    public static StaticLoggerBinder getSingleton() {
        return singleton;
    }

    public ILoggerFactory getLoggerFactory() {
        return this.loggerFactory;
    }

    public String getLoggerFactoryClassStr() {
        return this.loggerFactoryClassStr;
    }
}
