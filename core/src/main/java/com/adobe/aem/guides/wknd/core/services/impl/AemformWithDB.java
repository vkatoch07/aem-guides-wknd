package com.adobe.aem.guides.wknd.core.services.impl;
import com.adobe.aem.guides.wknd.core.services.AemFormsAndDB;
import com.adobe.aemfd.docmanager.Document;
import com.mergeandfuse.getserviceuserresolver.GetResolver;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.UUID;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.sql.DataSource;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = {AemFormsAndDB.class}, immediate = true)
public class AemformWithDB implements AemFormsAndDB{
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	@Reference(target = "(&(objectclass=javax.sql.DataSource)(datasource.name=aemformsds))")
	private DataSource dataSource;
	@Reference
	GetResolver getResolver;

	public String getData(String guid) {
		this.log.debug("### inside my getData of AemformWithDB");
		Connection con = this.getConnection();

		try {
			Statement st = con.createStatement();
			String query = "SELECT afdata FROM aemformsdb.formdatawithattachments where guid = '" + guid + "'";
			this.log.debug(" Got Result Set" + query);
			ResultSet rs = st.executeQuery(query);
			if (rs.next()) {
				return rs.getString("afdata");
			}
		} catch (SQLException var6) {
			var6.printStackTrace();
		}

		return null;
	}

	public Connection getConnection() {
		this.log.debug("Getting Connection ");
		Connection con = null;

		try {
			con = this.dataSource.getConnection();
			this.log.debug("got connection");
			return con;
		} catch (Exception var3) {
			this.log.debug("not able to get connection ");
			var3.printStackTrace();
			return null;
		}
	}

	public JSONObject getAFFormDataWithAttachments(String guid) {
		this.log.debug("### inside my getData of AF with attachments info");
		JSONObject afDataObject = new JSONObject();
		Connection con = this.getConnection();

		try {
			Statement st = con.createStatement();
			String query = "SELECT afdata,attachmentsInfo FROM aemformsdb.formdatawithattachments where guid = '"
					+ guid + "'";
			this.log.debug(" Got Result Set" + query);
			ResultSet rs = st.executeQuery(query);
			if (rs.next()) {
				afDataObject.put("afData", rs.getString("afdata"));
				afDataObject.put("afAttachments", rs.getString("attachmentsInfo"));
				return afDataObject;
			}
		} catch (JSONException | SQLException var7) {
			this.log.debug("The error message is " + var7.getMessage());
		}

		return null;
	}

	public String getMobileNumber(String guid) {
		this.log.debug("### inside my getData of AF with attachments info");
		JSONObject afDataObject = new JSONObject();
		Connection con = this.getConnection();

		try {
			Statement st = con.createStatement();
			String query = "SELECT telephoneNumber FROM aemformsdb.formdatawithattachments where guid = '" + guid
					+ "'";
			this.log.debug(" Got Result Set" + query);
			ResultSet rs = st.executeQuery(query);
			if (rs.next()) {
				afDataObject.put("mobileNumber", rs.getString("telephoneNumber"));
				return afDataObject.toString();
			}
		} catch (JSONException | SQLException var7) {
			this.log.debug("The error message is " + var7.getMessage());
		}

		return null;
	}

