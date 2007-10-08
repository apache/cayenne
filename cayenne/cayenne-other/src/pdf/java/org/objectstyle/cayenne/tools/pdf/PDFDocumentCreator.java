/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 *
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.tools.pdf;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;
import java.util.StringTokenizer;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.lowagie.text.Anchor;
import com.lowagie.text.Chapter;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.ListItem;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Section;
import com.lowagie.text.pdf.PdfAction;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfDestination;
import com.lowagie.text.pdf.PdfOutline;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPageLabels;
import com.lowagie.text.pdf.PdfWriter;

/**
 * A XML to PDF converter.
 * 
 * @author Anton Sakalouski
 */
class PDFDocumentCreator {

    static final String CAYENNE_LOGO = "xdocs/pdf-images/3dpepper.jpg";
    static final String PANEL_ICON = "xdocs/pdf-images/panel.gif";

    ArrayList contentTable;

    PDFDocumentCreator() {
        contentTable = new ArrayList();
    }

    /**
     * actually most significant method. At first save names of section and subsection in
     * ArrayList, render output pdf to memory. Then create completed document and save it
     * in file.
     * 
     * @param source - input xml source
     * @param resultStream - output pdf destination
     */
    public void run(InputStream source, OutputStream resultStream) throws Exception {

        //At first invoke parsing document to memory.
        //It necessary for building table of content. Page numbers and titles of
        //chapter are saved in collection.
        com.lowagie.text.Document noopDoc = new com.lowagie.text.Document();
        ByteArrayOutputStream noopStream = new ByteArrayOutputStream();
        PdfWriter noopWriter = PdfWriter.getInstance(noopDoc, noopStream);
        XMLReader noopXmlReader = XMLReaderFactory.createXMLReader();
        noopXmlReader.setContentHandler(new ContentCreator(noopDoc, noopWriter));
        noopXmlReader.parse(new InputSource(source));
        source.reset();

        //Parse document, and save results in pdf.
        com.lowagie.text.Document pdfDoc = new com.lowagie.text.Document();
        PdfWriter writer = PdfWriter.getInstance(pdfDoc, resultStream);
        XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        xmlReader.setContentHandler(new ContentCreator(pdfDoc, writer));
        xmlReader.parse(new InputSource(source));
    }

    /**
     * Represents the table of content item
     */
    class ContentItem {

        String name;
        int pageNumber;
        String type;

        ContentItem(String name, int pageNumber, String type) {
            this.name = name;
            this.pageNumber = pageNumber;
            this.type = type;
        }
    }

    /**
     * SAX - handler which does essential work for each tag.
     */
    class ContentCreator extends DefaultHandler implements PDFFonts {

        private com.lowagie.text.Document pdfDoc;
        private PdfWriter writer;

        private Stack elements; //elements which can be nested one to another
        private Stack tags;
        private Stack fonts;

        private Chapter currentChapter = null;
        private Section currentSection = null;
        private Section lastSection = null;
        private Section[] sectionArray = null;
        private String currentChapterName = null;
        private String panelName = null;
        private String[] currentLink = null;
        private String source = null;
        private String src = "";

        PdfPTable pdfTable = null;
        ArrayList pdfCells;
        int cellNumber = 0;
        ArrayList cellsWidth = null;

        com.lowagie.text.List list = null;
        com.lowagie.text.pdf.PdfPTable table = null;

        private int deep;
        boolean regularPageNumber = false;

        ContentCreator(com.lowagie.text.Document pdfDoc, PdfWriter writer) {
            this.pdfDoc = pdfDoc;
            this.writer = writer;
            elements = new Stack();
            tags = new Stack();
            fonts = new Stack();
            fonts.push(PDFFonts.regularFont);
        }

