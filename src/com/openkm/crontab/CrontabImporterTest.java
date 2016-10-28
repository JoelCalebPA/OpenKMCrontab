package com.openkm.crontab;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.openkm.api.OKMDocument;
import com.openkm.api.OKMRepository;
import com.openkm.automation.AutomationException;
import com.openkm.bean.Document;
import com.openkm.core.AccessDeniedException;
import com.openkm.core.DatabaseException;
import com.openkm.core.FileSizeExceededException;
import com.openkm.core.ItemExistsException;
import com.openkm.core.PathNotFoundException;
import com.openkm.core.RepositoryException;
import com.openkm.core.UnsupportedMimeTypeException;
import com.openkm.core.UserQuotaExceededException;
import com.openkm.core.VirusDetectedException;
import com.openkm.dao.ConfigDAO;
import com.openkm.extension.core.ExtensionException;
import com.openkm.module.db.stuff.DbSessionManager;

public class CrontabImporterTest {
	public static final String FILE_SYSTEM_FOLDER = "/home/openkm/Development/portable/import";
	public static final String FOLDER_UUID = "ebcd16a2-38f3-4a75-8aff-80a45662d191";
	
    private static StringBuffer mailResult = new StringBuffer();
    private static int imported = 0;
    
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
            mailResult = new StringBuffer();
            mailResult.append("<h1>Import report</h1>");
            mailResult.append("</br></br>");
            mailResult.append("<h2>Step1 : importing files</h2>");
            importFiles(systemToken);
            mailResult.append("</br></br>");
            mailResult.append("<h2>Results</h2>");
            mailResult.append("<table boder=\"0\" cellpadding=\"2\" cellspacing=\"0\">");
            mailResult.append("<tr>");
            mailResult.append("<td><b>Imported files:</b></td>");
            mailResult.append("<td>" + imported + "</td>");
            mailResult.append("</tr>");
            mailResult.append("</table>");
        } catch (DatabaseException e) {
            showError(e.getMessage());
            e.printStackTrace();
        } catch (PathNotFoundException e) {
            showError(e.getMessage());
            e.printStackTrace();
        } catch (RepositoryException e) {
            showError(e.getMessage());
            e.printStackTrace();
        } catch (UnsupportedMimeTypeException e) {
            showError(e.getMessage());
            e.printStackTrace();
        } catch (FileSizeExceededException e) {
            showError(e.getMessage());
            e.printStackTrace();
        } catch (UserQuotaExceededException e) {
            showError(e.getMessage());
            e.printStackTrace();
        } catch (VirusDetectedException e) {
            showError(e.getMessage());
            e.printStackTrace();
        } catch (ItemExistsException e) {
            showError(e.getMessage());
            e.printStackTrace();
        } catch (AccessDeniedException e) {
            showError(e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            showError(e.getMessage());
            e.printStackTrace();
        } catch (AutomationException e) {
            showError(e.getMessage());
            e.printStackTrace();
        } catch (ExtensionException e) {
            showError(e.getMessage());
            e.printStackTrace();
        }
        return mailResult.toString();
    }
    
    /**
     * importFiles
     */
    private static void importFiles(String systemToken) throws DatabaseException, PathNotFoundException, RepositoryException,
            UnsupportedMimeTypeException, FileSizeExceededException, UserQuotaExceededException, VirusDetectedException,
            ItemExistsException, AccessDeniedException, IOException, AutomationException, ExtensionException {
        // Mail header
        mailResult.append("<table boder=\"0\" cellpadding=\"2\" cellspacing=\"0\" width=\"100%\">");
        mailResult.append("<tr>");
        mailResult.append("<td bgcolor=\"silver\"><b>File name</b></td>");
        mailResult.append("<td bgcolor=\"silver\"><b>OKM path</b></td>");
        mailResult.append("<td bgcolor=\"silver\"><b>Status</b></td>");
        mailResult.append("</tr>");
        
        // Loading files
        imported = 0;
        String systemFolder = ConfigDAO.getString("sample.filesytem.import.folder", FILE_SYSTEM_FOLDER);
        String importFldUuid = ConfigDAO.getString("sample.import.folder", FOLDER_UUID);
        String importFldPath = OKMRepository.getInstance().getNodePath(systemToken, importFldUuid);
        File folder = new File(systemFolder);
        
        for (File file : folder.listFiles()) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".pdf")) {
                Document doc = new Document();
                doc.setPath(importFldPath + "/" + file.getName());
                FileInputStream fis = new FileInputStream(file);
                mailResult.append("<tr>");
                mailResult.append("<td>" + file.getName() + "</td>");
                mailResult.append("<td>" + doc.getPath() + "</td>");
                doc = OKMDocument.getInstance().create(systemToken, doc, fis);
                fis.close();
                file.delete();
                mailResult.append("<td><font color=\"green\">OK</font></td>");
                mailResult.append("</tr>");
                imported++;
            }
        }
        mailResult.append("</table>");
    }
    
    /**
     * showError
     */
    private static void showError(String message) {
        mailResult.append("</br></br>");
        mailResult.append("<h1><font color=\"red\">PROGRAM ABORTED ABNORMALLY</font></h1>");
        mailResult.append("</br></br>");
        mailResult.append(message);
    }
}
