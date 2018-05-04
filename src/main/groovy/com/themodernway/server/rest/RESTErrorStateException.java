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

import com.themodernway.common.api.java.util.CommonOps;
import com.themodernway.common.api.java.util.StringOps;

public class RESTErrorStateException extends Exception
{
    private static final long    serialVersionUID = 1L;

    private final RESTErrorState m_state;

    public RESTErrorStateException(final RESTErrorState state)
    {
        this(CommonOps.requireNonNullOrElse(state, RESTErrorState.UNKNOWN).toString(), state);
    }

    public RESTErrorStateException(final String message, final RESTErrorState state)
    {
        super(StringOps.requireTrimOrNull(message));

        m_state = CommonOps.requireNonNullOrElse(state, RESTErrorState.UNKNOWN);
    }

    public RESTErrorState getState()
    {
        return m_state;
    }
}
