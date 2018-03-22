package hamburg.remme.tinygit.gui.builder

import javafx.beans.binding.BooleanExpression
import javafx.beans.binding.IntegerExpression
import javafx.scene.Node

class Action(val text: String,
             val icon: (() -> Node)? = null,
             val shortcut: String? = null,
             val disabled: BooleanExpression? = null,
             val handler: () -> Unit,
             val count: IntegerExpression? = null)
