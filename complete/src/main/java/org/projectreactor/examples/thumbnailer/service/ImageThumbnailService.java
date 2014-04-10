package org.projectreactor.examples.thumbnailer.service;

import java.nio.file.Path;

/**
 * The service responsible for implementing the thumbnail functionality.
 */
public interface ImageThumbnailService {

	/**
	 * Thumbnail an image by reading in the data, performing the resize, then returning a {@link Path} to the thumbnailed
	 * image.
	 *
	 * @param src
	 * 		source path to the image to thumbnail
	 * @param maxLongSide
	 * 		the max length of the long side of the image
	 *
	 * @return path to the thumbnailed image
	 * @throws Exception
	 */
	Path thumbnailImage(Path src, int maxLongSide) throws Exception;

}
