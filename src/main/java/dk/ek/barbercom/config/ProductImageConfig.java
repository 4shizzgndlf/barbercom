package dk.ek.barbercom.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ProductImageConfig implements WebMvcConfigurer {
    private final String imageLocation;

    public ProductImageConfig(@Value("${barbercom.product-image-directory:uploads}") String imageDirectory) {
        String location = Path.of(imageDirectory).toAbsolutePath().normalize().toUri().toString();
        this.imageLocation = location.endsWith("/") ? location : location + "/";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**").addResourceLocations(imageLocation);
    }
}
