package hello;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;
import reactor.core.Reactor;
import reactor.event.Event;
import reactor.function.Consumer;
import reactor.net.NetChannel;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author Jon Brisbin
 */
public class ImageThumbnailerRestApi {

	public static final String IMG_THUMBNAIL_URI = "/image/thumbnail.jpg";
	public static final String THUMBNAIL_REQ_URI = "/thumbnail";
	public static final String SHUTDOWN_REQ_URI  = "/shutdown";

	public static Consumer<Throwable> errorHandler(NetChannel<FullHttpRequest, FullHttpResponse> channel) {
		return ev -> {
			DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
			                                                           HttpResponseStatus.INTERNAL_SERVER_ERROR);
			resp.content().writeBytes(ev.getMessage().getBytes());
			resp.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain");
			resp.headers().set(HttpHeaders.Names.CONTENT_LENGTH, resp.content().readableBytes());
			channel.send(resp);
		};
	}

	public static Consumer<FullHttpRequest> serveThumbnailImage(NetChannel<FullHttpRequest, FullHttpResponse> channel,
	                                                            AtomicReference<Path> thumbnail) {
		return req -> {
			if (req.getMethod() != HttpMethod.GET) {
				channel.send(badRequest(req.getMethod() + " not supported for this URI"));
			} else {
				try {
					channel.send(serveImage(thumbnail.get()));
				} catch (IOException e) {
					throw new IllegalStateException(e.getMessage(), e);
				}
			}
		};
	}

	public static Consumer<FullHttpRequest> thumbnailImage(NetChannel<FullHttpRequest, FullHttpResponse> channel,
	                                                       AtomicReference<Path> thumbnail,
	                                                       Reactor reactor) {
		return req -> {
			if (req.getMethod() != HttpMethod.POST) {
				channel.send(badRequest(req.getMethod() + " not supported for this URI"));
				return;
			}

			// write to a temp file
			Path imgIn = null;
			try {
				imgIn = readUpload(req.content());
			} catch (IOException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}

			// Asynchronously thumbnail the image to 250px on the long side
			reactor.sendAndReceive("thumbnail", Event.wrap(imgIn), ev -> {
				thumbnail.set(ev.getData());
				channel.send(redirect());
			});
		};
	}

	private static Path readUpload(ByteBuf content) throws IOException {
		byte[] bytes = new byte[content.readableBytes()];
		content.readBytes(bytes);
		content.release();

		// write to a temp file
		Path imgIn = Files.createTempFile("upload", ".jpg");
		Files.write(imgIn, bytes);

		imgIn.toFile().deleteOnExit();

		return imgIn;
	}

	public static FullHttpResponse ok() {
		DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HTTP_1_1, OK);
		resp.headers().set("Content-Length", 0);
		return resp;
	}

	public static FullHttpResponse badRequest(String msg) {
		DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST);
		resp.content().writeBytes(msg.getBytes());
		resp.headers().set(CONTENT_TYPE, "text/plain");
		resp.headers().set(CONTENT_LENGTH, resp.content().readableBytes());
		return resp;
	}

	public static FullHttpResponse redirect() {
		DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HTTP_1_1, MOVED_PERMANENTLY);
		resp.headers().set(CONTENT_LENGTH, 0);
		resp.headers().set(LOCATION, IMG_THUMBNAIL_URI);
		return resp;
	}

	public static FullHttpResponse serveImage(Path path) throws IOException {
		DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HTTP_1_1, OK);

		RandomAccessFile f = new RandomAccessFile(path.toString(), "r");
		resp.headers().set(CONTENT_TYPE, "image/jpeg");
		resp.headers().set(CONTENT_LENGTH, f.length());

		byte[] bytes = Files.readAllBytes(path);
		resp.content().writeBytes(bytes);

		return resp;
	}

}
