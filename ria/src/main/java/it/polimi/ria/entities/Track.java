package it.polimi.ria.entities;


/**
 * Classe per track
 */
public record Track(
        int id,
        int user_id,
        String title,
        String artist,
        Integer year,
        String album_title,
        String genre,
        String image_path,
        String song_path,
        String song_checksum,
        String image_checksum,
        int custom_ordering
) {
    /**
     * Track costruttore senza custom_ordering per html
     *
     * @param id track id
     * @param user_id user id
     * @param title track title
     * @param artist track artist
     * @param year track year
     * @param album_title track album_title
     * @param genre track genre
     * @param image_path track image_path
     * @param song_path track song_path
     * @param song_checksum track song_checksum
     * @param image_checksum track image_checksum
     */
    public Track(
            int id,
            int user_id,
            String title,
            String artist,
            Integer year,
            String album_title,
            String genre,
            String image_path,
            String song_path,
            String song_checksum,
            String image_checksum
    ) {
        this(id,user_id, title, artist, year, album_title, genre, image_path, song_path, song_checksum, image_checksum, 0);
    }
}

