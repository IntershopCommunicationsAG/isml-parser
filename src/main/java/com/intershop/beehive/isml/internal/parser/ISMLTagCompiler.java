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
package com.intershop.beehive.isml.internal.parser;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;

import com.intershop.beehive.isml.capi.ISMLTemplateConstants;
import com.intershop.beehive.isml.internal.CharacterSetMappings;

/**
 * The task of this class is to generate JAVA - server side scripting code for
 * each ISML tag found in the template. Therefore it contains one public method
 * "compileTag" and some private helper methods.
 */

public class ISMLTagCompiler implements ISMLtoJSPcompilerConstants
{
    private static AtomicInteger formCount = new AtomicInteger(0);
    
    /**
     * This method generates Java code for parsed ISML tags.
     * 
     * @param tag
     *            parsed tag as javacc Token
     * @param result
     *            output stream in that the generated Java code will be written
     * @param attributes
     *            Map that contains all parsed tag attributes
     * @param nestingTable
     *            Stack that contains all previously parsed relevant loop or
     *            conditional tags to check nesting levels etc.
     * @throws com.intershop.beehive.isml.internal.parser.ParseException
     *             if an ISML tag does not comply to the current standard
     */

    protected static void compileTag(Token tag, CompactingWriter result, Map<String, Object> attributes,
                    List<Token> nestingTable) throws ParseException
    {
        try
        {
            result.print(ISMLtoJSPcompiler.SCRIPTING_START);
            
            switch(tag.kind)
            {
                case ISBREAK:
                {
                    // check if ISBREAK is within ISLOOP
                    boolean isloop_flag = containsNestingTag(nestingTable, ISLOOP);

                    if (!isloop_flag)
                    {
                        throw new ParseException("ISBREAK outside ISLOOP.\n");
                    }

                    // check if ISBREAK is outside a loop if so log error
                    // message
                    result.print("if (getLoopStack().isEmpty()) ");
                    result.print('{');
                    result.print("Logger.error(");
                    result.print("this,");
                    result.print("\"ISBREAK occured outside ISLOOP. Line: {}\"," + tag.beginLine + ");");
                    result.print('}');

                    // if not remove upper loop stack entry and break while loop
                    result.print("else");
                    result.print('{');
                    result.print("getLoopStack().pop();");
                    result.print("break;");
                    result.print('}');
                    break;
                }
                case ISCACHE:
                {
                    // time default values
                    long minute = 0;
                    long hour = 0;
                    String minuteString = null;
                    String hourString = null;

                    // check if part of ISPlacement tag
                    isInISPlacement(nestingTable, "ISCACHE");

                    try
                    {
                        // process minute attribute (optional - both value and
                        // expression allowed)
                        if (hasValueAttribute(attributes, ATT_MINUTE))
                        {
                            minute = Long.parseLong(getValueAttribute(attributes, ATT_MINUTE));
                            minuteString = String.valueOf(minute);
                        }
                        else if (hasExpressionAttribute(attributes, ATT_MINUTE))
                        {
                            minuteString = "((Number)(" + getExpressionAttribute(attributes, ATT_MINUTE)
                                            + ")).longValue()";
                        }
                        else
                        {
                            minuteString = String.valueOf(minute);
                        }

                        // process hour attribute (optional - both value and
                        // expression allowed)
                        if (hasValueAttribute(attributes, ATT_HOUR))
                        {
                            hour = Long.parseLong(getValueAttribute(attributes, ATT_HOUR));
                            hourString = String.valueOf(hour);
                        }
                        else if (hasExpressionAttribute(attributes, ATT_HOUR))
                        {
                            hourString = "((Number)(" + getExpressionAttribute(attributes, ATT_HOUR) + ")).longValue()";
                        }
                        else
                        {
                            hourString = String.valueOf(hour);
                        }
                    }
                    catch(NumberFormatException e)
                    {
                        throw new ParseException(
                                        "Only numeric values or ISML expressions are allowed for the \"hour\" and \"minute\" attributes of ISCACHE tag.\n");
                    }
                    // start code block
                    result.print('{');
                    result.print("try{");
                    // process type attribute (required - value necessary)
                    if (hasValueAttribute(attributes, ATT_TYPE))
                    {
                        result.print("String currentCacheTime = (String)((com.intershop.beehive.core.capi.request.ServletResponse)response).getHeaderValue(TemplateConstants.PAGECACHE_HEADER);");

                        if (equalsAttribute(attributes, ATT_TYPE, "daily"))
                        {
                            result.print("if (currentCacheTime!=null && \"00\".equals(currentCacheTime)) {Logger.debug(this, \"ISCACHE declaration is ignored since a prior 'forbidden'.\");}");
                            result.print("else {");

                            // get current time in seconds
                            result.print("long time = System.currentTimeMillis()/1000;");
                            // check borders
                            result.print("long minute=" + minuteString + ';');
                            result.print("if (minute <0) minute=0;");
                            result.print("if (minute >59) minute=59;");
                            result.print("long hour=" + hourString + ';');
                            result.print("if (hour <0)  hour=0;");
                            result.print("if (hour >23) hour=23;");
                            // calculate date of expiring
                            result.print("Calendar calendar = new GregorianCalendar();");
                            // convert long value of minute and hour to int to match method signature
                            result.print("calendar.set(Calendar.HOUR_OF_DAY,Long.valueOf(hour).intValue());");
                            result.print("calendar.set(Calendar.MINUTE,Long.valueOf(minute).intValue());");
                            result.print("calendar.set(Calendar.SECOND,0);");
                            result.print("calendar.set(Calendar.MILLISECOND,0);");
                            result.print("long expireTime = calendar.getTime().getTime()/1000;");
                            // compare with current time
                            result.print("if (expireTime < time) { expireTime += 86400; }"); // add
                            // one
                            // day
                            result.print("time = expireTime;");
                            result.print("String extCacheTime = (String)((com.intershop.beehive.core.capi.request.ServletResponse)response).getHeaderValue(TemplateConstants.EXT_PAGECACHE_HEADER);");
                            result.print("Long oldTime=(currentCacheTime!=null)?Long.valueOf(currentCacheTime):(extCacheTime!=null)?Long.valueOf(extCacheTime):null;");
                            result.print("if (oldTime!=null && oldTime<time) {");
                            result.print("Logger.debug(this, \"ISCACHE declaration is ignored since a prior declaration with a smaller caching period.\");");
                            result.print("response.setHeader(TemplateConstants.PAGECACHE_HEADER, String.valueOf(oldTime));");
                            result.print("}");
                            result.print("else if (oldTime!=null && oldTime>time) {Logger.debug(this, \"ISCACHE declaration reduces a caching period set by a prior declaration.\");}");
                            result.print("if (oldTime==null || oldTime>time){");
                            // check if the time to set is over 4 BYTE value; set the time to max integer
                            result.print("if (time > Integer.MAX_VALUE){  time = Integer.MAX_VALUE;} ");
                            result.print("response.setHeader(TemplateConstants.PAGECACHE_HEADER, String.valueOf(time));");
                            result.print("}}");
                        }
                        else if (equalsAttribute(attributes, ATT_TYPE, "relative"))
                        {
                            result.print("if (currentCacheTime!=null && \"00\".equals(currentCacheTime)) {Logger.debug(this, \"ISCACHE declaration is ignored since a prior 'forbidden'.\");}");
                            result.print("else {");

                            // get current time in seconds
                            result.print("long time = System.currentTimeMillis()/1000;");
                            // check borders
                            result.print("long minute=" + minuteString + ';');
                            result.print("if (minute <0) minute=0;");
                            result.print("long hour=" + hourString + ';');
                            result.print("if (hour <0)  hour=0;"); // no max
                            // border !!
                            // calculate offset
                            result.print("time += 60*minute+3600*hour;");
                            result.print("String extCacheTime = (String)((com.intershop.beehive.core.capi.request.ServletResponse)response).getHeaderValue(TemplateConstants.EXT_PAGECACHE_HEADER);");
                            result.print("Long oldTime=(currentCacheTime!=null)?Long.valueOf(currentCacheTime):(extCacheTime!=null)?Long.valueOf(extCacheTime):null;");
                            result.print("if (oldTime!=null && oldTime<time) {");
                            result.print("Logger.debug(this, \"ISCACHE declaration is ignored since a prior declaration with a smaller caching period.\");");
                            result.print("response.setHeader(TemplateConstants.PAGECACHE_HEADER, String.valueOf(oldTime));");
                            result.print("}");
                            result.print("else if (oldTime!=null && oldTime>time) {Logger.debug(this, \"ISCACHE declaration reduces a caching period set by a prior declaration.\");}");
                            result.print("if (oldTime==null || oldTime>time){");
                            // check if the time to set is over 4 BYTE value; set the time to max integer
                            result.print("if (time > Integer.MAX_VALUE){  time = Integer.MAX_VALUE;} ");
                            result.print("response.setHeader(TemplateConstants.PAGECACHE_HEADER, String.valueOf(time));");
                            result.print("}}");
                        }
                        else if (equalsAttribute(attributes, ATT_TYPE, "forbidden"))
                        {
                            result.print("if (currentCacheTime!=null && !\"00\".equals(currentCacheTime)) {Logger.debug(this, \"ISCACHE 'forbidden' overwrites prior caching declaration.\");}");
                            result.print("response.setHeader(TemplateConstants.PAGECACHE_HEADER, \"00\");");
                        }
                        else
                        {
                            throw new ParseException("Attribute \"type\" in ISCACHE has a wrong value.\n"
                                            + getValueAttribute(attributes, ATT_TYPE));
                        }
                    }
                    else if (hasExpressionAttribute(attributes, ATT_TYPE))
                    {
                        throw new ParseException("Attribute \"type\" in ISCACHE must not have an expression value.\n");
                    }
                    else
                    {
                        throw new ParseException("Missing \"type\" attribute in ISCACHE.\n");
                    }

                    result.print("}catch(Exception e){");
                    result.print("Logger.error(");
                    result.print("this,\"ISCACHE failed. Line: {" + tag.beginLine + "}\",e);");
                    result.print('}'); // exception catch
                    result.print('}'); // code block

                    break;
                }
                case ISCACHEKEY:
                {
                    // check if part of ISPlacement tag
                    isInISPlacement(nestingTable, "ISCACHEKEY");

                    String keyword = null, object = null;

                    result.print('{');
                    // process "keyword" attribute
                    if (hasValueAttribute(attributes, ATT_KEYWORD))
                    {
                        keyword = '\"' + getValueAttribute(attributes, ATT_KEYWORD) + '\"';
                    }
                    else if (hasExpressionAttribute(attributes, ATT_KEYWORD))
                    {
                        keyword = "context.getFormattedValue(" + getExpressionAttribute(attributes, ATT_KEYWORD)
                                        + ",null)";
                    }

                    // process "object" attribute
                    if (hasValueAttribute(attributes, ATT_OBJECT))
                    {
                        object = getValueAttribute(attributes, ATT_OBJECT);
                        result.print("Object key_obj = \"" + object + "\"; ");
                    }
                    else if (hasExpressionAttribute(attributes, ATT_OBJECT))
                    {
                        object = getExpressionAttribute(attributes, ATT_OBJECT);
                        result.print("Object key_obj = " + object + "; ");
                    }

                    if (object == null && keyword == null)
                    {
                        throw new ParseException("Missing \"keyword\" or \"object\" attribute in ISCACHKEY.\n");
                    }

                    if (keyword != null)
                    {
                        result.print("NamingMgr.get(PageCacheMgr.class).getKeywords().add(" + keyword + ");");
                    }

                    if (object != null)
                    {
                        result.print("NamingMgr.get(PageCacheMgr.class).registerObject(" + object + ");");
                    }
                    result.print('}');

                    break;
                }
                case ISDICTIONARY:
                {
                    // check if part of ISPlacement tag
                    isInISPlacement(nestingTable, "ISDICTIONARY");

                    result.print('{');
                    result.print("try{");

                    // process attribute source (required - expression
                    // necessary)
                    if (hasValueAttribute(attributes, ATT_SOURCE))
                    {
                        throw new ParseException(
                                        "Attribute \"source\"  in ISDICTIONARY must have an expression value.\n");
                    }
                    else if (hasExpressionAttribute(attributes, ATT_SOURCE))
                    {
                        result.print(getExpressionAttribute(attributes, ATT_SOURCE));
                    }
                    else
                    {
                        throw new ParseException("Missing \"source\" attribute in ISDICTIONARY.\n");
                    }

                    // process attribute alias (optional - value necessary)
                    if (hasValueAttribute(attributes, ATT_ALIAS))
                    {
                        result.print('\"' + getValueAttribute(attributes, ATT_ALIAS) + '\"');
                    }
                    else
                    {
                        result.print("null");
                    }

                    result.print(");");

                    result.print("}catch(Exception e){");
                    result.print("Logger.error(");
                    result.print("this,\"ISDICTIONARY has an invalid expression. Line: {" + tag.beginLine + "}\",e);");
                    result.print('}'); // exception catch

                    result.print('}');
                    break;
                }
                case ISCONTENT:
                {
                    // check if part of ISPlacement tag
                    isInISPlacement(nestingTable, "ISCONTENT");

                    String type = null;
                    String dynamictype = null;
                    String charset = null;

                    // process attribute compact (optional - value necessary)
                    if (hasValueAttribute(attributes, ATT_COMPACT))
                    {
                        if (equalsAttribute(attributes, ATT_COMPACT, "true"))
                        {
                            // switch on compacting mode on writer
                            result.enable();
                        }
                        else if (equalsAttribute(attributes, ATT_COMPACT, "false"))
                        {
                            // nothing to do
                        }
                        else
                        {
                            throw new ParseException("Attribute \"compact\" in ISCONTENT has a wrong value.\n");
                        }
                    }
                    else if (hasExpressionAttribute(attributes, ATT_COMPACT))
                    {
                        throw new ParseException(
                                        "Attribute \"compact\" in ISCONTENT must not have an expression value.\n");
                    }

                    // process attribute compact (optional - value necessary)
                    if (hasValueAttribute(attributes, ATT_TEMPLATEMARKER))
                    {
                        if (equalsAttribute(attributes, ATT_TEMPLATEMARKER, "true"))
                        {
                            result.print("%><%! protected Boolean printTemplateMarker() { return Boolean.TRUE; } %><%");
                        }
                        else if (equalsAttribute(attributes, ATT_TEMPLATEMARKER, "false"))
                        {
                            result.print("%><%! protected Boolean printTemplateMarker() { return Boolean.FALSE; } %><%");
                        }
                        else
                        {
                            throw new ParseException("Attribute \"templatemarker\" in ISCONTENT has a wrong value.\n");
                        }
                    }
                    else if (hasExpressionAttribute(attributes, ATT_TEMPLATEMARKER))
                    {
                        throw new ParseException(
                                        "Attribute \"templatemarker\" in ISCONTENT must not have an expression value.\n");
                    }

                    // process attribute type (optional - value necessary)
                    if (hasValueAttribute(attributes, ATT_TYPE))
                    {
                        type = getValueAttribute(attributes, ATT_TYPE);
                    }
                    else if (hasExpressionAttribute(attributes, ATT_TYPE))
                    {
                        dynamictype = "context.getFormattedValue(" + getExpressionAttribute(attributes, ATT_TYPE)
                                        + ",null)";
                    }
                    else
                    {
                        type = "text/html";
                    }

                    // process attribute charset (optional - value necessary)
                    if (hasValueAttribute(attributes, ATT_CHARSET))
                    {
                        getValueAttribute(attributes, ATT_CHARSET);
                    }
                    else if (hasExpressionAttribute(attributes, ATT_CHARSET))
                    {
                        throw new ParseException(
                                        "Attribute \"charset\" in ISCONTENT must not have an expression value.\n");
                    }

                    charset = CharacterSetMappings.mapCharsetToHttp(result.getEncoding());

                    String encType;

                    // tell jsp processor about it
                    if (dynamictype == null)
                    {
                        result.print("%><%@ page contentType=\"" + type
                                        + (charset != null ? (";charset=" + charset) : "") + "\" %><%");
                        encType = type;
                    }
                    else
                    {
                        result.print("response.setContentType(" + dynamictype
                                        + (charset != null ? ("+\";charset=" + charset + "\"") : "") + ");");
                        encType = dynamictype;
                    }

                    // process attribute httpstatus (optional)

                    // UR:
                    // The value for the httpstatus can be an Integer a positive
                    // integer constant or an
                    // ISML expression resulting in a value > 0.
                    handleHTTPStatus(result, attributes, ATT_HTTPSTATUS, "ISCONTENT", null, 1, null);

                    // process attribute session (optional - value necessary)
                    if (hasValueAttribute(attributes, ATT_SESSION))
                    {
                        if (equalsAttribute(attributes, ATT_SESSION, "true"))
                        {
                            // do nothing, session context is available by
                            // default
                        }
                        else if (equalsAttribute(attributes, ATT_SESSION, "false"))
                        {
                            result.print("%><%@ page session=\"false\"%><%");
                        }
                        else
                        {
                            throw new ParseException("Attribute \"session\" in ISCONTENT has a wrong value.\n");
                        }
                    }

                    // process attribute personalized (optional, value
                    // necessary)
                    if (hasValueAttribute(attributes, ATT_PERSONALIZED))
                    {
                        if (equalsAttribute(attributes, ATT_PERSONALIZED, "true"))
                        {
                            result.print("response.setHeader(TemplateConstants.PERSONALIZED_HEADER, \"1\");");
                        }
                        else if (equalsAttribute(attributes, ATT_PERSONALIZED, "false"))
                        {
                            // default: don't send the header
                        }
                        else
                        {
                            throw new ParseException("Attribute \"personalized\" in ISCONTENT has a wrong value.\n");
                        }
                    }

                    // process attribute encoding (optional - value necessary)
                    if (hasValueAttribute(attributes, ATT_ENCODE))
                    {
                        if (equalsAttribute(attributes, ATT_ENCODE, "on"))
                        {
                            // do nothing, same as content type will be used
                        }
                        else if (equalsAttribute(attributes, ATT_ENCODE, "off"))
                        {
                            encType = ISMLTemplateConstants.TYPE_TEXT;
                        }
                        else if (equalsAttribute(attributes, ATT_ENCODE, "html"))
                        {
                            encType = ISMLTemplateConstants.TYPE_HTML;
                        }
                        else if (equalsAttribute(attributes, ATT_ENCODE, "xml"))
                        {
                            encType = ISMLTemplateConstants.TYPE_XML;
                        }
                        else if (equalsAttribute(attributes, ATT_ENCODE, "wml"))
                        {
                            encType = ISMLTemplateConstants.TYPE_WML;
                        }
                        else
                        {
                            throw new ParseException("Attribute \"encoding\" in ISCONTENT has a wrong value.\n");
                        }
                    }// else proceed

                    // set template's character entity encoding
                    if (dynamictype == null || !encType.equals(dynamictype))
                    {
                        result.print("setEncodingType(\"" + encType + "\");");
                    }
                    else
                    {
                        result.print("setEncodingType(" + encType + ");");
                    }

                    break;
                }
                case ISCOOKIE:
                {
                    // check if part of ISPlacement tag
                    isInISPlacement(nestingTable, "ISCOOKIE");

                    String value, name, comment, domain, maxAge, path, secure, version;

                    // process attribute name (required - both value &
                    // expression allowed)
                    if (hasValueAttribute(attributes, ATT_NAME))
                    {
                        name = '\"' + getValueAttribute(attributes, ATT_NAME) + '\"';
                    }
                    else if (hasExpressionAttribute(attributes, ATT_NAME))
                    {
                        name = "context.getFormattedValue(" + getExpressionAttribute(attributes, ATT_NAME) + ",null)";
                    }
                    else
                    {
                        throw new ParseException("Missing \"name\" attribute in ISCOOKIE.\n");
                    }

                    // process attribute value (required - both value &
                    // expression allowed)
                    if (hasValueAttribute(attributes, ATT_VALUE))
                    {
                        value = '\"' + getValueAttribute(attributes, ATT_VALUE) + '\"';
                    }
                    else if (hasExpressionAttribute(attributes, ATT_VALUE))
                    {
                        value = "context.getFormattedValue(" + getExpressionAttribute(attributes, ATT_VALUE) + ",null)";
                    }
                    else
                    {
                        throw new ParseException("Missing \"value\" attribute in ISCOOKIE.\n");
                    }

                    // process attribute comment (optional - both value &
                    // expression allowed)
                    if (hasValueAttribute(attributes, ATT_COMMENT))
                    {
                        comment = '\"' + getValueAttribute(attributes, ATT_COMMENT) + '\"';
                    }
                    else if (hasExpressionAttribute(attributes, ATT_COMMENT))
                    {
                        comment = "context.getFormattedValue(" + getExpressionAttribute(attributes, ATT_COMMENT)
                                        + ",null)";
                    }
                    else
                    {
                        comment = null;
                    }

                    // process attribute domain (optional - both value &
                    // expression allowed)
                    if (hasValueAttribute(attributes, ATT_DOMAIN))
                    {
                        domain = '\"' + getValueAttribute(attributes, ATT_DOMAIN) + '\"';
                    }
                    else if (hasExpressionAttribute(attributes, ATT_DOMAIN))
                    {
                        domain = "context.getFormattedValue(" + getExpressionAttribute(attributes, ATT_DOMAIN)
                                        + ",null)";
                    }
                    else
                    {
                        domain = null;
                    }

                    // process attribute path (optional - both value &
                    // expression allowed)
                    if (hasValueAttribute(attributes, ATT_PATH))
                    {
                        path = '\"' + getValueAttribute(attributes, ATT_PATH) + '\"';
                    }
                    else if (hasExpressionAttribute(attributes, ATT_PATH))
                    {
                        path = "context.getFormattedValue(" + getExpressionAttribute(attributes, ATT_PATH) + ",null)";
                    }
                    else
                    {
                        path = null;
                    }

                    try
                    {
                        // process attribute maxAge (optional - both value &
                        // expression allowed)
                        if (hasValueAttribute(attributes, ATT_MAXAGE))
                        {
                            maxAge = String.valueOf(Integer.parseInt(getValueAttribute(attributes, ATT_MAXAGE)));
                        }
                        else if (hasExpressionAttribute(attributes, ATT_MAXAGE))
                        {
                            maxAge = "((Number)(" + getExpressionAttribute(attributes, ATT_MAXAGE) + ")).intValue()";
                        }
                        else
                        {
                            maxAge = null;
                        }

                        // process attribute version (optional - both value &
                        // expression allowed)
                        if (hasValueAttribute(attributes, ATT_VERSION))
                        {
                            version = String.valueOf(Integer.parseInt(getValueAttribute(attributes, ATT_VERSION)));
                        }
                        else if (hasExpressionAttribute(attributes, ATT_VERSION))
                        {
                            version = "((Number)(" + getExpressionAttribute(attributes, ATT_VERSION) + ")).intValue()";
                        }
                        else
                        {
                            version = "0";
                        }
                    }
                    catch(NumberFormatException e)
                    {
                        throw new ParseException(
                                        "Only numeric values or ISML expressions are allowed for the \"maxage\" and \"version\" attributes of ISCOOKIE tag.\n");
                    }

                    // process attribute secure (optional - value necessary)
                    if (hasValueAttribute(attributes, ATT_SECURE))
                    {
                        if (equalsAttribute(attributes, ATT_SECURE, "on"))
                        {
                            secure = "true";
                        }
                        else if (equalsAttribute(attributes, ATT_SECURE, "off"))
                        {
                            secure = "false";
                        }
                        else
                        {
                            throw new ParseException("Attribute \"secure\" in ISCOOKIE has a wrong value.\n");
                        }
                    }
                    else if (hasExpressionAttribute(attributes, ATT_SECURE))
                    {
                        throw new ParseException(
                                        "Attribute \"secure\" in ISCOOKIE must not have an expression value.\n");
                    }
                    else
                    {
                        secure = "false";
                    }

                    result.print('{');
                    result.print("try{");
                    result.print("Cookie cookie=new Cookie(" + name + ',' + value + ");");
                    if (comment != null)
                    {
                        result.print("cookie.setComment(" + comment + ");");
                    }
                    if (domain != null)
                    {
                        result.print("cookie.setDomain(" + domain + ");");
                    }
                    if (path != null)
                    {
                        result.print("cookie.setPath(" + path + ");");
                    }
                    if (maxAge != null)
                    {
                        result.print("cookie.setMaxAge(" + maxAge + ");");
                    }
                    result.print("cookie.setVersion(" + version + ");");
                    result.print("cookie.setSecure(" + secure + ");");
                    result.print("response.addCookie(cookie);");
                    result.print("}catch(Exception e){");
                    result.print("Logger.error(");
                    result.print("this,\"ISCOOKIE could not be set. Line: {" + tag.beginLine + "}\",e);");
                    result.print('}'); // exception catch
                    result.print('}'); // code block

                    break;
                }
                case ISELSE:
                {
                    if (!checkNestingTopTag(nestingTable, ISIF) && !checkNestingTopTag(nestingTable, ISELSIF))
                    {
                        throw new ParseException("Nesting Error: There is no corresponding ISIF for this ISELSE.\n");
                    }

                    result.print("} else {");
                    nestingTable.add(tag);
                    break;
                }
                case ISELSIF:
                {
                    if (!checkNestingTopTag(nestingTable, ISIF) && !checkNestingTopTag(nestingTable, ISELSIF))
                    {
                        throw new ParseException("Nesting Error: There is no corresponding ISIF for this ISELSEIF.\n");
                    }
                    result.print("} else {");
                    // no break!!!!!
                }
                case ISIF:
                {
                    result.print("_boolean_result=false;");
                    result.print("try {");
                    result.print("_boolean_result=((Boolean)(");

                    // process attribute condition (required - expression
                    // necessary)
                    if (hasValueAttribute(attributes, ATT_CONDITION))
                    {
                        throw new ParseException("Attribute \"condition\" in ISIF must have an expression value.\n");
                    }
                    else if (hasExpressionAttribute(attributes, ATT_CONDITION))
                    {
                        result.print(getExpressionAttribute(attributes, ATT_CONDITION));
                    }
                    else
                    {
                        throw new ParseException("Missing \"condition\" attribute in ISIF or ISELSIF.\n");
                    }

                    result.print(")).booleanValue();");
                    result.print("} catch (Exception e) {");
                    result.print("Logger.debug(");
                    result.print("this,\"Boolean expression in line {} could not be evaluated. False returned. Consider using the 'isDefined' ISML function.\",");
                    result.print(tag.beginLine + ",e);"); // copy line of tag
                    // into code
                    result.print('}');
                    result.print("if (_boolean_result) {");

                    nestingTable.add(tag);
                    break;
                }
                case ISIF_END:
                {
                    if (!checkNestingTopTag(nestingTable, ISIF) && !checkNestingTopTag(nestingTable, ISELSIF)
                                    && !checkNestingTopTag(nestingTable, ISELSE))
                    {
                        throw new ParseException("Nesting Error: There is no corresponding ISIF for this /ISIF.\n");
                    }
                    // clear nesting table and write necessary closing curly
                    // brackets
                    while(!checkNestingTopTag(nestingTable, ISIF))
                    {
                        if (checkNestingTopTag(nestingTable, ISELSIF))
                        {
                            result.print('}');
                        }
                        removeNestingTopTag(nestingTable);
                    }
                    result.print('}');
                    removeNestingTopTag(nestingTable);
                    break;
                }
                case ISINCLUDE:
                {
                    // check if part of ISPlacement tag
                    isInISPlacement(nestingTable, "ISINCLUDE");

                    String template = null, url = null, username = null, password = null, dictName = null;
                    String extensionpoint = null;
                    int mode = 0;

                    // process attribute template (required - if attribute 'url'
                    // is missing
                    // - both value & expression allowed)
                    if (hasValueAttribute(attributes, ATT_TEMPLATE))
                    {
                        template = '\"' + getValueAttribute(attributes, ATT_TEMPLATE) + '\"';
                    }
                    else if (hasExpressionAttribute(attributes, ATT_TEMPLATE))
                    {
                        template = "context.getFormattedValue(" + getExpressionAttribute(attributes, ATT_TEMPLATE)
                                        + ",null)";
                    }
                    else if (hasValueAttribute(attributes, ATT_EXTENSIONPOINT))
                    {
                        extensionpoint = '\"' + getValueAttribute(attributes, ATT_EXTENSIONPOINT) + '\"';
                    }
                    else
                    {
                        // process attribute url (required - if attribute 'url'
                        // is missing
                        // - both value & expression allowed)
                        if (hasValueAttribute(attributes, ATT_URL))
                        {
                            url = '\"' + getValueAttribute(attributes, ATT_URL) + '\"';
                        }
                        else if (hasExpressionAttribute(attributes, ATT_URL))
                        {
                            url = "context.getFormattedValue(" + getExpressionAttribute(attributes, ATT_URL) + ",null)";
                        }
                        else
                        {
                            throw new ParseException("Missing template locator attribute in ISINCLUDE.\n");
                        }

                        // process attribute username (required if password is
                        // provided - both value & expression allowed)
                        if (hasValueAttribute(attributes, ATT_USERNAME))
                        {
                            username = '\"' + getValueAttribute(attributes, ATT_USERNAME) + '\"';
                        }
                        else if (hasExpressionAttribute(attributes, ATT_USERNAME))
                        {
                            username = "context.getFormattedValue(" + getExpressionAttribute(attributes, ATT_USERNAME)
                                            + ",null)";
                        }
                        else
                        {
                            username = null;
                        }

                        // process attribute password (required if username is
                        // provided - both value & expression allowed)
                        if (hasValueAttribute(attributes, ATT_PASSWORD))
                        {
                            password = '\"' + getValueAttribute(attributes, ATT_PASSWORD) + '\"';
                        }
                        else if (hasExpressionAttribute(attributes, ATT_PASSWORD))
                        {
                            password = "context.getFormattedValue(" + getExpressionAttribute(attributes, ATT_PASSWORD)
                                            + ",null)";
                        }
                        else if (username != null)
                        {
                            throw new ParseException(
                                            "Attribute \"username\" in ISINCLUDE has no corresponding \"password\" attribute.\n");
                        }
                        else
                        {
                            password = null;
                        }

                        // don't allow 'password' without 'username'
                        if ((username == null) && (password != null))
                        {
                            throw new ParseException(
                                            "Attribute \"password\" in ISINCLUDE has no corresponding \"username\" attribute.\n");
                        } // no else needed

                        // process attribute mode (optional - only values
                        // "automatic" and "server" are allowed)
                        if (hasValueAttribute(attributes, ATT_MODE))
                        {
                            if (equalsAttribute(attributes, ATT_MODE, "automatic"))
                            {
                                // do nothing, mode stays 0
                            }
                            else if (equalsAttribute(attributes, ATT_MODE, "server"))
                            {
                                mode = 1;
                            }
                            else
                            {
                                throw new ParseException("Attribute \"mode\" in ISINCLUDE has a wrong value.\n");
                            }
                        }
                    }

                    // process attribute dictionary (optional - both value &
                    // expression allowed)
                    if (hasValueAttribute(attributes, ATT_DICTIONARY))
                    {
                        dictName = '\"' + getValueAttribute(attributes, ATT_DICTIONARY) + '\"';
                    }
                    else if (hasExpressionAttribute(attributes, ATT_DICTIONARY))
                    {
                        dictName = "context.getFormattedValue(" + getExpressionAttribute(attributes, ATT_DICTIONARY)
                                        + ",null)";
                    }

                    if (template == null && dictName != null)
                    {
                        throw new ParseException(
                                        "Attribute \"dictionary\" in ISINCLUDE is only allowed with attribute \"template\".\n");
                    }

                    // start code block
                    result.print('{');
                    result.print("out.flush();");
                    if (template != null)
                    {
                        // handling for local includes
                        result.print("processLocalIncludeByServer((com.intershop.beehive.core.capi.request.ServletResponse)response,"
                                        + template + ", " + dictName + ", \"" + tag.beginLine + "\");");
                    }
                    else if (url != null)
                    {
                        // handling for remote includes
                        // remote includes are not rewritten, therefore a NullURLRewriteHandler
                        // is placed temporarily in the template context 
                        result.print("%><%@page import=\"com.intershop.beehive.core.capi.url.*\"%><%");
                        result.print("URLRewriteHandler handler = getTemplateExecutionConfig().getURLRewriteHandler();\n");
                        result.print("try\n{\n");
                        result.print("getTemplateExecutionConfig().setURLRewriteHandler(NullURLRewriteHandler.getInstance());\n");
                        if (mode == 1)
                        {
                            result.print("processRemoteIncludeByServer");
                        }
                        else
                        {
                            result.print("processRemoteIncludeAutomatic");
                        }
                        result.print("((com.intershop.beehive.core.capi.request.ServletResponse)response," + url + ", "
                                        + username + ", " + password + ", \"" + tag.beginLine + "\");");
                        result.print("}\nfinally\n{\n");
                        result.print("    getTemplateExecutionConfig().setURLRewriteHandler(handler);\n}");
                    }
                    else if (extensionpoint != null)
                    {
                        result.print("processExtensionPoint((com.intershop.beehive.core.capi.request.ServletResponse)response,"
                                        + extensionpoint + ", " + dictName + ", \"" + tag.beginLine + "\");");
                    }

                    // end code block
                    result.print('}');
                    break;
                }
                case ISLOOP:
                {
                    result.print("while (loop(\"");

                    // process attribute iterator (required - value necessary)
                    if (hasValueAttribute(attributes, ATT_ITERATOR))
                    {
                        result.print((getValueAttribute(attributes, ATT_ITERATOR)));
                    }
                    else
                    {
                        throw new ParseException("Missing \"iterator\" attribute in ISLOOP.\n");
                    }

                    result.print('\"');
                    result.print(',');

                    // process attribute alias (optional - value necessary)
                    if (hasValueAttribute(attributes, ATT_ALIAS))
                    {
                        result.print('\"' + getValueAttribute(attributes, ATT_ALIAS) + '\"');
                    }
                    else
                    {
                        result.print("null");
                    }

                    result.print(',');

                    // process attribute counter (optional - value necessary)
                    if (hasValueAttribute(attributes, ATT_COUNTER))
                    {
                        result.print('\"' + getValueAttribute(attributes, ATT_COUNTER) + '\"');
                    }
                    else
                    {
                        result.print("null");
                    }

                    result.print(")) {");

                    nestingTable.add(tag);
                    break;
                }
                case ISLOOP_END:
                {
                    if (!checkNestingTopTag(nestingTable, ISLOOP))
                    {
                        throw new ParseException("Nesting Error: There is no corresponding ISLOOP for this /ISLOOP.\n");
                    }
                    result.print('}');

                    removeNestingTopTag(nestingTable);
                    break;
                }
                case ISMODULE:
                {
                    // check if part of ISPlacement tag
                    isInISPlacement(nestingTable, "ISMODULE");

                    String template, name;

                    // process attribute template (required - both value &
                    // expression allowed)
                    if (hasValueAttribute(attributes, ATT_TEMPLATE))
                    {
                        template = '\"' + getValueAttribute(attributes, ATT_TEMPLATE) + '\"';
                    }
                    else if (hasExpressionAttribute(attributes, ATT_TEMPLATE))
                    {
                        template = "context.getFormattedValue(" + getExpressionAttribute(attributes, ATT_TEMPLATE)
                                        + ",null)";
                    }
                    else
                    {
                        throw new ParseException("Missing \"template\" attribute in ISMODULE.\n");
                    }

                    // process attribute name (required - value necessary)
                    if (hasValueAttribute(attributes, ATT_NAME))
                    {
                        name = getValueAttribute(attributes, ATT_NAME);
                    }
                    else if (hasExpressionAttribute(attributes, ATT_NAME))
                    {
                        throw new ParseException("Attribute \"name\" in ISMODULE must not have an expression value.\n");
                    }
                    else
                    {
                        throw new ParseException("Missing \"name\" attribute in ISMODULE.\n");
                    }

                    boolean isStrict = false;
                    // process the strict attribute
                    if (hasValueAttribute(attributes, ATT_STRICT))
                    {
                        isStrict = Boolean.parseBoolean(getValueAttribute(attributes, ATT_STRICT));
                    }

                    name = name.toLowerCase();

                    // add declaration information to execution context
                    result.print("context.setCustomTagTemplateName(\"" + name + "\"," + template + "," + isStrict);

                    @SuppressWarnings("unchecked")
                    Collection<String> parameters = (Collection<String>)attributes.get(String.valueOf(ATT_ATTRIBUTE));
                    result.print(",");
                    if (parameters != null)
                    {
                        result.print("new String[]{");
                        String delim = "";
                        for(String parametername : parameters)
                        {
                            result.print(delim);
                            delim = ",";
                            result.print('\"');
                            result.print(parametername);
                            result.print('\"');
                        }
                        result.print("}");
                    }
                    else
                    {
                        result.print("null");
                    }

                    @SuppressWarnings("unchecked")
                    Collection<String> returnValues = (Collection<String>)attributes.get(String
                                    .valueOf(ATT_RETURNATTRIBUTE));
                    result.print(",");
                    if (returnValues != null)
                    {
                        result.print("new String[]{");
                        String delim = "";
                        for(String returnValue : returnValues)
                        {
                            if (!isStrict)
                            {
                                throw new ParseException(
                                                "ISMODULE declares a returnattribute, but is not declared as to be strict.\n");
                            }
                            if (parameters != null && parameters.contains(returnValue))
                            {
                                throw new ParseException("ISMODULE attributes and returnattributes must be distinct.\n");
                            }
                            result.print(delim);
                            delim = ",";
                            result.print('\"');
                            result.print(returnValue);
                            result.print('\"');
                        }
                        result.print("}");
                    }
                    else
                    {
                        result.print("null");
                    }

                    result.print(");");
                    break;
                }
                case ISNEXT:
                {
                    // check if ISNEXT is within ISLOOP
                    boolean isloop_flag = containsNestingTag(nestingTable, ISLOOP);

                    if (!isloop_flag)
                    {
                        throw new ParseException("ISNEXT outside ISLOOP.\n");
                    }

                    // check if ISNEXT is outside a loop if so log error message
                    result.print("if (getLoopStack().isEmpty())");
                    result.print('{');
                    result.print("Logger.error(");
                    result.print("this,\"ISNEXT occured outside ISLOOP. Line: {}\",");
                    result.print(tag.beginLine + ");"); // copy line of tag into
                    // code
                    result.print('}');

                    // if not check if iterator still has elements
                    result.print("else");
                    result.print('{');
                    result.print("LoopStackEntry stackEntry = getLoopStack().peek();");
                    result.print("if (stackEntry.getIterator().hasNext())");
                    result.print('{');
                    // if yes getnext Loop element
                    result.print("stackEntry.setLoopObject(stackEntry.getIterator().next());");
                    result.print('}');
                    result.print("else");
                    result.print('{');
                    // if not 'continue' while loop => ends loop and removes
                    // upper loop stack entry
                    result.print("continue;");
                    result.print('}');
                    result.print('}');
                    break;
                }
                case ISPRINT:
                {
                    String format;
                    boolean encode = true;
                    String encodings = null;

                    result.print('{');
                    result.print("String value = null;");
                    result.print("try{");

                    // process attribute encoding (optional - value necessary)
                    if (hasValueAttribute(attributes, ATT_ENCODE))
                    {
                        if (equalsAttribute(attributes, ATT_ENCODE, "on")
                                        || equalsAttribute(attributes, ATT_ENCODE, "html"))
                        {
                            encode = true;
                        }
                        else if (equalsAttribute(attributes, ATT_ENCODE, "off"))
                        {
                            encode = false;
                        }
                        else
                        {
                            encodings = getValueAttribute(attributes, ATT_ENCODE);
                            if (encodings.trim().isEmpty())
                            {
                                throw new ParseException("Attribute \"encoding\" in ISPRINT has an invalid value.\n");
                            }
                        }
                    }
                    else
                    {
                        // enabled by default
                        encode = true;
                    }

                    // process attribute value (required - expression necessary)
                    if (hasValueAttribute(attributes, ATT_VALUE))
                    {
                        throw new ParseException("Attribute \"value\"  in ISPRINT must have an expression value.\n");
                    }
                    else if (hasExpressionAttribute(attributes, ATT_VALUE))
                    {
                        result.print("value=context.getFormattedValue(" + getExpressionAttribute(attributes, ATT_VALUE)
                                        + ',');
                    }
                    else
                    {
                        throw new ParseException("Missing \"value\" attribute in ISPRINT.\n");
                    }

                    // process style attribute (optional - value necessary)
                    if (hasValueAttribute(attributes, ATT_STYLE))
                    {
                        format = "Integer.valueOf(" + getValueAttribute(attributes, ATT_STYLE) + ')';
                    }
                    else if (hasExpressionAttribute(attributes, ATT_STYLE))
                    {
                        throw new ParseException("Attribute \"style\" in ISPRINT must not have an expression value.\n");
                    }
                    else
                    {
                        format = null; // do nothing
                    }

                    if (format == null)
                    {
                        // process formatter attribute (optional - both value
                        // and expression allowed)
                        if (hasValueAttribute(attributes, ATT_FORMAT))
                        {
                            format = "\"" + getValueAttribute(attributes, ATT_FORMAT) + "\"";
                        }
                        else if (hasExpressionAttribute(attributes, ATT_FORMAT))
                        {
                            format = "context.getFormattedValue(" + getExpressionAttribute(attributes, ATT_FORMAT)
                                            + ",null)";
                        }
                        else
                        {
                            format = "null";
                        }
                    }
                    else
                    {
                        // ignore any formatter attribute
                    }

                    String symbols = "null";
                    if (hasValueAttribute(attributes, ATT_SYMBOLS))
                    {
                        symbols = "\"" + escapeAsJavaString(getValueAttribute(attributes, ATT_SYMBOLS)) + "\"";
                    }
                    else if (hasExpressionAttribute(attributes, ATT_SYMBOLS))
                    {
                        symbols = "context.getFormattedValue(" + getExpressionAttribute(attributes, ATT_SYMBOLS)
                                        + ",null)";
                    }

                    result.print(format + "," + symbols + ");");

                    // process padding attribute (optional - both value and
                    // expression allowed)
                    if (hasValueAttribute(attributes, ATT_PADDING))
                    {
                        int padding = (new Double(getValueAttribute(attributes, ATT_PADDING))).intValue();

                        result.print("value=pad(value," + padding + ");");
                    }
                    else if (hasExpressionAttribute(attributes, ATT_PADDING))
                    {
                        String padding = getExpressionAttribute(attributes, ATT_PADDING);

                        result.print("value=pad(value,((Number)(" + padding + ")).intValue());");
                    }
                    else
                    {
                        // do nothing
                    }

                    result.print("}catch(Exception e){value=null;");
                    result.print("Logger.error(");
                    result.print("this,\"ISPRINT has an invalid expression. Returning empty string. Line: {"
                                    + tag.beginLine + "}\",e);");
                    result.print('}'); // exception catch

                    result.print("if (value==null) value=\"\";");

                    // character entity encoding

                    if (encodings != null)
                    {
                        result.print("value = encodeString(value,\"" + encodings + "\");");
                    }
                    else if (encode)
                    {
                        result.print("value = encodeString(value);");
                    }

                    result.print("out.write(value);");
                    result.print('}');
                    break;
                }
                case ISREDIRECT:
                {
                    // check if part of ISPlacement tag
                    isInISPlacement(nestingTable, "ISREDIRECT");

                    String url;

                    // process attribute location (required - both value &
                    // expression allowed)
                    if (hasValueAttribute(attributes, ATT_LOCATION))
                    {
                        url = '\"' + getValueAttribute(attributes, ATT_LOCATION) + '\"';
                    }
                    else if (hasExpressionAttribute(attributes, ATT_LOCATION))
                    {
                        url = "context.getFormattedValue(" + getExpressionAttribute(attributes, ATT_LOCATION)
                                        + ",null)";
                    }
                    else
                    {
                        throw new ParseException("Missing \"location\" attribute in ISREDIRECT.\n");
                    }

                    // process method attribute (optional), default 302
                    handleHTTPStatus(result, attributes, ATT_HTTPSTATUS, "ISREDIRECT", 302, 300, 399);

                    result.print("response.setHeader(\"Location\", " + url + ");");

                    break;
                }
                case ISSELECT:
                {
                    // check if part of ISPlacement tag
                    isInISPlacement(nestingTable, "ISSELECT");

                    String name;
                    String condition;
                    String value;
                    String iterator;
                    String description;
                    boolean encode = true;
                    boolean disabled = hasValueAttribute(attributes, ATT_DISABLED)
                                    && equalsAttribute(attributes, ATT_DISABLED, "true");
                    String clazz = hasValueAttribute(attributes, ATT_CLASS) ? getValueAttribute(attributes, ATT_CLASS)
                                    : "";

                    result.print('{');

                    // process attribute name (required - both value &
                    // expression allowed)
                    if (hasValueAttribute(attributes, ATT_NAME))
                    {
                        name = getValueAttribute(attributes, ATT_NAME);
                        result.print("out.write(\"<\");");
                        if (disabled)
                        {
                            result.print("out.write(\"SELECT class=\\\"" + clazz + "\\\" NAME=\\\"" + name
                                            + "\\\" disabled=\\\"disabled\\\">\");");
                        }
                        else
                        {
                            result.print("out.write(\"SELECT class=\\\"" + clazz + "\\\" NAME=\\\"" + name
                                            + "\\\">\");");
                        }
                    }
                    else if (hasExpressionAttribute(attributes, ATT_NAME))
                    {
                        name = getExpressionAttribute(attributes, ATT_NAME);
                        result.print("out.write(\"<\");");
                        if (disabled)
                        {
                            result.print("out.write(\"SELECT class=\\\"" + clazz
                                            + "\\\" NAME=\\\"\"+context.getFormattedValue(" + name
                                            + ",null)+\"\\\" disabled=\\\"disabled\\\">\");");
                        }
                        else
                        {
                            result.print("out.write(\"SELECT class=\\\"" + clazz
                                            + "\\\" NAME=\\\"\"+context.getFormattedValue(" + name
                                            + ",null)+\"\\\">\");");
                        }
                    }
                    else
                    {
                        throw new ParseException("Missing \"name\" attribute in ISSELECT.\n");
                    }

                    // process attribute iterator (required - value necessary)
                    if (hasValueAttribute(attributes, ATT_ITERATOR))
                    {
                        iterator = getValueAttribute(attributes, ATT_ITERATOR);
                    }
                    else
                    {
                        throw new ParseException("Missing \"iterator\" attribute in ISSELECT.\n");
                    }

                    // process attribute condition (optional - expression
                    // necessary)
                    if (hasValueAttribute(attributes, ATT_CONDITION))
                    {
                        throw new ParseException(
                                        "Attribute \"condition\"  in ISSELECT must have an expression value.\n");
                    }
                    else if (hasExpressionAttribute(attributes, ATT_CONDITION))
                    {
                        condition = getExpressionAttribute(attributes, ATT_CONDITION);
                    }
                    else
                    {
                        condition = null;
                    }

                    // process attribute value (required - expression necessary)
                    if (hasValueAttribute(attributes, ATT_VALUE))
                    {
                        throw new ParseException("Attribute \"value\"  in ISSELECT must have an expression value.\n");
                    }
                    else if (hasExpressionAttribute(attributes, ATT_VALUE))
                    {
                        value = getExpressionAttribute(attributes, ATT_VALUE);
                    }
                    else
                    {
                        throw new ParseException("Missing \"value\" attribute in ISSELECT.\n");
                    }

                    // process attribute description (required - expression
                    // necessary)
                    if (hasValueAttribute(attributes, ATT_DESCRIPTION))
                    {
                        throw new ParseException(
                                        "Attribute \"description\" in ISSELECT must have an expression value.\n");
                    }
                    else if (hasExpressionAttribute(attributes, ATT_DESCRIPTION))
                    {
                        description = getExpressionAttribute(attributes, ATT_DESCRIPTION);
                    }
                    else
                    {
                        throw new ParseException("Missing \"description\" attribute in ISSELECT.\n");
                    }

                    // process attribute encoding (optional - value necessary)
                    if (hasValueAttribute(attributes, ATT_ENCODE))
                    {
                        if (equalsAttribute(attributes, ATT_ENCODE, "on"))
                        {
                            encode = true;
                        }
                        else if (equalsAttribute(attributes, ATT_ENCODE, "off"))
                        {
                            encode = false;
                        }
                        else
                        {
                            throw new ParseException("Attribute \"encoding\" in ISSELECT has a wrong value.\n");
                        }
                    }
                    else
                    {
                        // enabled by default
                        encode = true;
                    }

                    // loop options
                    result.print("String value, description;");
                    result.print("while (loop(\"" + iterator + "\",null))");
                    result.print('{');
                    result.print("out.write(\"<\");"); // this splitting is a
                    // workaround
                    result.print("out.write(\"OPTION \");"); // for a serious
                    // JHTML parser bug

                    if (condition != null)
                    {
                        // mark selected option
                        result.print("_boolean_result=false;");
                        result.print("try {");
                        result.print("_boolean_result=((Boolean)(" + condition + ")).booleanValue();");
                        result.print("} catch (Exception e) {");
                        result.print("Logger.debug(");
                        result.print("this,\"Boolean expression in line {} could not be evaluated. False returned. Consider using the 'isDefined' ISML function.\",");
                        result.print(tag.beginLine + ",e);"); // copy line of
                        // tag into code
                        result.print('}');
                        result.print("if (_boolean_result) {");
                        result.print("out.write(\"SELECTED \");");
                        result.print('}');
                    }

                    result.print("out.print(\"VALUE =\\\"\");");

                    // generate form value and description(HTML encode if
                    // allowed)
                    result.print("value = context.getFormattedValue(" + value + ",null);");
                    result.print("description = context.getFormattedValue(" + description + ",null);");
                    if (encode)
                    {
                        result.print("value = encodeString(value);");
                        result.print("description = encodeString(description);");
                    }// else proceed
                    result.print("out.write(value + \"\\\">\");");
                    result.print("out.write(description + \"</OPTION>\");");
                    result.print('}'); // loop
                    result.print("out.write(\"</SELECT>\");");

                    result.print('}'); // code block
                    break;
                }
                case ISSET:
                {
                    String value, name;
                    boolean hasRequestScope = false;

                    result.print('{');

                    // process attribute "value" (required - both value &
                    // expression allowed)
                    if (hasValueAttribute(attributes, ATT_VALUE))
                    {
                        value = getValueAttribute(attributes, ATT_VALUE);
                        result.print("Object temp_obj = (\"" + value + "\"); ");
                    }
                    else if (hasExpressionAttribute(attributes, ATT_VALUE))
                    {
                        value = getExpressionAttribute(attributes, ATT_VALUE);
                        result.print("Object temp_obj = (" + value + "); ");
                    }
                    else
                    {
                        throw new ParseException("Missing \"value\" attribute in ISSET.\n");
                    }

                    // process attribute "name" (required - value necessary)
                    if (hasValueAttribute(attributes, ATT_NAME))
                    {
                        name = getValueAttribute(attributes, ATT_NAME);
                    }
                    else if (hasExpressionAttribute(attributes, ATT_NAME))
                    {
                        throw new ParseException("Attribute \"name\" in ISSET must not have an expression value.\n");
                    }
                    else
                    {
                        throw new ParseException("Missing \"name\" attribute in ISSET.\n");
                    }

                    // process attribute "scope" (optional - value necessary)
                    if (hasValueAttribute(attributes, ATT_SCOPE))
                    {
                        if (equalsAttribute(attributes, ATT_SCOPE, "session"))
                        {
                            // switch on compacting mode on writer
                            hasRequestScope = false;
                        }
                        else if (equalsAttribute(attributes, ATT_SCOPE, "request"))
                        {
                            hasRequestScope = true;
                        }
                        else
                        {
                            throw new ParseException(
                                            "Attribute \"scope\" in ISSET must have a value of either \"request\" or \"session\".\n");
                        }
                    }
                    else if (hasExpressionAttribute(attributes, ATT_SCOPE))
                    {
                        throw new ParseException("Attribute \"scope\" in ISSET must not have an expression value.\n");
                    }
                    else
                    {
                        throw new ParseException("Missing \"scope\" attribute in ISSET.\n");

                    }

                    // save user defined variable either in session or in
                    // pipeline dictionary
                    if (hasRequestScope)
                    {
                        result.print("getPipelineDictionary().put(\"" + name + "\", temp_obj);");
                    }
                    else
                    {
                        result.print("((SessionMgr) NamingMgr.getInstance().lookupManager(SessionMgr.REGISTRY_NAME)).getCurrentSession().putObject(\"T_"
                                        + name + "\", temp_obj);");
                    }
                    result.print('}');
                    break;
                }
                case ISFILE:
                {
                    if (!checkNestingTopTag(nestingTable, ISFILEBUNDLE))
                    {
                        throw new ParseException("Nesting Error: The ISFILE tag is only in a ISFILEBUNDLE allowed.\n");
                    }

                    result.print("{\nString fileName = ");
                    // process attribute "file" (required - both value &
                    // expression allowed)
                    if (hasValueAttribute(attributes, ATT_NAME))
                    {
                        result.print("\"");

                        result.print(getValueAttribute(attributes, ATT_NAME));
                        result.print("\"");
                    }
                    else if (hasExpressionAttribute(attributes, ATT_NAME))
                    {
                        //context.getFormattedValue("/",null)
                        result.print("context.getFormattedValue(");
                        result.print(getExpressionAttribute(attributes, ATT_NAME));
                        result.print(",null)");
                    }
                    else
                    {
                        throw new ParseException("Missing \"name\" attribute in ISFILE.\n");
                    }
                    result.print(";\n");

                    // process attribute "processors" (required - both value &
                    // expression allowed)
                    String attProcessors = null;
                    if (hasValueAttribute(attributes, ATT_PROCESSORS))
                    {
                        attProcessors = getValueAttribute(attributes, ATT_PROCESSORS);
                    }
                    else if (hasExpressionAttribute(attributes, ATT_PROCESSORS))
                    {
                        attProcessors = getExpressionAttribute(attributes, ATT_PROCESSORS);
                    }

                    // add processors
                    result.print("String[] processors = null;");
                    if (attProcessors != null)
                    {
                        result.print("processors = new String[]{");
                        String[] processors = attProcessors.split(",");
                        for(int i = 0; i < processors.length; i++)
                        {
                            if (i != 0)
                            {
                                result.print(",");
                            }
                            result.print("\"" + processors[i].trim() + "\"");
                        }
                        result.print("};");
                    }

                    boolean isInBundle = containsNestingTag(nestingTable, ISFILEBUNDLE);

                    if (!isInBundle)
                        result.print("ISFileBundle filebundle = new ISFileBundle(fileName);\n");
                    result.print("filebundle.addResource(fileName, processors);\n");
                    if (!isInBundle)
                        result.print("out.write(filebundle.toHTML());\n");

                    result.print("}\n");
                    break;
                }
                case ISRENDER:
                {
                    if (!checkNestingTopTag(nestingTable, ISFILEBUNDLE))
                    {
                        throw new ParseException("Nesting Error: The ISRENDER tag is only in a ISFILEBUNDLE allowed.\n");
                    }

                    result.print("}");

                    // setup parameters for the renderer
                    result.print("TagParameter[] parameters = new TagParameter[] {\n");
                    Iterator<String> keys = attributes.keySet().iterator();
                    while(keys.hasNext())
                    {
                        String attributeKey = keys.next();
                        Object attributeValue = attributes.get(attributeKey);
                        if (attributeValue instanceof StringBuilder)
                        {
                            // for ISML expressions
                            result.print("new TagParameter(\"" + attributeKey + "\"," + attributeValue.toString() + ")");
                        }
                        else
                        {
                            // for simple attributes
                            result.print("new TagParameter(\"" + attributeKey + "\",\"" + attributeValue + "\")");
                        }

                        if (keys.hasNext())
                        {
                            result.print(",\n");
                        }
                    }
                    result.print("};");

                    // create the renderer
                    result.print("\nCustomTag renderer = new CustomTag() {{\n");
                    result.print("isStrict = true;\n");
                    result.print("tagName = \"FileBundleRenderer\";\n");
                    result.print("}\n");
                    result.print("public void processOpenTag(PageContext pageContext, com.intershop.beehive.core.capi.request.ServletResponse response, AbstractTemplate template, int line) throws IOException, ServletException {\n");
                    result.print("ServletContext application = pageContext.getServletContext();\n");
                    result.print("ServletConfig config = pageContext.getServletConfig();\n");
                    result.print("JspWriter out = pageContext.getOut();\n");
                    result.print("Object page = template;\n");
                    result.print("TemplateExecutionConfig context = getTemplateExecutionConfig();");

                    nestingTable.add(tag);

                    break;
                }
                case ISRENDER_END:
                {
                    if (!checkNestingTopTag(nestingTable, ISRENDER))
                    {
                        throw new ParseException(
                                        "Nesting Error: There is no corresponding ISRENDER for this /ISRENDER.\n");
                    }

                    result.print("\n}};\n");

                    // adding resources aren't necessary if the target resources
                    // are cached
                    result.print("if (processesResources) {");

                    removeNestingTopTag(nestingTable);
                    break;
                }
                case ISFILEBUNDLE:
                {
                    // process attribute "name" (required - both value &
                    // expression allowed)
                    String name = null;
                    if (hasValueAttribute(attributes, ATT_NAME))
                    {
                        name = getValueAttribute(attributes, ATT_NAME);
                    }
                    else if (hasExpressionAttribute(attributes, ATT_NAME))
                    {
                        name = getExpressionAttribute(attributes, ATT_NAME);
                    }
                    else
                    {
                        throw new ParseException("Missing \"name\" attribute in ISBUNDLE.\n");
                    }

                    // process attribute "processors" (required - both value &
                    // expression allowed)
                    String attProcessors = null;
                    if (hasValueAttribute(attributes, ATT_PROCESSORS))
                    {
                        attProcessors = getValueAttribute(attributes, ATT_PROCESSORS);
                    }
                    else if (hasExpressionAttribute(attributes, ATT_PROCESSORS))
                    {
                        attProcessors = getExpressionAttribute(attributes, ATT_PROCESSORS);
                    }
                    else
                    {
                        throw new ParseException("Missing \"processers\" attribute in ISBUNDLE.\n");
                    }

                    result.print("{ ISFileBundle filebundle = new ISFileBundle(\"" + name + "\");");

                    // adding resources and processors aren't necessary if the
                    // target resources are cached
                    result.print("List<? extends Resource> resources = null;\n");
                    result.print("boolean processesResources = (filebundle.isCheckSource() || !filebundle.hasCachedResources());");
                    result.print("if (processesResources) {");

                    // add processors
                    String[] processors = attProcessors.split(",");
                    result.print("filebundle.setDefaultProcessors(new String[]{");
                    for(int i = 0; i < processors.length; i++)
                    {
                        if (i != 0)
                        {
                            result.print(",");
                        }
                        result.print("\"" + processors[i].trim() + "\"");
                    }
                    result.print("});");

                    nestingTable.add(tag);

                    break;
                }
                case ISFILEBUNDLE_END:
                {
                    if (!checkNestingTopTag(nestingTable, ISFILEBUNDLE))
                    {
                        throw new ParseException(
                                        "Nesting Error: There is no corresponding ISFILEBUNDLE for this /ISFILEBUNDLE.\n");
                    }
                    result.print("resources = filebundle.process();\n");
                    result.print("} else {");
                    result.print("resources = filebundle.getChachedResources();\n");
                    result.print("}");
                    result.print("for(Resource resource : resources) {\n");

                    // setup new dictionary
                    result.print("PipelineDictionary newDict = context.createPipelineDictionary();\n");
                    result.print("newDict.put(\"File\", resource);\n");
                    result.print("for(TagParameter parameter : parameters) {");
                    result.print("newDict.put(parameter.getKey(), parameter.getValue());");
                    result.print("}");
                    result.print("context.pushPipelineDictionary(newDict);");

                    // execute the renderer
                    result.print("renderer.processOpenTag(pageContext, (com.intershop.beehive.core.capi.request.ServletResponse) response, this, "
                                    + tag.beginLine + ");\n");
                    result.print("renderer.processCloseTag(pageContext, (com.intershop.beehive.core.capi.request.ServletResponse) response, this, "
                                    + tag.beginLine + ");\n");

                    // restore old dictionary
                    result.print("context.popPipelineDictionary();");
                    result.print("}}");

                    removeNestingTopTag(nestingTable);
                    break;
                }
                case ISFORM:
                {
                    final String FORM_SECURE = "secure"; 
                    final String FORM_SITE = "site";  
                    final String FORM_SERVERGROUP = "servergroup";  
                    final String FORM_ACTION = "action";  
                    final String FORM_METHOD = "method"; 

                    String site = null, serverGroup = null, action = null, method = null;
                    Boolean secure = null;
                    
                    StringBuilder formAttr = new StringBuilder();
                    
                    for (String key : attributes.keySet())
                    {
                        if (isFormAttribute(key, FORM_SECURE))
                        {
                            if (equalsFormAttribute(attributes, key, "true"))
                            { 
                                secure = Boolean.TRUE;
                            }
                            else if (equalsFormAttribute(attributes, key, "false"))
                            {
                                secure = Boolean.FALSE;
                            }
                            else
                            {
                                throw new ParseException("Attribute \"secure\" in <ISFORM> has a wrong value.\n");
                            }
                        }
                        else if (isFormExpressionAttribute(key, FORM_SECURE))
                        {
                            throw new ParseException(
                                "Attribute \"secure\" in <ISFORM> must not have an expression value.\n");
                        }

                        // process attribute site (required - both value &
                        // expression allowed)
                        else if (isFormAttribute(key, FORM_SITE))
                        {
                            site = '\"' + getFormAttribute(attributes, key) + '\"';
                        }
                        else if (isFormExpressionAttribute(key, FORM_SITE))
                        {
                            site = "context.getFormattedValue(" + getFormAttribute(attributes, key) + ",null)";
                        }
                
                        // process attribute servergroup (required - both value &
                        // expression allowed)
                        else if (isFormAttribute(key, FORM_SERVERGROUP))
                        {
                            serverGroup = '\"' + getFormAttribute(attributes, key) + '\"';
                        }
                        else if (isFormExpressionAttribute(key, FORM_SERVERGROUP))
                        {
                            serverGroup = "context.getFormattedValue(" + getFormAttribute(attributes, key) + ",null)";
                        }

                        // process method
                        else if (isFormAttribute(key, FORM_METHOD))
                        {
                            method = getFormAttribute(attributes, key);
                        }
                        else if (isFormExpressionAttribute(key, FORM_METHOD))
                        {
                            throw new ParseException(
                                            "Attribute \"method\" in <ISFORM> must not have an expression value.\n");
                        }
                        
                        // get the values of all other attributes
                        else
                        {
                            if (isFormAttribute(key, FORM_ACTION))
                            {
                                action = "\"" + getFormAttribute(attributes, key) + "\"";
                            }
                            else if (isFormExpressionAttribute(key, FORM_ACTION))
                            {
                                action = "context.getFormattedValue(" + getFormAttribute(attributes, key) + ",null)";
                            }

                            formAttr.append(writeFormAttribute(attributes, key));
                        }
                    }
                    
                    // validate method and secure
                    if (method == null)
                    {
                        if (secure == null ) secure = Boolean.FALSE;
                        if (!secure.booleanValue())
                        {
                            method = "GET";
                        }
                        else
                        {
                            throw new ParseException(
                                            "Attribute \"secure\" in <ISFORM> cannot be true for default method GET.\n");
                        }
                    }
                    else if (method.toUpperCase().equals("GET"))
                    {
                        if (secure == null ) secure = Boolean.FALSE;
                        if ( secure.booleanValue() )
                        {
                            throw new ParseException(
                                            "Attribute \"secure\" in <ISFORM> cannot be true for method GET.\n");
                        }
                    }
                    else if (method.toUpperCase().equals("POST"))
                    {
                        if (secure == null ) secure = Boolean.TRUE;
                    }
                    else
                    {
                        throw new ParseException(
                                        "Method \"" + method + "\" in <ISFORM> is not allowed.\n");
                    }
                    
                    String varId = String.valueOf(formCount.incrementAndGet()).replace("-", "_");
                    
                    // Prepare site and servergroup in JSP
                    result.print("URLPipelineAction action" + varId + " = new URLPipelineAction(" + action + ");");
                    result.print("String site" + varId + " = null;");
                    result.print("String serverGroup" + varId + " = null;");
                    result.print("String actionValue" + varId + " = " + action + ";");
                    if (site != null )
                    {
                        result.print( "site" + varId + " = \"" + site + "\";");
                    }
                    result.print("if (site" + varId + " == null)");
                    result.print("{" );
                    result.print("  site" + varId + " = action" + varId + ".getDomain();");
                    result.print("  if (site" + varId + " == null)");
                    result.print("  {");
                    result.print("      site" + varId + " = com.intershop.beehive.core.capi.request.Request.getCurrent().getRequestSite().getDomainName();");
                    result.print("  }");
                    result.print("}");
                    if ( serverGroup != null )
                    {
                        result.print("");
                    }
                    result.print("if (serverGroup" + varId + " == null)");
                    result.print("{");
                    result.print("  serverGroup" + varId + " = action" + varId + ".getServerGroup();");
                    result.print("  if (serverGroup" + varId + " == null)");
                    result.print("  {");
                    result.print("      serverGroup" + varId + " = com.intershop.beehive.core.capi.request.Request.getCurrent().getRequestSite().getServerGroup();");
                    result.print("  }");
                    result.print("}");

                    // Create the form
                    result.print("out.print(\"<form\");"); 
                    
                    // Write the method
                    result.print("out.print(\" method=\\\"\");"); 
                    result.print("out.print(\"");
                    result.print(method);
                    result.print("\");");
                    result.print("out.print(\"\\\"\");");

                    // Write the form attributes
                    result.print(formAttr.toString());

                    // Close the form tag
                    result.print("out.print(\">\");");

                    // Add the hidden field for the WACSRF tag
                    result.print("out.print(context.prepareWACSRFTag(actionValue" + varId + ", site" + varId + ", serverGroup" + varId + "," + secure.booleanValue() + "));");
                    
                    nestingTable.add(tag);
                    break;
                }
                case ISFORM_END:
                {
                    if (!checkNestingTopTag(nestingTable, ISFORM))
                    {
                        throw new ParseException(
                                        "Nesting Error: There is no corresponding <ISFORM> for this </ISFORM>.\n");
                    }

                    result.print("out.print(\"</form>\");");
                    removeNestingTopTag(nestingTable);
                    break;
                }
                case ISBINARY:
                {
                    // check if part of ISPlacement tag
                    isInISPlacement(nestingTable, "ISBINARY");

                    String file = null, resource = null, stream = null, bytes = null, downloadName = null;

                    // process attribute file (required - both value &
                    // expression allowed)
                    if (hasValueAttribute(attributes, ATT_FILE))
                    {
                        file = '\"' + getValueAttribute(attributes, ATT_FILE) + '\"';
                    }
                    else if (hasExpressionAttribute(attributes, ATT_FILE))
                    {
                        file = "context.getFormattedValue(" + getExpressionAttribute(attributes, ATT_FILE) + ",null)";
                    }

                    // process attribute stream (required - only expression
                    // allowed)
                    if (hasValueAttribute(attributes, ATT_STREAM))
                    {
                        throw new ParseException("Attribute \"stream\" in ISBINARY must not have a value.\n");
                    }
                    else if (hasExpressionAttribute(attributes, ATT_STREAM))
                    {
                        stream = "((java.io.InputStream)(" + getExpressionAttribute(attributes, ATT_STREAM) + "))";
                    }

                    // process attribute resource (required - both value &
                    // expression allowed)
                    if (hasValueAttribute(attributes, ATT_RESOURCE))
                    {
                        resource = '\"' + getValueAttribute(attributes, ATT_RESOURCE) + '\"';
                    }
                    else if (hasExpressionAttribute(attributes, ATT_RESOURCE))
                    {
                        resource = "context.getFormattedValue(" + getExpressionAttribute(attributes, ATT_RESOURCE)
                                        + ",null)";
                    }

                    // process attribute bytes (required - only expression
                    // allowed)
                    if (hasValueAttribute(attributes, ATT_BYTES))
                    {
                        throw new ParseException("Attribute \"bytes\" in ISBINARY must not have a value.\n");
                    }
                    else if (hasExpressionAttribute(attributes, ATT_BYTES))
                    {
                        bytes = "((byte[])(" + getExpressionAttribute(attributes, ATT_BYTES) + "))";
                    }

                    result.print('{');

                    // process attribute downloadname (optional - both value &
                    // expression allowed)
                    if (hasValueAttribute(attributes, ATT_DOWNLOADNAME))
                    {
                        downloadName = getValueAttribute(attributes, ATT_DOWNLOADNAME);

                    }
                    else if (hasExpressionAttribute(attributes, ATT_DOWNLOADNAME))
                    {
                        downloadName = "\" + context.getFormattedValue("
                                        + getExpressionAttribute(attributes, ATT_DOWNLOADNAME) + ",null) + \"";
                    }

                    if (downloadName != null)
                    {
                        result.print("response.setHeader(\"Content-Disposition\", \"attachment; filename=\\\""
                                        + downloadName + "\\\"\");");
                    }

                    if (file != null)
                    {
                        if ((stream != null) || (resource != null) || (bytes != null))
                        {
                            throw new ParseException(
                                            "Only one attribute \"file\",\"stream\",\"resource\" or \"bytes\" is allowed in ISBINARY.\n");
                        }
                        // quote backslashes on windows
                        String quotedFilename = "";
                        StringTokenizer st = new StringTokenizer(file, "\\");
                        while(st.hasMoreTokens())
                        {
                            quotedFilename += st.nextToken();
                            if (st.hasMoreTokens())
                            {
                                quotedFilename += "\\\\";
                            }
                        }
                        result.print("processBinaryOutputFile((com.intershop.beehive.core.capi.request.ServletResponse)response,new File("
                                        + quotedFilename + "));");
                    }
                    else if (stream != null)
                    {
                        if ((file != null) || (resource != null) || (bytes != null))
                        {
                            throw new ParseException(
                                            "Only one attribute \"file\",\"stream\",\"resource\" or \"bytes\" is allowed in ISBINARY.\n");
                        }
                        result.print("processBinaryOutputStream((com.intershop.beehive.core.capi.request.ServletResponse)response,"
                                        + stream + ");");
                    }
                    else if (resource != null)
                    {
                        if ((stream != null) || (file != null) || (bytes != null))
                        {
                            throw new ParseException(
                                            "Only one attribute \"file\",\"stream\",\"resource\" or \"bytes\" is allowed in ISBINARY.\n");
                        }
                        result.print("processBinaryOutputResource((com.intershop.beehive.core.capi.request.ServletResponse)response,"
                                        + resource + ");");
                    }
                    else if (bytes != null)
                    {
                        if ((stream != null) || (file != null) || (resource != null))
                        {
                            throw new ParseException(
                                            "Only one attribute \"file\",\"stream\",\"resource\" or \"bytes\" is allowed in ISBINARY.\n");
                        }
                        result.print("processBinaryOutputBytes((com.intershop.beehive.core.capi.request.ServletResponse)response,"
                                        + bytes + ");");
                    }
                    else
                    {
                        throw new ParseException(
                                        "No attribute \"file\",\"stream\",\"resource\" or \"bytes\" given in ISBINARY.\n");
                    }
                    result.print('}');
                    break;
                }
                case ISPIPELINE:
                {
                    // check if part of ISPlacement tag
                    isInISPlacement(nestingTable, "ISPIPELINE");

                    String pipeline = null, params = null, alias = null;

                    // process attribute pipeline (required - value or
                    // expression)
                    if (hasValueAttribute(attributes, ATT_PIPELINE))
                    {
                        pipeline = '\"' + getValueAttribute(attributes, ATT_PIPELINE) + '\"';
                    }
                    else if (hasExpressionAttribute(attributes, ATT_PIPELINE))
                    {
                        pipeline = "context.getFormattedValue(" + getExpressionAttribute(attributes, ATT_PIPELINE)
                                        + ",null)";
                    }
                    else
                    {
                        throw new ParseException("Missing \"pipeline\" attribute in ISPIPELINE.\n");
                    }

                    // process attribute params (optional - expression
                    // necessary)
                    if (hasValueAttribute(attributes, ATT_PARAMS))
                    {
                        throw new ParseException("Attribute \"params\" in ISPIPELINE must not have a value.\n");
                    }
                    else if (hasExpressionAttribute(attributes, ATT_PARAMS))
                    {
                        params = "((java.util.Map)(" + getExpressionAttribute(attributes, ATT_PARAMS) + "))";
                    }
                    else
                    {
                        // fix for #10885: no param given, use empty map
                        params = "java.util.Collections.emptyMap()";
                    }

                    // process attribute alias (required - value or expression
                    if (hasValueAttribute(attributes, ATT_ALIAS))
                    {
                        alias = '\"' + getValueAttribute(attributes, ATT_ALIAS) + '\"';
                    }
                    else if ((hasExpressionAttribute(attributes, ATT_ALIAS)))
                    {
                        alias = "context.getFormattedValue(" + getExpressionAttribute(attributes, ATT_ALIAS) + ",null)";
                    }
                    else
                    {
                        throw new ParseException("Missing \"alias\" attribute in ISPIPELINE.\n");
                    }

                    result.print("{try{executePipeline(" + pipeline + "," + params + "," + alias + ");");
                    result.print("}catch(Exception e){");
                    result.print("Logger.error(");
                    result.print("this,");
                    result.print("\"ISPIPELINE failed. Line: " + tag.beginLine + ".\",e);"); // copy
                    // exception
                    // and
                    // message
                    result.print('}'); // exception catch
                    result.print('}'); // code block
                    break;
                }
                case ISTEXT:
                {
                    // check if part of ISPlacement tag
                    isInISPlacement(nestingTable, "ISTEXT");

                    String key = null;
                    String encoding;
                    String locale = null;
                    String parameter0 = null;
                    String parameter1 = null;
                    String parameter2 = null;
                    String parameter3 = null;
                    String parameter4 = null;
                    String parameter5 = null;
                    String parameter6 = null;
                    String parameter7 = null;
                    String parameter8 = null;
                    String parameter9 = null;
                    // process attribute key(REQUIRED)
                    if (hasValueAttribute(attributes, ATT_KEY))
                    {
                        key = '\"' + getValueAttribute(attributes, ATT_KEY) + '\"';
                    }
                    else if (hasExpressionAttribute(attributes, ATT_KEY))
                    {
                        key = "context.getFormattedValue(" + getExpressionAttribute(attributes, ATT_KEY) + ",null)";
                    }
                    else
                    {
                        throw new ParseException("Missing \"key\" attribute in ISTEXT.\n");
                    }

                    // process attribute encoding (optional - value necessary)
                    if (hasValueAttribute(attributes, ATT_ENCODE))
                    {
                        if (equalsAttribute(attributes, ATT_ENCODE, "on")
                                        || equalsAttribute(attributes, ATT_ENCODE, "html"))
                        {
                            encoding = "\"\"";
                        }
                        else if (equalsAttribute(attributes, ATT_ENCODE, "off"))
                        {
                            encoding = null;
                        }
                        else
                        {
                            encoding = '\"' + getValueAttribute(attributes, ATT_ENCODE) + '\"';
                            if (encoding.trim().isEmpty())
                            {
                                throw new ParseException("Attribute \"encoding\" in ISTEXT has an invalid value.\n");
                            }
                        }
                    }
                    else
                    {
                        // enabled by default
                        encoding = "\"\"";
                    }

                    // locale
                    if (hasValueAttribute(attributes, ATT_LOCALE))
                    {
                        locale = '\"' + getValueAttribute(attributes, ATT_LOCALE) + '\"';
                    }
                    else if (hasExpressionAttribute(attributes, ATT_LOCALE))
                    {
                        locale = getExpressionAttribute(attributes, ATT_LOCALE);
                    }
                    // param0
                    if (hasValueAttribute(attributes, ATT_PARAMETER0))
                    {
                        parameter0 = '\"' + getValueAttribute(attributes, ATT_PARAMETER0) + '\"';
                    }
                    else if (hasExpressionAttribute(attributes, ATT_PARAMETER0))
                    {
                        parameter0 = getExpressionAttribute(attributes, ATT_PARAMETER0);
                    }
                    // param1
                    if (hasValueAttribute(attributes, ATT_PARAMETER1))
                    {
                        parameter1 = '\"' + getValueAttribute(attributes, ATT_PARAMETER1) + '\"';
                    }
                    else if (hasExpressionAttribute(attributes, ATT_PARAMETER1))
                    {
                        parameter1 = getExpressionAttribute(attributes, ATT_PARAMETER1);
                    }
                    // param2
                    if (hasValueAttribute(attributes, ATT_PARAMETER2))
                    {
                        parameter2 = '\"' + getValueAttribute(attributes, ATT_PARAMETER2) + '\"';
                    }
                    else if (hasExpressionAttribute(attributes, ATT_PARAMETER2))
                    {
                        parameter2 = getExpressionAttribute(attributes, ATT_PARAMETER2);
                    }
                    // param3
                    if (hasValueAttribute(attributes, ATT_PARAMETER3))
                    {
                        parameter3 = '\"' + getValueAttribute(attributes, ATT_PARAMETER3) + '\"';
                    }
                    else if (hasExpressionAttribute(attributes, ATT_PARAMETER3))
                    {
                        parameter3 = getExpressionAttribute(attributes, ATT_PARAMETER3);
                    }
                    // param4
                    if (hasValueAttribute(attributes, ATT_PARAMETER4))
                    {
                        parameter4 = '\"' + getValueAttribute(attributes, ATT_PARAMETER4) + '\"';
                    }
                    else if (hasExpressionAttribute(attributes, ATT_PARAMETER4))
                    {
                        parameter4 = getExpressionAttribute(attributes, ATT_PARAMETER4);
                    }
                    // param5
                    if (hasValueAttribute(attributes, ATT_PARAMETER5))
                    {
                        parameter5 = '\"' + getValueAttribute(attributes, ATT_PARAMETER5) + '\"';
                    }
                    else if (hasExpressionAttribute(attributes, ATT_PARAMETER5))
                    {
                        parameter5 = getExpressionAttribute(attributes, ATT_PARAMETER5);
                    }
                    // param6
                    if (hasValueAttribute(attributes, ATT_PARAMETER6))
                    {
                        parameter6 = '\"' + getValueAttribute(attributes, ATT_PARAMETER6) + '\"';
                    }
                    else if (hasExpressionAttribute(attributes, ATT_PARAMETER6))
                    {
                        parameter6 = getExpressionAttribute(attributes, ATT_PARAMETER6);
                    }
                    // param7
                    if (hasValueAttribute(attributes, ATT_PARAMETER7))
                    {
                        parameter7 = '\"' + getValueAttribute(attributes, ATT_PARAMETER7) + '\"';
                    }
                    else if (hasExpressionAttribute(attributes, ATT_PARAMETER7))
                    {
                        parameter7 = getExpressionAttribute(attributes, ATT_PARAMETER7);
                    }
                    // param8
                    if (hasValueAttribute(attributes, ATT_PARAMETER8))
                    {
                        parameter8 = '\"' + getValueAttribute(attributes, ATT_PARAMETER8) + '\"';
                    }
                    else if (hasExpressionAttribute(attributes, ATT_PARAMETER8))
                    {
                        parameter8 = getExpressionAttribute(attributes, ATT_PARAMETER8);
                    }
                    // param9
                    if (hasValueAttribute(attributes, ATT_PARAMETER9))
                    {
                        parameter9 = '\"' + getValueAttribute(attributes, ATT_PARAMETER9) + '\"';
                    }
                    else if (hasExpressionAttribute(attributes, ATT_PARAMETER9))
                    {
                        parameter9 = getExpressionAttribute(attributes, ATT_PARAMETER9);
                    }

                    result.print("{out.write(localizeISText(" + key + "," + encoding + "," + locale + "," + parameter0
                                    + "," + parameter1 + "," + parameter2 + "," + parameter3 + "," + parameter4 + ","
                                    + parameter5 + "," + parameter6 + "," + parameter7 + "," + parameter8 + ","
                                    + parameter9 + "));");
                    result.print('}');
                    break;
                }
                case ISX: // custom tags
                {
                    // check if part of ISPlacement tag
                    isInISPlacement(nestingTable, "ISX");

                    String name = tag.toString().substring(3).toLowerCase();
                    result.print("processOpenTag(response, pageContext, \"" + name + "\", new TagParameter[] {\n");

                    Iterator<String> keys = attributes.keySet().iterator();
                    while(keys.hasNext())
                    {
                        String attributeKey = keys.next();
                        Object attributeValue = attributes.get(attributeKey);
                        if (attributeValue instanceof StringBuilder)
                        {
                            // for ISML expressions
                            result.print("new TagParameter(\"" + attributeKey + "\"," + attributeValue.toString() + ")");
                        }
                        else
                        {
                            // for simple attributes
                            result.print("new TagParameter(\"" + attributeKey + "\",\"" + attributeValue + "\")");
                        }

                        if (keys.hasNext())
                        {
                            result.print(",\n");
                        }
                    }

                    result.print("}, " + tag.beginLine + ");");

                    break;
                }
                case ISX_END: // custom end tags
                {
                    // check if part of ISPlacement tag
                    isInISPlacement(nestingTable, "ISX_END");

                    String name = tag.toString().substring(4).toLowerCase();
                    result.print("processCloseTag(response, pageContext, \"" + name + "\", " + tag.beginLine + ");\n");

                    break;
                }
                case ISPLACEHOLDER: // ISPLACEHOLDER waplaceholder mapping rule
                {
                    // check if part of ISPlacement tag
                    isInISPlacement(nestingTable, "ISPLACEHOLDER");

                    String id = getExpressionAttributeString(attributes, ATT_ID);

                    // check for required ID attribute
                    if (null == id || (id.trim().length() == 0))
                    {
                        throw new ParseException("Missing 'id' attribute on <isplaceholder>.");
                    }

                    String prepend = getExpressionAttributeString(attributes, ATT_PREPEND);
                    String separator = getExpressionAttributeString(attributes, ATT_SEPARATOR);
                    String append = getExpressionAttributeString(attributes, ATT_APPEND);

                    String preserveOrder = getExpressionAttributeString(attributes, ATT_PRESERVEORDER);
                    String removeDuplicates = getExpressionAttributeString(attributes, ATT_REMOVEDUPLICATES);

                    String s = "out.print(context.prepareWAPlaceHolder(" + id + ", " + prepend + ", " + separator
                                    + ", " + append + ", " + preserveOrder + ", " + removeDuplicates + "));";

                    result.print(s);
                    break;
                }
                case ISPLACEMENT:
                {
                    // check if part of ISPlacement tag
                    isInISPlacement(nestingTable, "ISPLACEMENT");

                    String placeholderID = getExpressionAttributeString(attributes, ATT_PLACEHOLDERID);

                    // check for required placeholderID attribute
                    if (null == placeholderID || (placeholderID.trim().length() == 0))
                    {
                        throw new ParseException("Missing 'placeholderid' attribute on <isplacement> line "
                                        + tag.beginLine + ", column " + tag.beginColumn + ".");
                    }

                    result.print("out.print(context.prepareWAPlacement(" + placeholderID + "));");

                    // add ISPLACEMENT to nesting table
                    nestingTable.add(tag);

                    break;
                }
                case ISPLACEMENT_END:
                {
                    if (!checkNestingTopTag(nestingTable, ISPLACEMENT))
                    {
                        throw new ParseException(
                                        "Nesting Error: There is no corresponding <ISPLACEMENT> for this </ISPLACEMENT>.\n");
                    }

                    result.print("out.print(\"</waplacement>\");");

                    removeNestingTopTag(nestingTable);
                    break;
                }               
                default:
                    throw new ParseException("Invalid tag (\"" + tag + "\")");
            }
            result.print(ISMLtoJSPcompiler.SCRIPTING_END);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            throw new ParseException("Error in ISML tag in line " + tag.beginLine + ", column " + tag.beginColumn
                            + ":\n" + e.getMessage());
        }
    }

