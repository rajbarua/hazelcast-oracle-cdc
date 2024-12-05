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
-- Configure Oracle: Following enabled minimal supplemental logging.
ALTER DATABASE ADD SUPPLEMENTAL LOG DATA;
-- For each table we are interested in, enable supplemental logging for all columns. Example:
-- ALTER TABLE C##DBZUSER.CUSTOMERS ADD SUPPLEMENTAL LOG DATA (ALL) COLUMNS;

-- Configure Oracle: User setup
CONNECT sys/NDEcoGZOAc@FREE as sysdba;
CREATE TABLESPACE logminer_tbs DATAFILE '/opt/oracle/oradata/FREE/logminer_tbs.dbf' SIZE 25M REUSE AUTOEXTEND ON MAXSIZE UNLIMITED;

CONNECT sys/NDEcoGZOAc@FREEPDB1 as sysdba;
CREATE TABLESPACE logminer_tbs DATAFILE '/opt/oracle/oradata/FREE/FREEPDB1/logminer_tbs.dbf' SIZE 25M REUSE AUTOEXTEND ON MAXSIZE UNLIMITED;

CONNECT sys/NDEcoGZOAc@FREE as sysdba;
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
UPDATE customers SET name = '3Salles Thomas Jr.' WHERE id = 1001;
COMMIT;
CREATE TABLE LQUA
(
  MANDT                       VARCHAR2(9 BYTE)  NOT NULL,
  LGNUM                       VARCHAR2(9 BYTE)  NOT NULL,
  LQNUM                       VARCHAR2(30 BYTE) NOT NULL,
  MATNR                       VARCHAR2(54 BYTE) NOT NULL,
  WERKS                       VARCHAR2(12 BYTE) NOT NULL,
  CHARG                       VARCHAR2(30 BYTE) NOT NULL,
  BESTQ                       VARCHAR2(3 BYTE)  NOT NULL,
  SOBKZ                       VARCHAR2(3 BYTE)  NOT NULL,
  SONUM                       VARCHAR2(48 BYTE) NOT NULL,
  LGTYP                       VARCHAR2(9 BYTE)  NOT NULL,
  LGPLA                       VARCHAR2(30 BYTE) NOT NULL,
  PLPOS                       VARCHAR2(6 BYTE)  NOT NULL,
  SKZUE                       VARCHAR2(3 BYTE)  NOT NULL,
  SKZUA                       VARCHAR2(3 BYTE)  NOT NULL,
  SKZSE                       VARCHAR2(3 BYTE)  NOT NULL,
  SKZSA                       VARCHAR2(3 BYTE)  NOT NULL,
  SKZSI                       VARCHAR2(3 BYTE)  NOT NULL,
  SPGRU                       VARCHAR2(3 BYTE)  NOT NULL,
  ZEUGN                       VARCHAR2(30 BYTE) NOT NULL,
  BDATU                       VARCHAR2(24 BYTE) NOT NULL,
  BZEIT                       VARCHAR2(18 BYTE) NOT NULL,
  BTANR                       VARCHAR2(30 BYTE) NOT NULL,
  BTAPS                       VARCHAR2(12 BYTE) NOT NULL,
  EDATU                       VARCHAR2(24 BYTE) NOT NULL,
  EZEIT                       VARCHAR2(18 BYTE) NOT NULL,
  ADATU                       VARCHAR2(24 BYTE) NOT NULL,
  AZEIT                       VARCHAR2(18 BYTE) NOT NULL,
  ZDATU                       VARCHAR2(24 BYTE) NOT NULL,
  WDATU                       VARCHAR2(24 BYTE) NOT NULL,
  WENUM                       VARCHAR2(30 BYTE) NOT NULL,
  WEPOS                       VARCHAR2(12 BYTE) NOT NULL,
  LETYP                       VARCHAR2(9 BYTE)  NOT NULL,
  MEINS                       VARCHAR2(9 BYTE)  NOT NULL,
  GESME                       NUMBER(13,3)      NOT NULL,
  VERME                       NUMBER(13,3)      NOT NULL,
  EINME                       NUMBER(13,3)      NOT NULL,
  AUSME                       NUMBER(13,3)      NOT NULL,
  MGEWI                       NUMBER(11,3)      NOT NULL,
  GEWEI                       VARCHAR2(9 BYTE)  NOT NULL,
  TBNUM                       VARCHAR2(30 BYTE) NOT NULL,
  IVNUM                       VARCHAR2(30 BYTE) NOT NULL,
  IVPOS                       VARCHAR2(12 BYTE) NOT NULL,
  BETYP                       VARCHAR2(3 BYTE)  NOT NULL,
  BENUM                       VARCHAR2(30 BYTE) NOT NULL,
  LENUM                       VARCHAR2(60 BYTE) NOT NULL,
  QPLOS                       VARCHAR2(36 BYTE) NOT NULL,
  VFDAT                       VARCHAR2(24 BYTE) NOT NULL,
  QKAPV                       NUMBER(11,3)      NOT NULL,
  KOBER                       VARCHAR2(9 BYTE)  NOT NULL,
  LGORT                       VARCHAR2(12 BYTE) NOT NULL,
  VIRGO                       VARCHAR2(3 BYTE)  NOT NULL,
  TRAME                       NUMBER(13,3)      NOT NULL,
  KZHUQ                       VARCHAR2(3 BYTE)  NOT NULL,
  VBELN                       VARCHAR2(30 BYTE) NOT NULL,
  POSNR                       VARCHAR2(18 BYTE) NOT NULL,
  IDATU                       VARCHAR2(24 BYTE),
  MSR_INSP_GUID               RAW(16),
  SGT_SCAT                    VARCHAR2(48 BYTE),
  SHAREPLEX_SOURCE_TIME       TIMESTAMP(6),
  SHAREPLEX_SOURCE_OPERATION  VARCHAR2(20 BYTE),
  SHAREPLEX_SOURCE_SCN        NUMBER,
  SHAREPLEX_SOURCE_ROWID      ROWID,
  SUPPLEMENTAL LOG DATA (ALL) COLUMNS,
  PRIMARY KEY (MANDT, LGNUM, LQNUM)
)
TABLESPACE LOGMINER_TBS
PCTFREE    10
INITRANS   1
MAXTRANS   255
STORAGE    (
            INITIAL          64K
            NEXT             1M
            MINEXTENTS       1
            MAXEXTENTS       UNLIMITED
            PCTINCREASE      0
            BUFFER_POOL      DEFAULT
           )
