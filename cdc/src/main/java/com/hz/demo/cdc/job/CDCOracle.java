package com.hz.demo.cdc.job;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.connect.json.JsonDeserializer;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.dataconnection.impl.JdbcDataConnection;
import com.hazelcast.enterprise.jet.cdc.ChangeRecord;
import com.hazelcast.enterprise.jet.cdc.DebeziumCdcSources;
import com.hazelcast.enterprise.jet.cdc.Operation;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.pipeline.DataConnectionRef;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.ServiceFactories;
import com.hazelcast.jet.pipeline.ServiceFactory;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.StreamSource;
import com.hazelcast.jet.pipeline.StreamStage;
import com.hazelcast.jet.pipeline.WindowDefinition;
import com.hazelcast.sql.SqlResult;
import com.hz.demo.cdc.domain.LquaRecord;

/**
 * This class deploys a job that reads from Oracle via CDC and writes to a Map
 * with short expiry
 */
public class CDCOracle implements Serializable{

    private static final String JOB_NAME = "cdc-oracle-job";
    private static final String SNAPSHOT_NAME = "cdc-oracle-job";
    public static final String TOPIC = "lqua_raw";
    public static final String TABLE_PREFIX = "C##DBZUSER";
    public static final String SOURCE_DATA_CONNECTION_NAME = "source_oracle_db";
    public static final String TARGET_DATA_CONNECTION_NAME = "source_oracle_db";
    private StreamSource<ChangeRecord> cdcSource = getCdcSource();
    // private StreamSource<Entry<String, String>> cdcSource = getCdcSource();
    private Properties rawKafkaProps = getRawKafkaProps();

    public static void main(String[] args) {
        new CDCOracle().run();
    }

    public CDCOracle() {
    }

    /*
     * Used by test code
     */
    public CDCOracle(StreamSource<ChangeRecord> cdcSource, Properties rawKafkaProps) {
        this.cdcSource = cdcSource;
        this.rawKafkaProps = rawKafkaProps;
    }

    private void run() {
        HazelcastInstance instance = Hazelcast.bootstrappedInstance();
        deployJob(instance);
    }

    /*
     * Deploy the job but first delete the old one
     */
    public Job deployJob(HazelcastInstance instance) {
        Pipeline p = createPipeline();
        JobConfig jobConfig = new JobConfig()
                .setName(JOB_NAME)
                .addClass(CDCOracle.class);
        SqlResult jobs = instance.getSql().execute("SHOW JOBS;");
        // drop the job if it exists
        String finalSnapshot = jobs.stream()
                .filter(row -> row.getObject("name").equals(JOB_NAME))
                .map(row -> "DROP JOB IF EXISTS \"" + JOB_NAME + "\" WITH SNAPSHOT \"" + SNAPSHOT_NAME + "\";")
                .map(sql -> instance.getSql().execute(sql))
                .findAny()
                .map(rs -> SNAPSHOT_NAME)
                .orElse(null);
        jobConfig.setInitialSnapshotName(finalSnapshot);
        instance.getJet().getConfig().setResourceUploadEnabled(true);
        return instance.getJet().newJob(p, jobConfig);
    }

