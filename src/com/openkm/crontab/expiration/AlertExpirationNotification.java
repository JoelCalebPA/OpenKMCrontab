package com.openkm.crontab.expiration;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;

import com.openkm.api.OKMAuth;
import com.openkm.api.OKMPropertyGroup;
import com.openkm.api.OKMSearch;
import com.openkm.bean.Document;
import com.openkm.bean.QueryResult;
import com.openkm.bean.form.FormElement;
import com.openkm.bean.form.Input;
import com.openkm.bean.form.Option;
import com.openkm.bean.form.Select;
import com.openkm.core.AccessDeniedException;
import com.openkm.core.DatabaseException;
import com.openkm.core.NoSuchGroupException;
import com.openkm.core.ParseException;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.RepositoryException;
import com.openkm.dao.ConfigDAO;
import com.openkm.dao.DatabaseMetadataDAO;
import com.openkm.dao.bean.DatabaseMetadataValue;
import com.openkm.dao.bean.QueryParams;
import com.openkm.module.db.stuff.DbSessionManager;
import com.openkm.principal.PrincipalAdapterException;
import com.openkm.util.DatabaseMetadataUtils;
import com.openkm.util.ISO8601;
import com.openkm.util.MailUtils;
import com.openkm.util.PathUtils;
import com.openkm.util.TemplateUtils;

import freemarker.template.TemplateException;

/**
 * AlertExpirationNotification
 * 
 * @author jllort
 * 
 */
public class AlertExpirationNotification {
	
	public static void main(String[] args) {
        System.out.println(cronTask(new String[] { DbSessionManager.getInstance().getSystemToken() }));
    }
    
    // OpenKM 6.4.1
    public static String cronTask(String[] params) {
        return cronTask(params[0]);
    }
    
    // OpenKM 6.4.2
    public static String cronTask(String systemToken) {
		try {
			Map<String, List<Document>> usersToAlert = new HashMap<String, List<Document>>();
			String applicationUrl = ConfigDAO.getString("application.url", "http://localhost:8080/OpenKM");
			String datePattern = ConfigDAO.getString("expiration.date.pattern", "dd-MM-yyyy HH:mm:ss");
			int expirationDays = Integer.parseInt(ConfigDAO.getString("expiration.expiration.alert.days", "15"));
			String subject = ConfigDAO.getString("expiration.alert.subject", "Alert documents will expire");
			String template = ConfigDAO.getString("expiration.alert.template", "Document ${docUrl} will expire at ${date}<br/>");
			
			Calendar to = Calendar.getInstance(); // actual day
			to.set(Calendar.HOUR, 0);
			to.set(Calendar.MINUTE, 0);
			to.set(Calendar.SECOND, 0);
			Calendar from = (Calendar) to.clone(); // yesterday
			to.add(Calendar.DATE, expirationDays);
			
			// Preparing query from to
			Map<String, String> properties = new HashMap<String, String>();
			properties.put("okp:expiration.date", ISO8601.formatBasic(from) + "," + ISO8601.formatBasic(to));
			properties.put("okp:expiration.state", "valid");
			QueryParams params = new QueryParams();
			//params.setPath("/okm:root");
			params.setProperties(properties);
			List<QueryResult> results = OKMSearch.getInstance().find(systemToken, params);
			
			for (QueryResult result : results) {
				if (result.getNode() instanceof Document) {
					Document doc = (Document) result.getNode();
					List<FormElement> docProperties = OKMPropertyGroup.getInstance().getProperties(systemToken, doc.getPath(), "okg:expiration");
					
					for (FormElement formElement : docProperties) {
						if (formElement.getName().equals("okp:expiration.group.alert")) {
							Select select = (Select) formElement;
							
							for (Option option : select.getOptions()) {
								if (option.isSelected()) {
									String table = "group";
									String filter = "$gru_name='" + option.getValue() + "'";
									String order = "";
									String query = DatabaseMetadataUtils.buildQuery(table, filter, order);
									
									for (DatabaseMetadataValue dmv : DatabaseMetadataDAO.executeValueQuery(query)) {
										String user = DatabaseMetadataUtils.getDatabaseMetadataValueMap(dmv).get("gru_user");
										String mail = OKMAuth.getInstance().getMail(systemToken, user);
										if (user != null && mail != null && !mail.equals("")) {
											if (usersToAlert.containsKey(mail)) {
												List<Document> alertDocs = usersToAlert.get(mail);
												alertDocs.add(doc);
											} else {
												List<Document> alertDocs = new ArrayList<Document>();
												alertDocs.add(doc);
												usersToAlert.put(mail, alertDocs);
											}
										}
									}
								}
							}
						}
					}
					
					// Update property group
					// OKMPropertyGroup.getInstance().setProperties(systemToken, doc.getPath(), "okg:expiration", docProperties);
				}
			}
			
			// Notify to each user
			for (String userMail : usersToAlert.keySet()) {
				String msg = "";
				
				for (Document doc : usersToAlert.get(userMail)) {
					HashMap<String, Object> templateMap = new HashMap<String, Object>(); // Performs conversion
					String docUrl = "<a href=\"" + applicationUrl + "?docPath=" + URLEncoder.encode(doc.getPath(), "UTF-8") + "\">";
					docUrl += PathUtils.getName(doc.getPath()) + "</a>\n";
					templateMap.put("docUrl", docUrl);
					List<FormElement> docProperties = OKMPropertyGroup.getInstance().getProperties(systemToken, doc.getPath(), "okg:expiration");
					
					for (FormElement formElement : docProperties) {
						if (formElement.getName().equals("okp:expiration.date")) {
							Input expirationDate = (Input) formElement;
							Calendar date = ISO8601.parseBasic(expirationDate.getValue());
							date.set(Calendar.HOUR, 0);
							date.set(Calendar.MINUTE, 0);
							date.set(Calendar.SECOND, 0);
							date.set(Calendar.MILLISECOND, 0);
							SimpleDateFormat sf = new SimpleDateFormat(datePattern);
							templateMap.put("date", sf.format(date.getTime()));
						}
					}
					
					msg += TemplateUtils.replace("ALERT_NOTIFICATION", template, templateMap);
				}
				
				MailUtils.sendMessage(userMail, subject, msg);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (RepositoryException e) {
			e.printStackTrace();
		} catch (DatabaseException e) {
			e.printStackTrace();
		} catch (NoSuchGroupException e) {
			e.printStackTrace();
		} catch (PathNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		} catch (TemplateException e) {
			e.printStackTrace();
		} catch (PrincipalAdapterException e) {
			e.printStackTrace();
		} catch (AccessDeniedException e) {
			e.printStackTrace();
		}
		
		
		return "";
	}
}