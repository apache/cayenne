/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.asciidoc;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Map;

import org.asciidoctor.Options;
import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.Postprocessor;
import org.jsoup.Jsoup;

/**
 * <p>
 * AsciidoctorJ post processor, that extracts ToC into separate file and optionally can inject content into rendered document.
 * Can be used only for HTML backend, will <b>fail</b> if used with PDF.
 * <p>
 * It is targeted to inject "front-matter" section suitable for cayenne website tools.
 * <p>
 * Extension controlled by attributes in *.adoc file:
 * <ul>
 *     <li>cayenne-header: header file name or constant "front-matter" that will inject empty front matter markup
 *     <li>cayenne-header-position [optional]: "top" to inject just above all content or "body" to inject right after &gt;body&lt; tag
 *     <li>cayenne-footer: footer file name or constant "front-matter" that will inject empty front matter markup
 *     <li>cayenne-footer-position [optional]: "bottom" to inject just after all content or "body" to inject right before &gt;/body&lt; tag
 * </ul>
 *
 * @since 4.1
 * @deprecated in favour of io.bootique.tools.asciidoctorj.HugoExtension
 */
@Deprecated(since = "5.0", forRemoval = true)
public class CayennePostProcessor extends Postprocessor {

    private static final String FRONT_MATTER = "front-matter";
    private static final String EMPTY_FRONT_MATTER = "---\n---\n\n";
    private static final String POSITION_TOP = "top";
    private static final String POSITION_BODY = "body";
    private static final String POSITION_BOTTOM = "bottom";

    @SuppressWarnings("unused")
    public CayennePostProcessor() {
        super();
    }

    @SuppressWarnings("unused")
    public CayennePostProcessor(Map<String, Object> config) {
        super(config);
    }

    public String process(Document document, String output) {
        output = extractTableOfContents(document, output);
        output = fixupDom(document, output);
        output = processHeader(document, output);
        output = processFooter(document, output);
        return output;
    }

    private String fixupDom(Document document, String output) {
        org.jsoup.nodes.Document jsoupDoc = Jsoup.parseBodyFragment(output);

        jsoupDoc.select(".icon-note")
                .removeClass("icon-note")
                .addClass("fa-info-circle")
                .addClass("fa-2x");

        jsoupDoc.select(".icon-tip")
                .removeClass("icon-tip")
                .addClass("fa-lightbulb-o")
                .addClass("fa-2x");

        jsoupDoc.select("code").forEach(el -> {
            String codeClass = el.attr("data-lang");
            if(!codeClass.isEmpty()) {
                el.addClass(codeClass);
            }
        });

        jsoupDoc.select("div#preamble").remove();

        return jsoupDoc.body().html();
    }

    protected String extractTableOfContents(Document document, String output) {
        int start = output.indexOf("<div id=\"toc\" class=\"toc\">");
        if(start == -1) {
            // no toc found, exit
            return output;
        }

        String tocEndString = "</ul>\n</div>";
        int end = output.indexOf(tocEndString, start);
        if(end == -1) {
            // bad, no end..
            return output;
        }

        end += tocEndString.length() + 1;

        org.jsoup.nodes.Document tocDoc = Jsoup.parseBodyFragment(output.substring(start, end));
        tocDoc.select("ul").addClass("nav");
        tocDoc.select("a").addClass("nav-link");
        tocDoc.select("div#toc").addClass("toc-side");
        String toc = tocDoc.body().html();

        Object destDir = document.getOptions().get(Options.TO_DIR);
        Object docname = ((Map)document.getOptions().get(Options.ATTRIBUTES)).get("docname");

        Path path = FileSystems.getDefault().getPath((String) destDir, docname + ".toc.html");
        StandardOpenOption[] options = {
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
        };
        try(BufferedWriter br = Files.newBufferedWriter(path, options)) {
            br.write(toc, 0, toc.length());
            br.flush();
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }

        if(start == 0) {
            return output.substring(end);
        }

        return output.substring(0, start) + output.substring(end);
    }

    protected String processHeader(Document document, String output) {
        String headerFile = (String) document.getAttribute("cayenne-header", "");
        String headerPosition = (String)document.getAttribute("cayenne-header-position", POSITION_TOP);

        if(headerFile.isEmpty()) {
            return output;
        }

        String header;
        // inject empty front matter
        if(FRONT_MATTER.equals(headerFile.trim())) {
            header = EMPTY_FRONT_MATTER ;
        } else {
            // treat as a file
            header = document.readAsset(headerFile, Collections.emptyMap());
        }

        switch (headerPosition.trim()) {
            case POSITION_BODY: {
                int bodyStart = output.indexOf("<div id=\"header\">");
                if(bodyStart == -1) {
                    // no header
                    return header + output;
                }
                return output.substring(0, bodyStart) + header + output.substring(bodyStart);
            }

            case POSITION_TOP:
            default:
                return header + output;
        }
    }

    protected String processFooter(Document document, String output) {
        String footerFile = (String) document.getAttribute("cayenne-footer", "");
        String footerPosition = (String)document.getAttribute("cayenne-footer-position", POSITION_BOTTOM);

        if(footerFile.isEmpty()) {
            return output;
        }

        String footer = document.readAsset(footerFile, Collections.emptyMap());

        switch (footerPosition.trim()) {
            case POSITION_BODY: {
                int bodyStart = output.indexOf("</body>");
                if(bodyStart == -1) {
                    // no footer
                    return output + footer;
                }
                return output.substring(0, bodyStart) + footer + output.substring(bodyStart);
            }

            case POSITION_BOTTOM:
            default:
                return output + footer;
        }
    }
}