    /**
     * Sets the header (optional opertaion)
     * 
     * <pre>
     * X - IS - HTTPResponseStatus
     * </pre>
     * 
     * that is used by the webadapter to determine the HTTP status code of the
     * response sent to the client.
     * 
     * @param result
     *            the writer used to write the result
     * @param attributes
     * @param attributeID
     *            the ID of the attribute where the code or ISML expression is
     * @param defaultStatus
     *            the fallback HTTP status in case that the attribute is missing
     *            (optional)
     * @param minStatus
     *            the minimal number accepted as status
     * @param maxStatus
     *            the maximal number accepted as status
     * 
     * @throws ParseException
     * @throws IOException
     */
    private static void handleHTTPStatus(CompactingWriter result, Map<String, Object> attributes, int attributeID,
                    String tagName, Integer defaultStatus, Integer minStatus, Integer maxStatus) throws ParseException,
                    IOException
    {

        String methodString = defaultStatus != null ? String.valueOf(defaultStatus) : null;

        minStatus = minStatus != null ? minStatus : 0;
        maxStatus = maxStatus != null ? maxStatus : Integer.MAX_VALUE;

        try
        {
            if (hasValueAttribute(attributes, attributeID))
            {
                int method = Integer.parseInt(getValueAttribute(attributes, attributeID));
                if (method < minStatus || method > maxStatus)
                {
                    throw new NumberFormatException("Out of range!");
                }
                methodString = String.valueOf(method);
            }
            else if (hasExpressionAttribute(attributes, attributeID))
            {
                methodString = "((Number)(" + getExpressionAttribute(attributes, attributeID) + ")).intValue()";
            }
        }
        catch(NumberFormatException e)
        {
            throw new ParseException("Only numeric values [" + minStatus + " , " + maxStatus
                            + "] or ISML expressions are allowed for the \"httpstatus\" attribute of tag " + tagName
                            + ".\n");
        }

        // handle the satus code if specified
        if (methodString != null)
        {
            result.print("int _httpStatusCode = " + methodString + ";\n");
            result.print("if (_httpStatusCode < " + minStatus + " || _httpStatusCode > " + maxStatus + ") \n");
            result.print("{\n");
            result.print("    throw new ServletException(\n");
            result.print("      \"Redirection error in template \"+getTemplateExecutionConfig().getTemplateName()+\". \" + \n");
            result.print("      \"Unsupported HTTP status code \" + _httpStatusCode + \". Supported interval ["
                            + minStatus + ", " + maxStatus + "]\");\n");
            result.print("}");

            // Webadapter docs:
            // X-IS-HTTPResponseStatus: <HTTP status code>
            // Set the requested status code with the aggregated client
            // response.
            // In <wainclude> trees, the last header set this way wins.

            result.print("response.setHeader(\"X-IS-HTTPResponseStatus\", String.valueOf(_httpStatusCode));");
        }
    }

