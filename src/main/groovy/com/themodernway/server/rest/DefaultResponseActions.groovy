/*
 * Copyright (c) 2017, The Modern Way. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.themodernway.server.rest

import com.themodernway.server.core.servlet.ErrorResponseAction
import com.themodernway.server.core.servlet.ForwardResponseAction
import com.themodernway.server.core.servlet.IResponseAction
import com.themodernway.server.core.servlet.RedirectResponseAction
import com.themodernway.server.core.servlet.StatusCodeResponseAction

import groovy.transform.CompileStatic
import groovy.transform.Memoized

@CompileStatic
public class DefaultResponseActions
{
    public DefaultResponseActions()
    {
    }

    @Memoized
    public IResponseAction redirect(final String path)
    {
        new RedirectResponseAction(path)
    }

    @Memoized
    public IResponseAction forward(final String path)
    {
        new ForwardResponseAction(path)
    }

    @Memoized
    public IResponseAction failure(final int code)
    {
        new ErrorResponseAction(code)
    }

    @Memoized
    public IResponseAction failure(final int code, final String reason)
    {
        new ErrorResponseAction(code, reason)
    }

    @Memoized
    public IResponseAction failure(final String reason)
    {
        new ErrorResponseAction(reason)
    }

    @Memoized
    public IResponseAction code(final int code)
    {
        new StatusCodeResponseAction(code)
    }
}
