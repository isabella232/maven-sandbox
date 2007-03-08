package org.apache.maven.plugin.descriptor;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Describes a component requirement.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id: Requirement.java 495147 2007-01-11 07:47:53Z jvanzyl $
 */
public class Requirement
{
    private final String role;

    private final String roleHint;

    public Requirement( String role )
    {
        this.role = role;
        this.roleHint = null;
    }

    public Requirement( String role, String roleHint )
    {
        this.role = role;
        this.roleHint = roleHint;
    }

    public String getRole()
    {
        return role;
    }

    public String getRoleHint()
    {
        return roleHint;
    }
}
