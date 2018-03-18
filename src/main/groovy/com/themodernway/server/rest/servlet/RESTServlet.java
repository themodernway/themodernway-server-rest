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

package com.themodernway.server.rest.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;

import com.themodernway.server.core.NanoTimer;
import com.themodernway.server.core.file.FileAndPathUtils;
import com.themodernway.server.core.io.IO;
import com.themodernway.server.core.json.JSONObject;
import com.themodernway.server.core.json.ParserException;
import com.themodernway.server.core.json.binder.BinderType;
import com.themodernway.server.core.json.validation.IJSONValidator;
import com.themodernway.server.core.json.validation.IValidationContext;
import com.themodernway.server.core.logging.LoggingOps;
import com.themodernway.server.core.security.IAuthorizationResult;
import com.themodernway.server.core.security.session.IServerSession;
import com.themodernway.server.core.servlet.DefaultHeaderNameSessionIDFromRequestExtractor;
import com.themodernway.server.core.servlet.HTTPServletBase;
import com.themodernway.server.core.servlet.IResponseAction;
import com.themodernway.server.core.servlet.IServletResponseErrorCodeManager;
import com.themodernway.server.core.servlet.ISessionIDFromRequestExtractor;
import com.themodernway.server.rest.IRESTRequestContext;
import com.themodernway.server.rest.IRESTService;
import com.themodernway.server.rest.RESTException;
import com.themodernway.server.rest.RESTRequestContext;
import com.themodernway.server.rest.support.spring.IRESTContext;
import com.themodernway.server.rest.support.spring.RESTContextInstance;

public class RESTServlet extends HTTPServletBase
{
    private static final long serialVersionUID = 1L;

    private final long              m_size;

    private final List<String>      m_tags;

    public RESTServlet(final long size, final List<String> tags, final double rate, final List<String> role, final IServletResponseErrorCodeManager code, final ISessionIDFromRequestExtractor extr)
    {
        super(rate, role, code, extr);

        m_size = size;

        if (null != tags)
        {
            m_tags = toUnique(tags);
        }
        else
        {
            m_tags = arrayList();

            if (logger().isErrorEnabled())
            {
                logger().error(LoggingOps.THE_MODERN_WAY_MARKER, "null tags ignored");
            }
        }
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

    protected void doService(final HttpServletRequest request, final HttpServletResponse response, final boolean read, final HttpMethod type, JSONObject body)
    {
        final String bind = FileAndPathUtils.fixPathBinding(toTrimOrElse(request.getPathInfo(), FileAndPathUtils.SINGLE_SLASH));

        if (null == bind)
        {
            if (logger().isErrorEnabled())
            {
                logger().error(LoggingOps.THE_MODERN_WAY_MARKER, "empty service path found.");
            }
            sendErrorCode(request, response, HttpServletResponse.SC_NOT_FOUND);

            return;
        }
        final IRESTService service = getRESTContext().getBinding(bind, type);

        if (null == service)
        {
            if (getRESTContext().isBindingRegistered(bind))
            {
                if (logger().isErrorEnabled())
                {
                    logger().error(LoggingOps.THE_MODERN_WAY_MARKER, format("service (%s) not type (%s).", bind, type));
                }
                sendErrorCode(request, response, HttpServletResponse.SC_METHOD_NOT_ALLOWED);

                return;
            }
            else
            {
                if (logger().isErrorEnabled())
                {
                    logger().error(LoggingOps.THE_MODERN_WAY_MARKER, format("service or binding not found (%s).", bind));
                }
                sendErrorCode(request, response, HttpServletResponse.SC_NOT_FOUND);

                return;
            }
        }
        final List<String> tags = m_tags;

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
                if (logger().isErrorEnabled())
                {
                    logger().error(LoggingOps.THE_MODERN_WAY_MARKER, format("service (%s) for tags not found (%s).", bind, toPrintableString(tags)));
                }
                sendErrorCode(request, response, HttpServletResponse.SC_NOT_FOUND);

                return;
            }
        }
        if (type != service.getRequestMethodType())
        {
            if (logger().isErrorEnabled())
            {
                logger().error(LoggingOps.THE_MODERN_WAY_MARKER, format("service (%s) not type (%s).", bind, type));
            }
            sendErrorCode(request, response, HttpServletResponse.SC_METHOD_NOT_ALLOWED);

            return;
        }
        List<String> uroles = getDefaultRoles();

        final IServerSession session = getSession(requireNonNullOrElse(getSessionIDFromRequestExtractor(), DefaultHeaderNameSessionIDFromRequestExtractor.DEFAULT).getSessionID(request));

        if (null != session)
        {
            uroles = session.getRoles();
        }
        if ((null == uroles) || (uroles.isEmpty()))
        {
            uroles = getDefaultRoles();
        }
        final IAuthorizationResult auth = isAuthorized(request, session, service, uroles);

