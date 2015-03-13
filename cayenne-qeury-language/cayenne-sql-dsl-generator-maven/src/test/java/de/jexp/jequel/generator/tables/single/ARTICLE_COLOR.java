package de.jexp.jequel.generator.tables.single;

import de.jexp.jequel.table.BaseTable;
import de.jexp.jequel.table.Field;

/**
 * @author de.jexp.jequel.generator.JavaFileGenerationProcessor
 * @since Mi Okt 31 20:26:33 CET 2007
 *        null
 */

public final class ARTICLE_COLOR extends BaseTable<ARTICLE_COLOR> {
    /**
     * PK: ARTICLE_COLOR
     */
    public Field OID = numeric().primaryKey();
    public Field ARTICLE_COLOR_NO = string();
    /**
     * FK: ARTICLE; ARTICLE_TEST
     */
    public Field ARTICLE_OID = foreignKey(GEN_TEST_TABLES.ARTICLE.OID);

    {
        initFields();
    }
}
