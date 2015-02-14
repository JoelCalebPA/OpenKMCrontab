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
import com.openkm.automation.AutomationException;
import com.openkm.bean.Document;
import com.openkm.bean.QueryResult;
import com.openkm.bean.form.FormElement;
import com.openkm.bean.form.Input;
import com.openkm.bean.form.Option;
import com.openkm.bean.form.Select;
import com.openkm.core.AccessDeniedException;
import com.openkm.core.DatabaseException;
import com.openkm.core.LockException;
import com.openkm.core.NoSuchGroupException;
import com.openkm.core.NoSuchPropertyException;
import com.openkm.core.ParseException;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.RepositoryException;
import com.openkm.dao.ConfigDAO;
import com.openkm.dao.DatabaseMetadataDAO;
import com.openkm.dao.bean.DatabaseMetadataValue;
import com.openkm.dao.bean.QueryParams;
import com.openkm.extension.core.ExtensionException;
import com.openkm.module.db.stuff.DbSessionManager;
import com.openkm.principal.PrincipalAdapterException;
import com.openkm.util.DatabaseMetadataUtils;
import com.openkm.util.ISO8601;
import com.openkm.util.MailUtils;
import com.openkm.util.PathUtils;
import com.openkm.util.TemplateUtils;

import freemarker.template.TemplateException;

/**
 * InitialNotification
 * 
 * @author jllort
 * 
 */
public class InitialNotification {
	
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
			String applicationUrl = ConfigDAO.getString("application.url", "http://localhost:8080/OpenKM");
			String datePattern = ConfigDAO.getString("expiration.date.pattern", "dd-MM-yyyy HH:mm:ss");
			String subject = ConfigDAO.getString("expiration.notification.subject",	"New uploaded document with expiration");
			String template = ConfigDAO.getString("expiration.notification.template", "New document uploaded<br/>${docUrl}<br/>Expiration date:${date}<br/>");
			
			// Preparing query
			Map<String, String> properties = new HashMap<String, String>();
			properties.put("okp:expiration.state", "notify");
			QueryParams params = new QueryParams();
			//params.setPath("/okm:root");
			params.setProperties(properties);
			List<QueryResult> results = OKMSearch.getInstance().find(systemToken, params);
			
			for (QueryResult result : results) {
				if (result.getNode() instanceof Document) {
					HashMap<String, Object> templateMap = new HashMap<String, Object>(); // Performs conversion
					List<String> mails = new ArrayList<String>();
					Document doc = (Document) result.getNode();
					
					// Setting doc url
					String docUrl = "<a href=\"" + applicationUrl + "?docPath=" + URLEncoder.encode(doc.getPath(), "UTF-8") + "\">";
					docUrl += PathUtils.getName(doc.getPath()) + "</a>\n";
					templateMap.put("docUrl", docUrl);
					
					// Getting expiration dates and notification users
					List<FormElement> docProperties = OKMPropertyGroup.getInstance().getProperties(systemToken, doc.getPath(), "okg:expiration");
					
					for (FormElement formElement : docProperties) {
						if (formElement.getName().equals("okp:expiration.date")) {
							Input expirationDate = (Input) formElement;
							Calendar date = ISO8601.parseBasic(expirationDate.getValue());
							Calendar actualDay = Calendar.getInstance();
							
							// Test calendar date equal or greater than today otherside set to actual day
							if (actualDay.compareTo(date) > 0) {
								date = actualDay;
							}
							
							date.set(Calendar.HOUR, 0); // Always says 00:00:00
							date.set(Calendar.MINUTE, 0);
							date.set(Calendar.SECOND, 0);
							date.set(Calendar.MILLISECOND, 0);
							expirationDate.setValue(ISO8601.formatBasic(date));
							SimpleDateFormat sf = new SimpleDateFormat(datePattern);
							templateMap.put("date", sf.format(date.getTime()));
						} else if (formElement.getName().equals("okp:expiration.state")) {
							// Update property group okp:expiration.state to valid
							Select select = (Select) formElement;
							
							for (Option option : select.getOptions()) {
								if (option.getValue().equals("valid")) {
									option.setSelected(true);
								} else {
									option.setSelected(false);
								}
							}
						} else if (formElement.getName().equals("okp:expiration.group.notify")) {
							Select select = (Select) formElement;
							
							for (Option option : select.getOptions()) {
								if (option.isSelected()) {
									String table = "group";
									String filter = "$gru_name='" + option.getValue() + "'";
									String order = "";
									
									for (DatabaseMetadataValue dmv : DatabaseMetadataDAO.executeValueQuery(DatabaseMetadataUtils.buildQuery(table, filter, order))) {
										String user = DatabaseMetadataUtils.getDatabaseMetadataValueMap(dmv).get("gru_user");
										String mail = OKMAuth.getInstance().getMail(systemToken, user);
										
										if (user != null && mail != null && !mail.equals("")) {
											mails.add(mail);
										}
									}
								}
							}
						}
					}
					
					// Update property group
					OKMPropertyGroup.getInstance().setProperties(systemToken, doc.getPath(), "okg:expiration", docProperties); 
					
					// Sending mails
					if (mails.size() > 0) {
						MailUtils.sendMessage(mails, subject, TemplateUtils.replace("UPLOADED_NOTIFICATION", template, templateMap));
					}
				}
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
		} catch (NoSuchPropertyException e) {
			e.printStackTrace();
		} catch (LockException e) {
			e.printStackTrace();
		} catch (AccessDeniedException e) {
			e.printStackTrace();
		} catch (TemplateException e) {
			e.printStackTrace();
		} catch (ExtensionException e) {
			e.printStackTrace();
		} catch (PrincipalAdapterException e) {
			e.printStackTrace();
		} catch (AutomationException e) {
			e.printStackTrace();
		}
		
		return "";
	}
}