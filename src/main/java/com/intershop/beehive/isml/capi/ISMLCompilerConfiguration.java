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
package com.intershop.beehive.isml.capi;

/**
 * Configuration of the isml compiler.
 * 
 * @author Wolfgang Frank
 */
public interface ISMLCompilerConfiguration
{
    /**
     * Gets the default encoding of template source files.
     * @return Default encoding
     */
    String getDefaultContentEncoding();
    
    /**
     * Gets the default encoding for the given mimetype when generating the 
     * jsp file out of the isml file
     * @param mimeType Mimetype of the template
     * @return Encoding of the jsp file
     */
    String getJspEncoding(String mimeType);

}
