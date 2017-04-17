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

import javax.servlet.http.HttpServletResponse

import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service

import com.google.common.util.concurrent.RateLimiter
import com.themodernway.common.api.java.util.StringOps
import com.themodernway.server.core.json.JSONObject
import com.themodernway.server.core.json.schema.JSONSchema
import com.themodernway.server.rest.support.RESTSupport

import groovy.transform.CompileStatic
import groovy.transform.Memoized

@CompileStatic
public abstract class RESTServiceSupport extends RESTSupport implements IRESTService
{
    private RateLimiter m_ratelimit

    public RESTServiceSupport()
    {
        m_ratelimit = RateLimiterFactory.create(getClass())
    }

    @Override
    public void acquire()
    {
        if (m_ratelimit)
        {
            m_ratelimit.acquire()
        }
    }

    @Memoized
    public String getName()
    {
        final Class<?> claz = getClass()

        if (claz.isAnnotationPresent(Service))
        {
            final String name = StringOps.toTrimOrNull(claz.getAnnotation(Service).value())

            if (name)
            {
                return name
            }
        }
        claz.getSimpleName()
    }

    @Memoized
    public String getRequestBinding()
    {
        final Class<?> claz = getClass()

        if (claz.isAnnotationPresent(RequestBinding))
        {
            return fixRequestBinding(StringOps.toTrimOrNull(claz.getAnnotation(RequestBinding).value()))
        }
        null
    }
    
    @Memoized
    public String getRequestBindingAbsolute()
    {
        getRequestBinding() ?: fixRequestBinding(getName())
    }

    @Memoized
    public HttpMethod getRequestMethodType()
    {
        final Class<?> claz = getClass()

        if (claz.isAnnotationPresent(RequestMethod))
        {
            return claz.getAnnotation(RequestMethod).value()
        }
        HttpMethod.GET
    }

    @Override
    public JSONObject getSchemas()
    {
        json(request: getRequestSchema(), response: getResponseSchema())
    }

    @Override
    public JSONSchema getRequestSchema()
    {
        jsonSchema(type: 'object', properties: [:])
    }

    @Override
    public JSONSchema getResponseSchema()
    {
        jsonSchema(type: 'object', properties: [:])
    }

    @Override
    public JSONObject getSwaggerAttributes()
    {
        json(path: getRequestBindingAbsolute(), method: getRequestMethodType().name(), schemas: getSchemas())
    }
    
    protected JSONObject failure(final String reason, final int code) throws RESTException
    {
        throw new RESTException(reason, code)
    }
    
    protected JSONObject failure(final String reason) throws RESTException
    {
        failure(reason, HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
    }
}
