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

package com.themodernway.server.rest.support

import com.themodernway.server.core.support.CoreGroovySupport
import com.themodernway.server.rest.IRESTService
import com.themodernway.server.rest.support.spring.IRESTContext
import com.themodernway.server.rest.support.spring.IServiceRegistry
import com.themodernway.server.rest.support.spring.RESTContextInstance

import groovy.transform.CompileStatic
import groovy.transform.Memoized

@CompileStatic
public class RESTSupport extends CoreGroovySupport
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
    public IServiceRegistry getServiceRegistry()
    {
        getRESTContext().getServiceRegistry()
    }

    @Memoized
    public IRESTService getService(String name)
    {
        getServiceRegistry().getService(name)
    }

    @Memoized
    public IRESTService getBinding(String bind)
    {
        getServiceRegistry().getBinding(bind)
    }

    @Memoized
    public String fixRequestBinding(String bind)
    {
        getRESTContext().fixRequestBinding(bind)
    }

    @Memoized
    public List<IRESTService> getServices()
    {
        getServiceRegistry().getServices()
    }
}
