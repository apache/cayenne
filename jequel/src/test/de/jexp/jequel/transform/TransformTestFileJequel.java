package de.jexp.jequel.transform;

import static de.jexp.jequel.sql.Sql.*;
import static de.jexp.jequel.tables.TEST_TABLES.*;

/**
 * @author mh14 @ jexp.de
 * @since 12.11.2007 22:54:15 (c) 2007 jexp.de
 */
public class TransformTestFileJequel {
    public String getSqlString() {
        return
                Select(ARTICLE.OID).from(ARTICLE
                ).where(ARTICLE.OID.isNot(null)
                        .and(ARTICLE.ARTICLE_NO.eq(12345)
                )).orderBy(ARTICLE.ARTICLE_NO)
                        .toString();
    }
}