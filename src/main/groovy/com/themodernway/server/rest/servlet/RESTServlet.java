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

import org.apache.log4j.Logger;
import org.springframework.http.HttpMethod;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.themodernway.common.api.java.util.StringOps;
import com.themodernway.server.core.json.JSONObject;
import com.themodernway.server.core.json.ParserException;
import com.themodernway.server.core.json.binder.BinderType;
import com.themodernway.server.core.json.binder.IBinder;
import com.themodernway.server.core.security.AuthorizationResult;
import com.themodernway.server.core.security.session.IServerSession;
import com.themodernway.server.core.security.session.IServerSessionHelper;
import com.themodernway.server.core.security.session.IServerSessionRepository;
import com.themodernway.server.core.servlet.HTTPServletBase;
import com.themodernway.server.rest.IRESTService;
import com.themodernway.server.rest.RESTException;
import com.themodernway.server.rest.RESTRequestContext;
import com.themodernway.server.rest.support.spring.IRESTContext;
import com.themodernway.server.rest.support.spring.RESTContextInstance;

@SuppressWarnings("serial")
public class RESTServlet extends HTTPServletBase
{
    private static final Logger logger = Logger.getLogger(RESTServlet.class);

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
        if (false == isRunning())
        {
            logger.error("server is suspended, refused request");

            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);

