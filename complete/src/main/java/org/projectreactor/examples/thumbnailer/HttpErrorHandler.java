package org.projectreactor.examples.thumbnailer;

import io.netty.handler.codec.http.*;
import reactor.function.Consumer;
import reactor.net.NetChannel;

/**
 * @author Jon Brisbin
 */
class HttpErrorHandler implements Consumer<Throwable> {

	private final NetChannel<FullHttpRequest, FullHttpResponse> channel;

	public HttpErrorHandler(NetChannel<FullHttpRequest, FullHttpResponse> channel) {
		this.channel = channel;
	}

	@Override
	public void accept(Throwable throwable) {
		DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
		                                                           HttpResponseStatus.INTERNAL_SERVER_ERROR);
		resp.content().writeBytes(throwable.getMessage().getBytes());
		resp.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain");
		resp.headers().set(HttpHeaders.Names.CONTENT_LENGTH, resp.content().readableBytes());
		channel.send(resp);
	}

}
