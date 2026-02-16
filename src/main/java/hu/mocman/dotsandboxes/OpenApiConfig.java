package hu.mocman.dotsandboxes;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dotsAndBoxesOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dots and Boxes Tournament API")
                        .version("0.0.1")
                        .description("API for the Dots and Boxes tournament server. "
                                + "Bots register via /dab/hello, and the server orchestrates matches between them. "
                                + "Game replays are available as animated GIFs."));
    }
}
