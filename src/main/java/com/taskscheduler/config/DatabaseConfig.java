package com.taskscheduler.config;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import jakarta.persistence.EntityManagerFactory;


@Configuration
@EnableJpaRepositories(basePackages = "com.taskscheduler.repository")
@EnableTransactionManagement
public class DatabaseConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    
    @Value("${spring.datasource.url}")
    private String jdbcUrl;
    
    @Value("${spring.datasource.username}")
    private String username;
    
    @Value("${spring.datasource.password}")
    private String password;
    
    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;
    
    @Value("${spring.datasource.hikari.maximum-pool-size:20}")
    private int maximumPoolSize;
    
    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int minimumIdle;
    
    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;
    
    @Value("${spring.datasource.hikari.idle-timeout:600000}")
    private long idleTimeout;
    
    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;
    
    @Value("${spring.datasource.hikari.leak-detection-threshold:60000}")
    private long leakDetectionThreshold;
    
   
    @Bean
    @Primary
    public DataSource dataSource() {
        logger.info("Configuring HikariCP connection pool with max-pool-size: {}, min-idle: {}", 
                   maximumPoolSize, minimumIdle);
        
        HikariConfig config = new HikariConfig();
        
  
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        
        // Connection pool settings
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        config.setLeakDetectionThreshold(leakDetectionThreshold);
        
        // Connection validation and retry settings
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        config.setInitializationFailTimeout(30000);
        
        // Pool name for monitoring
        config.setPoolName("TaskSchedulerPool");
        
        // Connection failure handling
        config.addDataSourceProperty("socketTimeout", "30");
        config.addDataSourceProperty("loginTimeout", "30");
        config.addDataSourceProperty("connectTimeout", "30");
        
        
        config.addDataSourceProperty("retries", "3");
        config.addDataSourceProperty("retriesAllDown", "3");
        
       
        config.addDataSourceProperty("prepareThreshold", "3");
        config.addDataSourceProperty("preparedStatementCacheQueries", "256");
        config.addDataSourceProperty("preparedStatementCacheSizeMiB", "5");
        
        try {
            HikariDataSource dataSource = new HikariDataSource(config);
            logger.info("HikariCP connection pool configured successfully");
            return dataSource;
        } catch (Exception e) {
            logger.error("Failed to configure HikariCP connection pool", e);
            throw new RuntimeException("Database connection pool configuration failed", e);
        }
    }
    
    /**
     * Configures JPA transaction manager with appropriate isolation levels for task scheduling.
     * Uses READ_COMMITTED isolation to balance consistency and performance.
     */
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        logger.info("Configuring JPA transaction manager");
        
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        

        transactionManager.setDefaultTimeout(30);
        
       
        transactionManager.setRollbackOnCommitFailure(true);
        
        logger.info("JPA transaction manager configured successfully");
        return transactionManager;
    }
    
    /**
     * Validates database connection on startup and logs connection pool status.
     */
    @Bean
    public DatabaseHealthChecker databaseHealthChecker(DataSource dataSource) {
        return new DatabaseHealthChecker(dataSource);
    }
    
    /**
     * Database health checker that validates connection on startup and provides monitoring.
     */
    public static class DatabaseHealthChecker {
        private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthChecker.class);
        private final DataSource dataSource;
        
        public DatabaseHealthChecker(DataSource dataSource) {
            this.dataSource = dataSource;
            validateConnection();
        }
        
        private void validateConnection() {
            try {
                logger.info("Validating database connection...");
                dataSource.getConnection().close();
                logger.info("Database connection validated successfully");
                
                if (dataSource instanceof HikariDataSource) {
                    HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                    logger.info("Connection pool status - Active: {}, Idle: {}, Total: {}, Waiting: {}", 
                               hikariDataSource.getHikariPoolMXBean().getActiveConnections(),
                               hikariDataSource.getHikariPoolMXBean().getIdleConnections(),
                               hikariDataSource.getHikariPoolMXBean().getTotalConnections(),
                               hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
                }
            } catch (SQLException e) {
                logger.error("Database connection validation failed", e);
                throw new RuntimeException("Database connection validation failed", e);
            }
        }
        
        public boolean isHealthy() {
            try {
                dataSource.getConnection().close();
                return true;
            } catch (SQLException e) {
                logger.warn("Database health check failed", e);
                return false;
            }
        }
    }
}