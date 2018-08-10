package hamburg.remme.tinygit.ui

import hamburg.remme.tinygit.UTF8Support
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.ResourceBundle

/**
 * Configuration regarding all I18N related beans.
 */
@Configuration class LocalizationConfiguration {

    @Value("\${spring.messages.basename}") lateinit var basename: String

    /**
     * The default resource bundle for the application.
     */
    @Bean fun resourceBundle(): ResourceBundle {
        return ResourceBundle.getBundle(basename, UTF8Support())
    }

}
