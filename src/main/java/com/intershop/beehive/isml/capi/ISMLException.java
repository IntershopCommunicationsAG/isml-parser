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
 * This exception is thrown from the ISML template engine when loading,
 * looking up an ISML template or rendering failed due to an unexpected reason.
 */
public class ISMLException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    /**
     * Creates a ISMLException with a message of the reason.
     *
     * @param message    a message with explains the reason of the exception
     */
    public ISMLException(String message)
    {
        super(message);
    }

    /**
     * Creates a ISMLException with the embedded exception and the message for
     * throwing a ISMLException
     *
     * @param message    a message with explains the reason of the exception
     * @param exception  the embedded exception which is encapsulated
     */
    public ISMLException(String message, Throwable exception)
    {
        super(message, exception);
    }

    /**
     * Creates a ISMLException with the embedded exception
     *
     * @param exception  the embedded exception which is encapsulated
     */
    public ISMLException(Throwable exception)
    {
        super(exception);
    }
}
