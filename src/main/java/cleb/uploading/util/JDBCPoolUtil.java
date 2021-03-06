package cleb.uploading.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.servlet.http.HttpServlet;

/**
 * This utility servlet class instantiates connection pool and provides methods
 * for working with connections.
 */
public class JDBCPoolUtil extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // Logger for this class
    private static final Logger logger = LogManager
        .getLogger(JDBCPoolUtil.class.getName());

    private String dbURL;
    private String dbuser;
    private String dbpass;

    private static DataSource dataSource;

    @Override
    public void init() {
        // Load properties
        Properties properties = new Properties();

        try (InputStream propIn = getServletContext()
            .getResourceAsStream("/WEB-INF/classes/props.properties")) {
            properties.load(propIn);
        } catch (IOException e) {
            logger.error("Can not load properties", e);
        }

        // Initialize database URL for driver
        dbURL = properties.getProperty("database");

        // Initialize database user
        dbuser = properties.getProperty("dbuser");

        // Initialize database user password
        dbpass = properties.getProperty("dbpass");

        // Set properties for pool
        PoolProperties poolProps = new PoolProperties();
        poolProps.setUrl(dbURL);
        poolProps.setDriverClassName("org.postgresql.Driver");
        poolProps.setUsername(dbuser);
        poolProps.setPassword(dbpass);
        poolProps.setJmxEnabled(true);
        poolProps.setTestWhileIdle(false);
        poolProps.setTestOnBorrow(true);
        poolProps.setValidationQuery("SELECT 1");
        poolProps.setTestOnReturn(false);
        poolProps.setValidationInterval(30000);
        poolProps.setTimeBetweenEvictionRunsMillis(30000);
        poolProps.setMaxActive(20);
        poolProps.setMaxIdle(10);
        poolProps.setMinIdle(5);
        poolProps.setInitialSize(5);
        poolProps.setMaxWait(10000);
        poolProps.setRemoveAbandonedTimeout(60);
        poolProps.setMinEvictableIdleTimeMillis(30000);
        poolProps.setLogAbandoned(true);
        poolProps.setRemoveAbandoned(true);
        poolProps.setJdbcInterceptors(
            "org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;"
                + "org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer;"
                + "org.apache.tomcat.jdbc.pool.interceptor.ResetAbandonedTimer");

        // Initialize pool
        dataSource = new DataSource(poolProps);
    }

    /**
     * Attempts to return free connection from connection pool.
     *
     * @return Free {@code Connection} from pool.
     */
    public static Connection getConnection() {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
        } catch (SQLException e) {
            logger.error("Can not get connection from pool", e);
        }

        return connection;
    }

    /**
     * Attempts to close connection. Classes that use connections from this
     * connection pool should call {@code getConnection} method inside
     * try-with-resources for efficient closing instead of manually calling this
     * method.
     *
     * @param connection {@code Connection} object to close.
     */
    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.error("Can not return connection to pool", e);
            }
        }
    }

}
