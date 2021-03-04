package com.qtgl.iga.config.datasource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

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
}
