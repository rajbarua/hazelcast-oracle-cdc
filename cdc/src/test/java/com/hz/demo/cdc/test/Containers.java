package com.hz.demo.cdc.test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import org.testcontainers.utility.DockerImageName;

import com.hazelcast.enterprise.jet.cdc.ChangeRecord;
import com.hazelcast.enterprise.jet.cdc.mysql.MySqlCdcSources;
import com.hazelcast.jet.TestedVersions;
import com.hazelcast.jet.impl.util.ReflectionUtils;
import com.hazelcast.jet.pipeline.StreamSource;

public class Containers {
    private static final String TEST_KAFKA_VERSION = System.getProperty("test.kafka.version", "7.7.1");
    String db = "oracle";
    MySQLContainer<?> mysqlContainer = null;
    KafkaContainer kafkaContainer;
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
            try (Connection conn = DriverManager.getConnection(mysqlContainer.getJdbcUrl(), mysqlContainer.getUsername(),
                    mysqlContainer.getPassword());
                    Statement stmt = conn.createStatement()) {
                stmt.execute("""
                        CREATE TABLE LQUA (
                            MANDT VARCHAR(9) NOT NULL,
                            LGNUM VARCHAR(9) NOT NULL,
                            LQNUM VARCHAR(30) NOT NULL,
                            MATNR VARCHAR(54) NOT NULL,
                            WERKS VARCHAR(12) NOT NULL,
                            CHARG VARCHAR(30) NOT NULL,
                            BESTQ VARCHAR(3) NOT NULL,
                            SOBKZ VARCHAR(3) NOT NULL,
                            SONUM VARCHAR(48) NOT NULL,
                            LGTYP VARCHAR(9) NOT NULL,
                            LGPLA VARCHAR(30) NOT NULL,
                            PLPOS VARCHAR(6) NOT NULL,
                            SKZUE VARCHAR(3) NOT NULL,
                            SKZUA VARCHAR(3) NOT NULL,
                            SKZSE VARCHAR(3) NOT NULL,
                            SKZSA VARCHAR(3) NOT NULL,
                            SKZSI VARCHAR(3) NOT NULL,
                            SPGRU VARCHAR(3) NOT NULL,
                            ZEUGN VARCHAR(30) NOT NULL,
                            BDATU VARCHAR(24) NOT NULL,
                            BZEIT VARCHAR(18) NOT NULL,
                            BTANR VARCHAR(30) NOT NULL,
                            BTAPS VARCHAR(12) NOT NULL,
                            EDATU VARCHAR(24) NOT NULL,
                            EZEIT VARCHAR(18) NOT NULL,
                            ADATU VARCHAR(24) NOT NULL,
                            AZEIT VARCHAR(18) NOT NULL,
                            ZDATU VARCHAR(24) NOT NULL,
                            WDATU VARCHAR(24) NOT NULL,
                            WENUM VARCHAR(30) NOT NULL,
                            WEPOS VARCHAR(12) NOT NULL,
                            LETYP VARCHAR(9) NOT NULL,
                            MEINS VARCHAR(9) NOT NULL,
                            GESME DECIMAL(13,3) NOT NULL,
                            VERME DECIMAL(13,3) NOT NULL,
                            EINME DECIMAL(13,3) NOT NULL,
                            AUSME DECIMAL(13,3) NOT NULL,
                            MGEWI DECIMAL(11,3) NOT NULL,
                            GEWEI VARCHAR(9) NOT NULL,
                            PRIMARY KEY (MANDT, LGNUM, LQNUM)
                            );
                        """);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        //start kafka
        try {
			createKafkaCluster0();
		} catch (IOException e) {
            throw new RuntimeException(e);		
        }
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
        return MySqlCdcSources.mysql("payments")
                .setDatabaseAddress(mysqlContainer.getHost(), mysqlContainer.getMappedPort(MySQLContainer.MYSQL_PORT))
                .setDatabaseCredentials("debezium", "dbz")
                .setTableIncludeList("inventory.payments")
                .build();
    }

    @SuppressWarnings("deprecation")
	protected String createKafkaCluster0() throws IOException {
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
                .withCommand("-c",
                        "while [ ! -f %1$s ]; do sleep 0.1; done; sleep 0.5; %1$s"
                                .formatted(kafkaContainerStarterScript));
        kafkaContainer.start();

        return kafkaContainer.getBootstrapServers();
    }

}
