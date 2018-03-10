package org.apache.cayenne.configuration.server;

import org.apache.cayenne.dba.DbVersion;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.PkGenerator;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

public interface PkGeneratorFactory {

    /**
     * Discovering the primary key based on the database metadata
     *
     * @param dbType  database type
     * @param adapter adapter for generator instantiation
     * @param md      connection metadata
     * @return an instantiated instance of a specific generator for the current version of the database
     * @throws SQLException
     */
    PkGenerator detectPkGenerator(DbVersion.DbType dbType, JdbcAdapter adapter, DatabaseMetaData md) throws SQLException;
}
