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

import java.util.function.Function;

import com.themodernway.server.core.cache.AbstractConcurrentCache;
import com.themodernway.server.core.json.binder.BinderType;
import com.themodernway.server.core.json.binder.IBinder;

public class RESTBinderCache extends AbstractConcurrentCache<IBinder>
{
    private final boolean m_strict;

    public RESTBinderCache(final String name, final boolean strict)
    {
        super(name);

        m_strict = strict;
    }

    public boolean isStrict()
    {
        return m_strict;
    }

    @Override
    public Function<String, IBinder> getMappingFunction()
    {
        return type -> (isStrict() ? BinderType.forContentType(type).getBinder().setStrict() : BinderType.forContentType(type).getBinder());
    }
}
