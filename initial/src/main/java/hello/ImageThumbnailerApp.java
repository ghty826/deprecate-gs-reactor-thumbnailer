package hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import reactor.spring.context.config.EnableReactor;

/**
 * Simple Spring Boot app to start a Reactor+Netty-based REST API server for thumbnailing uploaded images.
 */
@EnableAutoConfiguration
@Configuration
@ComponentScan
@EnableReactor
public class ImageThumbnailerApp {

	public static void main(String... args) throws InterruptedException {
		ApplicationContext ctx = SpringApplication.run(ImageThumbnailerApp.class, args);
	}

}
