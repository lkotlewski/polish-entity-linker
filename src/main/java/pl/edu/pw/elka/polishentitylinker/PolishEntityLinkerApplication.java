package pl.edu.pw.elka.polishentitylinker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import pl.edu.pw.elka.polishentitylinker.tools.ItemsParser;

@SpringBootApplication
public class PolishEntityLinkerApplication implements CommandLineRunner {

    @Value("${datasource.wiki-items.filepath}")
    String wikiItemsFilepath;

    @Autowired
    ItemsParser itemsParser;

    public static void main(String[] args) {
        SpringApplication.run(PolishEntityLinkerApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        itemsParser.parseFile(wikiItemsFilepath);
    }
}
