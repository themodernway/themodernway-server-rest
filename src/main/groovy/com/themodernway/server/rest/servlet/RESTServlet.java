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

package com.themodernway.server.rest.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;

import com.themodernway.server.core.ITimeSupplier;
import com.themodernway.server.core.file.FileAndPathUtils;
import com.themodernway.server.core.io.IO;
import com.themodernway.server.core.json.JSONObject;
import com.themodernway.server.core.json.ParserException;
import com.themodernway.server.core.json.binder.BinderType;
import com.themodernway.server.core.security.IAuthorizationResult;
import com.themodernway.server.core.security.session.IServerSession;
import com.themodernway.server.core.security.session.IServerSessionHelper;
import com.themodernway.server.core.servlet.DefaultHeaderNameSessionIDFromRequestExtractor;
import com.themodernway.server.core.servlet.HTTPServletBase;
import com.themodernway.server.core.servlet.IResponseAction;
import com.themodernway.server.rest.IRESTRequestContext;
import com.themodernway.server.rest.IRESTService;
import com.themodernway.server.rest.RESTException;
import com.themodernway.server.rest.RESTRequestContext;
import com.themodernway.server.rest.support.spring.IRESTContext;
import com.themodernway.server.rest.support.spring.RESTContextInstance;

public class RESTServlet extends HTTPServletBase
{
    private static final long            serialVersionUID = 1L;

    private long                         m_size           = 0L;

    private List<String>                 m_tags           = arrayList();

    protected static final ITimeSupplier TIMER_MILLS      = ITimeSupplier.mills();

    protected static final ITimeSupplier TIMER_NANOS      = ITimeSupplier.nanos();

    public RESTServlet()
    {
    }

    protected RESTServlet(final double rate)
    {
        super(rate);
    }

    @Override
    public void doHead(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        doNeverCache(request, response);

        response.setContentLengthLong(0L);

        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        doService(request, response, false, HttpMethod.GET, getJSONParametersFromRequest(request));
    }

    @Override
    public void doPut(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        doService(request, response, true, HttpMethod.PUT, null);
    }

