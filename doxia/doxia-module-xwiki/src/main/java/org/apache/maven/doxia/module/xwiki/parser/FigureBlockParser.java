package org.apache.maven.doxia.module.xwiki.parser;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.doxia.module.xwiki.blocks.Block;
import org.apache.maven.doxia.parser.ParseException;
import org.apache.maven.doxia.util.ByLineSource;

/**
 * This parser is required to handle image macro not inside a paragraph (i.e. found on a line by itself).
 * However note that macros (and thus this image macro) are also parsed by
 * {@link org.apache.maven.doxia.module.xwiki.parser.MacroParser} when they're inside a paragraph.
 */
public class FigureBlockParser
    extends AbstractBlockParser
{
    private static String LS = System.getProperty( "line.separator" );

    private MacroParser macroParser = new MacroParser();

    public boolean accept( String line, ByLineSource source )
    {
        return line.startsWith( "{image:" );
    }

    public Block visit( String line, ByLineSource source )
        throws ParseException
    {
        macroParser.setCompatibilityMode( isInCompatibilityMode() );
        MacroParser.MacroParserResult result = macroParser.parse( line, 1 );
        return result.block;
    }
}
