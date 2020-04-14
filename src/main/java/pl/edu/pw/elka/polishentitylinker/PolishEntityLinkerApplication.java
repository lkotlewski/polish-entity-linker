package pl.edu.pw.elka.polishentitylinker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.cloud.openfeign.EnableFeignClients;
import pl.edu.pw.elka.polishentitylinker.flow.ProgramRunner;

@EnableFeignClients
@SpringBootApplication
public class PolishEntityLinkerApplication implements CommandLineRunner {

    @Autowired
    ProgramRunner programRunner;

    public static void main(String[] args) {
        SpringApplication.run(PolishEntityLinkerApplication.class, args);
    }

    @Override
    public void run(String... args) {
        programRunner.run();
    }
}