LOGGING
NOCOMPRESS
NOCACHE;
CREATE TABLE LQUA_TARGET
(
  MANDT                       VARCHAR2(9 BYTE)  NOT NULL,
  LGNUM                       VARCHAR2(9 BYTE)  NOT NULL,
  LQNUM                       VARCHAR2(30 BYTE) NOT NULL,
  MATNR                       VARCHAR2(54 BYTE) NOT NULL,
  WERKS                       VARCHAR2(12 BYTE) NOT NULL,
  CHARG                       VARCHAR2(30 BYTE) NOT NULL,
  BESTQ                       VARCHAR2(3 BYTE)  NOT NULL,
  SOBKZ                       VARCHAR2(3 BYTE)  NOT NULL,
  SONUM                       VARCHAR2(48 BYTE) NOT NULL,
  LGTYP                       VARCHAR2(9 BYTE)  NOT NULL,
  LGPLA                       VARCHAR2(30 BYTE) NOT NULL,
  PLPOS                       VARCHAR2(6 BYTE)  NOT NULL,
  SKZUE                       VARCHAR2(3 BYTE)  NOT NULL,
  SKZUA                       VARCHAR2(3 BYTE)  NOT NULL,
  SKZSE                       VARCHAR2(3 BYTE)  NOT NULL,
  SKZSA                       VARCHAR2(3 BYTE)  NOT NULL,
  SKZSI                       VARCHAR2(3 BYTE)  NOT NULL,
  SPGRU                       VARCHAR2(3 BYTE)  NOT NULL,
  ZEUGN                       VARCHAR2(30 BYTE) NOT NULL,
  BDATU                       VARCHAR2(24 BYTE) NOT NULL,
  BZEIT                       VARCHAR2(18 BYTE) NOT NULL,
  BTANR                       VARCHAR2(30 BYTE) NOT NULL,
  BTAPS                       VARCHAR2(12 BYTE) NOT NULL,
  EDATU                       VARCHAR2(24 BYTE) NOT NULL,
  EZEIT                       VARCHAR2(18 BYTE) NOT NULL,
  ADATU                       VARCHAR2(24 BYTE) NOT NULL,
  AZEIT                       VARCHAR2(18 BYTE) NOT NULL,
  ZDATU                       VARCHAR2(24 BYTE) NOT NULL,
  WDATU                       VARCHAR2(24 BYTE) NOT NULL,
  WENUM                       VARCHAR2(30 BYTE) NOT NULL,
  WEPOS                       VARCHAR2(12 BYTE) NOT NULL,
  LETYP                       VARCHAR2(9 BYTE)  NOT NULL,
  MEINS                       VARCHAR2(9 BYTE)  NOT NULL,
  GESME                       NUMBER(13,3)      NOT NULL,
  VERME                       NUMBER(13,3)      NOT NULL,
  EINME                       NUMBER(13,3)      NOT NULL,
  AUSME                       NUMBER(13,3)      NOT NULL,
  MGEWI                       NUMBER(11,3)      NOT NULL,
  GEWEI                       VARCHAR2(9 BYTE)  NOT NULL,
  TBNUM                       VARCHAR2(30 BYTE) NOT NULL,
  IVNUM                       VARCHAR2(30 BYTE) NOT NULL,
  IVPOS                       VARCHAR2(12 BYTE) NOT NULL,
  BETYP                       VARCHAR2(3 BYTE)  NOT NULL,
  BENUM                       VARCHAR2(30 BYTE) NOT NULL,
  LENUM                       VARCHAR2(60 BYTE) NOT NULL,
  QPLOS                       VARCHAR2(36 BYTE) NOT NULL,
  VFDAT                       VARCHAR2(24 BYTE) NOT NULL,
  QKAPV                       NUMBER(11,3)      NOT NULL,
  KOBER                       VARCHAR2(9 BYTE)  NOT NULL,
  LGORT                       VARCHAR2(12 BYTE) NOT NULL,
  VIRGO                       VARCHAR2(3 BYTE)  NOT NULL,
  TRAME                       NUMBER(13,3)      NOT NULL,
  KZHUQ                       VARCHAR2(3 BYTE)  NOT NULL,
  VBELN                       VARCHAR2(30 BYTE) NOT NULL,
  POSNR                       VARCHAR2(18 BYTE) NOT NULL,
  IDATU                       VARCHAR2(24 BYTE),
  MSR_INSP_GUID               RAW(16),
  SGT_SCAT                    VARCHAR2(48 BYTE),
  SHAREPLEX_SOURCE_TIME       TIMESTAMP(6),
  SHAREPLEX_SOURCE_OPERATION  VARCHAR2(20 BYTE),
  SHAREPLEX_SOURCE_SCN        NUMBER,
  SHAREPLEX_SOURCE_ROWID      ROWID,
  read_timestamp NUMBER,
  write_timestamp NUMBER,
  PRIMARY KEY (MANDT, LGNUM, LQNUM)
);
--ALTER TABLE C##DBZUSER.LQUA ADD SUPPLEMENTAL LOG DATA (ALL) COLUMNS;
Insert into LQUA
   (MANDT, LGNUM, LQNUM, MATNR, WERKS,
    CHARG, BESTQ, SOBKZ, SONUM, LGTYP,
    LGPLA, PLPOS, SKZUE, SKZUA, SKZSE,
    SKZSA, SKZSI, SPGRU, ZEUGN, BDATU,
    BZEIT, BTANR, BTAPS, EDATU, EZEIT,
    ADATU, AZEIT, ZDATU, WDATU, WENUM,
    WEPOS, LETYP, MEINS, GESME, VERME,
    EINME, AUSME, MGEWI, GEWEI, TBNUM,
    IVNUM, IVPOS, BETYP, BENUM, LENUM,
    QPLOS, VFDAT, QKAPV, KOBER, LGORT,
    VIRGO, TRAME, KZHUQ, VBELN, POSNR,
    IDATU, MSR_INSP_GUID, SGT_SCAT, SHAREPLEX_SOURCE_TIME, SHAREPLEX_SOURCE_OPERATION,
    SHAREPLEX_SOURCE_SCN, SHAREPLEX_SOURCE_ROWID)
