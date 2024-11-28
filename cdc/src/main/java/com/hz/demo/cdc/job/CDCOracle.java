package com.hz.demo.cdc.job;

import java.util.Properties;

import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.connect.json.JsonDeserializer;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.enterprise.jet.cdc.ChangeRecord;
import com.hazelcast.enterprise.jet.cdc.DebeziumCdcSources;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.config.ProcessingGuarantee;
import com.hazelcast.jet.kafka.KafkaSinks;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.StreamSource;
import com.hazelcast.jet.pipeline.StreamStage;
import com.hazelcast.sql.SqlResult;

import static com.hazelcast.jet.datamodel.Tuple2.tuple2;

/**
 * This class deploys a job that reads from Oracle via CDC and writes to a Map
 * with short expiry
 */
public class CDCOracle {

    private static final String JOB_NAME = "cdc-oracle-job";
    private static final String SNAPSHOT_NAME = "cdc-oracle-job";
    private StreamSource<ChangeRecord> cdcSource = getCdcSource();

    public static void main(String[] args) {
        new CDCOracle().run();
    }

    public CDCOracle() {}
    public CDCOracle(StreamSource<ChangeRecord> cdcSource) {
        this.cdcSource = cdcSource;
    }
    private void run() {
        HazelcastInstance instance = Hazelcast.bootstrappedInstance();
        deployJob(instance);
    }

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

    private Pipeline createPipeline() {

        Pipeline pipeline = Pipeline.create();
        StreamStage<ChangeRecord> filterStage = pipeline.readFrom(cdcSource)
                .withIngestionTimestamps()
                .peek()
                .filter(e -> !e.key().toString().contains("databaseName"));
        filterStage.map(customers -> tuple2(customers.key().toJson(), customers.toJson()))
                .writeTo(KafkaSinks.kafka(getRawKfkaProps(), "lqua_raw"))// TODO: pick topic name based on table name
        ;

        return pipeline;
    }

    private Properties getRawKfkaProps() {
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
        return DebeziumCdcSources.debezium("hz_customers", "io.debezium.connector.oracle.OracleConnector")
                .setProperty("connector.class", "io.debezium.connector.oracle.OracleConnector")
                .setProperty("database.hostname", "oracle-db23c-free-oracle-db23c-free.default.svc.cluster.local")
                .setProperty("database.port", "1521")
                .setProperty("database.user", "c##dbzuser")
                .setProperty("database.password", "dbz")
                .setProperty("database.dbname", "FREE")
                .setProperty("database.pdb.name", "FREEPDB1")
                .setProperty("tasks.max", "1")
                .setProperty("table.include.list", "C##DBZUSER.CUSTOMERS")
                // Debezium 1.9.x
                .setProperty("database.server.name", "server1")
                .build();
    }

}
