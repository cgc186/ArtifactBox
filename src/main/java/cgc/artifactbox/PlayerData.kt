package cgc.artifactbox

import cgc.artifactbox.data.PageInfo
import com.github.bryanser.brapi.util.KConfigurationSerializable
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.io.StringReader

class PlayerData() : KConfigurationSerializable {

    val pageInfos = hashMapOf<String, PageInfo>()

    lateinit var name: String

    private var _pageInfos: List<PageInfo>
        get() = pageInfos.values.toList()
        set(value) {
            pageInfos.clear()
            for (info in value) {
                pageInfos[info.name] = info
            }
        }

    constructor(name: String) : this() {
        for (page in Setting.pages.values) {
            pageInfos[page.name] = page.createPageInfo()
        }
        this.name = name
    }

    constructor(p: Player) : this() {
        for (page in Setting.pages.values) {
            pageInfos[page.name] = page.createPageInfo()
        }
        name = SQLManager.transform(p)
    }

    constructor(args: Map<String, Any?>) : this() {
        args.deserializeExist()
        val iter = pageInfos.iterator()
        while (iter.hasNext()) {
            val next = iter.next()
            val page = Setting.pages[next.key]
            if (page == null) {
                iter.remove()
                continue
            }
            page.updatePageInfo(next.value)
        }
    }


    @Volatile
    @Transient
    private var syncing = false
    fun syncData(force: Boolean = false) {
        if (force && syncing) {
            if (DEBUG) {
                Bukkit.getLogger().info("AC: syncData cancel for force but syncing")
            }
            Bukkit.getScheduler().runTaskAsynchronously(ArtifactBox.Plugin) {
                syncData(true)
            }
            return
        }
        if (syncing) {
            if (DEBUG) {
                Bukkit.getLogger().info("AC: syncData cancel for sycing")
            }
            return
        }
        Bukkit.getLogger().info("AC: syncData in")
        SQLManager.withSQL {
            try {
                val ps = prepareStatement("UPDATE ARTIFACTBOX SET `DATA` = ? WHERE `NAME` = ?")
                ps.setString(1, saveToString())
                ps.setString(2, name)
                val r = ps.executeUpdate()
                if (DEBUG) {
                    Bukkit.getLogger().info("AC: SQL update result: ${r > 0}")
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                syncing = false
            }
        }
        syncing = true
    }

    fun saveToString(): String {
        val config = YamlConfiguration()
        config["Data"] = this
        return config.saveToString()
    }

    companion object : Listener {
        const val DEBUG = false

        private val cache = hashMapOf<String, PlayerData>()
        private val loading = hashSetOf<String>()

        fun getCache(p: Player): PlayerData? {
            return cache[SQLManager.transform(p)]
        }


        @EventHandler
        fun onJoin(evt: PlayerJoinEvent) {
            val name = SQLManager.transform(evt.player)
            if (loading.add(name)) {
                evt.player.sendMessage("§6正在加载装备数据")
                Bukkit.getScheduler().runTaskLater(ArtifactBox.Plugin, {
                    if (loading.contains(name)) {
                        loadPlayerData(name){
                            evt.player.sendMessage("§6数据加载完成")
                            if(loading.remove(name)){
                                cache[name] = it
                            }
                        }
                    }
                }, 100)
            }
        }

        @EventHandler
        fun onQuit(evt:PlayerQuitEvent){
            val name = SQLManager.transform(evt.player)
            loading.remove(name)
            cache.remove(name)?.syncData(true)
        }

        fun loadPlayerData(name: String, load: (PlayerData) -> Unit) {
            SQLManager.withSQL {
                val get = prepareStatement("SELECT `DATA` FROM ARTIFACTBOX WHERE `NAME` = ?")
                get.setString(1, name)
                val getrs = get.executeQuery()
                if (getrs.next()) {
                    val data = getrs.getString(1)
                    if (DEBUG) {
                        Bukkit.getLogger().info("AC: SQL select data query success, result: $data")
                    }
                    val pd = loadFromString(data)
                    sync {
                        load(pd)
                    }
                    getrs.close()
                    get.close()
                    return@withSQL
                } else if (DEBUG) {
                    Bukkit.getLogger().info("AC: SQL select data query failed, result: not found")
                }
                getrs.close()
                get.close()
                val insert = prepareStatement("INSERT INTO ARTIFACTBOX VALUES (?, ?)")
                val pd = PlayerData(name)
                insert.setString(1, name)
                insert.setString(2, pd.saveToString())
                insert.executeUpdate()
                sync {
                    load(pd)
                }
                insert.close()
            }
        }

        fun loadFromString(str: String): PlayerData {
            val config = YamlConfiguration.loadConfiguration(StringReader(str))
            return config["Data"] as PlayerData
        }


    }
}