Values
   ('930', 'PEP', '0000299277', 'ISO7740DW', '1585',
    ' ', ' ', ' ', ' ', 'AS1',
    'AS-ISO7740', ' ', ' ', ' ', ' ',
    ' ', ' ', ' ', ' ', '20191017',
    '131913', '0000546803', '0001', '20181026', '143348',
    '20191017', '131913', '00000000', '20181026', '5001649870',
    '0001', ' ', 'EA', 510, 510,
    0, 0, 0.408, 'KG', '0000000000',
    ' ', '0000', 'L', '0788303121', ' ',
    '000000000000', '00000000', 0, ' ', 'PW01',
    ' ', 0, ' ', ' ', '000000',
    '00000000', '00000000000000000000000000000000', ' ', NULL, NULL,
    NULL, NULL);
Insert into LQUA
   (MANDT, LGNUM, LQNUM, MATNR, WERKS,
    CHARG, BESTQ, SOBKZ, SONUM, LGTYP,
    LGPLA, PLPOS, SKZUE, SKZUA, SKZSE,
    SKZSA, SKZSI, SPGRU, ZEUGN, BDATU,
    BZEIT, BTANR, BTAPS, EDATU, EZEIT,
    ADATU, AZEIT, ZDATU, WDATU, WENUM,
    WEPOS, LETYP, MEINS, GESME, VERME,
    EINME, AUSME, MGEWI, GEWEI, TBNUM,
    IVNUM, IVPOS, BETYP, BENUM, LENUM,
    QPLOS, VFDAT, QKAPV, KOBER, LGORT,
    VIRGO, TRAME, KZHUQ, VBELN, POSNR,
    IDATU, MSR_INSP_GUID, SGT_SCAT, SHAREPLEX_SOURCE_TIME, SHAREPLEX_SOURCE_OPERATION,
    SHAREPLEX_SOURCE_SCN, SHAREPLEX_SOURCE_ROWID)
