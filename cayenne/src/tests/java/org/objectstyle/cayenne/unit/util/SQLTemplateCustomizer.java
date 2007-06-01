package org.objectstyle.cayenne.unit.util;

import java.util.Map;

import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.query.SQLTemplate;

/**
 * Helper class to customize SQLTemplate queries used in test cases
 * per adapter.
 * 
 * @author Andrei Adamchik
 */
public class SQLTemplateCustomizer {
    protected DbAdapter adapter;
    protected Map sqlMap;

    public SQLTemplateCustomizer(Map sqlMap) {
        this.sqlMap = sqlMap;
    }

    /**
     * Customizes SQLTemplate, injecting the template for the current adapter.
     */
    public void updateSQLTemplate(SQLTemplate query) {
        Map customSQL = (Map) sqlMap.get(query.getDefaultTemplate());
        if (customSQL != null) {
            String key = adapter.getClass().getName();
            String template = (String) customSQL.get(key);
            if (template != null) {
                query.setTemplate(key, template);
            }
        }
    }

    public SQLTemplate createSQLTemplate(
        Class root,
        String defaultTemplate,
        boolean selecting) {
        SQLTemplate template = new SQLTemplate(root, defaultTemplate, selecting);
        updateSQLTemplate(template);
        return template;
    }

    public DbAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(DbAdapter adapter) {
        this.adapter = adapter;
    }
}
