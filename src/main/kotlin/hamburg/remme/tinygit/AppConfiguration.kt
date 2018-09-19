package hamburg.remme.tinygit

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * General application configuration.
 */
@Configuration class AppConfiguration {

    /**
     * @see Context
     */
    @Bean fun context(): Context = Context()

    /**
     * @see Settings
     */
    @Bean fun settings(): Settings = Settings()

}
