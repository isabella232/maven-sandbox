package org.apache.maven.artifact.repository.metadata;

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
 * Error while retrieving repository metadata from the repository.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id: RepositoryMetadataResolutionException.java 495147 2007-01-11 07:47:53Z jvanzyl $
 */
public class RepositoryMetadataResolutionException
    extends Exception
{
    public RepositoryMetadataResolutionException( String message )
    {
        super( message );
    }

    public RepositoryMetadataResolutionException( String message, Exception e )
    {
        super( message, e );
    }
}
