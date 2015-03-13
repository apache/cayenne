package de.jexp.jequel.generator.tables.single;

import de.jexp.jequel.table.BaseTable;
import de.jexp.jequel.table.Field;

/**
 * @author de.jexp.jequel.generator.JavaFileGenerationProcessor
 * @since Mi Okt 31 20:26:33 CET 2007
 *        ARTICLE_TEST
 */

public final class ARTICLE extends BaseTable<ARTICLE> {
    /**
     * PK: ARTICLE; ARTICLE_TEST
     */
    public Field OID = numeric().primaryKey();
    public Field NAME = string();
    /**
     * @deprecated
     */
    public Field ACTIVE = date();
    public Field ARTICLE_NO = integer();

    {
        initFields();
    }
}
