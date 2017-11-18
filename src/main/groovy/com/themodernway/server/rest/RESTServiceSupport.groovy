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

import org.springframework.http.HttpMethod

import com.google.common.util.concurrent.RateLimiter
import com.themodernway.server.core.ICoreCommon
import com.themodernway.server.core.file.FileAndPathUtils
import com.themodernway.server.rest.support.RESTSupport

import groovy.transform.CompileStatic
import groovy.transform.Memoized

@CompileStatic
public abstract class RESTServiceSupport extends RESTSupport implements IRESTService
{
    private RateLimiter m_ratelimit

    public RESTServiceSupport()
    {
        setRateLimit(RateLimiterFactory.create(getClass()))
    }

    public RESTServiceSupport(double ratelimit)
    {
        setRateLimit(ratelimit)
    }

    protected void setRateLimit(double ratelimit)
    {
        setRateLimit(RateLimiterFactory.create(ratelimit))
    }

    protected void setRateLimit(RateLimiter ratelimit)
    {
        m_ratelimit = ratelimit
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
    public List<String> getTaggigValues()
    {
        ICoreCommon.toTaggingValues(this)
    }

    @Memoized
    public long getMaxRequestBodySize()
    {
        final Class<?> claz = getClass()

        if (claz.isAnnotationPresent(MaxRequestBodySize))
        {
            return claz.getAnnotation(MaxRequestBodySize).value()
        }
        0L
    }

    @Memoized
    public String getRequestBinding()
    {
        final Class<?> claz = getClass()

        if (claz.isAnnotationPresent(RequestBinding))
        {
            return FileAndPathUtils.fixPathBinding(claz.getAnnotation(RequestBinding).value())
        }
        null
    }

    @Memoized
    public HttpMethod getRequestMethodType()
    {
        final Class<?> claz = getClass()

        if (claz.isAnnotationPresent(RequestMethod))
        {
            return claz.getAnnotation(RequestMethod).value() ?: HttpMethod.GET
        }
        HttpMethod.GET
    }

    @Memoized
    public DefaultResponseActions responses()
    {
        new DefaultResponseActions()
    }
}