    /**
     * Helper method to check the current tag level to be content of an
     * ISPLACEMENT tag. Not all ISML tags are allowed.
     * 
     * @param nestingTable
     * @param token
     * @throws ParseException
     */
    private static void isInISPlacement(List<Token> nestingTable, String token) throws ParseException
    {
        if (containsNestingTag(nestingTable, ISPLACEMENT))
        {
            throw new ParseException("Tag " + token + " not allowed in ISPLACEMENT.");
        }
    }

    protected static String getExpressionAttributeString(Map<String, Object> attributes, int attribute)
    {
        String value = null;
        if (hasValueAttribute(attributes, attribute))
        {
            value = "\"" + getValueAttribute(attributes, attribute) + "\"";
        }
        else if (hasExpressionAttribute(attributes, attribute))
        {
            value = getExpressionAttribute(attributes, attribute);
        }
        return value;
    }

    /*-------------------------------------------------------------------------
                            Private Helper Methods
    -------------------------------------------------------------------------*/

    /**
     * This is a convenience method for the compileTag() method. It checks
     * wether a special attribute was specified in the tag body or not AND if it
     * has a simple String value.
     * 
     * @param theAttributes
     *            Container of all tag attributes
     * @param aKey
     *            attribute key
     * @return <code>true</code> if attribute 'aKey' exist and has a simple
     *         value <code>false</code> otherwise
     */

