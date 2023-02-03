package com.adobe.aem.guides.wknd.core.services;

import org.apache.sling.api.SlingHttpServletRequest;
import org.json.JSONObject;
public interface AemFormsAndDB {
	String getData(String var1);

	JSONObject getAFFormDataWithAttachments(String var1);

	String getMobileNumber(String var1);

	String storeFormData(String var1, String var2, String var3);

	String storeAFAttachments(JSONObject var1, SlingHttpServletRequest var2);
}
