package org.apache.cayenne.reflect;

import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DbEntity;

public class AdditionalDbEntityDescriptor
{
    private CayennePath path;
    private DbEntity entity;
    private boolean noDelete;

    AdditionalDbEntityDescriptor(CayennePath path, DbEntity entity, boolean noDelete) {
        this.noDelete = noDelete;
        this.entity = entity;
        this.path = path;
    }

    public DbEntity getDbEntity() {
        return entity;
    }

    public CayennePath getPath() {
        return path;
    }

    public boolean noDelete() {
        return noDelete;
    }
}
