package hamburg.remme.tinygit.gui.builder

import javafx.scene.image.ImageView

inline fun imageView(block: ImageView.() -> Unit): ImageView {
    val image = ImageView()
    block.invoke(image)
    return image
}
