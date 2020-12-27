package cgc.artifactbox

import cgc.artifactbox.data.Page
import com.github.bryanser.brapi.Utils
import java.io.File


object Setting {
    val pages = hashMapOf<String, Page>()

    fun loadConfig(){
        pages.clear()
        val folder = File(ArtifactBox.Plugin.dataFolder,"/pages/")
        if(!folder.exists()){
            Utils.saveResource(ArtifactBox.Plugin,"page_example.yml", folder)
        }
        loadConfig(folder)
    }

    private fun loadConfig(f:File){
        if(f.isDirectory){
            for(t in f.listFiles()){
                loadConfig(t)
            }
            return
        }
        val page = Page(f)
        pages[page.name] = page
    }
}