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
package org.zaproxy.zap.extension.zest.dialogs;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.net.MalformedURLException;

import org.apache.log4j.Logger;
import org.mozilla.zest.core.v1.ZestAction;
import org.mozilla.zest.core.v1.ZestAssertion;
import org.mozilla.zest.core.v1.ZestConditional;
import org.mozilla.zest.core.v1.ZestRequest;
import org.mozilla.zest.core.v1.ZestScript;
import org.mozilla.zest.core.v1.ZestStatement;
import org.mozilla.zest.core.v1.ZestTransformation;
import org.mozilla.zest.impl.ZestScriptEngineFactory;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.extension.AbstractPanel;
import org.parosproxy.paros.network.HttpMessage;
import org.parosproxy.paros.view.View;
import org.zaproxy.zap.extension.script.ExtensionScript;
import org.zaproxy.zap.extension.script.ScriptNode;
import org.zaproxy.zap.extension.script.ScriptUI;
import org.zaproxy.zap.extension.script.ScriptWrapper;
import org.zaproxy.zap.extension.zest.ExtensionZest;
import org.zaproxy.zap.extension.zest.ZestElementWrapper;
import org.zaproxy.zap.extension.zest.ZestScriptWrapper;
import org.zaproxy.zap.extension.zest.ZestZapUtils;

public class ZestDialogManager extends AbstractPanel {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(ZestDialogManager.class);
	
	private ExtensionZest extension = null;
	private ScriptUI scriptUI = null;

	private ZestScriptsDialog scriptDialog = null;
	private ZestRequestDialog requestDialog = null;
	private ZestAssertionsDialog assertionsDialog = null;
	private ZestActionDialog actionDialog = null;
	private ZestConditionDialog conditionDialog = null;
	private ZestTransformDialog transformationDialog = null;

	public ZestDialogManager(ExtensionZest extension, ScriptUI scriptUI) {
		super();
		this.extension = extension;
		this.scriptUI = scriptUI;
		initialize();
	}
	
