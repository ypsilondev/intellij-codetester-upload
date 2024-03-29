package com.github.yniklas.intellijcodetesterupload.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "com.github.yniklas.intellijcodetesterupload.services.CodeTesterSettings",
    storages = [Storage("codetester.xml")]
)
class CodeTesterSetting : PersistentStateComponent<CodeTesterSetting> {

    var uniProject = true
    var saveBeforeTesting = true

    companion object {
        fun getInstance(project: Project): CodeTesterSetting {
            return ServiceManager.getService(project, CodeTesterSetting::class.java)
        }
    }

    /**
     * @return a component state. All properties, public and annotated fields are serialized. Only values, which differ
     * from the default (i.e., the value of newly instantiated class) are serialized. `null` value indicates
     * that the returned state won't be stored, as a result previously stored state will be used.
     * @see com.intellij.util.xmlb.XmlSerializer
     */
    override fun getState(): CodeTesterSetting {
        return this
    }

    /**
     * This method is called when new component state is loaded. The method can and will be called several times, if
     * config files were externally changed while IDE was running.
     *
     *
     * State object should be used directly, defensive copying is not required.
     *
     * @param state loaded component state
     * @see com.intellij.util.xmlb.XmlSerializerUtil.copyBean
     */
    override fun loadState(state: CodeTesterSetting) {
        XmlSerializerUtil.copyBean(state, this)
    }

}