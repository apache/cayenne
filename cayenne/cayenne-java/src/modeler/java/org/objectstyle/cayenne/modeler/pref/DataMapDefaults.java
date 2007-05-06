package org.objectstyle.cayenne.modeler.pref;

import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.util.Util;

public class DataMapDefaults extends _DataMapDefaults {

    public static final String DEFAULT_SUPERCLASS_PACKAGE_SUFFIX = "auto";

    /**
     * Returns a superlcass package tailored for a given DataMap.
     */
    public void updateSuperclassPackage(DataMap dataMap, boolean isClient) {

        String storedPackage = super.getSuperclassPackage();
        if (Util.isEmptyString(storedPackage)
                || DEFAULT_SUPERCLASS_PACKAGE_SUFFIX.equals(storedPackage)) {
            String mapPackage = (isClient) ? dataMap.getDefaultClientPackage() : dataMap
                    .getDefaultPackage();
            if (!Util.isEmptyString(mapPackage)) {

                if (mapPackage.endsWith(".")) {
                    mapPackage = mapPackage.substring(mapPackage.length() - 1);
                }

                if (!Util.isEmptyString(mapPackage)) {
                    String newPackage = mapPackage
                            + "."
                            + DEFAULT_SUPERCLASS_PACKAGE_SUFFIX;
                    if (!Util.nullSafeEquals(newPackage, storedPackage)) {
                        setSuperclassPackage(newPackage);
                    }
                }
            }
        }

        if (DEFAULT_SUPERCLASS_PACKAGE_SUFFIX.equals(getSuperclassPackage())) {
            setSuperclassPackage(null);
        }
    }

    public void setPersistenceState(int persistenceState) {

        // init defaults on insert...
        if (this.persistenceState == PersistenceState.TRANSIENT
                && persistenceState == PersistenceState.NEW) {
            setGeneratePairs(Boolean.TRUE);
        }
        super.setPersistenceState(persistenceState);
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
