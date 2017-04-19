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
import java.io.OutputStream;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;

import com.themodernway.server.core.ITimeSupplier;
import com.themodernway.server.core.json.JSONObject;
import com.themodernway.server.core.json.ParserException;
import com.themodernway.server.core.json.binder.BinderType;
import com.themodernway.server.core.security.AuthorizationResult;
import com.themodernway.server.core.security.session.IServerSession;
import com.themodernway.server.core.security.session.IServerSessionHelper;
import com.themodernway.server.core.servlet.HTTPServletBase;
import com.themodernway.server.rest.IRESTService;
import com.themodernway.server.rest.RESTException;
import com.themodernway.server.rest.RESTRequestContext;
import com.themodernway.server.rest.support.spring.IRESTContext;
import com.themodernway.server.rest.support.spring.RESTContextInstance;

@SuppressWarnings("serial")
public class RESTServlet extends HTTPServletBase
{
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

        response.setContentLength(0);

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
        doService(request, response, true, HttpMethod.DELETE, new JSONObject());
    }

    protected void doService(final HttpServletRequest request, final HttpServletResponse response, final boolean read, final HttpMethod type, JSONObject object) throws ServletException, IOException
    {
        String name = toTrimOrNull(request.getPathInfo());

        if (null != name)
        {
            int indx = name.indexOf("/");

            if (indx >= 0)
            {
                name = toTrimOrNull(name.substring(indx + 1));
            }
            if (null != name)
            {
                name = getRESTContext().fixRequestBinding(name);
            }
        }
        if (null == name)
        {
            logger().error("empty service path found.");

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            return;
        }
        IRESTService service = getRESTContext().getBinding(name);

        if (null == service)
        {
            service = getRESTContext().getService(name);

            if (null == service)
            {
                logger().error(format("service or binding not found (%s).", name));

                response.setStatus(HttpServletResponse.SC_NOT_FOUND);

                return;
            }
        }
        if (type != service.getRequestMethodType())
        {
            logger().error(format("service (%s) not type (%s).", name, type));

            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);

            return;
        }
        List<String> uroles = arrayList();

        IServerSession session = getSession(request);

        if (null != session)
        {
            uroles = session.getRoles();
        }
        if ((null == uroles) || (uroles.isEmpty()))
        {
            uroles = getDefaultRoles(request);
        }
        final AuthorizationResult resp = isAuthorized(request, session, service, uroles);

        if (false == resp.isAuthorized())
        {
            logger().error(format("service authorization failed for (%s) reason (%s).", name, resp.getText()));

            response.addHeader(WWW_AUTHENTICATE, "unauthorized");

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

            return;
        }
        if (read)
        {
            object = parseBODY(request, type);
        }
        if (null == object)
        {
            logger().error("body is null.");

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            return;
        }
        object = clean(object, false);

        final RESTRequestContext context = new RESTRequestContext(session, uroles, getServletContext(), request, response, type);

        try
        {
            service.acquire();

            final long mills = ITimeSupplier.mills().getTime();

            final long nanos = ITimeSupplier.nanos().getTime();

            JSONObject result = service.execute(context, object);

            final long ndiff = ITimeSupplier.nanos().getTime() - nanos;

            final long mdiff = ITimeSupplier.mills().getTime() - mills;

            if (mdiff < 1)
            {
                logger().info(format("calling service (%s) took (%s) nanos.", name, ndiff));
            }
            else
            {
                logger().info(format("calling service (%s) took (%s) mills.", name, mdiff));
            }
            if (null != result)
            {
                result = clean(result, true);
            }
            if (false == context.isClosed())
            {
                writeBODY(HttpServletResponse.SC_OK, request, response, result);
            }
        }
        catch (RESTException e)
        {
            if (false == context.isClosed())
            {
                errorBODY(e.getCode(), request, response, e.getReason());
            }
        }
        catch (Throwable e)
        {
            final String uuid = uuid();

            logger().error(format("error calling (%s) uuid (%s).", name, uuid), e);

            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, uuid);
        }
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

    protected JSONObject parseBODY(final HttpServletRequest request, final HttpMethod type)
    {
        if (type != HttpMethod.GET)
        {
            final int leng = request.getContentLength();

            if (leng > 0)
            {
                try
                {
                    return BinderType.forContentType(request.getContentType()).getBinder().bindJSON(request.getInputStream());
                }
                catch (ParserException e)
                {
                    logger().error("ParserException", e);

                    return null;
                }
                catch (IOException e)
                {
                    logger().error("IOException", e);

                    return null;
                }
            }
            if (leng == 0)
            {
                return new JSONObject();
            }
        }
        return new JSONObject();
    }

    protected void errorBODY(final int code, final HttpServletRequest request, final HttpServletResponse response, final String reason) throws IOException
    {
        writeBODY(code, request, response, new JSONObject("error", new JSONObject("code", code).set("reason", reason)));
    }

    protected void writeBODY(final int code, final HttpServletRequest request, final HttpServletResponse response, JSONObject output) throws IOException
    {
        logger().info("writeBODY()");

        doNeverCache(request, response);

        final String type = toTrimOrElse(request.getHeader(ACCEPT), CONTENT_TYPE_APPLICATION_JSON).toLowerCase();

        if (type.contains(CONTENT_TYPE_APPLICATION_JSON))
        {
            response.setContentType(CONTENT_TYPE_APPLICATION_JSON);
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
            response.setContentLength(0);

            response.setStatus(HttpServletResponse.SC_NO_CONTENT);

            return;
        }
        response.setStatus(code);

        final OutputStream stream = response.getOutputStream();

        stream.flush();

        try
        {
            BinderType.forContentType(type).getBinder().setStrict(isStrict(request)).send(stream, output);
        }
        catch (ParserException e)
        {
            throw new IOException(e);
        }
        stream.flush();
    }

    protected final IRESTContext getRESTContext()
    {
        return RESTContextInstance.getRESTContextInstance();
    }
}