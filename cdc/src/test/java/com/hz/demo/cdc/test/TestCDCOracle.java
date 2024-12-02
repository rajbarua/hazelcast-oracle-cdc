package com.hz.demo.cdc.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.jet.Job;
import com.hazelcast.jet.core.JetTestSupport;
import com.hazelcast.test.HazelcastSerialClassRunner;
import com.hz.demo.cdc.job.CDCOracle;
import static com.hazelcast.jet.core.JobAssertions.assertThat;
import static com.hazelcast.jet.core.JobStatus.RUNNING;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@RunWith(HazelcastSerialClassRunner.class)
public class TestCDCOracle extends JetTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(TestCDCOracle.class);
    Containers containers;
    
    @Before
    public void before() {
        containers = new Containers("mysql");
    }
    @Test
    public void testMySQLCDC() throws Exception {
        Config config = smallInstanceConfig();
        String path = System.getProperty("user.home") + "/hazelcast/v7expiring.license";
        String key = Files.readString(Paths.get(path));
        config.setLicenseKey(key);
        HazelcastInstance[] instances = createHazelcastInstances(config, 1);
        CDCOracle cdc = new CDCOracle(containers.getCDCSource(), containers.getRawKafkaProps());
        Job job = cdc.deployJob(instances[0]);

        assertThat(job).eventuallyHasStatus(RUNNING);
        
        Thread.sleep(Duration.ofSeconds(3).toMillis());

        LOG.info("Job started");
        performSetOfChanges(containers.mysqlContainer);
        //validate the results
        Properties consumerProp = new Properties();
        consumerProp.setProperty("group.id", "test");
        consumerProp.setProperty("key.deserializer", StringDeserializer.class.getCanonicalName());
        consumerProp.setProperty("value.deserializer", StringDeserializer.class.getCanonicalName());
        Consumer<String, String> consumer = containers.createKafkaDataConnection().newConsumer(consumerProp);
        consumer.subscribe(Collections.singletonList(CDCOracle.TOPIC));
        await().atMost(Duration.ofSeconds(30)).until(() -> {
            return consumer.poll(Duration.ofMillis(100)).count() > 0;
        });
        consumer.close();
    }
    @After
    public void after() {
        Hazelcast.shutdownAll();
        if(containers != null) {
            containers.shutdownKafkaCluster0();
            containers.mysqlContainer.stop();
        }
    }


    protected void performSetOfChanges(Container<?> container) {
        if (container instanceof JdbcDatabaseContainer<?> jdbcContainer) {
            //insert a new row and delete the first one that was inserted
            executeSql(jdbcContainer.getJdbcUrl(), jdbcContainer.getUsername(), jdbcContainer.getPassword(),
                    ("""
                        Insert into %1$s.LQUA
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
                            """.formatted(containers.tablePrefix())),
                    "DELETE FROM %1$s.LQUA WHERE MANDT='930' AND LGNUM='PEP' AND LQNUM='0000299277'".formatted(containers.tablePrefix()));
            //update the second row that was inserted initially
            executeSql(jdbcContainer.getJdbcUrl(), jdbcContainer.getUsername(), jdbcContainer.getPassword(),
                    "UPDATE %1$s.LQUA SET MATNR='UPDATED_THIS' WHERE MANDT='930' AND LGNUM='PEP' AND LQNUM='0000299271'".formatted(containers.tablePrefix()));
            LOG.info("Performed a set of queries");
        } else {
            throw new RuntimeException("Please override methods in AbstractCdcIntegrationTest");
        }
    }

    public static void executeSql(String jdbcUrl, String username, String password, String... s) {
        try (var connection = DriverManager.getConnection(jdbcUrl, username, password);
             var statement = connection.createStatement()) {
            for (String sql : s) {
                //noinspection SqlSourceToSinkFlow
                statement.addBatch(sql);
                System.out.println("Executing SQL: " + sql);
            }
            statement.executeBatch();
        } catch (SQLException e) {
            for (Throwable throwable : e) {
                LOG.error("Exception while executing sql statement", throwable);
            }
            throw new RuntimeException(e);
        }
    }


}
