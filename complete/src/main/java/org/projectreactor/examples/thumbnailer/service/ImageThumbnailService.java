package org.projectreactor.examples.thumbnailer.service;

import java.nio.file.Path;

/**
 * The service responsible for implementing the thumbnail functionality.
 *
 * @author Jon Brisbin
 */
public interface ImageThumbnailService {

	Path thumbnailImage(Path src, int maxLongSide) throws Exception;

}
