-- https://debezium.io/blog/2022/09/30/debezium-oracle-series-part-1/
-- Configure Oracle: Archive logs
ALTER SYSTEM SET db_recovery_file_dest_size = 5G;
ALTER SYSTEM SET db_recovery_file_dest = '/opt/oracle/oradata/FREE' scope=spfile;
SHUTDOWN IMMEDIATE
STARTUP MOUNT
ALTER DATABASE ARCHIVELOG;
ALTER DATABASE OPEN;

-- Configure Oracle: Redo logs
-- SELECT GROUP#, BYTES/1024/1024 SIZE_MB, STATUS FROM V$LOG ORDER BY 1;
-- ALTER DATABASE CLEAR LOGFILE GROUP 1;
-- ALTER DATABASE DROP LOGFILE GROUP 1;
-- ALTER DATABASE ADD LOGFILE GROUP 1 ('/opt/oracle/oradata/FREE/redo01.log') size 400M REUSE;
ALTER DATABASE CLEAR LOGFILE GROUP 2;
ALTER DATABASE DROP LOGFILE GROUP 2;
ALTER DATABASE ADD LOGFILE GROUP 2 ('/opt/oracle/oradata/FREE/redo02.log') size 400M REUSE;
ALTER DATABASE CLEAR LOGFILE GROUP 3;
ALTER DATABASE DROP LOGFILE GROUP 3;
ALTER DATABASE ADD LOGFILE GROUP 3 ('/opt/oracle/oradata/FREE/redo03.log') size 400M REUSE;
ALTER SYSTEM SWITCH LOGFILE;
-- Configure Oracle: Supplemental Logging. Do this for selected tabled only
ALTER DATABASE ADD SUPPLEMENTAL LOG DATA;
-- Configure Oracle: User setup
CONNECT sys/tBB27UWXTp@FREE as sysdba;
CREATE TABLESPACE logminer_tbs DATAFILE '/opt/oracle/oradata/FREE/logminer_tbs.dbf' SIZE 25M REUSE AUTOEXTEND ON MAXSIZE UNLIMITED;

CONNECT sys/tBB27UWXTp@FREEPDB1 as sysdba;
CREATE TABLESPACE logminer_tbs DATAFILE '/opt/oracle/oradata/FREE/FREEPDB1/logminer_tbs.dbf' SIZE 25M REUSE AUTOEXTEND ON MAXSIZE UNLIMITED;

CONNECT sys/tBB27UWXTp@FREE as sysdba;
CREATE USER c##dbzuser IDENTIFIED BY dbz DEFAULT TABLESPACE LOGMINER_TBS QUOTA UNLIMITED ON LOGMINER_TBS CONTAINER=ALL;

GRANT CREATE SESSION TO c##dbzuser CONTAINER=ALL;
GRANT SET CONTAINER TO c##dbzuser CONTAINER=ALL;
GRANT SELECT ON V_$DATABASE to c##dbzuser CONTAINER=ALL;
GRANT FLASHBACK ANY TABLE TO c##dbzuser CONTAINER=ALL;
GRANT SELECT ANY TABLE TO c##dbzuser CONTAINER=ALL;
GRANT SELECT_CATALOG_ROLE TO c##dbzuser CONTAINER=ALL;
GRANT EXECUTE_CATALOG_ROLE TO c##dbzuser CONTAINER=ALL;
GRANT SELECT ANY TRANSACTION TO c##dbzuser CONTAINER=ALL;
GRANT LOGMINING TO c##dbzuser CONTAINER=ALL;

GRANT CREATE TABLE TO c##dbzuser CONTAINER=ALL;
GRANT LOCK ANY TABLE TO c##dbzuser CONTAINER=ALL;
GRANT CREATE SEQUENCE TO c##dbzuser CONTAINER=ALL;

GRANT EXECUTE ON DBMS_LOGMNR TO c##dbzuser CONTAINER=ALL;
GRANT EXECUTE ON DBMS_LOGMNR_D TO c##dbzuser CONTAINER=ALL;

GRANT SELECT ON V_$LOG TO c##dbzuser CONTAINER=ALL;
GRANT SELECT ON V_$LOG_HISTORY TO c##dbzuser CONTAINER=ALL;
GRANT SELECT ON V_$LOGMNR_LOGS TO c##dbzuser CONTAINER=ALL;
GRANT SELECT ON V_$LOGMNR_CONTENTS TO c##dbzuser CONTAINER=ALL;
GRANT SELECT ON V_$LOGMNR_PARAMETERS TO c##dbzuser CONTAINER=ALL;
GRANT SELECT ON V_$LOGFILE TO c##dbzuser CONTAINER=ALL;
GRANT SELECT ON V_$ARCHIVED_LOG TO c##dbzuser CONTAINER=ALL;
GRANT SELECT ON V_$ARCHIVE_DEST_STATUS TO c##dbzuser CONTAINER=ALL;
GRANT SELECT ON V_$TRANSACTION TO c##dbzuser CONTAINER=ALL;

GRANT SELECT ON V_$MYSTAT TO c##dbzuser CONTAINER=ALL;
GRANT SELECT ON V_$STATNAME TO c##dbzuser CONTAINER=ALL;

CONNECT c##dbzuser/dbz@FREEPDB1;
CREATE TABLE customers (id number(9,0) primary key, name varchar2(50));
ALTER TABLE C##DBZUSER.CUSTOMERS ADD SUPPLEMENTAL LOG DATA (ALL) COLUMNS;
INSERT INTO customers VALUES (1001, 'Salles Thomas');
INSERT INTO customers VALUES (1002, 'George Bailey');
INSERT INTO customers VALUES (1003, 'Edward Walker');
INSERT INTO customers VALUES (1004, 'Anne Kretchmar');
COMMIT;
