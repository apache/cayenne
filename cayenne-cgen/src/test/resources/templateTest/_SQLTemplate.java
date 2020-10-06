package test;

import test.auto._SQLTemplate;

public class SQLTemplate extends _SQLTemplate {

    private static SQLTemplate instance;

    private SQLTemplate() {}

    public static SQLTemplate getInstance() {
        if(instance == null) {
            instance = new SQLTemplate();
        }

        return instance;
    }
}