    private static boolean hasValueAttribute(Map<String, Object> theAttributes, int aKey)
    {
        return theAttributes.containsKey(String.valueOf(aKey));
    }

    /**
     * This is a convenience method for the compileTag() method. It checks
     * wether a special attribute was specified in the tag body or not AND if it
     * has a value that is an ISML expression.
     * 
     * @param theAttributes
     *            Container of all tag attributes
     * @param aKey
     *            attribute key
     * @return <code>true</code> if attribute 'aKey' exist and has an ISML
     *         expression value <code>false</code> otherwise
     */

    private static boolean hasExpressionAttribute(Map<String, Object> theAttributes, int aKey)
    {
        return theAttributes.containsKey('#' + String.valueOf(aKey));
    }

    /**
     * This is a convenience method for the compileTag() method. It returns the
     * String value of an attribute with simple String value that was specified
     * in the tag body.
     * 
     * @param theAttributes
     *            Container of all tag attributes
     * @param aKey
     *            attribute key
     * @return String value of attribute or <code>null</code> iof attribute
     *         wasn't specified or if value is an ISML expression
     */

    private static String getValueAttribute(Map<String, Object> theAttributes, int aKey)
    {
        return (String)theAttributes.get(String.valueOf(aKey));
    }

    /**
     * This is a convenience method for the compileTag() method. It returns the
     * value of an attribute with ISML exprssion value that was specified in the
     * tag body as a String containing the compiled ISML expression.
     * 
     * @param theAttributes
     *            Container of all tag attributes
     * @param aKey
     *            attribute key
     * @return compiled ISML expression as String or <code>null</code> if
     *         attribute wasn't specified or if value is no ISML expression
     */

