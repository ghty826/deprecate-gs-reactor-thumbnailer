package org.projectreactor.examples.thumbnailer;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import org.projectreactor.examples.thumbnailer.service.ImageThumbnailService;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.Reactor;
import reactor.event.Event;
import reactor.spring.context.annotation.Consumer;
import reactor.spring.context.annotation.ReplyTo;
import reactor.spring.context.annotation.Selector;
import reactor.spring.context.annotation.SelectorType;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

/**
 * @author Jon Brisbin
 */
@Consumer
public class RestApiHandler {

	private final Reactor               reactor;
	private final CountDownLatch        closeLatch;
	private final ImageThumbnailService thumbnailer;

	private volatile Path thumbnail;

	@Autowired
	public RestApiHandler(Reactor reactor, CountDownLatch closeLatch, ImageThumbnailService thumbnailer) {
		this.reactor = reactor;
		this.closeLatch = closeLatch;
		this.thumbnailer = thumbnailer;
	}

	@Selector(value = "/img/thumbnail.jpg", type = SelectorType.URI)
	@ReplyTo
	public HttpResponse serveImage(Event<FullHttpRequest> ev) throws IOException {
		if (ev.getData().getMethod() != HttpMethod.GET) {
			return badRequest(ev.getData().getMethod() + " not supported for this URI");
		}
		return serveImage(thumbnail);
	}

	@Selector(value = "/thumbnail", type = SelectorType.URI)
	@ReplyTo
	public HttpResponse thumbnailImage(Event<FullHttpRequest> ev) throws Exception {
		if (ev.getData().getMethod() != HttpMethod.POST) {
			return badRequest(ev.getData().getMethod() + " not supported for this URI");
		}

		// write to a temp file
		Path imgIn = readUpload(ev.getData().content());

		// thumbnail the image to 250px on the long side
		thumbnail = thumbnailer.thumbnailImage(imgIn, 250);

		return redirect();
	}

	@Selector(value = "/shutdown", type = SelectorType.URI)
	@ReplyTo
	public HttpResponse shutdown() {
		closeLatch.countDown();
		return ok();
	}

	private static Path readUpload(ByteBuf content) throws IOException {
		byte[] bytes = new byte[content.readableBytes()];
		content.readBytes(bytes);
		content.release();

		// write to a temp file
		Path imgIn = Files.createTempFile("upload", ".jpg");
		Files.write(imgIn, bytes);

		return imgIn;
	}

	private static HttpResponse ok() {
		DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
		                                                           HttpResponseStatus.OK);
		resp.headers().set("Content-Length", 0);
		return resp;
	}

	private static HttpResponse badRequest(String msg) {
		DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
		                                                           HttpResponseStatus.BAD_REQUEST);
		resp.content().writeBytes(msg.getBytes());
		resp.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain");
		resp.headers().set(HttpHeaders.Names.CONTENT_LENGTH, resp.content().readableBytes());
		return resp;
	}

	private static HttpResponse redirect() {
		DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
		                                                           HttpResponseStatus.MOVED_PERMANENTLY);
		resp.headers().set(HttpHeaders.Names.CONTENT_LENGTH, 0);
		resp.headers().set(HttpHeaders.Names.LOCATION, "/img/thumbnail.jpg");
		return resp;
	}

	private static HttpResponse serveImage(Path path) throws IOException {
		DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
		                                                           HttpResponseStatus.OK);

		RandomAccessFile f = new RandomAccessFile(path.toString(), "r");
		resp.headers().set(HttpHeaders.Names.CONTENT_TYPE, "image/jpeg");
		resp.headers().set(HttpHeaders.Names.CONTENT_LENGTH, f.length());

		byte[] bytes = Files.readAllBytes(path);
		resp.content().writeBytes(bytes);

		return resp;
	}

}

