/*
 * Zed Attack Proxy (ZAP) and its related class files.
 * 
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 * 
 * Copyright 2013 The ZAP Development Team
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
package org.zaproxy.zap.extension.zest.menu;

import java.util.List;

import org.parosproxy.paros.Constant;
import org.parosproxy.paros.model.HistoryReference;
import org.zaproxy.zap.extension.script.ScriptNode;
import org.zaproxy.zap.extension.zest.ExtensionZest;
import org.zaproxy.zap.view.PopupMenuHistoryReference;

public class ZestAddToScriptMenu extends PopupMenuHistoryReference {

	private static final long serialVersionUID = 2282358266003940700L;
	
	private ExtensionZest extension;
	private ScriptNode parent;
	private String prefix = null;

	/**
	 * This method initializes 
	 * 
	 */
	public ZestAddToScriptMenu(ExtensionZest extension) {
		super(Constant.messages.getString("zest.addto.new.title"), true);
		this.extension = extension;
		this.parent = null;
		this.setPrecedeWithSeparator(true);
	}
	    
	public ZestAddToScriptMenu(ExtensionZest extension, ScriptNode parent) {
		super(parent.getNodeName(), true);
		this.extension = extension;
		this.parent = parent;
	}
	
    @Override
    public String getParentMenuName() {
    	return Constant.messages.getString("zest.addto.popup");
    }
    
    @Override
    public boolean isSubMenu() {
    	return true;
    }

	@Override
	public void performAction(HistoryReference href) throws Exception {
		if (href.getHttpMessage() == null) {
			return;
		}
		extension.addToParent(parent, href.getHttpMessage(), prefix);
	}
	
	@Override
    public void performActions (List<HistoryReference> hrefs) throws Exception {
		// Work out common root??
		String prefix2 = null;
		String url = null;
		for (HistoryReference href : hrefs) {
			url = href.getURI().toString();
			if (prefix2 == null) {
				// First one - select up to the last /
				prefix2 = url.substring(0, url.lastIndexOf("/"));
			} else if (!url.startsWith(prefix2)) {
				while (!url.startsWith(prefix2)) {
					prefix2 = prefix2.substring(0, prefix2.length() - 2);
				}
			}
		}
		this.prefix = prefix2;
		super.performActions(hrefs);
	}

	@Override
	public boolean isEnableForInvoker(Invoker invoker) {
		this.setEnabled(true);
		return true;
	}
	
    @Override
    public boolean isSafe() {
    	return true;
    }
}
