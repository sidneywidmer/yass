package ch.yass

import com.typesafe.config.Config
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeAll
import org.junit.platform.suite.api.SelectPackages
import org.junit.platform.suite.api.Suite
import org.kodein.di.direct
import org.kodein.di.instance

@Suite
@SelectPackages("ch.yass")
class YassSuite {

    companion object {
        @JvmStatic
        @BeforeAll
        fun bootstrap() {
            println("Initializing application logic...")
            val config: Config = Yass.container.direct.instance()

            Flyway.configure()
                .dataSource(
                    config.getString("db.url"),
                    config.getString("db.username"),
                    config.getString("db.password")
                )
                .locations("filesystem:server/main/resources/db/migration")
                .baselineOnMigrate(true)
                .load()
                .migrate()
        }
    }

}