	private void initialize() {
        this.setLayout(new CardLayout());
        this.setName(Constant.messages.getString("zest.scripts.panel.title"));
		this.setIcon(ExtensionZest.ZEST_ICON);
		
		this.scriptUI.addMouseListener(new java.awt.event.MouseAdapter() { 
			@Override
			public void mousePressed(java.awt.event.MouseEvent e) {
			}
				
			@Override
			public void mouseReleased(java.awt.event.MouseEvent e) {
				mouseClicked(e);
			}
			
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
			    if (e.getClickCount() > 1) {
			    	// Its a double click - edit the node
			    	ScriptNode sn = scriptUI.getSelectedNode();
			    	if (sn == null || sn.getUserObject() == null) {
			    		return;
			    	}
			    	
					ScriptNode parent = sn.getParent();
					
					if (sn.getUserObject() instanceof ZestScriptWrapper) {
			        	showZestEditScriptDialog(parent, (ZestScriptWrapper) sn.getUserObject(), ZestScript.Type.Active);
			        	
					} else if (sn.getUserObject() instanceof ZestElementWrapper) {
						ZestElementWrapper zew = (ZestElementWrapper) sn.getUserObject();
	
					    if (zew != null && zew.getElement() != null && ! ZestZapUtils.isShadow(sn)) {
					        Object obj = zew.getElement();
					        if (obj instanceof ZestRequest) {
					        	showZestEditRequestDialog(parent, sn);
					        } else if (obj instanceof ZestAssertion) {
					        	showZestAssertionDialog(parent, sn, (ZestAssertion)obj, false);
					        } else if (obj instanceof ZestAction) {
					        	showZestActionDialog(
					        			parent, sn, null, (ZestAction) obj, false);
					        } else if (obj instanceof ZestConditional) {
					        	showZestConditionalDialog(
					        			parent, sn, null, (ZestConditional) obj, false);
					        } else if (obj instanceof ZestTransformation) {
					        	/* TODO
					        	showZestTransformationDialog(
					        			extension.getScriptWrapper(node), 
					        			(ScriptNode)node.getParent(), 
					        			(ZestTransformation) obj, false);
					        			*/
					        }  
					    }
			    	}
			    }

			}
		});
			
	}
	
	public void showZestEditScriptDialog(ScriptNode parentNode, ZestScriptWrapper script, ZestScript.Type type) {
		this.showZestEditScriptDialog(parentNode, script, type, null);
	}

	public void showZestEditScriptDialog(ScriptNode parentNode, ZestScriptWrapper script, ZestScript.Type type, String prefix) {
		if (scriptDialog == null) {
			scriptDialog = new ZestScriptsDialog(extension, View.getSingleton().getMainFrame(), new Dimension(500, 500));
		} else if (scriptDialog.isVisible()) {
			// Already being displayed, dont overwrite anything
			return;
		}
		
		if (script == null) {
			ScriptWrapper sw = new ScriptWrapper();
			sw.setEngine(extension.getZestEngineWrapper());
			sw.setEngineName(ZestScriptEngineFactory.NAME);
			sw.setType(extension.getExtScript().getScriptType(ExtensionScript.TYPE_STANDALONE));
			script = new ZestScriptWrapper(sw);
			try {
				script.getZestScript().setPrefix(prefix);
			} catch (MalformedURLException e) {
				logger.error(e.getMessage(), e);
			}
			scriptDialog.init(parentNode, script, true, type);
			
		} else {
			scriptDialog.init(parentNode, script, false, type);
		}
		scriptDialog.setVisible(true);
	}
	
	private void showZestEditRequestDialog(ScriptNode script, ScriptNode request) {
		if (requestDialog == null) {
			requestDialog = new ZestRequestDialog(extension, View.getSingleton().getMainFrame(), new Dimension(500, 700));
		} else if (requestDialog.isVisible()) {
			// Already being displayed, dont overwrite anything
			return;
		} else {
		}
		requestDialog.init(request);
		requestDialog.setVisible(true);
	}

	public void showZestAssertionDialog(ScriptNode parent, ScriptNode child, ZestAssertion assertion, boolean add) {
		if (assertionsDialog == null) {
			assertionsDialog = new ZestAssertionsDialog(extension, View.getSingleton().getMainFrame(), new Dimension(300, 200));
		} else if (assertionsDialog.isVisible()) {
			// Already being displayed, dont overwrite anything
			return;
		}
		assertionsDialog.init(parent, child, assertion, add);
		assertionsDialog.setVisible(true);
	}

	public void showZestActionDialog(ScriptNode parent, ScriptNode child, ZestRequest req, ZestAction action, boolean add) {
		if (actionDialog == null) {
			actionDialog = new ZestActionDialog(extension, View.getSingleton().getMainFrame(), new Dimension(300, 200));
		} else if (actionDialog.isVisible()) {
			// Already being displayed, dont overwrite anything
			return;
		}
		actionDialog.init(parent, child, req, action, add);
		actionDialog.setVisible(true);
	}

	public void showZestConditionalDialog(ScriptNode parent, ScriptNode child, ZestStatement stmt, ZestConditional condition, boolean add) {
		if (conditionDialog == null) {
			conditionDialog = new ZestConditionDialog(extension, View.getSingleton().getMainFrame(), new Dimension(300, 200));
		} else if (conditionDialog.isVisible()) {
			// Already being displayed, dont overwrite anything
			return;
		}
		conditionDialog.init(parent, child, stmt, condition, add);
		conditionDialog.setVisible(true);
	}

	public void showZestTransformationDialog(ZestScript script, ScriptNode req, ZestTransformation transform, boolean add) {
		if (transformationDialog == null) {
			transformationDialog = new ZestTransformDialog(extension, View.getSingleton().getMainFrame(), new Dimension(400, 250));
		} else if (transformationDialog.isVisible()) {
			// Already being displayed, dont overwrite anything
			return;
		}
		transformationDialog.init(script, req, transform, add);
		transformationDialog.setVisible(true);
	}

	public void addDeferedMessage(HttpMessage msg) {
		scriptDialog.addDeferedMessage(msg);
	}

}
