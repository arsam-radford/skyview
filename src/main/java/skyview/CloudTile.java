package skyview;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.time.Instant;

/** One geographic cloud-mask image, its observation time, and tile coordinates. */
public class CloudTile {
    private final Instant observationTime;
    private final URI imageUri;
    private final BufferedImage image;
    private final int column;
    private final int row;
    private final int zoom;

    /**
     * Creates a downloaded cloud tile.
     * @param observationTime source frame time in UTC
     * @param imageUri source URL pinned to this frame
     * @param image decoded cloud-mask PNG
     * @param column tile pyramid column
     * @param row tile pyramid row
     * @param zoom tile pyramid zoom level
     */
    public CloudTile(Instant observationTime, URI imageUri, BufferedImage image, int column, int row, int zoom) {
        this.observationTime = observationTime;
        this.imageUri = imageUri;
        this.image = image;
        this.column = column;
        this.row = row;
        this.zoom = zoom;
    }

    /** @return the source's UTC observation time */
    public Instant getObservationTime() {
        return observationTime;
    }

    /** @return the exact downloaded image URL */
    public URI getImageUri() {
        return imageUri;
    }

    /** @return the decoded PNG for use as a geographic texture */
    public BufferedImage getImage() {
        return image;
    }

    /** @return the tile pyramid column */
    public int getColumn() {
        return column;
    }

    /** @return the tile pyramid row */
    public int getRow() {
        return row;
    }

    /** @return the tile pyramid zoom level */
    public int getZoom() {
        return zoom;
    }
}