        if (false == auth.isAuthorized())
        {
            if (logger().isErrorEnabled())
            {
                logger().error(LoggingOps.THE_MODERN_WAY_MARKER, format("service authorization failed for (%s) type (%s) reason (%s).", bind, type, auth.getText()));
            }
            response.addHeader(WWW_AUTHENTICATE, "unauthorized");

            sendErrorCode(request, response, HttpServletResponse.SC_FORBIDDEN);

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
                if (logger().isErrorEnabled())
                {
                    logger().error(LoggingOps.THE_MODERN_WAY_MARKER, format("service (%s) type (%s) null body.", bind, type));
                }
                sendErrorCode(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            return;
        }
        body = clean(body, false);

        final IJSONValidator validator = service.getValidator();

        if (null != validator)
        {
            final IValidationContext context = validator.validate(body);

            if ((null != context) && (false == context.isValid()))
            {
                if (logger().isErrorEnabled())
                {
                    logger().error(LoggingOps.THE_MODERN_WAY_MARKER, format("service (%s) type (%s) invalid body (%s).", bind, type, context.getErrorString()));
                }
                sendErrorCode(request, response, HttpServletResponse.SC_BAD_REQUEST);

                return;
            }
        }
        final IRESTRequestContext context = new RESTRequestContext(service, session, uroles, getServletContext(), request, response, type);

        try
        {
            service.acquire();

            final NanoTimer timer = new NanoTimer();

            final Object object = service.call(context, body);

            if (logger().isInfoEnabled())
            {
                logger().info(LoggingOps.THE_MODERN_WAY_MARKER, format("calling service (%s) took %s.", bind, timer.toString()));
            }
            if (context.isOpen())
            {
                if (object instanceof IResponseAction)
                {
                    ((IResponseAction) object).call(request, response, getServletResponseErrorCodeManager());

                    return;
                }
                JSONObject result = json(object);

                if (null != result)
                {
                    result = clean(result, true);
                }
                writeBODY(context, request, response, result);
            }
            else if (logger().isErrorEnabled())
            {
                logger().error(LoggingOps.THE_MODERN_WAY_MARKER, format("calling service (%s) context closed.", bind));
            }
        }
        catch (final RESTException e)
        {
            if (context.isOpen())
            {
                sendErrorCode(request, response, e.getCode(), e.getReason());
            }
            else if (logger().isErrorEnabled())
            {
                logger().error(LoggingOps.THE_MODERN_WAY_MARKER, format("calling service (%s) context closed.", bind));
            }
        }
        catch (final Exception e)
        {
            final String uuid = uuid();

            if (logger().isErrorEnabled())
            {
                logger().error(LoggingOps.THE_MODERN_WAY_MARKER, format("error calling (%s) uuid (%s).", bind, uuid), e);
            }
            sendErrorCode(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, uuid);
        }
    }

    protected JSONObject json(final Object object)
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
                    size = m_size;
                }
                if ((size > 0L) && (leng > size))
                {
                    if (logger().isErrorEnabled())
                    {
                        logger().error(LoggingOps.THE_MODERN_WAY_MARKER, format("error calling (%s) length (%d) greater than (%d).", bind, leng, size));
                    }
                    sendErrorCode(request, response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);

                    return null;
                }
                try
                {
                    if (size > 0L)
                    {
                        final String buff = IO.getStringAtMost(request.getReader(), size);

                        if (buff.length() > size)
                        {
                            if (logger().isErrorEnabled())
                            {
                                logger().error(LoggingOps.THE_MODERN_WAY_MARKER, format("error calling (%s) length (%d) greater than (%d).", bind, buff.length(), size));
                            }
                            sendErrorCode(request, response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);

                            return null;
                        }
                        return BinderType.forContentType(request.getContentType()).getBinder().bindJSON(buff);
                    }
                    return BinderType.forContentType(request.getContentType()).getBinder().bindJSON(request.getReader());
                }
                catch (final ParserException e)
                {
                    if (logger().isErrorEnabled())
                    {
                        logger().error(LoggingOps.THE_MODERN_WAY_MARKER, format("error calling (%s) ParserException.", bind), e);
                    }
                    return null;
                }
                catch (final IOException e)
                {
                    if (logger().isErrorEnabled())
                    {
                        logger().error(LoggingOps.THE_MODERN_WAY_MARKER, format("error calling (%s) IOException.", bind), e);
                    }
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

    protected void writeBODY(final IRESTRequestContext context, final HttpServletRequest request, final HttpServletResponse response, final JSONObject output) throws IOException
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
        response.setStatus(HttpServletResponse.SC_OK);

        final PrintWriter stream = response.getWriter();

        stream.flush();

        try
        {
            if (isStrict(request))
            {
                BinderType.forContentType(type).getBinder().setStrict().send(stream, output);
            }
            else
            {
                BinderType.forContentType(type).getBinder().send(stream, output);
            }
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
}