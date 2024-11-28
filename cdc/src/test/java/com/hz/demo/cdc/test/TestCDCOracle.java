package com.hz.demo.cdc.test;



import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.After;
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

@RunWith(HazelcastSerialClassRunner.class)
public class TestCDCOracle extends JetTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(TestCDCOracle.class);
    protected int testNumberModifier = 1000;

    @Test
    public void testMySQLCDC() throws IOException {
        Config config = smallInstanceConfig();
        //read license key from file at  ~/hazelcast/demo.license
        String path = System.getProperty("user.home") + "/hazelcast/v7expiring.license";
        String key = Files.readString(Paths.get(path));
        config.setLicenseKey(key);
        HazelcastInstance[] instances = createHazelcastInstances(config, 1);
        Containers db = new Containers("mysql");
        CDCOracle cdc = new CDCOracle(db.getCDCSource());
        Job job = cdc.deployJob(instances[0]);

        assertThat(job).eventuallyHasStatus(RUNNING);
        LOG.info("Job started");
        performSetOfChanges(db.mysqlContainer);
    }

    protected void performSetOfChanges(Container<?> container) {
        if (container instanceof JdbcDatabaseContainer<?> jdbcContainer) {
            executeSql(jdbcContainer.getJdbcUrl(), jdbcContainer.getUsername(), jdbcContainer.getPassword(),
                    ("INSERT INTO %1$s.customers (id, first_name, last_name, email) "
                            + "VALUES (1005, 'Jason%2$s', 'Bourne', 'jason@bourne.org')")
                            .formatted(tablePrefix(), testNumberModifier),
                    "DELETE FROM %1$s.customers WHERE id=1005".formatted(tablePrefix())
            );
            executeSql(jdbcContainer.getJdbcUrl(), jdbcContainer.getUsername(), jdbcContainer.getPassword(),
                    "UPDATE %1$s.customers SET first_name='Anne Marie%2$s' WHERE id=1004"
                            .formatted(tablePrefix(), testNumberModifier)
            );
            LOG.info("Performed a set of queries");
        } else {
            throw new RuntimeException("Please override methods in AbstractCdcIntegrationTest");
        }
    }

    protected String tablePrefix() {
        return "inventory";
    }

    public static void executeSql(String jdbcUrl, String username, String password,
            String... s) {
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

    @After
    public void after() {
        Hazelcast.shutdownAll();
    }

}
