package cgc.artifactbox

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import java.sql.Connection

object SQLManager {
    var uuid = false
    fun transform(p: Player): String {
        return if (uuid) {
            p.uniqueId.toString()
        } else {
            p.name
        }
    }
    lateinit var pool: HikariDataSource
    var init = false
    fun init(db: ConfigurationSection) {
        if (init) {
            return
        }
        init = true
        uuid = db.getBoolean("uuid", false)
        val sb = StringBuilder(String.format("jdbc:mysql://%s:%d/%s?user=%s&password=%s",
                db.getString("host"),
                db.getInt("port"),
                db.getString("database"),
                db.getString("user"),
                db.getString("password")
        ))
        for (s in db.getStringList("options")) {
            sb.append('&')
            sb.append(s)
        }
        val config = HikariConfig()
        config.jdbcUrl = sb.toString()
        config.addDataSourceProperty("cachePrepStmts", "true")
        config.addDataSourceProperty("prepStmtCacheSize", "250")
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        config.idleTimeout = 60000
        config.connectionTimeout = 60000
        config.validationTimeout = 3000
        config.maxLifetime = 60000
        pool = HikariDataSource(config)
        checkTable()
    }

    private fun checkTable() {
        withSQL(false){
            val sta = createStatement()
            sta.execute("CREATE TABLE IF NOT EXISTS ARTIFACTBOX(\n    `NAME` VARCHAR(80) NOT NULL PRIMARY KEY,\n    `DATA` TEXT\n) ENGINE = InnoDB DEFAULT CHARSET=utf8mb4")
        }
    }
    fun withSQL(async: Boolean = true, func: Connection.() -> Unit) {
        val task = {
            val conn = pool.connection
            try {
                conn.func()
            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {
                conn.close()
            }
        }

        if (async) {
            Bukkit.getScheduler().runTaskAsynchronously(ArtifactBox.Plugin, task)
        } else {
            task()
        }
    }
}
inline fun async(crossinline f: () -> Unit) {
    Bukkit.getScheduler().runTaskAsynchronously(ArtifactBox.Plugin) {
        f()
    }
}

inline fun sync(crossinline f: () -> Unit) {
    Bukkit.getScheduler().runTask(ArtifactBox.Plugin) {
        f()
    }
}