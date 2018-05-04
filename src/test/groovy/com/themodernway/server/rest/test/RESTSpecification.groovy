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

package com.themodernway.server.rest.test

import org.springframework.http.HttpMethod

import com.themodernway.server.core.NanoTimer
import com.themodernway.server.core.support.CoreGroovyTrait
import com.themodernway.server.core.support.spring.testing.spock.ServerCoreSpecification
import com.themodernway.server.rest.support.RESTGroovyTrait

public class RESTSpecification extends ServerCoreSpecification implements CoreGroovyTrait, RESTGroovyTrait
{
    def setupSpec()
    {
        setupServerCoreDefault(RESTSpecification,
            "classpath:/com/themodernway/server/rest/test/ApplicationContext.xml",
            "classpath:/com/themodernway/server/core/config/CoreApplicationContext.xml",
            "classpath:/com/themodernway/server/rest/config/RESTApplicationContext.xml"
        )
        getRESTServiceRegistry()
    }

    def cleanupSpec()
    {
        closeServerCoreDefault()
    }

    def "Test REST 0"()
    {
        setup:
        def bind = getBinding('/demo/limited', HttpMethod.GET)
        def time = new NanoTimer()
        bind.acquire()
        def json = bind.exec(null, json())
        echo time
        echo json

        expect:
        true == true
    }

    def "Test REST 1"()
    {
        setup:
        def bind = getBinding('/demo/testing', HttpMethod.POST)
        def time = new NanoTimer()
        bind.acquire()
        def json = bind.exec(null, json())
        echo time
        echo json

        expect:
        true == true
    }
}
