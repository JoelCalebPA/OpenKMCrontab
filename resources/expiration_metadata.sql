-- REGISTRER METADATA DATABASE TABLE
DELETE FROM OKM_DB_METADATA_TYPE WHERE DMT_TABLE='group';
INSERT INTO OKM_DB_METADATA_TYPE (DMT_TABLE, DMT_REAL_COLUMN, DMT_TYPE, DMT_VIRTUAL_COLUMN) VALUES ( 'group', 'col00', 'text', 'gru_name');
INSERT INTO OKM_DB_METADATA_TYPE (DMT_TABLE, DMT_REAL_COLUMN, DMT_TYPE, DMT_VIRTUAL_COLUMN) VALUES ( 'group', 'col01', 'text', 'gru_user');

-- REGISTER CONFIGURATION PROPERTIES
DELETE FROM OKM_CONFIG WHERE CFG_KEY LIKE '%expiration.%';
INSERT INTO OKM_CONFIG (CFG_KEY, CFG_TYPE, CFG_VALUE) VALUES ('expiration.date.pattern', 'string', 'dd-MM-yyyy HH:mm:ss');
INSERT INTO OKM_CONFIG (CFG_KEY, CFG_TYPE, CFG_VALUE) VALUES ('expiration.expiration.alert.days', 'string', '15');
INSERT INTO OKM_CONFIG (CFG_KEY, CFG_TYPE, CFG_VALUE) VALUES ('expiration.notification.subject', 'string', 'New uploaded document with expiration');
INSERT INTO OKM_CONFIG (CFG_KEY, CFG_TYPE, CFG_VALUE) VALUES ('expiration.notification.template', 'text', 'New document uploaded<br/>${docUrl}<br/>Expiration date:${date}<br/>');
INSERT INTO OKM_CONFIG (CFG_KEY, CFG_TYPE, CFG_VALUE) VALUES ('expiration.alert.subject', 'string', 'Alert documents will expire');
INSERT INTO OKM_CONFIG (CFG_KEY, CFG_TYPE, CFG_VALUE) VALUES ('expiration.alert.template', 'text', 'Document ${docUrl} will expire at ${date}<br/>');
INSERT INTO OKM_CONFIG (CFG_KEY, CFG_TYPE, CFG_VALUE) VALUES ('expiration.expired.subject', 'string', 'Documents expired');
INSERT INTO OKM_CONFIG (CFG_KEY, CFG_TYPE, CFG_VALUE) VALUES ('expiration.expired.template', 'text', 'Document ${docUrl} expired at ${date}<br/>');
