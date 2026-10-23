package test;

import test.auto._SelectQueryResultTypes;

public class SelectQueryResultTypes extends _SelectQueryResultTypes {

    private static SelectQueryResultTypes instance;

    private SelectQueryResultTypes() {}

    public static SelectQueryResultTypes getInstance() {
        if(instance == null) {
            instance = new SelectQueryResultTypes();
        }

        return instance;
    }
}
