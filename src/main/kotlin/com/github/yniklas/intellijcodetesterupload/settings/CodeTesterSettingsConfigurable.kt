package com.github.yniklas.intellijcodetesterupload.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class CodeTesterSettingsConfigurable(private val project: Project) : Configurable {

    private var component: CodeTesterSettingsComponent? = null

    /**
     * Creates new Swing form that enables user to configure the settings.
     * Usually this method is called on the EDT, so it should not take a long time.
     *
     *
     * Also this place is designed to allocate resources (subscriptions/listeners etc.)
     *
     * @return new Swing form to show, or `null` if it cannot be created
     * @see .disposeUIResources
     */
    override fun createComponent(): JComponent {
        component = CodeTesterSettingsComponent(project)
        return component!!.getPanel()
    }

    /**
     * Indicates whether the Swing form was modified or not.
     * This method is called very often, so it should not take a long time.
     *
     * @return `true` if the settings were modified, `false` otherwise
     */
    override fun isModified(): Boolean {
        val settings: CodeTesterSetting = CodeTesterSetting.getInstance(project)

        return settings.uniProject != component?.isUseSelected()
                || settings.saveBeforeTesting != component?.isSaveBeforeTestSelected()
    }

    /**
     * Stores the settings from the Swing form to the configurable component.
     * This method is called on EDT upon user's request.
     *
     * @throws ConfigurationException if values cannot be applied
     */
    override fun apply() {
        val settings: CodeTesterSetting = CodeTesterSetting.getInstance(project)
        settings.uniProject = component!!.isUseSelected()
        settings.saveBeforeTesting = component!!.isSaveBeforeTestSelected()
    }

    override fun reset() {
        val settings: CodeTesterSetting = CodeTesterSetting.getInstance(project)
        component!!.setUseSelected(settings.uniProject)
        component!!.setSaveBeforeTesting(settings.saveBeforeTesting)
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return component!!.getPreferredFocusedComponent()
    }

    override fun disposeUIResources() {
        component = null
    }

    /**
     * Returns the visible name of the configurable component.
     * Note, that this method must return the display name
     * that is equal to the display name declared in XML
     * to avoid unexpected errors.
     *
     * @return the visible name of the configurable component
     */
    override fun getDisplayName(): String {
        return "Code Tester Settings"
    }
}