            return;
        }
        if (read)
        {
            object = parseBODY(request, type);
        }
        if (null == object)
        {
            logger.error("passed body is not a JSONObject");

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            return;
        }
        String name = null;

        boolean irpc = false;

        if ((read) && isCommandInBody())
        {
            irpc = true;

            name = StringOps.toTrimOrNull(object.getAsString("command"));

            if (null == name)
            {
                logger.error("no command keys found in body");

                response.setStatus(HttpServletResponse.SC_NOT_FOUND);

                return;
            }
        }
        else
        {
            name = StringOps.toTrimOrNull(request.getPathInfo());

            if (null != name)
            {
                int indx = name.indexOf("/");

                if (indx >= 0)
                {
                    name = StringOps.toTrimOrNull(name.substring(indx + 1));
                }
                if (null != name)
                {
                    if (name.contains(".rpc"))
                    {
                        irpc = true;
                    }
                    name = getRESTContext().fixRequestBinding(name);
                }
            }
            if (null == name)
            {
                logger.error("empty service path found");

                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                return;
            }
        }
        IRESTService service = getRESTContext().getService(name);

        if (null == service)
        {
            service = getRESTContext().getBinding(name);

            if (null == service)
            {
                logger.error("service or binding not found " + name);

                response.setStatus(HttpServletResponse.SC_NOT_FOUND);

                return;
            }
        }
        if (type != service.getRequestMethodType())
        {
            logger.error("service " + name + " not type " + type);

            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);

            return;
        }
        if ((read) && (irpc))
        {
            if (false == object.isDefined("request"))
            {
                logger.error("no request key found");

                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                return;
            }
            object = object.getAsObject("request");

            if (null == object)
            {
                logger.error("empty request key found");

                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

                return;
            }
        }
        IServerSession session = null;

        List<String> uroles = getDefaultRoles(request);

        String userid = StringOps.toTrimOrNull(request.getHeader(X_USER_ID_HEADER));

        String sessid = StringOps.toTrimOrNull(request.getHeader(X_SESSION_ID_HEADER));

        String ctoken = StringOps.toTrimOrNull(request.getHeader(X_CLIENT_API_TOKEN_HEADER));

        String strict = StringOps.toTrimOrNull(request.getHeader(X_STRICT_JSON_FORMAT_HEADER));

        if (null != sessid)
        {
            final IServerSessionRepository repository = getRESTContext().getServerSessionRepository(getSessionProviderDomainName());

            if (null != repository)
            {
                session = repository.getSession(sessid);

                if (null == session)
                {
                    logger.error("unknown session " + sessid);

                    response.addHeader(WWW_AUTHENTICATE, "unknown session " + sessid);

                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);

                    return;
                }
                if (session.isExpired())
                {
                    logger.error("expired session " + sessid);

                    response.addHeader(WWW_AUTHENTICATE, "expired session " + sessid);

                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);

                    return;
                }
                uroles = session.getRoles();

                sessid = StringOps.toTrimOrElse(session.getId(), sessid);

                userid = StringOps.toTrimOrElse(session.getUserId(), userid);
            }
        }
        else if (null != ctoken)
        {
            final IServerSessionRepository repository = getRESTContext().getServerSessionRepository(getSessionProviderDomainName());

            if (null != repository)
            {
                session = repository.createSession(new JSONObject(X_CLIENT_API_TOKEN_HEADER, ctoken));

                if (null == session)
                {
                    logger.error("unknown token " + ctoken);

                    response.addHeader(WWW_AUTHENTICATE, "unknown token " + ctoken);

                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);

                    return;
                }
                if (session.isExpired())
                {
                    logger.error("expired session " + sessid);

                    response.addHeader(WWW_AUTHENTICATE, "expired session " + sessid);

                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);

                    return;
                }
                uroles = session.getRoles();

                sessid = StringOps.toTrimOrElse(session.getId(), sessid);

                userid = StringOps.toTrimOrElse(session.getUserId(), userid);
            }
        }
        if ((null == uroles) || (uroles.isEmpty()))
        {
            uroles = getDefaultRoles(request);
        }
        final AuthorizationResult resp = isAuthorized(request, session, service, uroles);

        if (false == resp.isAuthorized())
        {
            if (null == userid)
            {
                userid = UNKNOWN_USER;
            }
            logger.error("service authorization failed " + name + " for user " + userid + " code " + resp.getText());

            response.addHeader(WWW_AUTHENTICATE, "unauthorized " + resp.getText());

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

            return;
        }
        final RESTRequestContext context = new RESTRequestContext(session, userid, sessid, resp.isAdmin(), uroles, getServletContext(), request, response, type);

        try
        {
            final long time = System.nanoTime();

            final long tick = System.currentTimeMillis();

            service.acquire();

            JSONObject result = service.execute(context, object);

            final long fast = System.nanoTime() - time;

            final long done = System.currentTimeMillis() - tick;

            if (done < 1)
            {
                logger.info("calling service " + name + " took " + fast + " nano's");
            }
            else
            {
                logger.info("calling service " + name + " took " + done + " ms's");
            }
            if (null == result)
            {
                logger.error("service returned null JSON " + name);

                result = new JSONObject();
            }
            result = clean(result);

            if (false == context.isClosed())
            {
                if (irpc)
                {
                    writeBODY(HttpServletResponse.SC_OK, request, response, new JSONObject("result", result), isStrict(strict));
                }
                else
                {
                    writeBODY(HttpServletResponse.SC_OK, request, response, result, isStrict(strict));
                }
            }
        }
        catch (RESTException e)
        {
            if (false == context.isClosed())
            {
                writeBODY(e.getCode(), request, response, new JSONObject("error", new JSONObject("code", e.getCode()).set("reason", e.getReason())), isStrict(strict));
            }
        }
        catch (Throwable e)
        {
            final String oops = "calling " + name + " error uuid " + getRESTContext().uuid();

            logger.error(oops, e);

            if (false == context.isClosed())
            {
                writeBODY(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, request, response, new JSONObject("error", new JSONObject("code", HttpServletResponse.SC_INTERNAL_SERVER_ERROR).set("reason", oops)), isStrict(strict));
            }
        }
    }

    protected JSONObject clean(final JSONObject json)
    {
        return json;
    }

    protected List<String> getDefaultRoles(final HttpServletRequest request)
    {
        return IServerSessionHelper.SP_DEFAULT_ROLES_LIST;
    }

    protected boolean isStrict(String strict)
    {
        strict = StringOps.toTrimOrNull(strict);

        if ((null != strict) && ("true".equalsIgnoreCase(strict)))
        {
            return true;
        }
        return false;
    }

    protected boolean isCommandInBody()
    {
        return false;
    }

    protected JSONObject parseBODY(final HttpServletRequest request, final HttpMethod type)
    {
        if (type != HttpMethod.GET)
        {
            final int leng = request.getContentLength();

            if (leng > 0)
            {
                IBinder bind = null;

                final String cont = StringOps.toTrimOrElse(request.getContentType(), CONTENT_TYPE_APPLICATION_JSON).toLowerCase();

                if (cont.contains(CONTENT_TYPE_APPLICATION_JSON))
                {
                    bind = getServerContext().binder(BinderType.JSON);
                }
                else if ((cont.contains(CONTENT_TYPE_TEXT_XML)) || (cont.contains(CONTENT_TYPE_APPLICATION_XML)))
                {
                    bind = getServerContext().binder(BinderType.XML);
                }
                else if ((cont.contains(CONTENT_TYPE_TEXT_YAML)) || (cont.contains(CONTENT_TYPE_APPLICATION_YAML)))
                {
                    bind = getServerContext().binder(BinderType.YAML);
                }
                else
                {
                    bind = getServerContext().binder(BinderType.JSON);
                }
                try
                {
                    return bind.bindJSON(request.getInputStream());
                }
                catch (ParserException e)
                {
                    logger.error("ParserException", e);

                    return null;
                }
                catch (IOException e)
                {
                    logger.error("IOException", e);

                    return null;
                }
            }
            if (leng == 0)
            {
                logger.error("empty body on " + type.name());

                return new JSONObject();
            }
        }
        return new JSONObject();
    }

    protected void writeBODY(final int code, final HttpServletRequest request, final HttpServletResponse response, JSONObject output, final boolean strict) throws IOException
    {
        doNeverCache(request, response);

        response.setStatus(code);

        IBinder bind = null;

        final String cont = StringOps.toTrimOrElse(request.getHeader(ACCEPT), CONTENT_TYPE_APPLICATION_JSON).toLowerCase();

        if (cont.contains(CONTENT_TYPE_APPLICATION_JSON))
        {
            response.setContentType(CONTENT_TYPE_APPLICATION_JSON);

            bind = getServerContext().binder(BinderType.JSON);
        }
        else if (cont.contains(CONTENT_TYPE_TEXT_XML))
        {
            output = new JSONResult(output);

            response.setContentType(CONTENT_TYPE_TEXT_XML);

            bind = getServerContext().binder(BinderType.XML);
        }
        else if (cont.contains(CONTENT_TYPE_APPLICATION_XML))
        {
            output = new JSONResult(output);

            response.setContentType(CONTENT_TYPE_APPLICATION_XML);

            bind = getServerContext().binder(BinderType.XML);
        }
        else if (cont.contains(CONTENT_TYPE_TEXT_YAML))
        {
            response.setContentType(CONTENT_TYPE_TEXT_YAML);

            bind = getServerContext().binder(BinderType.YAML);
        }
        else if (cont.contains(CONTENT_TYPE_APPLICATION_YAML))
        {
            response.setContentType(CONTENT_TYPE_APPLICATION_YAML);

            bind = getServerContext().binder(BinderType.YAML);
        }
        else
        {
            response.setContentType(CONTENT_TYPE_APPLICATION_JSON);

            bind = getServerContext().binder(BinderType.JSON);
        }
        bind = bind.setStrict(strict);

        final OutputStream stream = response.getOutputStream();

        stream.flush();

        try
        {
            bind.send(stream, output);
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

    @Override
    public void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
    {
        if ("PATCH".equalsIgnoreCase(request.getMethod()))
        {
            doPatch(request, response);

            return;
        }
        super.service(request, response);
    }

    @JacksonXmlRootElement(localName = "result")
    protected static class JSONResult extends JSONObject
    {
        public JSONResult(final JSONObject object)
        {
            super(object);
        }
    }
}