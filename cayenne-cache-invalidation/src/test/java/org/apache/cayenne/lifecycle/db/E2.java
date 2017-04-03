package org.apache.cayenne.lifecycle.db;

import org.apache.cayenne.lifecycle.cache.CacheGroup;
import org.apache.cayenne.lifecycle.cache.CacheGroups;
import org.apache.cayenne.lifecycle.db.auto._E2;


@CacheGroups(
        value = {"g1", "g2"},
        groups = {
            @CacheGroup("g3"),
            @CacheGroup(value = "g4", keyType = String.class, valueType = Object.class),
            @CacheGroup(value = "g5", keyType = Integer.class, valueType = Object.class),
        }
)
@CacheGroup("g6")
public class E2 extends _E2 {

    private static final long serialVersionUID = 1L; 

}
