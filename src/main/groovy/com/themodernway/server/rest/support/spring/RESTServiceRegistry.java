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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.http.HttpMethod;

import com.themodernway.common.api.java.util.CommonOps;
import com.themodernway.server.core.file.FileUtils;
import com.themodernway.server.core.logging.IHasLogging;
import com.themodernway.server.core.logging.LoggingOps;
import com.themodernway.server.rest.IRESTService;

public class RESTServiceRegistry implements IRESTServiceRegistry, BeanFactoryAware, IHasLogging
{
    private final Logger                                                     m_logger   = LoggingOps.getLogger(getClass());

    private final HashSet<String>                                            m_valpaths = new HashSet<>();

    private final ArrayList<IRESTService>                                    m_services = new ArrayList<>();

    private final LinkedHashMap<String, LinkedHashMap<String, IRESTService>> m_bindings = new LinkedHashMap<>();

    public RESTServiceRegistry()
    {
        for (final HttpMethod method : HttpMethod.values())
        {
            m_bindings.put(method.name(), new LinkedHashMap<String, IRESTService>());
        }
    }

    @Override
    public boolean doBindRESTService(final IRESTService service)
    {
        if (null != service)
        {
            final String bind = FileUtils.fixPathBinding(service.getRequestBinding());

            if (null != bind)
            {
                final HttpMethod method = service.getRequestMethodType();

                if (null != method)
                {
                    final LinkedHashMap<String, IRESTService> find = m_bindings.get(method.name());

                    if (null == find.get(bind))
                    {
                        m_valpaths.add(bind);

                        m_services.add(service);

                        find.put(bind, service);

                        if (logger().isInfoEnabled())
                        {
                            logger().info(LoggingOps.THE_MODERN_WAY_MARKER, String.format("RESTServiceRegistry.addService(%s,%s) registered.", bind, method.name()));
                        }
                        return true;
                    }
                    else if (logger().isErrorEnabled())
                    {
                        logger().error(LoggingOps.THE_MODERN_WAY_MARKER, String.format("RESTServiceRegistry.addService(%s,%s) ignored.", bind, method.name()));
                    }
                }
                else if (logger().isErrorEnabled())
                {
                    logger().error(LoggingOps.THE_MODERN_WAY_MARKER, String.format("RESTServiceRegistry.addService(%s) null type.", bind));
                }
            }
            else if (logger().isErrorEnabled())
            {
                logger().error(LoggingOps.THE_MODERN_WAY_MARKER, "RESTServiceRegistry.addService() null binding.");
            }
        }
        else if (logger().isErrorEnabled())
        {
            logger().error(LoggingOps.THE_MODERN_WAY_MARKER, "RESTServiceRegistry.addService() null service.");
        }
        return false;
    }

    @Override
    public IRESTService getBinding(String bind, final HttpMethod method)
    {
        bind = FileUtils.fixPathBinding(bind);

        if (null != bind)
        {
            return m_bindings.get(method.name()).get(bind);
        }
        return null;
    }

    @Override
    public boolean isBindingRegistered(String bind)
    {
        bind = FileUtils.fixPathBinding(bind);

        if (null != bind)
        {
            return m_valpaths.contains(bind);
        }
        return false;
    }

    @Override
    public List<IRESTService> getRESTServices()
    {
        return CommonOps.toUnmodifiableList(m_services);
    }

    @Override
    public void setBeanFactory(final BeanFactory factory) throws BeansException
    {
        if (factory instanceof DefaultListableBeanFactory)
        {
            for (final IRESTService service : ((DefaultListableBeanFactory) factory).getBeansOfType(IRESTService.class).values())
            {
                doBindRESTService(service);
            }
        }
    }

    @Override
    public void close() throws IOException
    {
        for (final IRESTService service : getRESTServices())
        {
            if (null != service)
            {
                try
                {
                    service.close();
                }
                catch (final Exception e)
                {
                    if (logger().isErrorEnabled())
                    {
                        logger().error(LoggingOps.THE_MODERN_WAY_MARKER, "RESTServiceRegistry.close().", e);
                    }
                }
            }
        }
    }

    @Override
    public Logger logger()
    {
        return m_logger;
    }
}
