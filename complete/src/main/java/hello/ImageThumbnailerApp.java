package hello;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import reactor.Environment;
import reactor.io.net.Spec.TcpServerSpec;
import reactor.io.net.config.ServerSocketOptions;
import reactor.io.net.impl.netty.NettyServerSocketOptions;
import reactor.io.net.impl.netty.tcp.NettyTcpServer;
import reactor.rx.Stream;
import reactor.spring.context.config.EnableReactor;

/**
 * Simple Spring Boot app to start a Reactor+Netty-based REST API server for thumbnailing uploaded images.
 */
@EnableAutoConfiguration
@Configuration
@ComponentScan
@EnableReactor
public class ImageThumbnailerApp {

	@Bean
	public Reactor reactor(Environment env) {
		Reactor reactor = Reactors.reactor(env, Environment.THREAD_POOL);

		// Register our thumbnailer on the Reactor
		reactor.receive($("thumbnail"), new BufferedImageThumbnailer(250));

		return reactor;
	}

	@Bean
	public ServerSocketOptions serverSocketOptions() {
		return new NettyServerSocketOptions()
				.pipelineConfigurer(pipeline -> pipeline.addLast(new HttpServerCodec())
				                                        .addLast(new HttpObjectAggregator(16 * 1024 * 1024)));
	}

	@Bean
	public NetServer<FullHttpRequest, FullHttpResponse> restApi(Environment env,
	                                                            ServerSocketOptions opts,
	                                                            Reactor reactor,
	                                                            CountDownLatch closeLatch) throws InterruptedException {
		AtomicReference<Path> thumbnail = new AtomicReference<>();

		NetServer<FullHttpRequest, FullHttpResponse> server = new TcpServerSpec<FullHttpRequest, FullHttpResponse>(
				NettyTcpServer.class)
				.env(env).dispatcher("sync").options(opts)
				.consume(ch -> {
					// filter requests by URI via the input Stream
					Stream<FullHttpRequest> in = ch.in();

					// serve image thumbnail to browser
					in.filter((FullHttpRequest req) -> ImageThumbnailerRestApi.IMG_THUMBNAIL_URI.equals(req.getUri()))
					  .when(Throwable.class, ImageThumbnailerRestApi.errorHandler(ch))
					  .consume(ImageThumbnailerRestApi.serveThumbnailImage(ch, thumbnail));

					// take uploaded data and thumbnail it
					in.filter((FullHttpRequest req) -> ImageThumbnailerRestApi.THUMBNAIL_REQ_URI.equals(req.getUri()))
					  .when(Throwable.class, ImageThumbnailerRestApi.errorHandler(ch))
					  .consume(ImageThumbnailerRestApi.thumbnailImage(ch, thumbnail, reactor));

					// shutdown this demo app
					in.filter((FullHttpRequest req) -> "/shutdown".equals(req.getUri()))
					  .consume(req -> closeLatch.countDown());
				})
				.get();

		server.start().await();

		return server;
	}

	@Bean
	public CountDownLatch closeLatch() {
		return new CountDownLatch(1);
	}

	public static void main(String... args) throws InterruptedException {
		ApplicationContext ctx = SpringApplication.run(ImageThumbnailerApp.class, args);

		// Reactor's TCP servers are non-blocking so we have to do something to keep from exiting the main thread
		CountDownLatch closeLatch = ctx.getBean(CountDownLatch.class);
		closeLatch.await();
	}

}
