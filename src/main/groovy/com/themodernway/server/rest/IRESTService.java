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

import org.springframework.http.HttpMethod;

import com.themodernway.server.core.json.JSONObject;
import com.themodernway.server.core.json.validation.IJSONValidator;
import com.themodernway.server.core.limiting.IRateLimited;

public interface IRESTService extends IRateLimited, Closeable
{
    public String getRequestBinding();

    public long getMaxRequestBodySize();

    public List<String> getTaggingValues();

    public HttpMethod getRequestMethodType();

    public IJSONValidator getValidator();

    public Object call(IRESTRequestContext context, JSONObject object) throws Exception;

    public Object exec(IRESTRequestContext context, JSONObject object) throws Exception;

    public boolean init(IRESTRequestContext context, JSONObject object) throws Exception;

    public boolean done(IRESTRequestContext context, JSONObject object, Object answer) throws Exception;
}