    private static String getExpressionAttribute(Map<String, Object> theAttributes, int aKey)
    {
        return (String)theAttributes.get('#' + String.valueOf(aKey));
    }

    /**
     * This is a convenience method for the compileTag() method. It is used for
     * attributes that have enumerated values like 'on' or 'off'. It checks if
     * the attribute was specified in the tag body AND if it has a simple String
     * value AND if this simple Value is equal to aValue (case insensitive).
     * 
     * @param theAttributes
     *            Container of all tag attributes
     * @param aKey
     *            attribute key
     * @param aValue
     *            String to be compared
     * @return <code>true</code> if attribute 'aKey' exist and has a simple
     *         value aValue <code>false</code> otherwise
     */

    private static boolean equalsAttribute(Map<String, Object> theAttributes, int aKey, String aValue)
    {
        String value = getValueAttribute(theAttributes, aKey);
        if (value != null)
        {
            return value.equalsIgnoreCase(aValue);
        }
        else
        {
            return false;
        }
    }

    /**
     * This is a convenience method for the compileTag() method. It is used to
     * check the top level tag of the nesting table for validity. Some tags that
     * must be enclosed inside flow-control tags like ISLOOP and ISIF
     * 
     * @param nestingTable
     *            Container of flow-control tags
     * @param aTag
     *            flow-control tag
     * @return <code>true</code> if there has been a previous flow-control tag
     *         <code>false</code> otherwise
     */

