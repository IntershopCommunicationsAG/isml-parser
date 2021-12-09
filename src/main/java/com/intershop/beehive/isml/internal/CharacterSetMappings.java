/*
 * Copyright 2021 Intershop Communications AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intershop.beehive.isml.internal;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * This class contains the character set mappings.
 */

public class CharacterSetMappings
{
    private static final Map<String, Charset> STANDARD_CHARSETS = Charset.availableCharsets();
    
    /**
     * Character set mappings holder (system).
     */
    private static final Map<String, String> javaToHttpTable = new HashMap<>();

    /**
     * Character set mappings holder (HTTP).
     */
    private static final Map<String, String> httpToJavaTable = new HashMap<>();

    /**
     * The default HTTP charset (RFC 2616, section 3.7.1).
     */

    public static final String DEFAULT_HTML_ENCODING = "iso-8859-1";

    /**
     * The default system encoding.
     */
    public static final String DEFAULT_SYSTEM_ENCODING = "ISO8859_1";
    
    /**
     * default encoding uses system property
     */
    private static final String DEFAULT_ENCODING = System.getProperty("file.encoding", DEFAULT_SYSTEM_ENCODING);
    /*
     * The mappings.
     */

    static
    {
        javaToHttpTable.put("us-ascii",         "us-ascii");
        javaToHttpTable.put("big5",             "big5");
        javaToHttpTable.put("cp037",            "cp037");
        javaToHttpTable.put("cp1006",           "cp1006");
        javaToHttpTable.put("cp1025",           "cp1025");
        javaToHttpTable.put("cp1026",           "cp1026");
        javaToHttpTable.put("cp1097",           "cp1097");
        javaToHttpTable.put("cp1098",           "cp1098");
        javaToHttpTable.put("cp1112",           "cp1112");
        javaToHttpTable.put("cp1122",           "cp1122");
        javaToHttpTable.put("cp1123",           "cp1123");
        javaToHttpTable.put("cp1124",           "cp1124");
        javaToHttpTable.put("cp1250",           "windows-1250");
        javaToHttpTable.put("cp1251",           "windows-1251");
        javaToHttpTable.put("cp1252",           "windows-1252");
        javaToHttpTable.put("cp1253",           "windows-1253");
        javaToHttpTable.put("cp1254",           "windows-1254");
        javaToHttpTable.put("cp1255",           "windows-1255");
        javaToHttpTable.put("cp1256",           "windows-1256");
        javaToHttpTable.put("cp1257",           "windows-1257");
        javaToHttpTable.put("cp1258",           "windows-1258");
        javaToHttpTable.put("cp1381",           "cp1381");
        javaToHttpTable.put("cp1383",           "cp1383");
        javaToHttpTable.put("cp273",            "cp273");
        javaToHttpTable.put("cp277",            "cp277");
        javaToHttpTable.put("cp278",            "cp278");
        javaToHttpTable.put("cp280",            "cp280");
        javaToHttpTable.put("cp284",            "cp284");
        javaToHttpTable.put("cp285",            "cp285");
        javaToHttpTable.put("cp297",            "cp297");
        javaToHttpTable.put("cp33722",          "cp33722");
        javaToHttpTable.put("cp420",            "cp420");
        javaToHttpTable.put("cp424",            "cp424");
        javaToHttpTable.put("cp437",            "cp437");
        javaToHttpTable.put("cp500",            "cp500");
        javaToHttpTable.put("cp737",            "cp737");
        javaToHttpTable.put("cp775",            "cp775");
        javaToHttpTable.put("cp838",            "cp838");
        javaToHttpTable.put("cp850",            "cp850");
        javaToHttpTable.put("cp852",            "cp852");
        javaToHttpTable.put("cp855",            "cp855");
        javaToHttpTable.put("cp856",            "cp856");
        javaToHttpTable.put("cp857",            "cp857");
        javaToHttpTable.put("cp860",            "cp860");
        javaToHttpTable.put("cp861",            "cp861");
        javaToHttpTable.put("cp862",            "cp862");
        javaToHttpTable.put("cp863",            "cp863");
        javaToHttpTable.put("cp864",            "cp864");
        javaToHttpTable.put("cp865",            "cp865");
        javaToHttpTable.put("cp866",            "cp866");
        javaToHttpTable.put("cp868",            "cp868");
        javaToHttpTable.put("cp869",            "cp869");
        javaToHttpTable.put("cp870",            "cp870");
        javaToHttpTable.put("cp871",            "cp871");
        javaToHttpTable.put("cp874",            "cp874");
        javaToHttpTable.put("cp875",            "cp875");
        javaToHttpTable.put("cp918",            "cp918");
        javaToHttpTable.put("cp921",            "cp921");
        javaToHttpTable.put("cp922",            "cp922");
        javaToHttpTable.put("cp930",            "cp930");
        javaToHttpTable.put("cp933",            "cp933");
        javaToHttpTable.put("cp935",            "cp935");
        javaToHttpTable.put("cp937",            "cp937");
        javaToHttpTable.put("cp939",            "cp939");
        javaToHttpTable.put("cp942",            "cp942");
        javaToHttpTable.put("cp942c",           "cp942c");
        javaToHttpTable.put("cp943",            "cp943");
        javaToHttpTable.put("cp943c",           "cp943c");
        javaToHttpTable.put("cp948",            "cp948");
        javaToHttpTable.put("cp949",            "cp949");
        javaToHttpTable.put("cp949c",           "cp949c");
        javaToHttpTable.put("cp950",            "cp950");
        javaToHttpTable.put("cp964",            "cp964");
        javaToHttpTable.put("cp970",            "cp970");
        javaToHttpTable.put("euc_cn",           "gb2312");
        javaToHttpTable.put("euc_jp",           "euc-jp");
        javaToHttpTable.put("euc_kr",           "euc-kr");
        javaToHttpTable.put("euc_tw",           "euc-tw");
        javaToHttpTable.put("iso2022jp",        "iso-2022-jp");
        javaToHttpTable.put("iso8859_1",       "iso-8859-1");
        javaToHttpTable.put("iso-8859-15",      "iso_8859-15");
        javaToHttpTable.put("iso8859_2",        "iso-8859-2");
        javaToHttpTable.put("iso8859_3",        "iso-8859-3");
        javaToHttpTable.put("iso8859_4",        "iso-8859-4");
        javaToHttpTable.put("iso8859_5",        "iso-8859-5");
        javaToHttpTable.put("iso8859_6",        "iso-8859-6");
        javaToHttpTable.put("iso8859_7",        "iso-8859-7");
        javaToHttpTable.put("iso8859_8",        "iso-8859-8");
        javaToHttpTable.put("iso8859_9",        "iso-8859-9");
        javaToHttpTable.put("jisautodetect",    "jis auto detect");
        javaToHttpTable.put("johab",            "ksc5601-1992");
        javaToHttpTable.put("koi8_r",           "koi8-r");
        javaToHttpTable.put("ms874",            "windows-874");
        javaToHttpTable.put("shift_jis",        "Shift_JIS");
        javaToHttpTable.put("ms949",            "windows-949");
        javaToHttpTable.put("sjis",             "Shift_JIS");
        javaToHttpTable.put("utf8",             "utf-8");
        javaToHttpTable.put("utf-16",           "UTF-16");
        javaToHttpTable.put("utf-16be",         "UTF-16BE");
        javaToHttpTable.put("utf-16le",         "UTF-16LE");

        httpToJavaTable.put("us-ascii",        "US-ASCII");
        httpToJavaTable.put("big5",            "Big5");
        httpToJavaTable.put("cp037",           "Cp037");
        httpToJavaTable.put("cp1006",          "Cp1006");
        httpToJavaTable.put("cp1025",          "Cp1025");
        httpToJavaTable.put("cp1026",          "Cp1026");
        httpToJavaTable.put("cp1097",          "Cp1097");
        httpToJavaTable.put("cp1098",          "Cp1098");
        httpToJavaTable.put("cp1112",          "Cp1112");
        httpToJavaTable.put("cp1122",          "Cp1122");
        httpToJavaTable.put("cp1123",          "Cp1123");
        httpToJavaTable.put("cp1124",          "Cp1124");
        httpToJavaTable.put("windows-1250",    "Cp1250");
        httpToJavaTable.put("windows-1251",    "Cp1251");
        httpToJavaTable.put("windows-1252",    "Cp1252");
        httpToJavaTable.put("windows-1253",    "Cp1253");
        httpToJavaTable.put("windows-1254",    "Cp1254");
        httpToJavaTable.put("windows-1255",    "Cp1255");
        httpToJavaTable.put("windows-1256",    "Cp1256");
        httpToJavaTable.put("windows-1257",    "Cp1257");
        httpToJavaTable.put("windows-1258",    "Cp1258");
        httpToJavaTable.put("cp1381",          "Cp1381");
        httpToJavaTable.put("cp1383",          "Cp1383");
        httpToJavaTable.put("cp273",           "Cp273");
        httpToJavaTable.put("cp277",           "Cp277");
        httpToJavaTable.put("cp278",           "Cp278");
        httpToJavaTable.put("cp280",           "Cp280");
        httpToJavaTable.put("cp284",           "Cp284");
        httpToJavaTable.put("cp285",           "Cp285");
        httpToJavaTable.put("cp297",           "Cp297");
        httpToJavaTable.put("cp33722",         "Cp33722");
        httpToJavaTable.put("cp420",           "Cp420");
        httpToJavaTable.put("cp424",           "Cp424");
        httpToJavaTable.put("cp437",           "Cp437");
        httpToJavaTable.put("cp500",           "Cp500");
        httpToJavaTable.put("cp737",           "Cp737");
        httpToJavaTable.put("cp775",           "Cp775");
        httpToJavaTable.put("cp838",           "Cp838");
        httpToJavaTable.put("cp850",           "Cp850");
        httpToJavaTable.put("cp852",           "Cp852");
        httpToJavaTable.put("cp855",           "Cp855");
        httpToJavaTable.put("cp856",           "Cp856");
        httpToJavaTable.put("cp857",           "Cp857");
        httpToJavaTable.put("cp860",           "Cp860");
        httpToJavaTable.put("cp861",           "Cp861");
        httpToJavaTable.put("cp862",           "Cp862");
        httpToJavaTable.put("cp863",           "Cp863");
        httpToJavaTable.put("cp864",           "Cp864");
        httpToJavaTable.put("cp865",           "Cp865");
        httpToJavaTable.put("cp866",           "Cp866");
        httpToJavaTable.put("cp868",           "Cp868");
        httpToJavaTable.put("cp869",           "Cp869");
        httpToJavaTable.put("cp870",           "Cp870");
        httpToJavaTable.put("cp871",           "Cp871");
        httpToJavaTable.put("cp874",           "Cp874");
        httpToJavaTable.put("cp875",           "Cp875");
        httpToJavaTable.put("cp918",           "Cp918");
        httpToJavaTable.put("cp921",           "Cp921");
        httpToJavaTable.put("cp922",           "Cp922");
        httpToJavaTable.put("cp930",           "Cp930");
        httpToJavaTable.put("cp933",           "Cp933");
        httpToJavaTable.put("cp935",           "Cp935");
        httpToJavaTable.put("cp937",           "Cp937");
        httpToJavaTable.put("cp939",           "Cp939");
        httpToJavaTable.put("cp942",           "Cp942");
        httpToJavaTable.put("cp942c",          "Cp942c");
        httpToJavaTable.put("cp943",           "Cp943");
        httpToJavaTable.put("cp943c",          "Cp943c");
        httpToJavaTable.put("cp948",           "Cp948");
        httpToJavaTable.put("cp949",           "Cp949");
        httpToJavaTable.put("cp949c",          "Cp949c");
        httpToJavaTable.put("cp950",           "Cp950");
        httpToJavaTable.put("cp964",           "Cp964");
        httpToJavaTable.put("cp970",           "Cp970");
        httpToJavaTable.put("gb2312",          "EUC_CN");
        httpToJavaTable.put("euc-jp",          "EUC_JP");
        httpToJavaTable.put("euc-kr",          "EUC_KR");
        httpToJavaTable.put("euc-tw",          "EUC_TW");
        httpToJavaTable.put("iso-2022-jp",     "ISO2022JP");
        httpToJavaTable.put("iso-8859-1",      "ISO8859_1");
        httpToJavaTable.put("iso_8859-15",     "ISO-8859-15");
        httpToJavaTable.put("latin-9",         "ISO-8859-15");
        httpToJavaTable.put("iso-8859-2",      "ISO8859_2");
        httpToJavaTable.put("iso-8859-3",      "ISO8859_3");
        httpToJavaTable.put("iso-8859-4",      "ISO8859_4");
        httpToJavaTable.put("iso-8859-5",      "ISO8859_5");
        httpToJavaTable.put("iso-8859-6",      "ISO8859_6");
        httpToJavaTable.put("iso-8859-7",      "ISO8859_7");
        httpToJavaTable.put("iso-8859-8",      "ISO8859_8");
        httpToJavaTable.put("iso-8859-9",      "ISO8859_9");
        httpToJavaTable.put("jis auto detect", "JISAutoDetect");
        httpToJavaTable.put("ksc5601-1992",    "Johab");
        httpToJavaTable.put("koi8-r",          "KOI8_R");
        httpToJavaTable.put("windows-874",     "MS874");
        httpToJavaTable.put("x-sjis",          "Shift_JIS");
        httpToJavaTable.put("shift-jis",       "Shift_JIS");
        httpToJavaTable.put("shift_jis",       "Shift_JIS");
        httpToJavaTable.put("windows-949",     "MS949");
        httpToJavaTable.put("utf-8",           "UTF8");
        httpToJavaTable.put("utf-16",          "UTF-16");
        httpToJavaTable.put("utf-16be",        "UTF-16BE");
        httpToJavaTable.put("utf-16le",        "UTF-16LE");
    }

    /**
     * Lookup method.
     *
     * @param systemCharset the system char set
     * @return the corresponding HTTP charset or the default (iso-8859-1)
     * if the provided system charset is null or not supported.
     */

    public static String mapCharsetToHttp(String systemCharset)
    {
        String charSet = null;

        if (null != systemCharset)
        {
            charSet = javaToHttpTable.get(systemCharset.toLowerCase());
        }// else proceed

        return charSet==null?DEFAULT_HTML_ENCODING:charSet;
    }

    /**
     * Lookup method.
     *
     * @param httpCharset the http charset
     * @return the corresponding system charset or the system default
     * if the provided http charset is null or not supported.
     */
    public static String mapHttpToCharset(String httpCharset)
    {
        String charSet = null;

        if (null != httpCharset)
        {
            charSet = httpToJavaTable.get(httpCharset.toLowerCase());

            if (null == charSet)
            {
                // try sun's converter class
                charSet = STANDARD_CHARSETS.get(httpCharset).name();
            }// else proceed
        }// else proceed

        // return system's default encoding if failed
        return charSet==null? DEFAULT_ENCODING:charSet;
    }
}
