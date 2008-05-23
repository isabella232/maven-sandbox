//========================================================================
//Copyright 2008 Sonatype Inc.
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package org.apache.maven.mercury.client;

import java.io.File;

/**
 * Binding
 * <p/>
 * A Binding represents a remote url whose contents are to be downloaded
 * and stored in a local file, or a local file whose contents are to be
 * uploaded to the remote url.
 */
public class Binding
{
    protected String remoteUrl;
    protected File localFile;
    protected boolean lenientChecksum;

    public String getRemoteUrl()
    {
        return remoteUrl;
    }

    public void setRemoteUrl( String remoteUrl )
    {
        this.remoteUrl = remoteUrl;
    }

    public File getLocalFile()
    {
        return localFile;
    }

    public void setLocalFile( File localFile )
    {
        this.localFile = localFile;
    }

    public boolean isLenientChecksum()
    {
        return lenientChecksum;
    }

    public void setLenientChecksum( boolean leniantChecksum )
    {
        this.lenientChecksum = leniantChecksum;
    }

    public String toString()
    {
        return "[" + remoteUrl + "," + localFile + "," + lenientChecksum + "]";
    }
}