Values
   ('930', 'PEP', '0000299271', 'TPD1E04U04DPLT', '1585',
    ' ', ' ', ' ', ' ', 'AS1',
    'AS-TPD1E04', ' ', ' ', ' ', ' ',
    ' ', ' ', ' ', ' ', '20181026',
    '144745', '0000261049', '0001', '20181026', '144745',
    '00000000', '000000', '00000000', '20181026', '5001650020',
    '0001', ' ', 'EA', 500, 500,
    0, 0, 0.001, 'KG', '0000000000',
    ' ', '0000', 'B', '4513649157', ' ',
    '000000000000', '00000000', 0, ' ', 'PW01',
    ' ', 0, ' ', ' ', '000000',
    '00000000', '00000000000000000000000000000000', ' ', NULL, NULL,
    NULL, NULL);
Insert into LQUA
   (MANDT, LGNUM, LQNUM, MATNR, WERKS,
    CHARG, BESTQ, SOBKZ, SONUM, LGTYP,
    LGPLA, PLPOS, SKZUE, SKZUA, SKZSE,
    SKZSA, SKZSI, SPGRU, ZEUGN, BDATU,
    BZEIT, BTANR, BTAPS, EDATU, EZEIT,
    ADATU, AZEIT, ZDATU, WDATU, WENUM,
    WEPOS, LETYP, MEINS, GESME, VERME,
    EINME, AUSME, MGEWI, GEWEI, TBNUM,
    IVNUM, IVPOS, BETYP, BENUM, LENUM,
    QPLOS, VFDAT, QKAPV, KOBER, LGORT,
    VIRGO, TRAME, KZHUQ, VBELN, POSNR,
    IDATU, MSR_INSP_GUID, SGT_SCAT, SHAREPLEX_SOURCE_TIME, SHAREPLEX_SOURCE_OPERATION,
    SHAREPLEX_SOURCE_SCN, SHAREPLEX_SOURCE_ROWID)
