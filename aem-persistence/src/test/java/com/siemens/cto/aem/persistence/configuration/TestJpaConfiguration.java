package com.siemens.cto.aem.persistence.configuration;

import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.h2.Driver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.support.SharedEntityManagerBean;
import org.springframework.orm.jpa.vendor.OpenJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class TestJpaConfiguration {

    @Bean
    public DataSource getDataSource() {
        return new SimpleDriverDataSource(new Driver(),
                                          "jdbc:h2:mem:test-persistence;DB_CLOSE_DELAY=-1;LOCK_MODE=0",
                                          "sa",
                                          "");
    }

    @Bean
    public JpaVendorAdapter getJpaVendorAdapter() {
        return new OpenJpaVendorAdapter();
    }

    @Bean(name = "openJpaProperties")
    public Properties getJpaProperties() {
        final Properties properties = new Properties();
        properties.setProperty("openjpa.jdbc.DBDictionary", "org.apache.openjpa.jdbc.sql.H2Dictionary");
        properties.setProperty("openjpa.jdbc.SynchronizeMappings", "buildSchema(ForeignKeys=true)");
        return properties;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean getEntityManagerFactoryBean() {
        final LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();

        factory.setJpaVendorAdapter(getJpaVendorAdapter());
        factory.setPersistenceXmlLocation("classpath:META-INF/test-persistence.xml");
        factory.setDataSource(getDataSource());
        factory.setJpaProperties(getJpaProperties());

        return factory;
    }

    @Bean
    public EntityManagerFactory getEntityManagerFactory() {
        return getEntityManagerFactoryBean().getObject();
    }

    @Bean(name = "transactionManager")
    public PlatformTransactionManager getTransactionManager() {
        final PlatformTransactionManager manager = new JpaTransactionManager(getEntityManagerFactory());
        return manager;
    }

    @Bean
    public SharedEntityManagerBean getSharedEntityManager() {
        final SharedEntityManagerBean shared = new SharedEntityManagerBean();
        shared.setEntityManagerFactory(getEntityManagerFactory());
        return shared;
    }
}
