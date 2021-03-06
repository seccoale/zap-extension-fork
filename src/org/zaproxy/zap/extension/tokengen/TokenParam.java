/*
 * Zed Attack Proxy (ZAP) and its related class files.
 * 
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */
package org.zaproxy.zap.extension.tokengen;

import org.parosproxy.paros.common.AbstractParam;

public class TokenParam extends AbstractParam {

	private static final String THREADS_PER_SCAN = "tokengen.threadPerScan";
		
	private int threadPerScan = 20;
	
    /**
     * @param rootElementName
     */
    public TokenParam() {
    }

    @Override
    protected void parse(){
        
		try {
			setThreadPerScan(getConfig().getInt(THREADS_PER_SCAN, 1));
		} catch (Exception e) {}
    }

	public int getThreadPerScan() {
		return threadPerScan;
	}

	public void setThreadPerScan(int threadPerScan) {
		this.threadPerScan = threadPerScan;
        getConfig().setProperty(THREADS_PER_SCAN, Integer.toString(this.threadPerScan));
	}
	
}