    @Override
    public void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        doService(request, response, true, HttpMethod.POST, null);
    }

    @Override
    public void doPatch(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        doService(request, response, true, HttpMethod.PATCH, null);
    }

    @Override
    public void doDelete(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        doService(request, response, true, HttpMethod.DELETE, null);
    }

    protected void doService(final HttpServletRequest request, final HttpServletResponse response, final boolean read, final HttpMethod type, JSONObject body) throws ServletException, IOException
    {
        final String bind = FileAndPathUtils.fixPathBinding(toTrimOrElse(request.getPathInfo(), FileAndPathUtils.SINGLE_SLASH));

        if (null == bind)
        {
            logger().error("empty service path found.");

            response.setStatus(HttpServletResponse.SC_NOT_FOUND);

            return;
        }
        final IRESTService service = getRESTContext().getBinding(bind, type);

        if (null == service)
        {
            if (getRESTContext().isBindingRegistered(bind))
            {
                logger().error(format("service (%s) not type (%s).", bind, type));

                response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);

                return;
            }
            else
            {
                logger().error(format("service or binding not found (%s).", bind));

                response.setStatus(HttpServletResponse.SC_NOT_FOUND);

                return;
            }
        }
        final List<String> tags = getTags();

        if ((null != tags) && (false == tags.isEmpty()))
        {
            boolean find = false;

            final List<String> vals = service.getTaggigValues();

            if (null != vals)
            {
                for (final String valu : vals)
                {
                    if (tags.contains(valu))
                    {
                        find = true;

                        break;
                    }
                }
            }
            if (false == find)
            {
                logger().error(format("service (%s) for tags not found (%s).", bind, toPrintableString(tags)));

                response.setStatus(HttpServletResponse.SC_NOT_FOUND);

                return;
            }
        }
        if (type != service.getRequestMethodType())
        {
            logger().error(format("service (%s) not type (%s).", bind, type));

            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);

            return;
        }
        List<String> uroles = arrayList();

        final IServerSession session = getSession(requireNonNullOrElse(getSessionIDFromRequestExtractor(), DefaultHeaderNameSessionIDFromRequestExtractor.DEFAULT).getSessionID(request));

        if (null != session)
        {
            uroles = session.getRoles();
        }
        if ((null == uroles) || (uroles.isEmpty()))
        {
            uroles = getDefaultRoles(request);
        }
        final IAuthorizationResult auth = isAuthorized(request, session, service, uroles);

        if (false == auth.isAuthorized())
        {
            logger().error(format("service authorization failed for (%s) type (%s) reason (%s).", bind, type, auth.getText()));

            response.addHeader(WWW_AUTHENTICATE, "unauthorized");

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

            return;
        }
        if (read)
        {
            body = parseBODY(service, request, response, type, bind);
        }
        if (null == body)
        {
            if (response.getStatus() == HttpServletResponse.SC_OK)
            {
                logger().error(format("service (%s) type (%s) null body.", bind, type));

                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            return;
        }
        body = clean(body, false);

        final IRESTRequestContext context = new RESTRequestContext(service, session, uroles, getServletContext(), request, response, type);

        try
        {
            service.acquire();

            final long mills = TIMER_MILLS.getTime();

            final long nanos = TIMER_NANOS.getTime();

            final Object object = service.call(context, body);

            final long ndiff = TIMER_NANOS.getTime() - nanos;

            final long mdiff = TIMER_MILLS.getTime() - mills;

            if (mdiff < 1)
            {
                logger().info(format("calling service (%s) took (%s) nanos.", bind, ndiff));
            }
            else
            {
                logger().info(format("calling service (%s) took (%s) mills.", bind, mdiff));
            }
            if (context.isOpen())
            {
                if (object instanceof IResponseAction)
                {
                    ((IResponseAction) object).call(request, response);

                    return;
                }
                JSONObject result = json(object);

                if (null != result)
                {
                    result = clean(result, true);
                }
                writeBODY(HttpServletResponse.SC_OK, context, request, response, result);
            }
            else
            {
                logger().error(format("calling service (%s) context closed.", bind));
            }
        }
        catch (final RESTException e)
        {
            if (context.isOpen())
            {
                errorBODY(e.getCode(), context, request, response, e.getReason());
            }
            else
            {
                logger().error(format("calling service (%s) context closed.", bind));
            }
        }
        catch (final Throwable e)
        {
            final String uuid = uuid();

            logger().error(format("error calling (%s) uuid (%s).", bind, uuid), e);

            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, uuid);
        }
    }

    protected JSONObject json(final Object object) throws Exception
    {
        if (null == object)
        {
            return null;
        }
        if (object instanceof JSONObject)
        {
            return ((JSONObject) object);
        }
        return getRESTContext().json(object);
    }

    protected JSONObject clean(final JSONObject json, final boolean outbound)
    {
        return json;
    }

    protected List<String> getDefaultRoles(final HttpServletRequest request)
    {
        return IServerSessionHelper.SP_DEFAULT_ROLES_LIST;
    }

    protected boolean isStrict(final HttpServletRequest request)
    {
        return Boolean.parseBoolean(toTrimOrNull(request.getHeader(X_STRICT_JSON_FORMAT_HEADER)));
    }

    protected JSONObject parseBODY(final IRESTService service, final HttpServletRequest request, final HttpServletResponse response, final HttpMethod type, final String bind)
    {
        if (type != HttpMethod.GET)
        {
            final long leng = request.getContentLengthLong();

            if (leng > 0L)
            {
                long size = service.getMaxRequestBodySize();

                if (false == (size > 0L))
                {
                    size = getMaxRequestBodySize();
                }
                if ((size > 0L) && (leng > size))
                {
                    logger().error(format("error calling (%s) length (%d) greater than (%d).", bind, leng, size));

                    response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);

                    return null;
                }
                try
                {
                    if (size > 0L)
                    {
                        final String buff = IO.getStringAtMost(request.getReader(), size);

                        if (buff.length() > size)
                        {
                            logger().error(format("error calling (%s) length (%d) greater than (%d).", bind, buff.length(), size));

                            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);

                            return null;
                        }
                        return BinderType.forContentType(request.getContentType()).getBinder().bindJSON(buff);
                    }
                    return BinderType.forContentType(request.getContentType()).getBinder().bindJSON(request.getReader());
                }
                catch (final ParserException e)
                {
                    logger().error(format("error calling (%s) ParserException.", bind), e);

                    return null;
                }
                catch (final IOException e)
                {
                    logger().error(format("error calling (%s) IOException.", bind), e);

                    return null;
                }
            }
            if (leng == 0L)
            {
                return new JSONObject();
            }
        }
        return new JSONObject();
    }

    protected void errorBODY(final int code, final IRESTRequestContext context, final HttpServletRequest request, final HttpServletResponse response, final String reason) throws IOException
    {
        writeBODY(code, context, request, response, new JSONObject("error", new JSONObject("code", code).set("reason", reason)));
    }

    protected void writeBODY(final int code, final IRESTRequestContext context, final HttpServletRequest request, final HttpServletResponse response, final JSONObject output) throws IOException
    {
        doNeverCache(request, response);

        final String type = toTrimOrElse(request.getHeader(ACCEPT), CONTENT_TYPE_APPLICATION_JSON).toLowerCase();

        if (type.contains(CONTENT_TYPE_APPLICATION_JSON))
        {
            response.setContentType(CONTENT_TYPE_APPLICATION_JSON);
        }
        if (type.contains(CONTENT_TYPE_TEXT_PROPERTIES))
        {
            response.setContentType(CONTENT_TYPE_TEXT_PROPERTIES);
        }
        else if (type.contains(CONTENT_TYPE_TEXT_XML))
        {
            response.setContentType(CONTENT_TYPE_TEXT_XML);
        }
        else if (type.contains(CONTENT_TYPE_APPLICATION_XML))
        {
            response.setContentType(CONTENT_TYPE_APPLICATION_XML);
        }
        else if (type.contains(CONTENT_TYPE_TEXT_YAML))
        {
            response.setContentType(CONTENT_TYPE_TEXT_YAML);
        }
        else if (type.contains(CONTENT_TYPE_APPLICATION_YAML))
        {
            response.setContentType(CONTENT_TYPE_APPLICATION_YAML);
        }
        else
        {
            response.setContentType(CONTENT_TYPE_APPLICATION_JSON);
        }
        if (null == output)
        {
            response.setContentLengthLong(0L);

            response.setStatus(HttpServletResponse.SC_NO_CONTENT);

            return;
        }
        response.setStatus(code);

        final PrintWriter stream = response.getWriter();

        stream.flush();

        try
        {
            BinderType.forContentType(type).getBinder().setStrict(isStrict(request)).send(stream, output);
        }
        catch (final ParserException e)
        {
            throw new IOException(e);
        }
        stream.flush();
    }

    protected IRESTContext getRESTContext()
    {
        return RESTContextInstance.getRESTContextInstance();
    }

    public void setTags(final List<String> tags)
    {
        if (null != tags)
        {
            m_tags = toUnique(tags);
        }
        else
        {
            logger().error("null tags ignored");
        }
    }

    public void setTags(String tags)
    {
        tags = toTrimOrNull(tags);

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

    public void setMaxRequestBodySize(final long size)
    {
        m_size = size;
    }

    public long getMaxRequestBodySize()
    {
        return m_size;
    }
}