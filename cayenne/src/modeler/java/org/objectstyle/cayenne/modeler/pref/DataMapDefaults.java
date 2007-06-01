package org.objectstyle.cayenne.modeler.pref;

import org.objectstyle.cayenne.PersistenceState;

public class DataMapDefaults extends _DataMapDefaults {

    public void setPersistenceState(int persistenceState) {

        // init defaults on insert...
        if (this.persistenceState == PersistenceState.TRANSIENT
                && persistenceState == PersistenceState.NEW) {
            setGeneratePairs(Boolean.TRUE);
            setSuperclassPackageSuffix("auto");
        }
        super.setPersistenceState(persistenceState);
    }

    public String getSuperclassPackage() {
        return getSuperclassPackageSuffix();
    }

    public void setSuperclassPackage(String superclassPackage) {
        // TODO: rename "SuperclassPackageSuffix" to "SuperclassPackage"
        setSuperclassPackageSuffix(superclassPackage);
    }

    /**
     * Sets superclass package, building it by "normalizing" and concatenating prefix and
     * suffix.
     */
    public void setSuperclassPackage(String prefix, String suffix) {
        if (prefix == null) {
            prefix = "";
        }
        else if (prefix.endsWith(".")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }

        if (suffix == null) {
            suffix = "";
        }
        else if (suffix.startsWith(".")) {
            suffix = suffix.substring(1);
        }

        String dot = (suffix.length() > 0 && prefix.length() > 0) ? "." : "";
        setSuperclassPackage(prefix + dot + suffix);
    }
}

