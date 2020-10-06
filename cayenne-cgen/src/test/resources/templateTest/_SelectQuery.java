package test;

import test.auto._SelectQuery;

public class SelectQuery extends _SelectQuery {

    private static SelectQuery instance;

    private SelectQuery() {}

    public static SelectQuery getInstance() {
        if(instance == null) {
            instance = new SelectQuery();
        }

        return instance;
    }
}