Values
   ('930', 'PEP', '0000229279', 'OPA2333SHKQ', '1585',
    ' ', ' ', ' ', ' ', 'AS1',
    'AS-OPA2333', ' ', ' ', ' ', ' ',
    ' ', ' ', ' ', ' ', '20181026',
    '145247', '0000261050', '0001', '20181026', '145247',
    '00000000', '000000', '00000000', '20181026', '5001650080',
    '0001', ' ', 'EA', 20, 20,
    0, 0, 0.009, 'KG', '0000000000',
    ' ', '0000', 'B', '4513652122', ' ',
    '000000000000', '00000000', 0, ' ', 'PW01',
    ' ', 0, ' ', ' ', '000000',
    '00000000', '00000000000000000000000000000000', ' ', NULL, NULL,
    NULL, NULL);
Insert into LQUA
   (MANDT, LGNUM, LQNUM, MATNR, WERKS,
    CHARG, BESTQ, SOBKZ, SONUM, LGTYP,
    LGPLA, PLPOS, SKZUE, SKZUA, SKZSE,
    SKZSA, SKZSI, SPGRU, ZEUGN, BDATU,
    BZEIT, BTANR, BTAPS, EDATU, EZEIT,
    ADATU, AZEIT, ZDATU, WDATU, WENUM,
    WEPOS, LETYP, MEINS, GESME, VERME,
    EINME, AUSME, MGEWI, GEWEI, TBNUM,
    IVNUM, IVPOS, BETYP, BENUM, LENUM,
    QPLOS, VFDAT, QKAPV, KOBER, LGORT,
    VIRGO, TRAME, KZHUQ, VBELN, POSNR,
    IDATU, MSR_INSP_GUID, SGT_SCAT, SHAREPLEX_SOURCE_TIME, SHAREPLEX_SOURCE_OPERATION,
    SHAREPLEX_SOURCE_SCN, SHAREPLEX_SOURCE_ROWID)
Values
   ('930', 'PEP', '0000239280', 'INA129SJD', '1585',
    ' ', ' ', ' ', ' ', 'AS1',
    'AS-INA129S', ' ', ' ', ' ', ' ',
    ' ', ' ', ' ', ' ', '20190729',
    '151713', '0000486153', '0001', '20181026', '145321',
    '20190729', '151713', '00000000', '20181026', '5001650089',
    '0001', ' ', 'EA', 38, 38,
    0, 0, 0.057, 'KG', '0000000000',
    ' ', '0000', 'L', '0786673361', ' ',
    '000000000000', '00000000', 0, ' ', 'PW01',
    ' ', 0, ' ', ' ', '000000',
    '00000000', '00000000000000000000000000000000', ' ', NULL, NULL,
    NULL, NULL);
Insert into LQUA
   (MANDT, LGNUM, LQNUM, MATNR, WERKS,
    CHARG, BESTQ, SOBKZ, SONUM, LGTYP,
    LGPLA, PLPOS, SKZUE, SKZUA, SKZSE,
    SKZSA, SKZSI, SPGRU, ZEUGN, BDATU,
    BZEIT, BTANR, BTAPS, EDATU, EZEIT,
    ADATU, AZEIT, ZDATU, WDATU, WENUM,
    WEPOS, LETYP, MEINS, GESME, VERME,
    EINME, AUSME, MGEWI, GEWEI, TBNUM,
    IVNUM, IVPOS, BETYP, BENUM, LENUM,
    QPLOS, VFDAT, QKAPV, KOBER, LGORT,
    VIRGO, TRAME, KZHUQ, VBELN, POSNR,
    IDATU, MSR_INSP_GUID, SGT_SCAT, SHAREPLEX_SOURCE_TIME, SHAREPLEX_SOURCE_OPERATION,
    SHAREPLEX_SOURCE_SCN, SHAREPLEX_SOURCE_ROWID)
Values
   ('930', 'PEP', '0000292283', 'ISO7762DBQ', '1585',
    ' ', ' ', ' ', ' ', 'AS1',
    'AS-ISO7762', ' ', ' ', ' ', ' ',
    ' ', ' ', ' ', ' ', '20191129',
    '061028', '0000572830', '0003', '20181026', '143356',
    '20191129', '061028', '20191107', '20181026', '5001649873',
    '0001', ' ', 'EA', 2181, 2181,
    0, 0, 0.262, 'KG', '0000000000',
    ' ', '0000', 'L', '0789154084', ' ',
    '000000000000', '00000000', 0, ' ', 'PW01',
    ' ', 0, ' ', ' ', '000000',
    '00000000', '00000000000000000000000000000000', ' ', NULL, NULL,
    NULL, NULL);
