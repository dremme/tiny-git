package hamburg.remme.tinygit.gui.dialog

import hamburg.remme.tinygit.I18N
import hamburg.remme.tinygit.domain.Credentials
import hamburg.remme.tinygit.gui.builder.addStylesheet
import hamburg.remme.tinygit.gui.builder.password
import hamburg.remme.tinygit.gui.builder.textField
import hamburg.remme.tinygit.gui.builder.vbox
import javafx.application.Platform
import javafx.stage.Window

class CredentialsDialog(host: String, window: Window) : Dialog<Credentials>(window, I18N["dialog.credentials.title"]) {

    init {
        val username = textField {
            promptText = I18N["dialog.credentials.user"]
            Platform.runLater { requestFocus() }
        }
        val password = password { promptText = I18N["dialog.credentials.password"] }

        header = I18N["dialog.credentials.header", host]
        content = vbox {
            addStylesheet("input-dialog.css") // TODO: replace
            spacing = 6.0
            +username
            +password
        }

        +DialogButton(DialogButton.OK, username.textProperty().isEmpty.or(password.textProperty().isEmpty))
        +DialogButton(DialogButton.CANCEL)

        okAction = { Credentials(username.text, password.text) }
    }

}