    private static boolean checkNestingTopTag(List<Token> nestingTable, int aTag)
    {
        if (nestingTable.isEmpty())
        {
            return false;
        }

        Token last = nestingTable.get(nestingTable.size()-1);

        return last.kind == aTag;
    }

    /**
     * Convenient method to check the nesting stack for a tag. This can be used
     * to validate the current nesting level.
     * 
     * @param nestingTable
     *            Container of flow-control tags
     * @param aTag
     *            The tag to be checked
     * @return <code>True</code> if the tag is part of the nesting stack
     */
    private static boolean containsNestingTag(List<Token> nestingTable, int aTag)
    {
        boolean result = false;

        // loop through the nesting stack backwards
        for(int i = (nestingTable.size() - 1); i >= 0; i--)
        {
            if (nestingTable.get(i).kind == aTag)
            {
                result = true;
                break;
            }
        }

        return result;
    }

    /**
     * 
     * @param nestingTable
     *            Container of flow-control tags
     */
    private static void removeNestingTopTag(List<?> nestingTable) throws ParseException
    {
        int size = nestingTable.size();
        if (size > 0)
        {
            // clear nesting table
            nestingTable.remove(size - 1);
        }
        else
        {
            throw new ParseException("Try to remove tag from empty nesting table.");
        }
    }

