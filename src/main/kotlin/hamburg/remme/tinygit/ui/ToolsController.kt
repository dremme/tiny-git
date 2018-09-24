package hamburg.remme.tinygit.ui

import hamburg.remme.tinygit.Context
import hamburg.remme.tinygit.Settings
import javafx.scene.control.ToolBar
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Controller

/**
 * Controller handling the [ToolBar].
 */
@Controller class ToolsController(publisher: ApplicationEventPublisher,
                                  settings: Settings,
                                  context: Context) : ActionController(publisher, settings, context)
