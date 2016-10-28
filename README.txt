-- SAMPLES
-----------
CrontabTest -> simple crontab 
CrontabImporterTest -> import all PDF files from "/home/openkm/Development/portable/import" into "/okm:root/import" 
InitialNotification -> immediately notify ( crontab executed each minute ) after has been added expiration to document
AlertExpirationNotification -> notify document will expire soon ( crontab executed at night always after ExpirationNotification )
ExpirationNotification -> mark documents as expired and notify ( execute at night always before AlertExpirationNotification

-- RESOURCES
-----------
expiration_metadata.sql -> define database metadata table called group and register configuration parameters ( already executed, not need to execute again )
PropertyGroup.xml -> property group definition needed by expiration ( yet registered, not need to register again )

-- Instructions to create jar file:
------------------------------------
1- Select file "CrontabTest.java" from Project Explorer or Navigator. 
2- Right click ( contextual menu ) and choose "export".
3- Choose Java -> Jar file
4- Select the destination folder, for example /home/openkm/Development/workspace/crontab-sample/crontab.jar
5- Click next
6- Click next
7- Choose "CrontabTest.java" as the class of the entry point
8- Click finish

Register to OpenKM:
1- Go to Administration -> Crontab
2- Click add icon ( new crontab )
3- Choose the jar file