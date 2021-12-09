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

import com.intershop.beehive.isml.capi.ISMLException;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStreamWriter;

/**
 * This interface must be implemented by all classes
 * that act as template compilers.
 */

public interface TemplateCompiler
{
    /**
     *  security level constant:
     *  every syntax element especially server side scripting is allowed
     */
    static int ALLOW_ALL = 255;

    /**
     *  security level constant:
     *  every syntax element is allowed except server side scripting
     */
    static int DISABLE_SSS = 127;

    /**
     * Compile a Template to JSP code.
     *
     * @param   securityLevel see security level constants
     * @param   out         the output writer
     * @param   sourceFile  the input file
     * @return          is true if compilation was successful and false if compilation failed
     *
     * @throws ISMLException if something goes bad
     */
    boolean compileTemplate(int securityLevel, OutputStreamWriter out, File sourceFile, InputStream in)
           throws ISMLException;
}
