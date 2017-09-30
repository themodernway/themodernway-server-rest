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

package com.themodernway.server.rest.support.spring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;

import org.springframework.web.context.WebApplicationContext;

import com.themodernway.server.core.support.spring.IServletFactory;
import com.themodernway.server.core.support.spring.IServletFactoryContextCustomizer;
import com.themodernway.server.core.support.spring.ServletFactoryContextCustomizer;
import com.themodernway.server.rest.servlet.RESTServlet;

public class RESTServletContextCustomizer extends ServletFactoryContextCustomizer implements IServletFactory
{
    private List<String> m_tags = new ArrayList<String>();

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
            m_tags = toUniqueStringList(tags);
        }
        else
        {
            logger().error("null tags ignored");
        }
    }

    public void setTags(String tags)
    {
        tags = this.toTrimOrNull(tags);

        if (null != tags)
        {
            m_tags = toUniqueTokenStringList(tags);
        }
        else
        {
            logger().error("null or empty tags ignored");
        }
    }

    public List<String> getTags()
    {
        return toUnmodifiableList(m_tags);
    }

    @Override
    public Servlet make(final IServletFactoryContextCustomizer customizer, final ServletContext sc, final WebApplicationContext context)
    {
        final RESTServlet inst = new RESTServlet();

        inst.setTags(getTags());

        inst.setRateLimit(getRateLimit());

        inst.setRequiredRoles(getRequiredRoles());

        return inst;
    }
}
