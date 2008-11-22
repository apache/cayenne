package org.apache.art;

import org.apache.art.auto._Testmap;

public class Testmap extends _Testmap {

    private static Testmap instance;

    private Testmap() {}

    public static Testmap getInstance() {
        if(instance == null) {
            instance = new Testmap();
        }

        return instance;
    }
}
