/*
 * Copyright (c) 2018, The Modern Way. All rights reserved.
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

package com.themodernway.server.rest.support.spring;

import java.util.Collection;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;

import org.springframework.web.context.WebApplicationContext;

import com.themodernway.server.core.logging.LoggingOps;
import com.themodernway.server.core.support.spring.IServletFactory;
import com.themodernway.server.core.support.spring.IServletFactoryContextCustomizer;
import com.themodernway.server.core.support.spring.ServletFactoryContextCustomizer;
import com.themodernway.server.rest.servlet.RESTServlet;

public class RESTServletContextCustomizer extends ServletFactoryContextCustomizer implements IServletFactory
{
    private long         m_size = 0L;

    private List<String> m_tags = arrayList();

    public RESTServletContextCustomizer(final String name, final String maps)
    {
        super(name, maps);

        setServletFactory(this);
    }

    public RESTServletContextCustomizer(final String name, final Collection<String> maps)
    {
        super(name, maps);

        setServletFactory(this);
    }

    public void setTags(final List<String> tags)
    {
        if (null != tags)
        {
            m_tags = toUnique(tags);
        }
        else if (logger().isErrorEnabled())
        {
            logger().error(LoggingOps.THE_MODERN_WAY_MARKER, "null tags ignored");
        }
    }

    public void setTags(String tags)
    {
        tags = toTrimOrNull(tags);

        if (null != tags)
        {
            m_tags = toUniqueTokenStringList(tags);
        }
        else if (logger().isErrorEnabled())
        {
            logger().error(LoggingOps.THE_MODERN_WAY_MARKER, "null or empty tags ignored");
        }
    }

    public List<String> getTags()
    {
        return toUnmodifiableList(m_tags);
    }

    public void setMaxRequestBodySize(final long size)
    {
        m_size = size;
    }

    public long getMaxRequestBodySize()
    {
        return m_size;
    }

    @Override
    public Servlet make(final IServletFactoryContextCustomizer customizer, final ServletContext sc, final WebApplicationContext context)
    {
        return new RESTServlet(getMaxRequestBodySize(), getTags(), getRateLimit(), getRequiredRoles(), customizer.getServletResponseErrorCodeManager(), customizer.getSessionIDFromRequestExtractor());
    }
}
