package org.apache.cayenne.testdo.mt;

import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.testdo.mt.auto._MtTable1;

public class MtTable1 extends _MtTable1 {

    protected ObjEntity entity;

    public ObjEntity getObjEntity() {
        return (entity != null) ? entity : super.getObjEntity();
    }
    
    public void setObjEntity(ObjEntity entity) {
        this.entity = entity;
    }
}
