package hamburg.remme.tinygit.gui

import javafx.beans.value.ObservableBooleanValue
import javafx.beans.value.ObservableIntegerValue
import javafx.scene.Node

class Action(val text: String,
             val icon: () -> Node? = { null },
             val shortcut: String? = null,
             val disable: ObservableBooleanValue? = null,
             val handler: () -> Unit,
             val count: ObservableIntegerValue? = null)
