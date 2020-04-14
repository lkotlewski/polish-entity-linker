package pl.edu.pw.elka.polishentitylinker.processing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@Configuration
public class CategoriesProcessorConfig {

    @Value("#{'${categories.root}'.split(',')}")
    List<String> rootCategories;
}
