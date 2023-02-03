package com.adobe.aem.guides.wknd.core.services.impl;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.Session;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.adobe.aemfd.docmanager.Document;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
@Component(property = {
  Constants.SERVICE_DESCRIPTION + "=Custom component to wrtie form attachments to file system",
  Constants.SERVICE_VENDOR + "=VKIT",
  "process.label" + "=Custom component to wrtie form attachments to file system"
})
public class SaveFilesCustomWorkflowComp implements WorkflowProcess {
	private static final Logger log = LoggerFactory.getLogger(SaveFilesCustomWorkflowComp.class);
	  @Reference
	  QueryBuilder queryBuilder;

	  @Override
	  public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap)
	  throws WorkflowException {

	    String attachmentsPath = metaDataMap.get("attachmentsPath", String.class);
	    log.debug("Got Attachments PAth" + attachmentsPath);
	    
	    String saveToLocation = metaDataMap.get("saveToLocation", String.class);	
	    log.debug("Got Save Location" + saveToLocation);

	    log.debug("The seperator is" + File.separator);
	    String payloadPath = workItem.getWorkflowData().getPayload().toString();
	    Map < String, String > map = new HashMap < String, String > ();
	    map.put("path", payloadPath + "/" + attachmentsPath);
	    File saveLocationFolder = new File(saveToLocation);
	    if (!saveLocationFolder.exists()) {
	      saveLocationFolder.mkdirs();
	    }

	    map.put("type", "nt:file");
	    Query query = queryBuilder.createQuery(PredicateGroup.create(map), workflowSession.adaptTo(Session.class));
	    query.setStart(0);
	    query.setHitsPerPage(20);

	    SearchResult result = query.getResult();
	    log.debug("Got  " + result.getHits().size() + " attachments ");
	    Node attachmentNode = null;
	    for (Hit hit: result.getHits()) {
	      try {
	        String path = hit.getPath();
	        log.debug("The attachment title is  " + hit.getTitle() + " and the attachment path is  " + path);
	        attachmentNode = workflowSession.adaptTo(Session.class).getNode(path + "/jcr:content");
	        InputStream documentStream = attachmentNode.getProperty("jcr:data").getBinary().getStream();
	        Document attachmentDoc = new Document(documentStream);
	        attachmentDoc.copyToFile(new File(saveLocationFolder + File.separator + hit.getTitle()));
	        attachmentDoc.close();
	      } catch (Exception e) {
	        log.error("Error saving file " + e.getMessage());
	      }
	    }
	  }
}
