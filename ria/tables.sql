USE tiw2025;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS user CASCADE;
DROP TABLE IF EXISTS track CASCADE;
DROP TABLE IF EXISTS playlist CASCADE;
DROP TABLE IF EXISTS playlist_tracks CASCADE;

SET FOREIGN_KEY_CHECKS = 1;

# TABLE CREATION

CREATE TABLE user
(
    user_id  integer     not null auto_increment,
    nickname varchar(32) not null unique,
    password varchar(64) not null,
    name     varchar(32) not null,
    surname  varchar(32) not null,

    primary key (user_id)
);

CREATE TABLE track
(
    track_id       integer       not null auto_increment,
    user_id        integer       not null,
    title          varchar(128)  not null,
    album_title    varchar(128)  not null,
    artist         varchar(64)   not null,
    year           year          not null,
    genre          varchar(64),
    song_checksum  char(64)      not null default '0000000000000000000000000000000000000000000000000000000000000000' comment "SHA256 checksum",
    image_checksum char(64)      not null default '0000000000000000000000000000000000000000000000000000000000000000' comment "SHA256 checksum",
    song_path      varchar(1024) not null,
    image_path     varchar(1024) not null,

    primary key (track_id),
    foreign key (user_id) references user (user_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    unique (user_id, song_checksum),
    unique (user_id, title, artist),
    #unique vuole dire che non possono esserci due canzoni con lo stesso checksum per lo stesso utente
    check (genre in ('Classical', 'Rock', 'Edm', 'Pop', 'Hip-hop', 'R&B', 'Country', 'Jazz', 'Blues', 'Metal', 'Folk', 'Soul', 'Funk', 'Electronic', 'Indie', 'Reggae', 'Disco'))
);

CREATE TABLE playlist
(
    playlist_id    integer     not null auto_increment,
    playlist_title varchar(32) not null,
    creation_date  datetime    not null default NOW(),
    user_id        integer     not null,

    primary key (playlist_id),
    unique (playlist_title, user_id),
    foreign key (user_id) references user (user_id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE TABLE playlist_tracks
(
    playlist_track_id integer auto_increment,
    playlist_id       integer not null,
    track_id          integer not null,
    custom_order      integer,

    primary key (playlist_track_id),
    unique (playlist_id, track_id),
    foreign key (playlist_id) references playlist (playlist_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    foreign key (track_id) references track (track_id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

# DATA LOADING FROM CSVs
# user -> track -> user_tracks -> playlist  -> playlist_tracks

LOAD DATA LOCAL INFILE 'mock_data/user.csv'
    INTO TABLE user
    FIELDS TERMINATED BY ','
    ENCLOSED BY ","
    LINES TERMINATED BY '\n'
    IGNORE 1 LINES
    (user_id, nickname, password, name, surname)
;
#
# LOAD DATA LOCAL INFILE 'mock_data/track.csv'
#     INTO TABLE track
#     FIELDS TERMINATED BY ','
#     ENCLOSED BY ","
#     LINES TERMINATED BY '\n'
#     IGNORE 1 LINES
#     (track_id, user_id, title, album_title, artist, year, genre, song_checksum, image_checksum, song_path, image_path)
# ;
#
# LOAD DATA LOCAL INFILE 'mock_data/playlist.csv'
#     INTO TABLE playlist
#     FIELDS TERMINATED BY ','
#     ENCLOSED BY ","
#     LINES TERMINATED BY '\n'
#     IGNORE 1 LINES
#     (playlist_id, playlist_title, @creation_date, user_id)
#     SET creation_date = STR_TO_DATE(@creation_date, '%Y-%m-%d')
# ;
#
# LOAD DATA LOCAL INFILE 'mock_data/playlist_tracks.csv'
#     INTO TABLE playlist_tracks
#     FIELDS TERMINATED BY ','
#     ENCLOSED BY ","
#     LINES TERMINATED BY '\n'
#     IGNORE 1 LINES
#     (playlist_id, track_id, custom_order)
# ;
