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

package com.themodernway.server.rest;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;

import com.themodernway.server.core.json.JSONObject;
import com.themodernway.server.core.security.session.IServerSession;
import com.themodernway.server.core.servlet.HTTPUtils;
import com.themodernway.server.rest.support.spring.IRESTContext;
import com.themodernway.server.rest.support.spring.RESTContextInstance;

public class RESTRequestContext implements IRESTRequestContext
{
    private final AtomicBoolean       m_is_open = new AtomicBoolean(true);

    private final List<String>        m_rolesof;

    private final HttpMethod          m_reqtype;

    private final IServerSession      m_session;

    private final IRESTService        m_service;

    private final ServletContext      m_servlet_context;

    private final HttpServletRequest  m_servlet_request;

    private final HttpServletResponse m_servlet_respons;

    public RESTRequestContext(final IRESTService service, final IServerSession session, final List<String> rolesof, final ServletContext context, final HttpServletRequest request, final HttpServletResponse respons, final HttpMethod reqtype)
    {
        m_service = service;

        m_reqtype = reqtype;

        m_session = session;

        m_rolesof = toUnmodifiableList(toUnique(rolesof));

        m_servlet_context = context;

        m_servlet_request = request;

        m_servlet_respons = respons;
    }

    @Override
    public boolean isGet()
    {
        return (HttpMethod.GET == getRequestType());
    }

    @Override
    public boolean isPut()
    {
        return (HttpMethod.PUT == getRequestType());
    }

    @Override
    public boolean isPost()
    {
        return (HttpMethod.POST == getRequestType());
    }

    @Override
    public boolean isHead()
    {
        return (HttpMethod.HEAD == getRequestType());
    }

    @Override
    public boolean isDelete()
    {
        return (HttpMethod.DELETE == getRequestType());
    }

    @Override
    public HttpMethod getRequestType()
    {
        return m_reqtype;
    }

    @Override
    public IRESTContext getRESTContext()
    {
        return RESTContextInstance.getRESTContextInstance();
    }

    @Override
    public ServletContext getServletContext()
    {
        return m_servlet_context;
    }

    @Override
    public HttpServletRequest getServletRequest()
    {
        return m_servlet_request;
    }

    @Override
    public HttpServletResponse getServletResponse()
    {
        return m_servlet_respons;
    }

    @Override
    public Cookie setCookie(final String name, final String value)
    {
        return setCookie(name, value, null);
    }

    @Override
    public Cookie setCookie(final String name, final String value, final String path)
    {
        return HTTPUtils.setCookie(getServletRequest(), getServletResponse(), name, value, path);
    }

    @Override
    public Cookie setCookie(final String name, final String value, final TimeUnit unit, final long duration)
    {
        return setCookie(name, value, null, unit, duration);
    }

    @Override
    public Cookie setCookie(final String name, final String value, final String path, final TimeUnit unit, final long duration)
    {
        return HTTPUtils.setCookie(getServletRequest(), getServletResponse(), name, value, path, unit, duration);
    }

    @Override
    public JSONObject getJSONHeaders()
    {
        return getJSONHeadersFromRequest(getServletRequest());
    }

    @Override
    public JSONObject getJSONParameters()
    {
        return getJSONParametersFromRequest(getServletRequest());
    }

    @Override
    public List<String> getRoles()
    {
        final IServerSession sess = getSession();

        if (null != sess)
        {
            final List<String> valu = sess.getRoles();

            if ((null != valu) && (false == valu.isEmpty()))
            {
                return toUnmodifiableList(valu);
            }
        }
        return m_rolesof;
    }

    @Override
    public void close() throws IOException
    {
        m_is_open.set(false);
    }

    @Override
    public boolean isOpen()
    {
        return m_is_open.get();
    }

    @Override
    public IServerSession getSession()
    {
        return m_session;
    }

    @Override
    public void setMaxContentTypeLength(final int max)
    {
    }

    @Override
    public String getName()
    {
        final IServerSession sess = getSession();

        if (null != sess)
        {
            return toTrimOrElse(sess.getUserId(), UNKNOWN_USER);
        }
        return UNKNOWN_USER;
    }

    @Override
    public IRESTService getService()
    {
        return m_service;
    }
}