        public void startElement(String uri, String local, String qName, Attributes atts)
                throws SAXException {
            try {
                if ("document".equals(qName)) {
                    //first page. Include title of document, emblem and copyright.
                    String title = atts.getValue("title");
                    writer.setViewerPreferences(PdfWriter.PageModeUseThumbs);
                    pdfDoc.open();
                    pdfDoc.setPageSize(PageSize.A4);
                    com.lowagie.text.Image image = com.lowagie.text.Image
                            .getInstance(CAYENNE_LOGO);
                    image.setAlignment(Element.ALIGN_CENTER);

                    PdfPageLabels pageLabels = new PdfPageLabels();
                    pageLabels.addPageLabel(1, PdfPageLabels.LOWERCASE_ROMAN_NUMERALS);
                    writer.setPageLabels(pageLabels);

                    Paragraph sp = new Paragraph("");
                    sp.setSpacingAfter(70f);
                    pdfDoc.add(sp);
                    pdfDoc.add(image);
                    Paragraph tp = new Paragraph(title, titleFont);
                    tp.setAlignment(Element.ALIGN_CENTER);
                    tp.setSpacingAfter(100f);
                    pdfDoc.add(tp);
                    Paragraph spr = new Paragraph("");
                    spr.setSpacingAfter(100f);
                    pdfDoc.add(spr);
                    Paragraph cp = new Paragraph(
                            "Copyright ©2001-2005 ObjectStyle Group",
                            copyrightFont);
                    cp.setSpacingBefore(70f);
                    cp.setAlignment(Element.ALIGN_CENTER);
                    pdfDoc.add(cp);
                    writer.setPageEvent(new PageNumberBuilder());
                    pdfDoc.newPage();

                    //The beginner page of content table - title.
                    pdfDoc.add(new Paragraph("Contents", sectionFont));
                    PdfContentByte cb = writer.getDirectContent();
                    PdfOutline root = cb.getRootOutline();
                    new PdfOutline(root, PdfAction.gotoLocalPage(2, new PdfDestination(
                            PdfDestination.FITH,
                            1000), writer), "Contents");
                    //One link for each section or susection.
                    for (Iterator it = contentTable.iterator(); it.hasNext();) {
                        ContentItem item = (ContentItem) it.next();
                        Chunk ch;
                        if ("section".equals(item.type)) {
                            String name = item.name + " ";
                            ch = new Chunk(name, boldFont); //If link to section, bold
                            // font is used
                            while (ch.getWidthPoint() < 500) {
                                ch.append(".");
                            }
                            ch.append("" + item.pageNumber);
                        }
                        else {
                            String name = "           " + item.name + " ";
                            ch = new Chunk(name, regularFont);
                            while (ch.getWidthPoint() < 500) { //If link to subsection,
                                // regular font and
                                ch.append("."); //intend are used.
                            }
                            ch.append("" + item.pageNumber);
                        }
                        ch.setLocalGoto(item.name);
                        Paragraph pr = new Paragraph();
                        pr.add(ch);
                        pdfDoc.add(pr);
                    }
                    int number = writer.getPageNumber();
                    pageLabels.addPageLabel(number + 1,
                            PdfPageLabels.DECIMAL_ARABIC_NUMERALS);
                    regularPageNumber = true;
                    pdfDoc.resetPageCount();
                    pdfDoc.newPage();
                    cb = writer.getDirectContent();
                    root = cb.getRootOutline();
                    new PdfOutline(root, PdfAction.gotoLocalPage(number + 1,
                            new PdfDestination(PdfDestination.FITH, 1000),
                            writer), "Preface");

                }
                if ("section".equals(qName)) {
                    String sectionName = atts.getValue("name");
                    currentChapterName = sectionName;
                    int indx = sectionName.indexOf(" ");
                    int chapterNumber = 0;

                    try {
                        chapterNumber = Integer.parseInt(sectionName.substring(0,
                                indx - 1));
                    }
                    catch (NumberFormatException infe) {
                        if (!contentTable.isEmpty()) {
                            Chunk anchor = new Chunk(
                                    sectionName.trim(),
                                    PDFFonts.invisibleFont);
                            anchor.setLocalDestination(currentChapterName);
                            pdfDoc.add(anchor);
                        }
                        Paragraph preface = new Paragraph(currentChapterName, sectionFont);
                        pdfDoc.add(preface);
                        contentTable.add(new ContentItem(currentChapterName, writer
                                .getPageNumber(), "section"));
                        return;
                    }
                    //Set the anchor to this chapter with invisible font.
                    //Used in table of content above.
                    Paragraph chapterTitle = new Paragraph(
                            sectionName.substring(indx),
                            sectionFont);
                    Chapter chapter = new Chapter(chapterTitle, chapterNumber);
                    chapter.setIndentation(1f);
                    ArrayList chunks = chapter.getBookmarkTitle().getChunks();
                    String name = "";
                    for (Iterator it = chunks.iterator(); it.hasNext();)
                        name += ((Chunk) it.next()).content();

                    Chunk anchor = new Chunk(name.trim(), PDFFonts.invisibleFont);
                    anchor.setLocalDestination(name.trim());
                    anchor.setHorizontalScaling(1f);
                    chapter.setBookmarkOpen(false);

                    currentChapter = chapter;
                    deep = 2;
                    sectionArray = new Section[100];
                    currentSection = currentChapter;
                    sectionArray[deep] = currentSection;
                    currentChapter.add(anchor);
                }
                if ("subsection".equals(qName)) {
                    String subSectionName = atts.getValue("name");
                    if (subSectionName == null) {
                        throw new NullPointerException("Null subsection name, " + uri);
                    }

                    //Build necessary bookmarks and anchors.
                    if (currentChapter == null) {
                        Chunk firstSubSection = new Chunk(subSectionName, subsectionFont);
                        pdfDoc.add(firstSubSection);
                    }
                    else {
                        int indx = subSectionName.indexOf(" ");
                        StringTokenizer st = new StringTokenizer("substr="
                                + subSectionName.substring(0, indx), ".");
                        int count = st.countTokens();
                        Paragraph title = new Paragraph(
                                subSectionName.substring(indx),
                                subsectionFont);
                        for (int i = 0; i < st.countTokens() - 1; i++)
                            st.nextToken();

                        if (count == deep) {
                            Section section = currentSection.addSection(title, deep);
                            section.setIndentation(1f);
                            ArrayList chunks = section.getBookmarkTitle().getChunks();
                            String name = "";
                            for (Iterator it = chunks.iterator(); it.hasNext();)
                                name += ((Chunk) it.next()).content();
                            Chunk anchor = new Chunk(name.trim(), PDFFonts.invisibleFont);
                            anchor.setLocalDestination(name.trim());
                            Paragraph pr = new Paragraph();
                            pr.add(anchor);
                            section.add(0, pr);

                            lastSection = section;
                            lastSection.setBookmarkOpen(false);
                            return;
                        }
                        if (count > deep) {
                            deep = count;
                            currentSection = lastSection;
                            sectionArray[deep] = currentSection;
                            lastSection = currentSection.addSection(title, deep);
                            lastSection.setIndentation(1f);
                            ArrayList chunks = lastSection.getBookmarkTitle().getChunks();
                            String name = "";
                            for (Iterator it = chunks.iterator(); it.hasNext();)
                                name += ((Chunk) it.next()).content();
                            Chunk anchor = new Chunk(name.trim(), PDFFonts.invisibleFont);
                            anchor.setLocalDestination(name.trim());
                            lastSection.add(0, anchor);
                            lastSection.setBookmarkOpen(false);
                            return;
                        }
                        if (count < deep) {
                            deep = count;
                            currentSection = sectionArray[deep];
                            lastSection = currentSection.addSection(title, deep);
                            lastSection.setIndentation(1f);
                            ArrayList chunks = lastSection.getBookmarkTitle().getChunks();
                            String name = "";
                            for (Iterator it = chunks.iterator(); it.hasNext();)
                                name += ((Chunk) it.next()).content();
                            Chunk anchor = new Chunk(name.trim(), PDFFonts.invisibleFont);
                            anchor.setLocalDestination(name.trim());
                            pdfDoc.add(anchor);
                            lastSection.add(0, anchor);
                            lastSection.setBookmarkOpen(false);
                            return;
                        }
                    }
                }
                if ("img".equals(qName))
                    src = atts.getValue("src");
                if ("p".equals(qName)) {
                    Paragraph pr = new Paragraph();
                    pr.setIndentationLeft(0f);
                    pr.setAlignment(Element.ALIGN_JUSTIFIED);
                    elements.push(pr);
                    tags.push("p");
                }
                if ("b".equals(qName)) {
                    if (fonts.peek().equals(PDFFonts.sourceFont))
                        fonts.push(PDFFonts.sourceBoldFont);
                    if (fonts.peek().equals(PDFFonts.italicFont))
                        fonts.push(PDFFonts.boldItalicFont);
                    if (fonts.peek().equals(PDFFonts.regularFont))
                        fonts.push(PDFFonts.boldFont);
                }
                if ("i".equals(qName)) {
                    if (fonts.peek().equals(PDFFonts.boldFont))
                        fonts.push(PDFFonts.boldItalicFont);
                    else
                        fonts.push(PDFFonts.italicFont);
                }
                if ("strong".equals(qName))
                    fonts.push(PDFFonts.boldFont);
                if ("code".equals(qName)) {
                    if (fonts.peek().equals(PDFFonts.italicFont))
                        fonts.push(PDFFonts.sourceItalicFont);
                    if (fonts.peek().equals(PDFFonts.boldFont))
                        fonts.push(PDFFonts.sourceBoldFont);
                    if (fonts.peek().equals(PDFFonts.boldItalicFont))
                        fonts.push(PDFFonts.sourceBoldItalicFont);
                    if (fonts.peek().equals(PDFFonts.regularFont))
                        fonts.push(PDFFonts.sourceFont);
                }
                if ("source".equals(qName)) {
                    source = "";
                    tags.push("source");
                    elements.push(new Paragraph(""));
                }
                if ("panel".equals(qName)) {
                    tags.push("panel");
                    elements.push(new Paragraph(""));
                    panelName = atts.getValue("name");
                }
                if ("ul".equals(qName)) {
                    list = new com.lowagie.text.List(false, 15f);
                    list.setIndentationLeft(30f);
                    Chunk symbol = new Chunk(".", FontFactory.getFont("TIMES",
                            45,
                            Font.BOLD,
                            new Color(0, 0, 0)));
                    list.setListSymbol(symbol);
                    tags.push("ul");
                }
                if ("li".equals(qName)) {
                    tags.push("li");
                    elements.push(new Paragraph(""));
                }
                if ("table".equals(qName)) {
                    tags.push("table");
                    cellsWidth = new ArrayList();
                }
                if ("tr".equals(qName)) {
                    tags.push("tr");
                    pdfCells = new ArrayList();
                }
                if ("th".equals(qName)) {
                    tags.push("th");
                    elements.push(new Paragraph("", PDFFonts.panelBoldFont));
                    String width;
                    if ((width = atts.getValue("width")) != null) {
                        cellsWidth
                                .add(new Integer(width.substring(0, width.length() - 1)));
                    }
                }
                if ("td".equals(qName)) {
                    tags.push("td");
                    elements.push(new Paragraph(""));
                    String width;
                    if ((width = atts.getValue("width")) != null) {
                        cellsWidth
                                .add(new Integer(width.substring(0, width.length() - 1)));
                    }
                }
                if ("a".equals(qName)) {
                    String href = atts.getValue("href");
                    currentLink = new String[3];
                    currentLink[2] = "";
                    if ((href.indexOf("http:/") != -1) || (href.indexOf("https:/") != -1))
                        currentLink[0] = "out";
                    else
                        currentLink[0] = "in";
                    currentLink[1] = href;
                    tags.push("a");
                }
            }
            catch (Exception e) {
                throw new RuntimeException("Error starting element: " + qName, e);
            }
        }

