package it.polimi.pure_html.view;

import it.polimi.pure_html.entities.Track;

import java.util.Objects;

/**
 * Simple view model that exposes the information needed by the All Tracks
 * Thymeleaf template. It converts raw {@link Track} entities into values that
 * already contain context-aware URLs ready to be consumed by the front-end.
 */
public class TrackView {

    private final int trackId;
    private final String title;
    private final String artist;
    private final String albumTitle;
    private final String songUrl;
    private final String imageUrl;
    private final String durationFormatted;

    private TrackView(int trackId,
                      String title,
                      String artist,
                      String albumTitle,
                      String songUrl,
                      String imageUrl,
                      String durationFormatted) {
        this.trackId = trackId;
        this.title = title;
        this.artist = artist;
        this.albumTitle = albumTitle;
        this.songUrl = songUrl;
        this.imageUrl = imageUrl;
        this.durationFormatted = durationFormatted;
    }

    public static TrackView fromTrack(Track track, String contextPath) {
        Objects.requireNonNull(track, "track");
        String basePath = contextPath == null ? "" : contextPath;
        return new TrackView(
                track.id(),
                track.title(),
                track.artist(),
                track.album_title(),
                buildUrl(basePath, track.song_path()),
                buildUrl(basePath, track.image_path()),
                null
        );
    }

    private static String buildUrl(String contextPath, String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return relativePath;
        }
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return relativePath;
        }
        if (contextPath == null || contextPath.isBlank()) {
            return relativePath;
        }
        if (relativePath.startsWith("/")) {
            return contextPath + relativePath;
        }
        return contextPath + '/' + relativePath;
    }

    public int getTrackId() {
        return trackId;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbumTitle() {
        return albumTitle;
    }

    public String getSongUrl() {
        return songUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getDurationFormatted() {
        return durationFormatted;
    }
}

