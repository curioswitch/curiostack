/*
 * MIT License
 *
 * Copyright (c) 2017 Choko (choko@curioswitch.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.curioswitch.common.server.framework.database;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import java.util.concurrent.Executors;
import javax.inject.Singleton;
import javax.sql.DataSource;
import org.curioswitch.common.server.framework.ApplicationModule;
import org.curioswitch.common.server.framework.config.DatabaseConfig;
import org.curioswitch.common.server.framework.config.ModifiableDatabaseConfig;
import org.curioswitch.common.server.framework.inject.EagerInit;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;

@Module(includes = ApplicationModule.class)
public abstract class DatabaseModule {

  @Provides
  @Singleton
  static DatabaseConfig dbConfig(Config config) {
    return ConfigBeanFactory.create(config.getConfig("database"), ModifiableDatabaseConfig.class)
        .toImmutable();
  }

  @Provides
  @ForDatabase
  @Singleton
  static ListeningExecutorService dbExecutor() {
    return MoreExecutors.listeningDecorator(
        Executors.newFixedThreadPool(
            20, new ThreadFactoryBuilder().setNameFormat("dbio-%d").setDaemon(true).build()));
  }

  @Provides
  @Singleton
  static DataSource dataSource(DatabaseConfig config) {
    HikariConfig hikari = new HikariConfig();
    hikari.setJdbcUrl(config.getJdbcUrl());
    hikari.setUsername(config.getUsername());
    hikari.setPassword(config.getPassword());
    hikari.addDataSourceProperty("cachePrepStmts", "true");
    hikari.addDataSourceProperty(
        "statementInterceptors", "brave.mysql.TracingStatementInterceptor");
    hikari.addDataSourceProperty("useUnicode", "yes");
    hikari.setMetricsTrackerFactory(new PrometheusMetricsTrackerFactory());
    return new HikariDataSource(hikari);
  }

  @Provides
  @Singleton
  static DSLContext dbContext(
      DataSource dataSource, @ForDatabase ListeningExecutorService dbExecutor) {
    Configuration configuration =
        new DefaultConfiguration()
            .set(dbExecutor)
            .set(SQLDialect.MYSQL)
            .set(new Settings().withRenderSchema(false))
            .set(new DataSourceConnectionProvider(dataSource))
            .set(DatabaseUtil.sfmRecordMapperProvider());
    DSLContext ctx = DSL.using(configuration);
    // Eagerly trigger JOOQ classinit for better startup performance.
    ctx.select().from("curio_server_framework_init").getSQL();
    return ctx;
  }

  @Provides
  @EagerInit
  @IntoSet
  static Object init(DSLContext dslContext) {
    return dslContext;
  }
}