Insert into LQUA
   (MANDT, LGNUM, LQNUM, MATNR, WERKS,
    CHARG, BESTQ, SOBKZ, SONUM, LGTYP,
    LGPLA, PLPOS, SKZUE, SKZUA, SKZSE,
    SKZSA, SKZSI, SPGRU, ZEUGN, BDATU,
    BZEIT, BTANR, BTAPS, EDATU, EZEIT,
    ADATU, AZEIT, ZDATU, WDATU, WENUM,
    WEPOS, LETYP, MEINS, GESME, VERME,
    EINME, AUSME, MGEWI, GEWEI, TBNUM,
    IVNUM, IVPOS, BETYP, BENUM, LENUM,
    QPLOS, VFDAT, QKAPV, KOBER, LGORT,
    VIRGO, TRAME, KZHUQ, VBELN, POSNR,
    IDATU, MSR_INSP_GUID, SGT_SCAT, SHAREPLEX_SOURCE_TIME, SHAREPLEX_SOURCE_OPERATION,
    SHAREPLEX_SOURCE_SCN, SHAREPLEX_SOURCE_ROWID)
Values
   ('930', 'PEP', '0000291284', 'LM317LILP', '1585',
    ' ', ' ', ' ', ' ', 'AS1',
    'AS-LM317LI', ' ', ' ', ' ', ' ',
    ' ', ' ', ' ', ' ', '20191001',
    '032027', '0000537463', '0004', '20181026', '144926',
    '20191001', '032027', '00000000', '20181026', '5001650025',
    '0001', ' ', 'EA', 980, 980,
    0, 0, 0.196, 'KG', '0000000000',
    ' ', '0000', 'L', '0787984546', ' ',
    '000000000000', '00000000', 0, ' ', 'PW01',
    ' ', 0, ' ', ' ', '000000',
    '00000000', '00000000000000000000000000000000', ' ', NULL, NULL,
    NULL, NULL);
Insert into LQUA
   (MANDT, LGNUM, LQNUM, MATNR, WERKS,
    CHARG, BESTQ, SOBKZ, SONUM, LGTYP,
    LGPLA, PLPOS, SKZUE, SKZUA, SKZSE,
    SKZSA, SKZSI, SPGRU, ZEUGN, BDATU,
    BZEIT, BTANR, BTAPS, EDATU, EZEIT,
    ADATU, AZEIT, ZDATU, WDATU, WENUM,
    WEPOS, LETYP, MEINS, GESME, VERME,
    EINME, AUSME, MGEWI, GEWEI, TBNUM,
    IVNUM, IVPOS, BETYP, BENUM, LENUM,
    QPLOS, VFDAT, QKAPV, KOBER, LGORT,
    VIRGO, TRAME, KZHUQ, VBELN, POSNR,
    IDATU, MSR_INSP_GUID, SGT_SCAT, SHAREPLEX_SOURCE_TIME, SHAREPLEX_SOURCE_OPERATION,
    SHAREPLEX_SOURCE_SCN, SHAREPLEX_SOURCE_ROWID)
Values
   ('930', 'PEP', '0000293285', 'TPS2513ADBVT', '1585',
    ' ', ' ', ' ', ' ', 'AS1',
    'AS-TPS2513', ' ', ' ', ' ', ' ',
    ' ', ' ', ' ', ' ', '20181026',
    '145042', '0000261103', '0001', '20181026', '145042',
    '00000000', '000000', '00000000', '20181026', '5001650070',
    '0001', ' ', 'EA', 250, 250,
    0, 0, 0.005, 'KG', '0000000000',
    ' ', '0000', 'B', '4513650570', ' ',
    '000000000000', '00000000', 0, ' ', 'PW01',
    ' ', 0, ' ', ' ', '000000',
    '00000000', '00000000000000000000000000000000', ' ', NULL, NULL,
    NULL, NULL);
COMMIT;


