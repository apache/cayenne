<?xml version="1.0" encoding="ASCII"?>
<!--
    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements. See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to you under the Apache License, Version
    2.0 (the "License"); you may not use this file except in compliance
    with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0 Unless required by
    applicable law or agreed to in writing, software distributed under the
    License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
    CONDITIONS OF ANY KIND, either express or implied. See the License for
    the specific language governing permissions and limitations under the
    License.
-->
<!--This file was created automatically by html2xhtml-->
<!--from the HTML stylesheets.-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:d="http://docbook.org/ns/docbook"
                xmlns:xslthl="http://xslthl.sf.net" xmlns="http://www.w3.org/1999/xhtml" exclude-result-prefixes="xslthl d" version="1.0">

    <xsl:import href="urn:docbkx:stylesheet/highlight.xsl"/>
    <!-- ********************************************************************
   $Id: highlight.xsl 8911 2010-09-28 17:02:06Z abdelazer $
   ********************************************************************

   This file is part of the XSL DocBook Stylesheet distribution.
   See ../README or http://docbook.sf.net/release/xsl/current/ for
   and other information.



   ******************************************************************** -->
    <!-- <xsl:import href="urn:/highlighting/common.xsl"/> -->
    <xsl:template match="xslthl:keyword" mode="xslthl">
        <span class="hl-keyword">
            <xsl:apply-templates mode="xslthl"/>
        </span>
    </xsl:template>
    <xsl:template match="xslthl:string" mode="xslthl">
        <span class="hl-string">
            <xsl:apply-templates mode="xslthl"/>
        </span>
    </xsl:template>
    <xsl:template match="xslthl:comment" mode="xslthl">
        <span class="hl-comment">
            <xsl:apply-templates mode="xslthl"/>
        </span>
    </xsl:template>
    <xsl:template match="xslthl:directive" mode="xslthl">
        <span class="hl-directive">
            <xsl:apply-templates mode="xslthl"/>
        </span>
    </xsl:template>
    <xsl:template match="xslthl:tag" mode="xslthl">
        <span class="hl-tag">
            <xsl:apply-templates mode="xslthl"/>
        </span>
    </xsl:template>
    <xsl:template match="xslthl:attribute" mode="xslthl">
        <span class="hl-attribute">
            <xsl:apply-templates mode="xslthl"/>
        </span>
    </xsl:template>
    <xsl:template match="xslthl:value" mode="xslthl">
        <span class="hl-value">
            <xsl:apply-templates mode="xslthl"/>
        </span>
    </xsl:template>
    <xsl:template match="xslthl:html" mode="xslthl">
        <strong>
            <span>
                <xsl:apply-templates mode="xslthl"/>
            </span>
        </strong>
    </xsl:template>
    <xsl:template match="xslthl:xslt" mode="xslthl">
        <span>
            <xsl:apply-templates mode="xslthl"/>
        </span>
    </xsl:template>
    <!-- Not emitted since XSLTHL 2.0 -->
    <xsl:template match="xslthl:section" mode="xslthl">
        <span>
            <xsl:apply-templates mode="xslthl"/>
        </span>
    </xsl:template>
    <xsl:template match="xslthl:number" mode="xslthl">
        <span class="hl-number">
            <xsl:apply-templates mode="xslthl"/>
        </span>
    </xsl:template>
    <xsl:template match="xslthl:annotation" mode="xslthl">
        <span class="hl-annotation">
            <xsl:apply-templates mode="xslthl"/>
        </span>
    </xsl:template>
    <!-- Not sure which element will be in final XSLTHL 2.0 -->
    <xsl:template match="xslthl:doccomment|xslthl:doctype" mode="xslthl">
        <strong class="hl-tag">
            <xsl:apply-templates mode="xslthl"/>
        </strong>
    </xsl:template>
</xsl:stylesheet>