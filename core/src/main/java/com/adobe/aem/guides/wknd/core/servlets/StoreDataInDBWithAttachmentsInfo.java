package com.adobe.aem.guides.wknd.core.servlets;

import com.adobe.aem.guides.wknd.core.services.AemFormsAndDB;
import com.google.gson.JsonObject;
import java.io.PrintWriter;
import javax.servlet.Servlet;
import javax.sql.DataSource;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = { Servlet.class }, property = { "sling.servlet.methods=post",
		"sling.servlet.paths=/bin/storeafdatawithattachments" })
public class StoreDataInDBWithAttachmentsInfo extends SlingAllMethodsServlet {
	private static final Logger log = LoggerFactory.getLogger(StoreDataInDBWithAttachmentsInfo.class);
	private static final long serialVersionUID = 1L;
	@Reference(target = "(&(datasource.name=aemformsds))")
	private DataSource dataSource;
	@Reference
	AemFormsAndDB aemFormsAndDB;

	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) {
		log.debug("#### Inside doPost of StoreDataInDBWithAttachmentsInfo ####");
		String afData = request.getParameter("data");
		String tel = request.getParameter("mobileNumber");
		log.debug("$$$The telephone number is  " + tel);
		log.debug("The request parameter " + afData);

		try {
			JSONObject fileMap = new JSONObject(request.getParameter("fileMap").toString());
			String newFileMap = this.aemFormsAndDB.storeAFAttachments(fileMap, request);
			String application_id = this.aemFormsAndDB.storeFormData(afData, newFileMap.toString(), tel);
			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty("path", application_id);
			response.setContentType("application/json");
			response.setHeader("Cache-Control", "nocache");
			response.setCharacterEncoding("utf-8");
			PrintWriter out = null;
			out = response.getWriter();
			out.println(jsonObject.toString());
		} catch (Exception var10) {
			log.debug(var10.getMessage());
		}

	}
}
