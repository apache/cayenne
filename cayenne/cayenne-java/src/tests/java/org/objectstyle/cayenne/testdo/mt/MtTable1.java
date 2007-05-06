package org.objectstyle.cayenne.testdo.mt;

import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.testdo.mt.auto._MtTable1;

public class MtTable1 extends _MtTable1 {

    protected ObjEntity entity;

    public ObjEntity getObjEntity() {
        return (entity != null) ? entity : super.getObjEntity();
    }
    
    public void setObjEntity(ObjEntity entity) {
        this.entity = entity;
    }
}
