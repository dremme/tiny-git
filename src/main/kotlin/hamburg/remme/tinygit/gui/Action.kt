package hamburg.remme.tinygit.gui

import javafx.beans.value.ObservableBooleanValue
import javafx.beans.value.ObservableIntegerValue
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Node

class Action(val text: String,
             val icon: () -> Node? = { null },
             val shortcut: String? = null,
             val disable: ObservableBooleanValue? = null,
             val action: EventHandler<ActionEvent>,
             val count: ObservableIntegerValue? = null)
