package org.apache.cayenne.dba;

import java.util.Objects;

/**
 * Description of the database version and its type
 */
public class DbVersion implements Comparable<DbVersion> {

    /**
     * DbVersion: majorVersion = 11, minorVersion = 0
     */
    public static final DbVersion MS_SQL_2012;

    /**
     * DbVersion: majorVersion = 10, minorVersion = 0
     */
    public static final DbVersion MS_SQL_2008;

    static {
        MS_SQL_2012 = new DbVersion(DbType.MS_SQL, 11, 0);
        MS_SQL_2008 = new DbVersion(DbType.MS_SQL, 10, 0);
    }

    private final DbType dbType;
    private final int majorVersion;
    private final int minorVersion;

    public DbVersion(DbType dbType, int majorVersion, int minorVersion) {
        this.dbType = dbType;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof DbVersion)) {
            return false;
        }

        DbVersion dbVersion = (DbVersion) obj;

        return dbVersion.majorVersion == this.majorVersion &&
                dbVersion.minorVersion == this.minorVersion;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.majorVersion, this.minorVersion);
    }

    @Override
    public String toString() {
        return "DbVersion: [dbType: '" + this.dbType.getType() +
                "', majorVersion:'" + this.majorVersion +
                "', minorVersion:'" + this.minorVersion + "']";
    }

    /**
     * @return {@code true} if the types of equivalents,
     * {@code false} otherwise
     */
    public boolean isTypeCheck(DbVersion o) {
        return this.dbType == o.dbType;
    }

    /**
     * Compare by version, type is not taken into account.
     * Before calling this method, do a check:
     * {@link DbVersion#isTypeCheck(org.apache.cayenne.dba.DbVersion)}
     */
    @Override
    public int compareTo(DbVersion o) {
        if (o.equals(this)) {
            return 0;
        }
        if (this.majorVersion == o.minorVersion) {
            return this.minorVersion - o.minorVersion;
        }
        return this.majorVersion - o.majorVersion;
    }

    public enum DbType {

        MS_SQL("MICROSOFT SQL SERVER");

        private final String type;

        DbType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }
}
