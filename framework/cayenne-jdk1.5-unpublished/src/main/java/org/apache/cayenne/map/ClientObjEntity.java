package org.apache.cayenne.map;

import java.util.Collection;
import java.util.Collections;

/**
 * A client version of ObjEntity that delegates some of its method calls to
 * its corresponding server entity.
 *
 * @since 3.0
 * @author Kevin Menard
 */
public class ClientObjEntity extends ObjEntity {

    private Collection<String> primaryKeyNames;

    public ClientObjEntity() {
        super();
    }

    public ClientObjEntity(final String name) {
        super(name);
    }

    @Override
    public Collection<String> getPrimaryKeyNames() {
        return Collections.unmodifiableCollection(primaryKeyNames);
    }

    public void setPrimaryKeyNames(final Collection<String> primaryKeyNames) {
        this.primaryKeyNames = primaryKeyNames;
    }
}
