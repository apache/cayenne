package org.apache.cayenne.configuration.server;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.dba.DbVersion;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.JdbcPkGenerator;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.JdbcEventLogger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

import static org.apache.cayenne.dba.DbVersion.MS_SQL_2008;
import static org.apache.cayenne.dba.DbVersion.MS_SQL_2012;

/**
 * A factory of PkGenerators that also loads user-provided pkGenerator or guesses
 * the pkGenerator from the database metadata.
 */
public class DefaultPkGeneratorFactory implements PkGeneratorFactory {

    @Inject
    protected JdbcEventLogger jdbcEventLogger;

    protected Map<String, Class> pkGenerators;

    public DefaultPkGeneratorFactory(@Inject(Constants.SERVER_PK_GENERATORS_MAP) Map<String, Class> pkGenerators) {
        this.pkGenerators = Objects.requireNonNull(pkGenerators, () -> "Null pkGenerators list");
    }

    @SuppressWarnings("unchecked")
    @Override
    public PkGenerator detectPkGenerator(DbVersion.DbType dbType, JdbcAdapter adapter, DatabaseMetaData md) throws SQLException {

        final int majorVersion = md.getDatabaseMajorVersion();
        final int minorVersion = md.getDatabaseMinorVersion();

        Class pkGeneratorClazz = null;
        DbVersion currentDbVersion = new DbVersion(dbType, majorVersion, minorVersion);
        switch (dbType) {
            case MS_SQL:
                pkGeneratorClazz = detectPkGenerator4MSSQL(currentDbVersion);
                break;
            default:
                jdbcEventLogger.log("Failed to detect PkGenerator, using generic generator");
                pkGeneratorClazz = defaultPkGenerator();
        }

        jdbcEventLogger.log("DB - '" + currentDbVersion + "'; PkGenerator - " + pkGeneratorClazz.getName());

        try {
            return newInstancePk(pkGeneratorClazz, adapter);
        } catch (Throwable throwable) {
            throw new ConfigurationException("Could not instantiate " + currentDbVersion, throwable);
        }
    }

    /**
     * @param pkGeneratorClazz {@link Class} for instantiation PkGenerator
     * @param adapter          adapter for installation in PkGenerator
     */
    protected PkGenerator newInstancePk(Class pkGeneratorClazz, JdbcAdapter adapter) throws Throwable {
        Constructor pkGeneratorConstructor = pkGeneratorClazz.getDeclaredConstructor(JdbcAdapter.class);
        pkGeneratorConstructor.setAccessible(true);

        MethodHandle createPkGenerator = MethodHandles.lookup()
                .unreflectConstructor(pkGeneratorConstructor);

        return (PkGenerator) createPkGenerator.invoke(adapter);
    }

    protected Class defaultPkGenerator() {
        return JdbcPkGenerator.class;
    }

    /**
     * Choosing a specific generator, depending on the version of the database
     *
     * @param currentDbVersion version of the database for which you need to determine the PkGenerator
     */
    protected Class detectPkGenerator4MSSQL(DbVersion currentDbVersion) {

        if (!isCheckTypeGenerator(currentDbVersion, MS_SQL_2008, MS_SQL_2012)) {
            return defaultPkGenerator();
        }

        if (currentDbVersion.compareTo(MS_SQL_2012) >= 0) {
            String version = String.valueOf(MS_SQL_2012);
            if (pkGenerators.containsKey(version)) {
                return pkGenerators.get(version);
            }
        } else {
            String version = String.valueOf(MS_SQL_2008);
            if (pkGenerators.containsKey(version)) {
                return pkGenerators.get(version);
            }
        }

        jdbcEventLogger.log("Failed to detect PkGenerator, using generic generator");
        return defaultPkGenerator();
    }

    private boolean isCheckTypeGenerator(DbVersion currentDbVersion, DbVersion... dbVersions) {
        for (DbVersion item : dbVersions) {
            if (!item.isTypeCheck(currentDbVersion)) {
                return false;
            }
        }
        return true;
    }
}
