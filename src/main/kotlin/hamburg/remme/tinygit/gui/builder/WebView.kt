package hamburg.remme.tinygit.gui.builder

import javafx.scene.web.WebView

inline fun webView(block: WebView.() -> Unit): WebView {
    val webView = WebView()
    block.invoke(webView)
    return webView
}
