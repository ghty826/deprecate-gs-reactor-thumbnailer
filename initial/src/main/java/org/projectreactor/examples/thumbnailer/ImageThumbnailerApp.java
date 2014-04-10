package org.projectreactor.examples.thumbnailer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author Jon Brisbin
 */
@EnableAutoConfiguration
@Configuration
@ComponentScan
public class ImageThumbnailerApp {

	public static void main(String... args) throws InterruptedException {
		ApplicationContext ctx = SpringApplication.run(ImageThumbnailerApp.class, args);
	}

}
