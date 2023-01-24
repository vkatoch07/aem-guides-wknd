package com.adobe.aem.guides.wknd.core.services.impl;

import com.adobe.forms.common.service.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Component
public class PrefillAdaptiveForm implements DataProvider {
	private static final Logger log = LoggerFactory.getLogger(PrefillAdaptiveForm.class);

	public String getServiceDescription() {
		return "Custom Aem Form Pre Fill Service";
	}

	public String getServiceName() {
		return "CustomAemFormsPrefillService";
	}

	public PrefillData getPrefillData(final DataOptions dataOptions) throws FormsException {
		PrefillData prefillData = new PrefillData() {
			public InputStream getInputStream() {
				return getData(dataOptions);
			}

			public ContentType getContentType() {
				return ContentType.XML;
			}
		};
		return prefillData;
	}

	private InputStream getData(DataOptions dataOptions) throws FormsException {
		log.debug("Geting xml");
		InputStream xmlDataStream = null;
		Resource aemFormContainer = dataOptions.getFormResource();
		ResourceResolver resolver = aemFormContainer.getResourceResolver();
		Session session = (Session) resolver.adaptTo(Session.class);

		try {
			try {
				UserManager um = ((JackrabbitSession) session).getUserManager();
				Authorizable loggedinUser = um.getAuthorizable(session.getUserID());
				log.debug("The path of the user is" + loggedinUser.getPath());
				DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
				Document doc = docBuilder.newDocument();
				Element rootElement = doc.createElement("data");
				doc.appendChild(rootElement);
				Element firstNameElement = doc.createElement("fname");
				firstNameElement.setTextContent(loggedinUser.getProperty("profile/givenName")[0].getString());
				log.debug("created firstNameElement  " + loggedinUser.getProperty("profile/givenName")[0].getString());
				Element lastNameElement = doc.createElement("lname");
				Element jobTitleElement = doc.createElement("jobTitle");
				jobTitleElement.setTextContent(loggedinUser.getProperty("profile/jobTitle")[0].getString());
				Element cityElement = doc.createElement("city");
				cityElement.setTextContent(loggedinUser.getProperty("profile/city")[0].getString());
				log.debug("created cityElement  " + loggedinUser.getProperty("profile/city")[0].getString());
				Element countryElement = doc.createElement("country");
				countryElement.setTextContent(loggedinUser.getProperty("profile/country")[0].getString());
				Element streetElement = doc.createElement("street");
				streetElement.setTextContent(loggedinUser.getProperty("profile/street")[0].getString());
				Element postalCodeElement = doc.createElement("postalCode");
				postalCodeElement.setTextContent(loggedinUser.getProperty("profile/postalCode")[0].getString());
				Element genderElement = doc.createElement("gender");
				genderElement.setTextContent(loggedinUser.getProperty("profile/gender")[0].getString());
				lastNameElement.setTextContent(loggedinUser.getProperty("profile/familyName")[0].getString());
				Element emailElement = doc.createElement("email");
				emailElement.setTextContent(loggedinUser.getProperty("profile/email")[0].getString());
				rootElement.appendChild(firstNameElement);
				rootElement.appendChild(lastNameElement);
				rootElement.appendChild(emailElement);
				rootElement.appendChild(streetElement);
				rootElement.appendChild(countryElement);
				rootElement.appendChild(cityElement);
				rootElement.appendChild(jobTitleElement);
				rootElement.appendChild(postalCodeElement);
				rootElement.appendChild(genderElement);
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				Transformer transformer = transformerFactory.newTransformer();
				DOMSource source = new DOMSource(doc);
				StreamResult outputTarget = new StreamResult(outputStream);
				TransformerFactory.newInstance().newTransformer().transform(source, outputTarget);
				xmlDataStream = new ByteArrayInputStream(outputStream.toByteArray());
				return xmlDataStream;
			} catch (AccessDeniedException var36) {
				var36.printStackTrace();
			} catch (UnsupportedRepositoryOperationException var37) {
				var37.printStackTrace();
			} catch (RepositoryException var38) {
				var38.printStackTrace();
			} catch (ParserConfigurationException var39) {
				var39.printStackTrace();
			} catch (TransformerConfigurationException var40) {
				var40.printStackTrace();
			} catch (TransformerException var41) {
				var41.printStackTrace();
			} catch (TransformerFactoryConfigurationError var42) {
				var42.printStackTrace();
			}

			return null;
		} finally {
			;
		}
	}
}
