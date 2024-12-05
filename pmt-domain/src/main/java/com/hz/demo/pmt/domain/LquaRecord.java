package com.hz.demo.pmt.domain;

import java.sql.ResultSet;

public record LquaRecord(
        String mandt,
        String lgnum,
        String lqnum,
        String matnr,
        String werks,
        String charg,
        String bestq,
        String sobkz,
        String sonum,
        String lgtyp,
        String lgpla,
        String plpos,
        String skzue,
        String skzua,
        String skzse,
        String skzsa,
        String skzsi,
        String spgru,
        String zeugn,
        String bdatu,
        String bzeit,
        String btanr,
        String btaps,
        String edatu,
        String ezeit,
        String adatu,
        String azeit,
        String zdatu,
        String wdatu,
        String wenum,
        String wepos,
        String letyp,
        String meins,
        double gesme,
        double verme,
        double einme,
        double ausme,
        double mgewi,
        String gewei,
        String tbnum,
        String ivnum,
        String ivpos,
        String betyp,
        String benum,
        String lenum,
        String qplos,
        String vfdat,
        double qkapv,
        String kober,
        String lgort,
        String virgo,
        double trame,
        String kzhuq,
        String vbeln,
        String posnr,
        String idatu,
        byte[] msrInspGuid,
        String sgtScat,
        java.sql.Timestamp shareplexSourceTime,
        String shareplexSourceOperation,
        double shareplexSourceScn,
        java.sql.RowId shareplexSourceRowid,
        boolean isDeleted,
        long timestamp) {

    public LquaRecord() {
        this(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0.0, 0.0, 0.0,
                0.0, 0.0, null, null, null, null, null, null, null, null, null, 0.0, null, null, null, 0.0, null, null,
                null, null, null, null, null, null, 0.0, null, true, 0);

    }

    public LquaRecord(ResultSet rs, long timestamp) throws Exception {
        this(rs.getString("MANDT"), rs.getString("LGNUM"), rs.getString("LQNUM"),
                rs.getString("MATNR"),
                rs.getString("WERKS"), rs.getString("CHARG"), rs.getString("BESTQ"),
                rs.getString("SOBKZ"),
                rs.getString("SONUM"), rs.getString("LGTYP"), rs.getString("LGPLA"),
                rs.getString("PLPOS"),
                rs.getString("SKZUE"), rs.getString("SKZUA"), rs.getString("SKZSE"),
                rs.getString("SKZSA"),
                rs.getString("SKZSI"), rs.getString("SPGRU"), rs.getString("ZEUGN"),
                rs.getString("BDATU"),
                rs.getString("BZEIT"), rs.getString("BTANR"), rs.getString("BTAPS"),
                rs.getString("EDATU"),
                rs.getString("EZEIT"), rs.getString("ADATU"), rs.getString("AZEIT"),
                rs.getString("ZDATU"),
                rs.getString("WDATU"), rs.getString("WENUM"), rs.getString("WEPOS"),
                rs.getString("LETYP"),
                rs.getString("MEINS"), rs.getDouble("GESME"), rs.getDouble("VERME"),
                rs.getDouble("EINME"),
                rs.getDouble("AUSME"), rs.getDouble("MGEWI"), rs.getString("GEWEI"),
                rs.getString("TBNUM"),
                rs.getString("IVNUM"), rs.getString("IVPOS"), rs.getString("BETYP"),
                rs.getString("BENUM"),
                rs.getString("LENUM"), rs.getString("QPLOS"), rs.getString("VFDAT"),
                rs.getDouble("QKAPV"),
                rs.getString("KOBER"), rs.getString("LGORT"), rs.getString("VIRGO"),
                rs.getDouble("TRAME"),
                rs.getString("KZHUQ"), rs.getString("VBELN"), rs.getString("POSNR"),
                rs.getString("IDATU"),
                rs.getBytes("MSR_INSP_GUID"), rs.getString("SGT_SCAT"),
                rs.getTimestamp("SHAREPLEX_SOURCE_TIME"),
                rs.getString("SHAREPLEX_SOURCE_OPERATION"), rs.getDouble("SHAREPLEX_SOURCE_SCN"),
                rs.getRowId("SHAREPLEX_SOURCE_ROWID"), false, timestamp);

    }

}