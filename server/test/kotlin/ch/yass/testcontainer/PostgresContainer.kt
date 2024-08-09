package ch.yass.testcontainer

import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait

object PostgresContainer {
    val instance by lazy { startContainer() }

    private fun startContainer(): PostgreSQLContainer<out PostgreSQLContainer<*>> {
        return PostgreSQLContainer("postgres:16-alpine").apply {
            setWaitStrategy(Wait.forListeningPort())
            start()

            Flyway.configure()
                .dataSource(
                    this.jdbcUrl,
                    this.username,
                    this.password
                )
                .locations("filesystem:server/main/resources/db/migration")
                .baselineOnMigrate(true)
                .load()
                .migrate()
        }
    }
}