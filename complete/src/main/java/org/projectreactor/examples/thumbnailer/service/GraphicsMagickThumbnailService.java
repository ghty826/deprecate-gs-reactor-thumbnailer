package org.projectreactor.examples.thumbnailer.service;

import org.gm4java.engine.support.GMConnectionPoolConfig;
import org.gm4java.engine.support.PooledGMService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Uses the GraphicsMagick package to do the image resizing.
 *
 * @author Jon Brisbin
 */
public class GraphicsMagickThumbnailService implements ImageThumbnailService {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final PooledGMService gm;

	public GraphicsMagickThumbnailService() {
		GMConnectionPoolConfig config = new GMConnectionPoolConfig();
		this.gm = new PooledGMService(config);
	}

	@Override
	public Path thumbnailImage(Path src, int maxLongSide) throws Exception {
		Path thumbnailPath = Files.createTempFile("thumbnail", ".jpg").toAbsolutePath();
		BufferedImage imgIn = ImageIO.read(src.toFile());

		double scale;
		if (imgIn.getWidth() >= imgIn.getHeight()) {
			// horizontal or square image
			scale = Math.min(maxLongSide, imgIn.getWidth()) / (double) imgIn.getWidth();
		} else {
			// vertical image
			scale = Math.min(maxLongSide, imgIn.getHeight()) / (double) imgIn.getHeight();
		}

		gm.execute("convert",
		           src.toString(),
		           "-resize", Math.round(100 * scale) + "%",
		           thumbnailPath.toString());

		log.info("Image thumbnail now at: {}", thumbnailPath);

		return thumbnailPath;
	}

}
