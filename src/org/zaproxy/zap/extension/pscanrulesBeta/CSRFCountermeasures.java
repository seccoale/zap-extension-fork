/**
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
package org.zaproxy.zap.extension.pscanrulesBeta;

import java.util.List;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;

import org.apache.log4j.Logger;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.core.scanner.Alert;
import org.parosproxy.paros.core.scanner.Category;
import org.parosproxy.paros.network.HttpMessage;
import org.zaproxy.zap.extension.anticsrf.ExtensionAntiCSRF;
import org.zaproxy.zap.extension.pscan.PassiveScanThread;
import org.zaproxy.zap.extension.pscan.PluginPassiveScanner;
import org.zaproxy.zap.model.Vulnerabilities;
import org.zaproxy.zap.model.Vulnerability;

/**
 * The CSRFCountermeasures plugin identifies *potential* vulnerabilities with
 * the lack of known CSRF countermeasures in pages with forms.
 * Based 95% on org.zaproxy.zap.extension.pscan.scanner.CrossSiteRequestForgeryScanner
 * but converted to an extension, with clarified class name, and alert message and details
 * to reflect the fact that the class scans for a lack of CSRF countermeasures 
 * rather than for CSRF vulnerabilities.
 * This class is intended to deprecate org.zaproxy.zap.extension.pscan.scanner.CrossSiteRequestForgeryScanner
 * or to allow org.zaproxy.zap.extension.pscan.scanner.CrossSiteRequestForgeryScanner to actually 
 * scan for CSRF vulnerabilities.
 * 
 *  @author Colm O'Flaherty, Encription Ireland Ltd
 */
public class CSRFCountermeasures extends PluginPassiveScanner {
    
    /**
     * contains the base vulnerability that this plugin refers to
     */
    private static Vulnerability vuln = Vulnerabilities.getVulnerability("wasc_9");
    
    /**
     * the passive scan thread class used
     */
	private PassiveScanThread parent = null;
	
	/**
	 * the logger
	 */
	private static Logger logger = Logger.getLogger(CSRFCountermeasures.class);

	public CSRFCountermeasures() {
		super();
		PscanUtils.registerI18N();
	}

	@Override
	public void setParent (PassiveScanThread parent) {
		this.parent = parent;
	}

	/**
	 * does nothing. The request itself is not scanned. Only the response is scanned.
	 */
	@Override
	public void scanHttpRequestSend(HttpMessage msg, int id) {
		// Ignore
	}

	/**
	 * gets the plugin id for this extension
	 * @return the plugin id for this extension
	 */
	private int getId() {
		return 40014;
	}

	/**
	 * scans each form in the HTTP response for known anti-CSRF tokens. If any form 
	 * exists that does not contain a known anti-CSRF token, raise an alert.
	 */
	@Override
	public void scanHttpResponseReceive(HttpMessage msg, int id, Source source) {
		//need to do this if we are to be able to get an element's parent. Do it as early as possible in the logic 
		source.fullSequentialParse();
		
		long start = System.currentTimeMillis();
		
		ExtensionAntiCSRF extAntiCSRF = 
			(ExtensionAntiCSRF) Control.getSingleton().getExtensionLoader().getExtension(ExtensionAntiCSRF.NAME);
		
		if (extAntiCSRF == null) {
			return;
		}
		
		List<Element> formElements = source.getAllElements(HTMLElementName.FORM);
		List<String> tokenNames = extAntiCSRF.getAntiCsrfTokenNames();
		boolean foundCsrfToken = false;
		
		if (formElements != null && formElements.size() > 0) {
			
			// Loop through all of the FORM tags
			logger.debug("Found " + formElements.size() + " forms");
			
			StringBuilder sb = new StringBuilder();
			int i = 1;
			
			for (Element formElement : formElements) {
				logger.debug("FORM ["+ formElement + "] has parent ["+ formElement.getParentElement()+"]");
				//if the form has no parent, it is pretty likely invalid HTML (or Javascript!!!), so we will not report
				//any alerts on it.  
				//ie. This logic is necessary to eliminate false positives on non-HTML files.
				if (formElement.getParentElement() == null ) {
					logger.debug ("Skipping HTML form because it has no parent. Likely not actually HTML.");
					foundCsrfToken=true;  //do not report a missing anti-CSRF field on this form
					continue;
				}
					
				
				List<Element> inputElements = formElement.getAllElements(HTMLElementName.INPUT);
				if (sb.length() > 0) {
					sb.append("], ");
				} 
				sb.append("[Form "+i+": ");
				
				if (inputElements != null && inputElements.size() > 0) {
					// Loop through all of the INPUT elements
					logger.debug("Found " + inputElements.size() + " inputs");
					for (Element inputElement : inputElements) {
						String attId = inputElement.getAttributeValue("ID");
						if (attId != null) {
							for (String tokenName : tokenNames) {
								if (tokenName.equalsIgnoreCase(attId)) {
									foundCsrfToken = true;
									break;
								}
							}
						}
						String name = inputElement.getAttributeValue("NAME");
						if (name != null) {
							sb.append("\""+name + "\" ");
							for (String tokenName : tokenNames) {
								if (tokenName.equalsIgnoreCase(name)) {
									foundCsrfToken = true;
									break;
								}
							}
						}
					}
				}
				if (foundCsrfToken) {
					break;
				}
				i++;
			}
			if (sb.length() > 0) {
				sb.append(']');
			}
			
			if (!foundCsrfToken) {
				//No known Anti-CSRF tokens found in a form. Not a vulnerability per-se.
				//but alert it, as a low priority
				String formDetails = sb.toString();
				String tokenNamesFlattened = tokenNames.toString();
				
				String desc = Constant.messages.getString("pscanbeta.noanticsrftokens.desc");
				String attack = Constant.messages.getString("pscanbeta.noanticsrftokens.alert.attack");
				String extraInfo = Constant.messages.getString("pscanbeta.noanticsrftokens.alert.extrainfo", tokenNamesFlattened, formDetails);
				
			    Alert alert = new Alert(getId(), Alert.RISK_LOW, Alert.WARNING,  getName());
			    alert.setDetail(
			    			desc + "\n"+getDescription(), 
				    		msg.getRequestHeader().getURI().toString(),
				    		"",  //parameter: none.
				    		attack, 
				    		extraInfo,
				    		getSolution(), 
				            getReference(), 
							attack,	// Evidence
							0,	// TODO CWE Id
							9,	// WASC Id
				            msg);

			    parent.raiseAlert(id, alert);
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("\tScan of record " + id + " took " + (System.currentTimeMillis() - start) + " ms");
		}
		
	}

	@Override
	public String getName() {
		//do not use the name of the related vulnerability 
		//(because we have not actually discovered an instance of this vulnerability class!)
		return Constant.messages.getString("pscanbeta.noanticsrftokens.name");
	}
	
    public String getDescription() {
    	if (vuln != null) {
    		return vuln.getDescription();
    	}
    	return "Failed to load vulnerability description from file";
    }

    public int getCategory() {
        return Category.MISC;
    }

    public String getSolution() {
    	if (vuln != null) {
    		return vuln.getSolution();
    	}
    	return "Failed to load vulnerability solution from file";
    }

    public String getReference() {
    	if (vuln != null) {
    		StringBuilder sb = new StringBuilder();
    		for (String ref : vuln.getReferences()) {
    			if (sb.length() > 0) {
    				sb.append('\n');
    			}
    			sb.append(ref);
    		}
    		return sb.toString();
    	}
    	return "Failed to load vulnerability reference from file";
    }

}
