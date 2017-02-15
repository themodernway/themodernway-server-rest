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

package com.themodernway.server.rest.system.standard.services

import com.themodernway.server.core.json.JSONObject
import com.themodernway.server.rest.*

import groovy.transform.CompileStatic

@CompileStatic
public abstract class AbstractStandardEchoRequest extends RESTServiceSupport
{
    @Override
    public JSONObject execute(final IRESTRequestContext context, final JSONObject object) throws Exception
    {        
        def rqst = context.getServletRequest()
        
        json(path: getRequestBinding(), method: getRequestMethodType().name(), url: rqst.getRequestURL(), protocol: rqst.getProtocol(), type: rqst.getContentType(), body: object, headers: context.getJSONHeaders())
    }
}
