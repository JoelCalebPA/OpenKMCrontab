package com.openkm.crontab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openkm.module.db.stuff.DbSessionManager;

public class CrontabTest {
	private static Logger log = LoggerFactory.getLogger(CrontabTest.class);

	public static void main(String[] args) {
		System.out.println(cronTask(new String[] { DbSessionManager.getInstance().getSystemToken() }));
	}
	
	public static String cronTask() {
		log.info("cronTask()");
		return cronTask(DbSessionManager.getInstance().getSystemToken());
	}
	
	// OpenKM 6.4.1
	public static String cronTask(String[] params) {
		log.info("cronTask({})",params[0]);
		return cronTask(params[0]);
	}
	
	// OpenKM 6.4.2+
	public static String cronTask(String systemToken) {
		log.info("cronTask({})",systemToken);
		return "mail body here";
	}
}