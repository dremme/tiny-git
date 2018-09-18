package hamburg.remme.tinygit

import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * General application configuration.
 */
@EnableCaching
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
