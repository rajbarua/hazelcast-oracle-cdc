package com.hz.demo.cdc.job;


import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.cdc.ChangeRecord;
import com.hazelcast.jet.cdc.DebeziumCdcSources;
import com.hazelcast.jet.config.JobConfig;
import com.hazelcast.jet.config.ProcessingGuarantee;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.StreamSource;

/**
 * This class deploys a job that reads from Oracle via CDC and writes to a Map with short expiry
 */
public class CDCOracle {


    private static final String JOB_NAME = "cdc-oracle-job";
    public static void main(String[] args) {
        new CDCOracle().run();
    }

    private void run() {
        HazelcastInstance instance = Hazelcast.bootstrappedInstance();
        deployJob(instance);
    }

    public void deployJob(HazelcastInstance instance) {
        Pipeline p = createPipeline();
        JobConfig jobConfig = new JobConfig()
            .setName(JOB_NAME)
            .setProcessingGuarantee(ProcessingGuarantee.EXACTLY_ONCE)
            .addClass(CDCOracle.class);
        Job oldJob = instance.getJet().getJob(JOB_NAME);
        if (oldJob != null) {
            oldJob.cancel();
        }
        instance.getJet().getConfig().setResourceUploadEnabled(true);
        instance.getJet().newJob(p, jobConfig);
    }
    private Pipeline createPipeline() {
        
        StreamSource<ChangeRecord> source 
            = DebeziumCdcSources.debezium("hz_customers", "io.debezium.connector.oracle.OracleConnector")
                .setProperty("connector.class", "io.debezium.connector.oracle.OracleConnector")
                .setProperty("database.hostname", "oracle-db23c-free-oracle-db23c-free.default.svc.cluster.local")
                .setProperty("database.port", "1521")
                .setProperty("database.user", "c##dbzuser")
                .setProperty("database.password", "dbz")
                .setProperty("database.dbname", "FREE")
                .setProperty("database.pdb.name", "FREEPDB1")
                .setProperty("tasks.max", "1")
                // .setProperty("table.include.list", ".CUSTOMERS")
                // Debezium 1.9.x
                .setProperty("database.server.name", "server1")
                .build();
            // .withNativeTimestamps(0)
            Pipeline pipeline = Pipeline.create();
            pipeline.readFrom(source)
                .withIngestionTimestamps()
                .peek()
                .writeTo(Sinks.map("customers", e -> e.key().toString(), e -> e.value().toString()));
                
        return pipeline;
    }

}