	public String saveDocumentInCrx(String jcrPath, String fileName, Document documentToSave) {
		this.log.debug("Storing in crx" + jcrPath);
		this.log.debug("Storing in CRX  " + jcrPath);
		ResourceResolver serviceResolver = this.getResolver.getFormsServiceResolver();
		this.log.debug("Got Resolver");
		UUID uuid = UUID.randomUUID();
		String uuidString = uuid.toString();

		try {
			Node ocrFiles = (Node) serviceResolver.getResource(jcrPath).adaptTo(Node.class);
			Node newNode = ocrFiles.addNode(uuidString, "nt:folder");
			this.log.debug("Got ocr files" + ocrFiles.getPath());
			Node afAttachment = newNode.addNode(fileName, "nt:file");
			Node resNode = afAttachment.addNode("jcr:content", "nt:resource");
			this.log.debug("Got resNode files" + resNode.getPath());
			Session session = (Session) serviceResolver.adaptTo(Session.class);
			ValueFactory valueFactory = session.getValueFactory();
			Binary contentValue = valueFactory.createBinary(documentToSave.getInputStream());
			resNode.setProperty("jcr:data", contentValue);
			serviceResolver.commit();
			this.log.debug("committed");
			return afAttachment.getPath();
		} catch (Exception var14) {
			this.log.debug(var14.getMessage());
			this.log.debug("The ocrFiles node was created");
			return null;
		}
	}

	public String storeFormData(String formData, String attachmentsInfo, String telephoneNumber) {
		this.log.debug("******Inside my AEMFormsWith DB service*****");
		this.log.debug(
				"### Inserting data ... " + formData + "and the telephone number to insert is  " + telephoneNumber);
		String insertRowSQL = "INSERT INTO aemformsdb.formdatawithattachments(guid,afdata,attachmentsInfo,telephoneNumber) VALUES(?,?,?,?)";
		UUID uuid = UUID.randomUUID();
		String randomUUIDString = uuid.toString();
		this.log.debug("The insert query is " + insertRowSQL);
		Connection c = this.getConnection();
		PreparedStatement pstmt = null;

		try {
			pstmt = null;
			pstmt = c.prepareStatement(insertRowSQL);
			pstmt.setString(1, randomUUIDString);
			pstmt.setString(2, formData);
			pstmt.setString(3, attachmentsInfo);
			pstmt.setString(4, telephoneNumber);
			this.log.debug("Executing the insert statment  " + pstmt.executeUpdate());
			c.commit();
		} catch (SQLException var22) {
			this.log.error("unable to insert data in the table", var22.getMessage());
		} finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (SQLException var21) {
					this.log.debug("error in closing prepared statement " + var21.getMessage());
				}
			}

			if (c != null) {
				try {
					c.close();
				} catch (SQLException var20) {
					this.log.debug("error in closing connection " + var20.getMessage());
				}
			}

		}

		return randomUUIDString;
	}

	public String storeAFAttachments(JSONObject fileMap, SlingHttpServletRequest request) {
		JSONObject newFileMap = new JSONObject();

		try {
			Iterator keys = fileMap.keys();
			this.log.debug("The file map is  " + fileMap.toString());

			while (keys.hasNext()) {
				String key = (String) keys.next();
				this.log.debug("#### The key is " + key);
				String attacmenPath = (String) fileMap.get(key);
				this.log.debug("The attachment path is " + attacmenPath);
				if (!attacmenPath.contains("/content/afattachments")) {
					String fileName = attacmenPath.split("/")[1];
					this.log.debug("#### The attachment name  is " + fileName);
					InputStream is = request.getPart(attacmenPath).getInputStream();
					Document aemFDDocument = new Document(is);
					String crxPath = this.saveDocumentInCrx("/content/afattachments", fileName, aemFDDocument);
					this.log.debug(" ##### written to crx repository  " + attacmenPath.split("/")[1]);
					newFileMap.put(key, crxPath);
				} else {
					this.log.debug("$$$$ The attachment was already added " + key);
					this.log.debug("$$$$ The attachment path is " + attacmenPath);
					int position = attacmenPath.indexOf("//");
					this.log.debug("$$$$ After substring " + attacmenPath.substring(position + 1));
					this.log.debug("$$$$ After splitting " + attacmenPath.split("/")[1]);
					newFileMap.put(key, attacmenPath.substring(position + 1));
				}
			}
		} catch (Exception var11) {
			this.log.debug(var11.getMessage());
		}

		this.log.debug("$$$$ The new file map is " + newFileMap.toString());
		return newFileMap.toString();
	}
}
