package de.jexp.jequel.transform;

/**
 * @author mh14 @ jexp.de
 * @since 12.11.2007 22:54:15 (c) 2007 jexp.de
 */
public class TransformTestFile {
    public String getSqlString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("select OID from ARTICLE ");
        sb.append(" where article.oid is not null");
        sb.append(" and article.article_no = 12345");
        sb.append(" order by article.article_no ");
        return sb.toString();
    }
}
