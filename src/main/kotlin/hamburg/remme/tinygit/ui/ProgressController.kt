package hamburg.remme.tinygit.ui

import hamburg.remme.tinygit.event.HideProgressEvent
import hamburg.remme.tinygit.event.ShowProgressEvent
import hamburg.remme.tinygit.logger
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.ReadOnlyBooleanWrapper
import javafx.fxml.FXML
import javafx.scene.shape.Circle
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Controller
import java.util.ArrayDeque

@Controller class ProgressController {

    private val visibleProperty = ReadOnlyBooleanWrapper(false)
    fun visibleProperty(): ReadOnlyBooleanProperty = visibleProperty.readOnlyProperty
    /**
     * `true` when there is a show progress event in the stack.
     */
    val isVisible: Boolean get() = visibleProperty.value

    private val log = logger<ProgressController>()
    private val stack = ArrayDeque<ShowProgressEvent>()

    @FXML private lateinit var circle1: Circle
    @FXML private lateinit var circle2: Circle
    @FXML private lateinit var circle3: Circle
    private val animation1 by lazy { scaleAnimation(circle1) }
    private val animation2 by lazy { scaleAnimation(circle2, delay = 0.2) }
    private val animation3 by lazy { scaleAnimation(circle3, delay = 0.4) }

    /**
     * Handles a show progress overlay.
     * @param event the event.
     */
    @EventListener fun handleShowProgress(event: ShowProgressEvent) {
        stack.push(event)
        visibleProperty.value = true
        animation1.playFromStart()
        animation2.playFromStart()
        animation3.playFromStart()
    }

    /**
     * Handles a hide progress overlay.
     * @param event the event.
     */
    @EventListener fun handleHideProgress(event: HideProgressEvent) {
        stack.pop() ?: log.warn("Show and hide progress events are not balanced.")
        if (stack.isEmpty()) {
            visibleProperty.value = false
            animation1.stop()
            animation2.stop()
            animation3.stop()
        }
    }

}
