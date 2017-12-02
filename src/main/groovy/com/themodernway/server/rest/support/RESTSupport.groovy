/*
 * Copyright (c) 2017, 2018, The Modern Way. All rights reserved.
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

package com.themodernway.server.rest.support

import org.springframework.http.HttpMethod

import com.themodernway.server.core.support.CoreGroovySupport
import com.themodernway.server.rest.IRESTService
import com.themodernway.server.rest.support.spring.IRESTContext
import com.themodernway.server.rest.support.spring.IRESTServiceRegistry
import com.themodernway.server.rest.support.spring.RESTContextInstance

import groovy.transform.CompileStatic
import groovy.transform.Memoized

@CompileStatic
public class RESTSupport extends CoreGroovySupport implements IRESTContext
{
    private static final RESTSupport INSTANCE = new RESTSupport()

    @Memoized
    public static final RESTSupport getRESTSupport()
    {
        INSTANCE
    }

    @Memoized
    public IRESTContext getRESTContext()
    {
        RESTContextInstance.getRESTContextInstance()
    }

    @Memoized
    public IRESTServiceRegistry getRESTServiceRegistry()
    {
        getRESTContext().getRESTServiceRegistry()
    }

    @Override
    public IRESTService getBinding(String bind, HttpMethod method)
    {
        getRESTServiceRegistry().getBinding(bind, method)
    }

    @Override
    public boolean isBindingRegistered(String bind)
    {
        getRESTServiceRegistry().isBindingRegistered(bind)
    }
}
