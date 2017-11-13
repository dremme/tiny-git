package hamburg.remme.tinygit.gui

import javafx.beans.binding.BooleanBinding
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Node

class Action(val text: String,
             val icon: () -> Node? = { null },
             val shortcut: String? = null,
             val disable: BooleanBinding? = null,
             val action: EventHandler<ActionEvent>)
