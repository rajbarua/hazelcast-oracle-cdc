package com.hz.demo.cdc.test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Properties;

import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import org.testcontainers.utility.DockerImageName;

import com.hazelcast.config.DataConnectionConfig;
import com.hazelcast.enterprise.jet.cdc.ChangeRecord;
import com.hazelcast.enterprise.jet.cdc.mysql.MySqlCdcSources;
import com.hazelcast.jet.TestedVersions;
import com.hazelcast.jet.impl.util.ReflectionUtils;
import com.hazelcast.jet.kafka.KafkaDataConnection;
import com.hazelcast.jet.pipeline.StreamSource;
import com.hz.demo.cdc.job.CDCOracle;

public class Containers {
    private static final String TEST_KAFKA_VERSION = System.getProperty("test.kafka.version", "7.7.1");
    String db = "oracle";
    MySQLContainer<?> mysqlContainer = null;
    @SuppressWarnings("deprecation")
    KafkaContainer kafkaContainer;
    private String brokerConnectionString;
    private static final Logger LOGGER = LoggerFactory.getLogger(Containers.class);

    public Containers(String db) {
        if (db == null || db.isEmpty()) {
            throw new RuntimeException("Database type must be provided. Should be 'oracle' or 'mysql'");
        }
        this.db = db;
        if (db.equals("mysql")) {
            mysqlContainer = new MySQLContainer<>(TestedVersions.DEBEZIUM_MYSQL_IMAGE)
                    .withDatabaseName("mysql")
                    .withUsername("mysqluser")
                    .withPassword("mysqlpw")
                    .withExposedPorts(3306)
                    .withNetworkAliases("inputDatabase");
            mysqlContainer.start();
            try (Connection conn = DriverManager.getConnection(mysqlContainer.getJdbcUrl(),
                    mysqlContainer.getUsername(),
                    mysqlContainer.getPassword());
                    Statement stmt = conn.createStatement()) {
                stmt.execute("""
                        CREATE TABLE %1$s.LQUA (
                            MANDT VARCHAR(9) NOT NULL,
                            LGNUM VARCHAR(9) NOT NULL,
                            LQNUM VARCHAR(30) NOT NULL,
                            MATNR VARCHAR(54) ,
                            WERKS VARCHAR(12) ,
                            CHARG VARCHAR(30) ,
                            BESTQ VARCHAR(3) ,
                            SOBKZ VARCHAR(3) ,
                            SONUM VARCHAR(48) ,
                            LGTYP VARCHAR(9) ,
                            LGPLA VARCHAR(30) ,
                            PLPOS VARCHAR(6) ,
                            SKZUE VARCHAR(3) ,
                            SKZUA VARCHAR(3) ,
                            SKZSE VARCHAR(3) ,
                            SKZSA VARCHAR(3) ,
                            SKZSI VARCHAR(3) ,
                            SPGRU VARCHAR(3) ,
                            ZEUGN VARCHAR(30) ,
                            BDATU VARCHAR(24) ,
                            BZEIT VARCHAR(18) ,
                            BTANR VARCHAR(30) ,
                            BTAPS VARCHAR(12) ,
                            EDATU VARCHAR(24) ,
                            EZEIT VARCHAR(18) ,
                            ADATU VARCHAR(24) ,
                            AZEIT VARCHAR(18) ,
                            ZDATU VARCHAR(24) ,
                            WDATU VARCHAR(24) ,
                            WENUM VARCHAR(30) ,
                            WEPOS VARCHAR(12) ,
                            LETYP VARCHAR(9) ,
                            MEINS VARCHAR(9) ,
                            GESME DECIMAL(13,3) ,
                            VERME DECIMAL(13,3) ,
                            EINME DECIMAL(13,3) ,
                            AUSME DECIMAL(13,3) ,
                            MGEWI DECIMAL(11,3) ,
                            GEWEI VARCHAR(9) ,
                            TBNUM VARCHAR(30) ,
                            IVNUM VARCHAR(30) ,
                            IVPOS VARCHAR(12) ,
                            BETYP VARCHAR(3) ,
                            BENUM VARCHAR(30) ,
                            LENUM VARCHAR(60) ,
                            QPLOS VARCHAR(36) ,
                            VFDAT VARCHAR(24) ,
                            QKAPV DECIMAL(13,3) ,
                            KOBER VARCHAR(9) ,
                            LGORT VARCHAR(12) ,
                            VIRGO VARCHAR(3) ,
                            TRAME DECIMAL(13,3) ,
                            KZHUQ VARCHAR(3) ,
                            VBELN VARCHAR(30) ,
                            POSNR VARCHAR(18) ,
                            IDATU VARCHAR(24) ,
                            MSR_INSP_GUID VARCHAR(36) ,
                            SGT_SCAT VARCHAR(48) ,
                            SHAREPLEX_SOURCE_TIME VARCHAR(24) ,
                            SHAREPLEX_SOURCE_OPERATION VARCHAR(3) ,
                            SHAREPLEX_SOURCE_SCN VARCHAR(18) ,
                            SHAREPLEX_SOURCE_ROWID VARCHAR(36) ,
                            PRIMARY KEY (MANDT, LGNUM, LQNUM)
                            );
                        """.formatted(tablePrefix()));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            try (Connection conn = DriverManager.getConnection(mysqlContainer.getJdbcUrl(),
                    mysqlContainer.getUsername(),
                    mysqlContainer.getPassword());
                    Statement stmt = conn.createStatement()) {
                stmt.execute("""
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
                            """.formatted(tablePrefix()));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            try (Connection conn = DriverManager.getConnection(mysqlContainer.getJdbcUrl(),
                    mysqlContainer.getUsername(),
                    mysqlContainer.getPassword());
                    Statement stmt = conn.createStatement()) {
                    stmt.execute("""
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
                        """.formatted(tablePrefix()));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

        }
        // start kafka
        try {
            createKafkaCluster0();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String tablePrefix() {
        return "inventory";
    }

    public StreamSource<ChangeRecord> getCDCSource() {
        if (db.equals("oracle")) {
            return getOracleCDCSource();
        } else if (db.equals("mysql")) {
            return getMysqlCDCSource();
        } else {
            throw new RuntimeException("Unsupported database: " + db);
        }

    }

    private StreamSource<ChangeRecord> getOracleCDCSource() {
        throw new UnsupportedOperationException("Unimplemented method 'getOracleCDCSource'");
    }

    private StreamSource<ChangeRecord> getMysqlCDCSource() {
        return MySqlCdcSources.mysql("cdc_mysql")
                .setDatabaseAddress(mysqlContainer.getHost(), mysqlContainer.getMappedPort(MySQLContainer.MYSQL_PORT))
                .setDatabaseCredentials("debezium", "dbz")
                .setTableIncludeList(tablePrefix()+"LQUA")
                .build();
    }

    @SuppressWarnings("deprecation")
    protected void createKafkaCluster0() throws IOException {
        String kafkaContainerStarterScript = (String) Objects
                .requireNonNull(
                        ReflectionUtils.readStaticFieldOrNull(KafkaContainer.class.getName(), "STARTER_SCRIPT"));

        kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka").withTag(TEST_KAFKA_VERSION))
                // .withEmbeddedZookeeper()
                .withLogConsumer(new Slf4jLogConsumer(LOGGER))
        // Workaround for
        // https://github.com/testcontainers/testcontainers-java/issues/3288
        // It adds 0.5s sleep before running the script copied from the host to the
        // container.
        // .withCommand("-c",
        // "while [ ! -f %1 ]; do sleep 0.1; done; sleep 0.5; %1"
        // .formatted(kafkaContainerStarterScript))
        ;
        kafkaContainer.start();

        brokerConnectionString = kafkaContainer.getBootstrapServers();
    }

    protected void shutdownKafkaCluster0() {
        if (kafkaContainer != null) {
            kafkaContainer.stop();
            kafkaContainer = null;
        }
    }

    protected KafkaDataConnection createKafkaDataConnection() {
        DataConnectionConfig config = new DataConnectionConfig("kafka-data-connection")
                .setType("Kafka")
                .setShared(false)
                .setProperties(getRawKafkaProps());

        return new KafkaDataConnection(config);
    }

    public Properties getRawKafkaProps() {
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", brokerConnectionString);
        properties.setProperty("key.deserializer", JsonDeserializer.class.getCanonicalName());
        properties.setProperty("value.deserializer", JsonDeserializer.class.getCanonicalName());
        properties.setProperty("key.serializer", StringSerializer.class.getCanonicalName());
        properties.setProperty("value.serializer", StringSerializer.class.getCanonicalName());
        properties.setProperty("auto.offset.reset", "earliest");
        return properties;
    }

}