        public void characters(char buf[], int offset, int length) throws SAXException {
            String content = new String(buf, offset, length);

            if (tags.size() == 0) {
                return;
            }

            if ("p".equals(tags.peek())) {
                ((Paragraph) elements.peek()).add(new Chunk(
                        filter(content) + " ",
                        (Font) fonts.peek()));
            }

            if ("source".equals(tags.peek())) {
                source += content;
            }

            if ("panel".equals(tags.peek())) {
                ((Paragraph) elements.peek()).add(new Chunk(
                        " " + filter(content) + " ",
                        regularFont));
            }

            if ("td".equals(tags.peek())
                    || "th".equals(tags.peek())
                    || "li".equals(tags.peek())) {
                ((Paragraph) elements.peek()).add(new Chunk(
                        " " + filter(content) + " ",
                        regularFont));
            }

            if ("a".equals(tags.peek()))
                currentLink[2] += filter(content) + " ";

        }

        public void endElement(String uri, String local, String qName)
                throws SAXException {
            try {
                if ("br".equals(qName) && !elements.isEmpty()) {
                    ((Paragraph) elements.peek()).add("\n");
                }
                if ("img".equals(qName)) {
                    File imgFile = new File(src);
                    if (imgFile.exists()) {
                        com.lowagie.text.Image image = com.lowagie.text.Image
                                .getInstance(src);
                        image.setAlignment(Element.ALIGN_CENTER);
                        PdfPTable imgTable = new PdfPTable(1);
                        imgTable.setSpacingBefore(5f);
                        imgTable.setWidthPercentage(90f);
                        PdfPCell imgCell = new PdfPCell(image);
                        imgCell.setBorderWidth(0f);
                        imgCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        imgCell.setRightIndent(20f);
                        imgTable.addCell(imgCell);

                        if (tags.size() > 0) {
                            if (tags.peek().equals("p")) {
                                ((Paragraph) elements.peek()).add(image);
                                return;
                            }
                        }
                        if (lastSection != null) {
                            lastSection.add(imgTable);
                        }
                        else if (currentChapter != null)
                            currentChapter.add(imgTable);
                        else
                            pdfDoc.add(imgTable);
                    }
                    else {
                        if (lastSection != null)
                            lastSection.add(new Chunk(
                                    "no image in path " + src,
                                    PDFFonts.errorFont));
                        else if (currentChapter != null)
                            currentChapter.add(new Chunk(
                                    "no image in path " + src,
                                    PDFFonts.errorFont));
                        else
                            pdfDoc.add(new Chunk(
                                    "no image in path " + src,
                                    PDFFonts.errorFont));
                    }
                    src = null;
                }
                if ("document".equals(qName)) {
                    pdfDoc.close();
                }
                if ("section".equals(qName)) {
                    if (currentChapter != null)
                        pdfDoc.add(currentChapter);
                    deep = 1;
                    currentChapter = null;
                }
                if ("subsection".equals(qName)) {
                    lastSection = null;
                }
                if ("p".equals(qName)) {
                    if ((list != null) || (pdfTable != null)) {
                        Paragraph pr = (Paragraph) elements.pop();
                        ((Paragraph) elements.peek()).add(pr);
                        tags.pop();
                        return;
                    }
                    if (lastSection != null)
                        lastSection.add(elements.pop());
                    else if (currentChapter != null)
                        currentChapter.add(elements.pop());
                    else
                        pdfDoc.add((Paragraph) elements.pop());
                    tags.pop();
                }
                if ("b".equals(qName))
                    fonts.pop();
                if ("i".equals(qName))
                    fonts.pop();
                if ("strong".equals(qName))
                    fonts.pop();
                if ("code".equals(qName))
                    fonts.pop();
                if ("source".equals(qName)) {
                    ((Paragraph) elements.peek()).add(new Chunk(" "
                            + filterXML(source)
                            + " ", sourceFont));
                    PdfPTable sourceTable = new PdfPTable(1);
                    sourceTable.setWidthPercentage(100f);
                    PdfPCell sourceCell = new PdfPCell((Paragraph) elements.pop());
                    sourceCell.setBackgroundColor(new Color(240, 240, 240));
                    sourceTable.addCell(sourceCell);
                    sourceTable.setSpacingBefore(10f);
                    sourceTable.setSpacingAfter(10f);
                    if (lastSection != null)
                        lastSection.add(sourceTable);
                    else if (currentChapter != null)
                        currentChapter.add(sourceTable);
                    else
                        pdfDoc.add(sourceTable);
                    tags.pop();
                }
                if ("panel".equals(qName)) {
                    tags.pop();
                    float[] widths = {
                            0.05f, 2f
                    };
                    PdfPTable panelTable = new PdfPTable(widths);
                    panelTable.setWidthPercentage(100f);
                    com.lowagie.text.Image im = com.lowagie.text.Image
                            .getInstance(PANEL_ICON);
                    PdfPCell imCell = new PdfPCell(im);
                    imCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    imCell.setBorderWidthRight(0f);
                    imCell.setBorderWidthBottom(0f);
                    imCell.setFixedHeight(15f);
                    imCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
                    imCell.setBackgroundColor(new Color(240, 240, 240));
                    panelTable.addCell(imCell);

                    PdfPCell nCell = new PdfPCell(new Paragraph(
                            panelName,
                            PDFFonts.panelBoldFont));
                    nCell.setBackgroundColor(new Color(240, 240, 240));
                    nCell.setFixedHeight(15f);
                    nCell.setBorderWidthLeft(0f);
                    nCell.setBorderWidthBottom(0f);
                    nCell.setVerticalAlignment(Element.ALIGN_TOP);
                    nCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                    panelTable.addCell(nCell);

                    PdfPCell panelCell = new PdfPCell((Paragraph) elements.pop());
                    panelCell.setBackgroundColor(new Color(240, 240, 240));
                    panelCell.setColspan(2);
                    panelCell.setBorderWidthTop(0f);
                    panelTable.addCell(panelCell);
                    panelTable.setSpacingBefore(10f);
                    panelTable.setSpacingAfter(10f);

                    if (lastSection != null)
                        lastSection.add(panelTable);
                    else if (currentChapter != null)
                        currentChapter.add(panelTable);
                    else
                        pdfDoc.add(panelTable);

                }
                if ("ul".equals(qName)) {
                    tags.pop();
                    if (tags.size() > 0) {
                        if (tags.peek().equals("p")) {
                            Paragraph pr = new Paragraph("");
                            pr.add(list);
                            ((Paragraph) elements.peek()).add(pr);
                            list = null;
                            return;
                        }
                    }
                    if (lastSection != null)
                        lastSection.add(list);
                    else if (currentChapter != null)
                        currentChapter.add(list);
                    else
                        pdfDoc.add(list);
                    list = null;
                }
                if ("li".equals(qName)) {
                    ListItem listItem = new ListItem((Paragraph) elements.pop());
                    list.add(listItem);
                    tags.pop();
                }

                if ("table".equals(qName)) {
                    tags.pop();
                    float[] widths = new float[cellsWidth.size()];
                    for (int i = 0; i < cellsWidth.size(); i++)
                        widths[i] = ((Integer) cellsWidth.get(i)).intValue();
                    if (widths.length > 0)
                        pdfTable.setWidths(widths);

                    if (tags.size() > 0) {
                        if (tags.peek().equals("p")) {
                            Paragraph pr = new Paragraph("");
                            pr.add(pdfTable);
                            ((Paragraph) elements.peek()).add(pr);
                            pdfTable = null;
                            return;
                        }
                    }
                    if (lastSection != null)
                        lastSection.add(pdfTable);
                    else if (currentChapter != null)
                        currentChapter.add(pdfTable);
                    else
                        pdfDoc.add(pdfTable);
                    pdfTable = null;

                }
                if ("tr".equals(qName)) {
                    if (pdfTable == null) {
                        pdfTable = new PdfPTable(pdfCells.size());
                        pdfTable.setSpacingBefore(10f);
                        pdfTable.setSpacingAfter(10f);
                        pdfTable.setWidthPercentage(100f);
                    }
                    for (int i = 0; i < pdfCells.size(); i++)
                        pdfTable.addCell((PdfPCell) pdfCells.get(i));
                    tags.pop();
                }
                if ("th".equals(qName)) {
                    Paragraph paragraph = (Paragraph) elements.pop();
                    paragraph.setAlignment(Element.ALIGN_CENTER);
                    PdfPCell pdfCell = new PdfPCell();
                    pdfCell.addElement(paragraph);
                    pdfCell.setBackgroundColor(new Color(240, 240, 240));
                    pdfCell.setVerticalAlignment(Element.ALIGN_TOP);
                    pdfCells.add(pdfCell);
                    tags.pop();
                }
                if ("td".equals(qName)) {
                    Paragraph paragraph = (Paragraph) elements.pop();
                    paragraph.setAlignment(Element.ALIGN_LEFT);
                    PdfPCell pdfCell = new PdfPCell();
                    pdfCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    pdfCell.addElement(paragraph);
                    pdfCells.add(pdfCell);
                    tags.pop();
                }

                if ("a".equals(qName)) {
                    if ("out".equals(currentLink[0])) {
                        Anchor anchor = new Anchor(" " + currentLink[2], FontFactory
                                .getFont(FontFactory.TIMES, 12, Font.NORMAL, new Color(
                                        0,
                                        0,
                                        255)));
                        anchor.setReference(currentLink[1]);
                        if (elements.size() != 0) {
                            ((Paragraph) elements.peek()).add(anchor);
                        }
                        else
                            lastSection.add(anchor);
                    }
                    else {
                        Chunk chunk = new Chunk(" " + currentLink[2], regularFont);
                        if (elements.size() != 0) {
                            ((Paragraph) elements.peek()).add(chunk);
                        }
                        else
                            lastSection.add(chunk);
                    }
                    tags.pop();
                }
            }
            catch (Exception e) {
                throw new RuntimeException("Error parsing element: " + qName, e);
            }
        }

