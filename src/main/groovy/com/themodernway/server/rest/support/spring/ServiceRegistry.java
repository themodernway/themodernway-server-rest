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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.themodernway.common.api.java.util.StringOps;
import com.themodernway.server.rest.IRESTService;

/**
 * ServiceRegistry - Registry of all IRESTService services found in the application.
 */
@ManagedResource
public class ServiceRegistry implements IServiceRegistry, BeanFactoryAware
{
    private static final Logger                       logger     = Logger.getLogger(ServiceRegistry.class);

    private final LinkedHashMap<String, IRESTService> m_services = new LinkedHashMap<String, IRESTService>();

    private final LinkedHashMap<String, IRESTService> m_bindings = new LinkedHashMap<String, IRESTService>();

    public ServiceRegistry()
    {
    }

    protected void addService(final IRESTService service)
    {
        if (null != service)
        {
            String name = StringOps.toTrimOrNull(service.getName());

            if (null != name)
            {
                if (null == m_services.get(name))
                {
                    m_services.put(name, service);

                    logger.info("ServiceRegistry.addService(" + name + ") Registered");
                }
                else
                {
                    logger.error("ServiceRegistry.addService(" + name + ") Duplicate ignored");
                }
            }
            else
            {
                logger.error("ServiceRegistry.addService(" + service.getClass().getSimpleName() + ") has null or empty name.");
            }
            String bind = StringOps.toTrimOrNull(service.getRequestBinding());

            if (null != bind)
            {
                if (null == m_bindings.get(bind))
                {
                    m_bindings.put(bind, service);

                    logger.info("ServiceRegistry.addService(" + bind + ") Binding Registered");
                }
                else
                {
                    logger.error("ServiceRegistry.addService(" + bind + ") Duplicate binding ignored");
                }
            }
            if ((null != name) && (null == bind))
            {
                bind = "/" + name;

                if (null == m_bindings.get(bind))
                {
                    m_bindings.put(bind, service);

                    logger.info("ServiceRegistry.addService(" + bind + ") Binding Registered");
                }
                else
                {
                    logger.error("ServiceRegistry.addService(" + bind + ") Duplicate binding ignored");
                }
            }
        }
        else
        {
            logger.error("ServiceRegistry.addService(null)");
        }
    }

    @Override
    public IRESTService getService(String name)
    {
        name = StringOps.toTrimOrNull(name);

        if (null != name)
        {
            return m_services.get(name);
        }
        return null;
    }

    @Override
    public IRESTService getBinding(String bind)
    {
        bind = StringOps.toTrimOrNull(bind);

        if (null != bind)
        {
            if (false == bind.startsWith("/"))
            {
                bind = "/" + bind;
            }
            return m_bindings.get(bind);
        }
        return null;
    }

    @Override
    @ManagedAttribute(description = "Get IRESTService names.")
    public List<String> getServiceNames()
    {
        return Collections.unmodifiableList(new ArrayList<String>(m_services.keySet()));
    }

    @Override
    @ManagedAttribute(description = "Get IRESTService RequestBindings.")
    public List<String> getRequestBindings()
    {
        return Collections.unmodifiableList(new ArrayList<String>(m_bindings.keySet()));
    }

    @Override
    public List<IRESTService> getServices()
    {
        return Collections.unmodifiableList(new ArrayList<IRESTService>(m_services.values()));
    }

    @Override
    public void setBeanFactory(final BeanFactory factory) throws BeansException
    {
        if (factory instanceof DefaultListableBeanFactory)
        {
            for (IRESTService service : ((DefaultListableBeanFactory) factory).getBeansOfType(IRESTService.class).values())
            {
                addService(service);
            }
        }
    }

    @Override
    public void close() throws IOException
    {
        for (IRESTService service : getServices())
        {
            if (null != service)
            {
                try
                {
                    logger.info("ServiceRegistry.close(" + service.getName() + ")");

                    service.close();
                }
                catch (Exception e)
                {
                    logger.error("ServiceRegistry.close(" + service.getName() + ") ERROR ", e);
                }
            }
        }
    }
}
