package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.domain.Credentials
import hamburg.remme.tinygit.gui.builder.addStylesheet
import hamburg.remme.tinygit.gui.builder.password
import hamburg.remme.tinygit.gui.builder.textField
import hamburg.remme.tinygit.gui.builder.vbox
import javafx.application.Platform
import javafx.stage.Window

class CredentialsDialog(host: String, window: Window) : Dialog<Credentials>(window, "Credentials") {

    init {
        val username = textField {
            promptText = "User"
            Platform.runLater { requestFocus() }
        }
        val password = password { promptText = "Password" }

        header = "Enter credentials for $host"
        content = vbox {
            addStylesheet("input-dialog.css")
            spacing = 6.0
            +username
            +password
        }

        +DialogButton(DialogButton.OK, username.textProperty().isEmpty.or(password.textProperty().isEmpty))
        +DialogButton(DialogButton.CANCEL)

        okAction = { Credentials(username.text, password.text) }
    }

}