        /**
         * Filter string, deletes all "\n" symbols, and redundant spaces.
         * 
         * @param source - string which should be filtered
         * @return
         */
        private String filter(String source) {
            StringBuffer st = new StringBuffer(source);
            int pos;
            while ((pos = st.indexOf("\n")) != -1)
                st.delete(pos, pos + 1);
            while ((pos = st.indexOf("\r")) != -1)
                st.delete(pos, pos + 1);
            String result = new String("");
            StringTokenizer strTok = new StringTokenizer(st.toString(), " ");

            while (strTok.hasMoreTokens()) {
                StringBuffer st1 = new StringBuffer(strTok.nextToken());
                int pos1;
                while ((pos1 = st1.indexOf(" ")) != -1)
                    st1.delete(pos1, pos1 + 1);
                result += st1.toString() + " ";
            }
            return result.trim();
        }

        private String filterXML(String source) {
            StringBuffer st = new StringBuffer(source);
            while (st.indexOf("&lt;") != -1) {
                int pos = st.indexOf("&lt;");
                st.replace(pos, pos + 4, "<");
            }
            while (st.indexOf("&gt;") != -1) {
                int pos = st.indexOf("&gt;");
                st.replace(pos, pos + 4, ">");
            }
            return st.toString();
        }

        /**
         * Creates page header, footer and items in contentTable.
         */
        class PageNumberBuilder extends PdfPageEventHelper {

