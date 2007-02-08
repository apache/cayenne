package org.objectstyle.petstore.dao;

import java.util.Map;

/**
 * A singleton storage of DAO's.
 * 
 * @author Andrus Adamchik
 */
public class DaoManager {

    static DaoManager manager;

    protected Map daosByType;

    public static DaoManager getManager() {
        return manager;
    }

    public static void setManager(DaoManager manager) {
        DaoManager.manager = manager;
    }

    public DaoManager(Map daosByType) {
        this.daosByType = daosByType;
    }

    /**
     * Returns a DAO instance registered for a given type.
     */
    public Object getDao(Class daoType) {
        Object dao = daosByType.get(daoType);

        if (dao == null) {
            throw new IllegalStateException("No DAO registered for " + daoType);
        }

        if (!(daoType.isAssignableFrom(dao.getClass()))) {
            throw new IllegalStateException("Invalid DAO type registered for " + daoType);
        }

        return dao;
    }
}