    /**
     * Escape a string to be used as Java String parameter
     * 
     * @param value
     *            The current string
     * @return the escaped string
     */
    private static String escapeAsJavaString(String value)
    {
        return value.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\"");
    }
    
    private static boolean isFormAttribute(String aKey, String aName)
    {
        return aKey != null && aName != null && aKey.equals(aName);
    }

    private static boolean isFormExpressionAttribute(String aKey, String aName)
    {
        return aKey != null && aName != null && aKey.equals("#" + aName);
    }

    private static String getFormAttribute(Map<String, Object> theAttributes, String aKey)
    {
        return theAttributes.get(aKey).toString();
    }
    
    private static boolean equalsFormAttribute(Map<String, Object> theAttributes, String aKey, String aValue)
    {
        String value = getFormAttribute(theAttributes, aKey);
        if (value != null)
        {
            return value.equalsIgnoreCase(aValue);
        }
        else
        {
            return false;
        }
    }
    
    private static String writeFormAttribute(Map<String, Object> theAttributes, String aKey)
    {
        String attributeValue = null;
        
        if (aKey.startsWith("#"))
        {
            attributeValue = "context.getFormattedValue(" + getFormAttribute(theAttributes, aKey) + ",null)";
        }
        else
        {
            attributeValue = escapeAsJavaString('\"' + getFormAttribute(theAttributes, aKey) + '\"');
        }

        StringBuilder attribute = new StringBuilder()
            .append("out.print(\" ")
            .append(aKey.startsWith("#") ? aKey.substring(1) : aKey)
            .append("=\\\"\");")
            .append("out.print(")
            .append(attributeValue)
            .append(");")
            .append("out.print(\"\\\"\");");
        
        return attribute.toString();
    }
}
