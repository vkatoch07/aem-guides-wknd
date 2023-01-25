package com.adobe.aem.guides.wknd.core.servlets;
import com.adobe.aemfd.docmanager.Document;
import com.adobe.fd.assembler.client.AssemblerOptionSpec;
import com.adobe.fd.assembler.client.AssemblerResult;
import com.adobe.fd.assembler.client.OperationException;
import com.adobe.fd.assembler.service.AssemblerService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mergeandfuse.getserviceuserresolver.GetResolver;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.servlet.Servlet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

@Component(service = {Servlet.class}, property = {"sling.servlet.methods=post",
		"sling.servlet.paths=/bin/assemblenewsletter"})
public class AssembleNewsLetters extends SlingAllMethodsServlet {
	@Reference
	AssemblerService assemblerService;
	@Reference
	GetResolver getResolver;
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(AssembleNewsLetters.class);

	public Document orgw3cDocumentToAEMFDDocument(org.w3c.dom.Document xmlDocument) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		DOMSource source = new DOMSource(xmlDocument);
		log.debug("$$$$In orgW3CDocumentToAEMFDDocument method");
		StreamResult outputTarget = new StreamResult(outputStream);

		try {
			TransformerFactory.newInstance().newTransformer().transform(source, outputTarget);
			InputStream is1 = new ByteArrayInputStream(outputStream.toByteArray());
			Document xmlAEMFDDocument = new Document(is1);
			if (log.isDebugEnabled()) {
				xmlAEMFDDocument.copyToFile(new File("dataxmldocument.xml"));
			}

			return xmlAEMFDDocument;
		} catch (Exception var7) {
			log.debug("Error in generating ddx " + var7.getMessage());
			return null;
		}
	}

	public Document createDDXFromMapOfDocuments(Map<String, Object> mapOfDocuments) {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		org.w3c.dom.Document ddx = null;

		try {
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			ddx = docBuilder.newDocument();
			Element rootElement = ddx.createElementNS("http://ns.adobe.com/DDX/1.0/", "DDX");
			ddx.appendChild(rootElement);
			Element pdfResult = ddx.createElement("PDF");
			pdfResult.setAttribute("result", "GeneratedPDF.pdf");
			rootElement.appendChild(pdfResult);
			Iterator var7 = mapOfDocuments.keySet().iterator();

			while (var7.hasNext()) {
				String key = (String) var7.next();
				log.debug(key + " " + mapOfDocuments.get(key));
				Element pdfSourceElement = ddx.createElement("PDF");
				pdfSourceElement.setAttribute("source", key);
				pdfSourceElement.setAttribute("bookmarkTitle", key);
				pdfResult.appendChild(pdfSourceElement);
			}

			return this.orgw3cDocumentToAEMFDDocument(ddx);
		} catch (ParserConfigurationException var10) {
			log.debug("Error:" + var10.getMessage());
			return null;
		}
	}

	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) {
		String[] newsletters = request.getParameter("selectedNewsLetters").split(",");
		Session session = (Session) request.getResourceResolver().adaptTo(Session.class);
		Map<String, Object> mapOfDocuments = new HashMap();

		Document newsletter;
		for (int i = 0; i < newsletters.length; ++i) {
			Resource resource = request.getResourceResolver().getResource(newsletters[i]);
			log.debug("The resource name is " + resource.getName());
			newsletter = new Document(resource.getPath());
			mapOfDocuments.put(resource.getName(), newsletter);
		}

		log.debug("The newsletters selected: " + newsletters);
		Document ddxDocument = this.createDDXFromMapOfDocuments(mapOfDocuments);
		AssemblerOptionSpec aoSpec = new AssemblerOptionSpec();
		aoSpec.setFailOnError(true);
		newsletter = null;

		try {
			AssemblerResult ar = this.assemblerService.invoke(ddxDocument, mapOfDocuments, aoSpec);
			Document assembledPDF = (Document) ar.getDocuments().get("GeneratedPDF.pdf");
			ResourceResolver formsServiceResolver = this.getResolver.getFormsServiceResolver();
			Resource nodeResource = formsServiceResolver.getResource("/content/newsletters");
			UUID uuid = UUID.randomUUID();
			String uuidString = uuid.toString();
			Node assembledNewsletters = (Node) nodeResource.adaptTo(Node.class);
			Node assembledNewsletter = assembledNewsletters.addNode(uuidString + ".pdf", "nt:file");
			Node resNode = assembledNewsletter.addNode("jcr:content", "nt:resource");
			ValueFactory valueFactory = ((Session) formsServiceResolver.adaptTo(Session.class)).getValueFactory();
			Binary contentValue = valueFactory.createBinary(assembledPDF.getInputStream());
			resNode.setProperty("jcr:data", contentValue);
			formsServiceResolver.commit();
			PrintWriter out = response.getWriter();
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			JsonObject asset = new JsonObject();
			asset.addProperty("assetPath", assembledNewsletter.getPath());
			out.print((new Gson()).toJson(asset));
			out.flush();
		} catch (OperationException | RepositoryException | IOException var21) {
			log.debug("Error is " + var21.getMessage());
		}

	}
}
