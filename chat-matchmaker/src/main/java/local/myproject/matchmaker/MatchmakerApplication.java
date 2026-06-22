package local.myproject.matchmaker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MatchmakerApplication {
    public static void main(String[] args) {
        SpringApplication.run(MatchmakerApplication.class, args);
    }
}
