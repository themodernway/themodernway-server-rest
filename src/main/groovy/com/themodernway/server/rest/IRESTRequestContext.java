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

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;

import com.themodernway.server.core.json.JSONObject;
import com.themodernway.server.core.security.session.IServerSession;
import com.themodernway.server.core.servlet.IServletCommonOperations;
import com.themodernway.server.rest.support.spring.IRESTContext;

public interface IRESTRequestContext extends IServletCommonOperations, Closeable
{
    public IRESTService getService();

    public IServerSession getSession();

    public JSONObject getJSONHeaders();

    public JSONObject getJSONParameters();

    public HttpMethod getRequestType();

    public IRESTContext getRESTContext();

    public ServletContext getServletContext();

    public HttpServletRequest getServletRequest();

    public HttpServletResponse getServletResponse();

    public List<String> getRoles();

    public boolean isDelete();

    public boolean isGet();

    public boolean isHead();

    public boolean isPost();

    public boolean isPut();

    public Cookie newCookie(String name, String value);

    public Cookie newCookie(String name, String value, String path);

    public Cookie newCookie(String name, String value, TimeUnit unit, long duration);

    public Cookie newCookie(String name, String value, String path, TimeUnit unit, long duration);

    public boolean isOpen();
}
