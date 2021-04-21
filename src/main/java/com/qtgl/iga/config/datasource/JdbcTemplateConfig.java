package com.qtgl.iga.config.datasource;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

@Configuration
public class JdbcTemplateConfig {

    @Bean
    JdbcTemplate jdbcIGA(@Qualifier("dsIGA") DataSource dsIGA) {
        return new JdbcTemplate(dsIGA);
    }

    @Bean
    JdbcTemplate jdbcSSO(@Qualifier("dsSSO") DataSource dsSSO) {
        return new JdbcTemplate(dsSSO);
    }

    @Bean
    JdbcTemplate jdbcSSOAPI(@Qualifier("dsSSOAPI") DataSource dsSSOAPI) {
        return new JdbcTemplate(dsSSOAPI);
    }

    /**
     * 装配事务管理器
     */
    @Bean(name = "transactionManager")
    public DataSourceTransactionManager transactionManager(@Qualifier("dsSSOAPI") DataSource dsSSOAPI) {
        return new DataSourceTransactionManager(dsSSOAPI);
    }

    /**
     * JDBC事务操作配置
     */
    @Bean(name = "txTemplate")
    public TransactionTemplate transactionTemplate(@Qualifier("transactionManager") DataSourceTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    /**
     * 装配事务管理器
     */
    @Bean(name = "transactionManager2")
    @Primary
    public DataSourceTransactionManager transactionManager2(@Qualifier("dsSSO") DataSource dsSSO) {
        return new DataSourceTransactionManager(dsSSO);
    }

    /**
     * JDBC事务操作配置
     */
    @Bean(name = "txTemplate2")
    public TransactionTemplate transactionTemplate2(@Qualifier("transactionManager2") DataSourceTransactionManager transactionManager2) {
        return new TransactionTemplate(transactionManager2);
    }


}
