package com.thecookiezen.akka

import java.sql.PreparedStatement
import java.util.Properties
import javax.sql.DataSource

import akka.Done
import com.codahale.metrics.MetricRegistry
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

import scala.concurrent.Future

class PostgresqlExample(metricRegistry: MetricRegistry) extends DBRepository with Transactional {
  override val getDataSource: DataSource = getDataSource(metricRegistry)

  def insertData(name: String, hash: String): Future[Done] = {
    doInTx(createStatement => {
      using(createStatement(s"INSERT INTO (t.name, t.hash) VALUES (?,?)")) { preparedStatement =>
        preparedStatement.setString(1, name)
        preparedStatement.setString(2, hash)
        preparedStatement.executeUpdate()
        Future.successful(Done)
      }
    })
  }

  def fetchDataByHash(hash: String): Future[(Int, String)] = {
    doInTx(queryStatement => {
      using(queryStatement(s"SELECT t.id, t.name FROM test t WHERE t.hash=?")) { preparedStatement =>
        preparedStatement.setString(1, hash)
        val resultSet = preparedStatement.executeQuery()
        Future.successful(resultSet.getInt(1), resultSet.getString(2))
      }
    })
  }
}

trait Transactional extends Control {
  val getDataSource: DataSource

  def doInTx[T](sqlCommand: (String => PreparedStatement) => T): T = {
    using(getDataSource.getConnection) { connection =>
      val result = sqlCommand(connection.prepareStatement)
      connection.commit()
      result
    }
  }
}

trait Control {
  def using[Closeable <: {def close() : Unit}, B](closeable: Closeable)(closeableFunction: Closeable => B): B = {
    try {
      closeableFunction(closeable)
    } catch {
      case ex: Exception => throw new IllegalStateException(ex)
    } finally {
      if (closeable != null)
        closeable.close()
    }
  }
}

trait DBRepository {
  def getDataSource(metricRegistry: MetricRegistry): DataSource = {
    val config = new HikariConfig()

    config.setPoolName("postgresql_connection_pool")
    config.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource")
    config.setMaximumPoolSize(15)
    config.setMaxLifetime(600000)

    val properties = new Properties()
    properties.setProperty("url", "__CHANGEME__")
    properties.setProperty("user", "__CHANGEME__")
    properties.setProperty("password", "__CHANGEME__")
    properties.setProperty("prepStmtCacheSize", "250")
    properties.setProperty("prepStmtCacheSqlLimit", "2048")
    properties.setProperty("cachePrepStmts", "true")
    properties.setProperty("useServerPrepStmts", "true")

    config.setDataSourceProperties(properties)
    config.setMetricRegistry(metricRegistry)

    new HikariDataSource(config)
  }
}