    /*
     * Build the pipeline for the job
     */
    private Pipeline createPipeline() {

        ServiceFactory<?, Connection> jdbcServiceFactory = ServiceFactories.sharedService(ctx -> {
            return ctx.dataConnectionService().getAndRetainDataConnection(SOURCE_DATA_CONNECTION_NAME, JdbcDataConnection.class)
                    .getConnection();
        },
                con -> con.close())
                .toNonCooperative();

        Pipeline pipeline = Pipeline.create();
        StreamStage<ChangeRecord> filterStage = 
        pipeline.readFrom(cdcSource)
                .withIngestionTimestamps()
                .filter(changeRecord -> changeRecord.operation() != Operation.UNSPECIFIED)
                // .filter(changeRecord -> changeRecord.key() != null)
                ;
                
        StreamStage<LquaRecord> enrichedStream = filterStage
                .groupingKey(ChangeRecord::key)
                // collect all records in a session (maybe caused by burst of updates).
                // Window will close after 100 milliseconds of inactivity using a tumbling
                // window will ALWAYS wait for a fix time whereas as session
                // progresses when no data arrives for 100 ms
                .window(WindowDefinition.session(TimeUnit.MILLISECONDS.toMillis(100)))
                .distinct().setName("Pick any one for the key")
                .mapUsingService(jdbcServiceFactory, (conn, record) -> {
                    // FIXME make it async and batched maybe
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "SELECT * FROM " + TABLE_PREFIX + ".lqua WHERE mandt = ? AND lgnum = ? AND lqnum = ?")) {
                        Map<String, Object> map = record.key().toMap();
                        stmt.setString(1, map.get("MANDT").toString());
                        stmt.setString(2, map.get("LGNUM").toString());
                        stmt.setString(3, map.get("LQNUM").toString());
                        ResultSet rs = stmt.executeQuery();
                        if (rs.next()) {
                            return new LquaRecord(rs);
                        } else {
                            return new LquaRecord();
                        }
                    }
                })
                .setName("Repopulated from source");
        ;
        // DELETE only
        enrichedStream.filter(lqua -> lqua.isDeleted()).setName("Deleted Records")
                .writeTo(Sinks.jdbc(
                        "DELETE FROM " + TABLE_PREFIX + ".lqua_target WHERE mandt = ? AND lgnum = ? AND lqnum = ?",
                        DataConnectionRef.dataConnectionRef(TARGET_DATA_CONNECTION_NAME), 
                        (stmt, lqua) -> {
                            stmt.setString(1, lqua.mandt());
                            stmt.setString(2, lqua.lgnum());
                            stmt.setString(3, lqua.lqnum());
                        }))
                .setName("Delete target row");
        // INSERT/UPDATE only
        enrichedStream.filter(lqua -> !lqua.isDeleted()).setName("Upserted Records")
                // merge to target table
                .writeTo(Sinks.jdbc(getMergeStatement(),
                        DataConnectionRef.dataConnectionRef(TARGET_DATA_CONNECTION_NAME), 
                        (stmt, record) -> {
                            int paramIndex = 1;
                            stmt.setString(paramIndex++, record.mandt());
                            stmt.setString(paramIndex++, record.lgnum());
                            stmt.setString(paramIndex++, record.lqnum());
                            stmt.setString(paramIndex++, record.matnr());
                            stmt.setString(paramIndex++, record.werks());
                            stmt.setString(paramIndex++, record.charg());
                            stmt.setString(paramIndex++, record.bestq());
                            stmt.setString(paramIndex++, record.sobkz());
                            stmt.setString(paramIndex++, record.sonum());
                            stmt.setString(paramIndex++, record.lgtyp());
                            stmt.setString(paramIndex++, record.lgpla());
                            stmt.setString(paramIndex++, record.plpos());
                            stmt.setString(paramIndex++, record.skzue());
                            stmt.setString(paramIndex++, record.skzua());
                            stmt.setString(paramIndex++, record.skzse());
                            stmt.setString(paramIndex++, record.skzsa());
                            stmt.setString(paramIndex++, record.skzsi());
                            stmt.setString(paramIndex++, record.spgru());
                            stmt.setString(paramIndex++, record.zeugn());
                            stmt.setString(paramIndex++, record.bdatu());
                            stmt.setString(paramIndex++, record.bzeit());
                            stmt.setString(paramIndex++, record.btanr());
                            stmt.setString(paramIndex++, record.btaps());
                            stmt.setString(paramIndex++, record.edatu());
                            stmt.setString(paramIndex++, record.ezeit());
                            stmt.setString(paramIndex++, record.adatu());
                            stmt.setString(paramIndex++, record.azeit());
                            stmt.setString(paramIndex++, record.zdatu());
                            stmt.setString(paramIndex++, record.wdatu());
                            stmt.setString(paramIndex++, record.wenum());
                            stmt.setString(paramIndex++, record.wepos());
                            stmt.setString(paramIndex++, record.letyp());
                            stmt.setString(paramIndex++, record.meins());
                            stmt.setDouble(paramIndex++, record.gesme());
                            stmt.setDouble(paramIndex++, record.verme());
                            stmt.setDouble(paramIndex++, record.einme());
                            stmt.setDouble(paramIndex++, record.ausme());
                            stmt.setDouble(paramIndex++, record.mgewi());
                            stmt.setString(paramIndex++, record.gewei());
                            stmt.setString(paramIndex++, record.tbnum());
                            stmt.setString(paramIndex++, record.ivnum());
                            stmt.setString(paramIndex++, record.ivpos());
                            stmt.setString(paramIndex++, record.betyp());
                            stmt.setString(paramIndex++, record.benum());
                            stmt.setString(paramIndex++, record.lenum());
                            stmt.setString(paramIndex++, record.qplos());
                            stmt.setString(paramIndex++, record.vfdat());
                            stmt.setDouble(paramIndex++, record.qkapv());
                            stmt.setString(paramIndex++, record.kober());
                            stmt.setString(paramIndex++, record.lgort());
                            stmt.setString(paramIndex++, record.virgo());
                            stmt.setDouble(paramIndex++, record.trame());
                            stmt.setString(paramIndex++, record.kzhuq());
                            stmt.setString(paramIndex++, record.vbeln());
                            stmt.setString(paramIndex++, record.posnr());
                            stmt.setString(paramIndex++, record.idatu());
                        }))
                .setName("Upsert target row");

        return pipeline;
    }

    private String getMergeStatement() {
        return """
                   MERGE INTO lqua_target t USING
                (SELECT ? as mandt, ? as lgnum, ? as lqnum, ? as matnr, ? as werks,
                      ? as charg, ? as bestq, ? as sobkz, ? as sonum, ? as lgtyp,
                      ? as lgpla, ? as plpos, ? as skzue, ? as skzua, ? as skzse,
                      ? as skzsa, ? as skzsi, ? as spgru, ? as zeugn, ? as bdatu,
                      ? as bzeit, ? as btanr, ? as btaps, ? as edatu, ? as ezeit,
                      ? as adatu, ? as azeit, ? as zdatu, ? as wdatu, ? as wenum,
                      ? as wepos, ? as letyp, ? as meins, ? as gesme, ? as verme,
                      ? as einme, ? as ausme, ? as mgewi, ? as gewei, ? as tbnum,
                      ? as ivnum, ? as ivpos, ? as betyp, ? as benum, ? as lenum,
                      ? as qplos, ? as vfdat, ? as qkapv, ? as kober, ? as lgort,
                      ? as virgo, ? as trame, ? as kzhuq, ? as vbeln, ? as posnr) s
                ON (t.mandt = s.mandt AND t.lgnum = s.lgnum AND t.lqnum = s.lqnum)
                WHEN MATCHED THEN
                UPDATE SET
                   t.matnr = s.matnr, t.werks = s.werks, t.charg = s.charg,
                   t.bestq = s.bestq, t.sobkz = s.sobkz, t.sonum = s.sonum,
                   t.lgtyp = s.lgtyp, t.lgpla = s.lgpla, t.plpos = s.plpos,
                   t.skzue = s.skzue, t.skzua = s.skzua, t.skzse = s.skzse,
                   t.skzsa = s.skzsa, t.skzsi = s.skzsi, t.spgru = s.spgru,
                   t.zeugn = s.zeugn, t.bdatu = s.bdatu, t.bzeit = s.bzeit,
                   t.btanr = s.btanr, t.btaps = s.btaps, t.edatu = s.edatu,
                   t.ezeit = s.ezeit, t.adatu = s.adatu, t.azeit = s.azeit,
                   t.zdatu = s.zdatu, t.wdatu = s.wdatu, t.wenum = s.wenum,
                   t.wepos = s.wepos, t.letyp = s.letyp, t.meins = s.meins,
                   t.gesme = s.gesme, t.verme = s.verme, t.einme = s.einme,
                   t.ausme = s.ausme, t.mgewi = s.mgewi, t.gewei = s.gewei,
                   t.tbnum = s.tbnum, t.ivnum = s.ivnum, t.ivpos = s.ivpos,
                   t.betyp = s.betyp, t.benum = s.benum, t.lenum = s.lenum,
                   t.qplos = s.qplos, t.vfdat = s.vfdat, t.qkapv = s.qkapv,
                   t.kober = s.kober, t.lgort = s.lgort, t.virgo = s.virgo,
                   t.trame = s.trame, t.kzhuq = s.kzhuq, t.vbeln = s.vbeln,
                   t.posnr = s.posnr
                WHEN NOT MATCHED THEN
                INSERT (mandt, lgnum, lqnum, matnr, werks, charg, bestq, sobkz,
                      sonum, lgtyp, lgpla, plpos, skzue, skzua, skzse, skzsa,
                      skzsi, spgru, zeugn, bdatu, bzeit, btanr, btaps, edatu,
                      ezeit, adatu, azeit, zdatu, wdatu, wenum, wepos, letyp,
                      meins, gesme, verme, einme, ausme, mgewi, gewei, tbnum,
                      ivnum, ivpos, betyp, benum, lenum, qplos, vfdat, qkapv,
                      kober, lgort, virgo, trame, kzhuq, vbeln, posnr)
                VALUES (s.mandt, s.lgnum, s.lqnum, s.matnr, s.werks, s.charg,
                      s.bestq, s.sobkz, s.sonum, s.lgtyp, s.lgpla, s.plpos,
                      s.skzue, s.skzua, s.skzse, s.skzsa, s.skzsi, s.spgru,
                      s.zeugn, s.bdatu, s.bzeit, s.btanr, s.btaps, s.edatu,
                      s.ezeit, s.adatu, s.azeit, s.zdatu, s.wdatu, s.wenum,
                      s.wepos, s.letyp, s.meins, s.gesme, s.verme, s.einme,
                      s.ausme, s.mgewi, s.gewei, s.tbnum, s.ivnum, s.ivpos,
                      s.betyp, s.benum, s.lenum, s.qplos, s.vfdat, s.qkapv,
                      s.kober, s.lgort, s.virgo, s.trame, s.kzhuq, s.vbeln,
                      s.posnr)                    """;
    }

    /*
     * Properties to connect to Kafka
     */
    private Properties getRawKafkaProps() {
        Properties props = new Properties();
        String bootstrapServers = "my-cluster-kafka-bootstrap.kafka:9092"; // Updated to include the namespace
        props.setProperty("bootstrap.servers", bootstrapServers);
        props.setProperty("key.deserializer", JsonDeserializer.class.getCanonicalName());
        props.setProperty("value.deserializer", JsonDeserializer.class.getCanonicalName());
        props.setProperty("key.serializer", StringSerializer.class.getCanonicalName());
        props.setProperty("value.serializer", StringSerializer.class.getCanonicalName());
        props.setProperty("auto.offset.reset", "earliest");

        return props;
    }

    private StreamSource<ChangeRecord> getCdcSource() {
    // private StreamSource<Entry<String, String>> getCdcSource() {
        return DebeziumCdcSources.debezium("cdc_oracle", "io.debezium.connector.oracle.OracleConnector")
                .setProperty("connector.class", "io.debezium.connector.oracle.OracleConnector")
                .setProperty("database.hostname", "oracle-db23c-free-oracle-db23c-free.default.svc.cluster.local")
                .setProperty("database.port", "1521")
                .setProperty("database.user", "c##dbzuser")
                .setProperty("database.password", "dbz")
                .setProperty("database.dbname", "FREE")
                .setProperty("database.pdb.name", "FREEPDB1")
                .setProperty("tasks.max", "1")
                .setProperty("table.include.list", TABLE_PREFIX + ".LQUA")
                .changeRecord()
                // .json()
                // Debezium 1.9.x
                // .setProperty("database.server.name", "server1")
                .build();
    }

}
