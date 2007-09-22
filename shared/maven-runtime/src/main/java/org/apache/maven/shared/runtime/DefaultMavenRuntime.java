package org.apache.maven.shared.runtime;

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

import java.util.List;

/**
 * Default implementation of <code>MavenRuntime</code>.
 * 
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id$
 * @see MavenRuntime
 * @plexus.component role="org.apache.maven.shared.runtime.MavenRuntime"
 */
public class DefaultMavenRuntime implements MavenRuntime
{
    // MavenRuntime methods ---------------------------------------------------

    /*
     * @see org.apache.maven.shared.runtime.MavenRuntime#getProjectProperties(java.lang.ClassLoader)
     */
    public List getProjectProperties( ClassLoader classLoader ) throws MavenRuntimeException
    {
        PropertiesMavenRuntimeVisitor visitor = new PropertiesMavenRuntimeVisitor();

        new ClassLoaderHelper( classLoader ).accept( visitor );

        return visitor.getProjects();
    }

    /*
     * @see org.apache.maven.shared.runtime.MavenRuntime#getProjects(java.lang.ClassLoader)
     */
    public List getProjects( ClassLoader classLoader ) throws MavenRuntimeException
    {
        XMLMavenRuntimeVisitor visitor = new XMLMavenRuntimeVisitor();

        new ClassLoaderHelper( classLoader ).accept( visitor );

        return visitor.getProjects();
    }

    /*
     * @see org.apache.maven.shared.runtime.MavenRuntime#getSortedProjects(java.lang.ClassLoader)
     */
    public List getSortedProjects( ClassLoader classLoader ) throws MavenRuntimeException
    {
        XMLMavenRuntimeVisitor visitor = new XMLMavenRuntimeVisitor();

        new ClassLoaderHelper( classLoader ).accept( visitor );

        return visitor.getSortedProjects();
    }
}