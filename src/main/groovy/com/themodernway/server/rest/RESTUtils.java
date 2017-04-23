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

package com.themodernway.server.rest;

import java.util.regex.Pattern;

import com.themodernway.common.api.java.util.StringOps;
import com.themodernway.server.core.file.FilePathUtils;

public final class RESTUtils
{
    private final static Pattern NO_WHITESPACE = Pattern.compile("\\s");
    
    private RESTUtils()
    {
    }

    public static final String fixBinding(String bind)
    {
        if (null != (bind = StringOps.toTrimOrNull(bind)))
        {
            if (null != (bind = StringOps.toTrimOrNull(FilePathUtils.normalize(NO_WHITESPACE.matcher(bind).replaceAll(StringOps.EMPTY_STRING)))))
            {
                if (false == bind.startsWith(FilePathUtils.SINGLE_SLASH))
                {
                    bind = FilePathUtils.SINGLE_SLASH + bind;
                }
                while ((bind.length() > 1) && (bind.endsWith(FilePathUtils.SINGLE_SLASH)))
                {
                    bind = bind.substring(0, bind.length() - 1);
                }
                bind = StringOps.toTrimOrNull(bind);
            }
        }
        return bind;
    }
}
