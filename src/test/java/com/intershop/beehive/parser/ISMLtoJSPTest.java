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
package com.intershop.beehive.parser;

import com.intershop.beehive.isml.internal.parser.ISMLtoJSPcompiler;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStreamWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * See {@link com.intershop.beehive.parser.ISMLtoJSP}.
 * 
 * @author stefanh
 */
class ISMLtoJSPTest
{
    @Test
    void testCompile() throws Exception
    {
        assertTrue(compile("#URLEx('', '',  Action('Default-Start'))#").contains("url("));
        assertTrue(compile(
                        "\"#sessionlessURLEx('','','',Action('Default-Start', '', application:Site:URL, application:Pipeline, application:DefaultCurrency, application:UrlIdentifie))#")
                                        .contains("url("));

        assertTrue(compile("#test(a + b)#").contains("customFunction(\"test\","));
        assertTrue(compile("#test()#").contains("customFunction(\"test\")"));
        assertTrue(compile("#test(a,b,c,isDefined(d))#").contains("customFunction(\"test\","));
        assertTrue(compile("#pipeline:pipeline#").contains("pipeline:pipeline"));
        assertTrue(compile("#pipeline:url#").contains("pipeline:url"));
        assertTrue(compile("#URL(Service(\"a/b\"))#").contains("URLServiceAction("));
        assertTrue(compile(
                        "\"#URLEX('','',Action('Default-Start', '', application:Site:URL, application:Pipeline, application:DefaultCurrency, application:UrlIdentifie))#")
                                        .contains("url("));
        assertTrue(compile("#localizeTextEx('A','',A:service)#").contains("localizeTextEx("));

        assertTrue(compile("#stringToHtml(webroot())#").contains("stringToHtml("));
    }

    protected String compile(String source) throws Exception
    {
        ByteArrayInputStream in = new ByteArrayInputStream(source.getBytes());

        ISMLtoJSPcompiler compiler = new ISMLtoJSPcompiler(in);

        ByteArrayOutputStream jspOut = new ByteArrayOutputStream();

        compiler.compileTemplate(ISMLtoJSPcompiler.ALLOW_ALL, new OutputStreamWriter(jspOut), new File("test"), in);

        return new String(jspOut.toByteArray());
    }
}