            public void onChapter(
                    PdfWriter writer,
                    Document document,
                    float paragraphPosition,
                    Paragraph title) {
                ArrayList chunks = title.getChunks();
                String name = "";
                for (Iterator it = chunks.iterator(); it.hasNext();)
                    name += ((Chunk) it.next()).content();
                contentTable.add(new ContentItem(
                        name.trim(),
                        writer.getPageNumber(),
                        "section"));
            }

            public void onSection(
                    PdfWriter writer,
                    Document document,
                    float paragraphPosition,
                    int depth,
                    Paragraph title) {
                ArrayList chunks = title.getChunks();
                String name = "";
                for (Iterator it = chunks.iterator(); it.hasNext();)
                    name += ((Chunk) it.next()).content();
                contentTable.add(new ContentItem(
                        name.trim(),
                        writer.getPageNumber(),
                        "subsection"));
            }

            public void onEndPage(PdfWriter writer, Document document) {
                Phrase pFooter = new Phrase(
                        "Cayenne Framework                                                                    ",
                        sourceFont);
                if (regularPageNumber) {
                    HeaderFooter footer = new HeaderFooter(pFooter, new Phrase(""));
                    footer.setBorderWidthBottom(0f);
                    document.setFooter(footer);
                }
                else {
                    HeaderFooter footer = new HeaderFooter(
                            new Phrase("Contents"),
                            false);
                    footer.setBorderWidthBottom(0f);
                    footer.setAlignment(Element.ALIGN_CENTER);
                    document.setFooter(footer);
                }
                if (currentChapter != null) {
                    StringTokenizer st = new StringTokenizer(currentChapterName, " ");
                    st.nextToken();
                    String name = "";
                    while (st.hasMoreTokens())
                        name += st.nextToken() + " ";
                    Paragraph paragraph = new Paragraph(name, sourceFont);
                    HeaderFooter header = new HeaderFooter(paragraph, false);
                    header.setAlignment(Element.ALIGN_CENTER);
                    header.setBorderWidthTop(0f);
                    document.setHeader(header);
                }
            }
        }
    }
}

