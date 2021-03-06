package com.github.anrimian.musicplayer.data.storage.source;

import static com.github.anrimian.musicplayer.domain.utils.FileUtils.getFileName;

import android.os.Build;

import com.github.anrimian.musicplayer.data.models.composition.CompositionId;
import com.github.anrimian.musicplayer.data.storage.exceptions.TagReaderException;
import com.github.anrimian.musicplayer.data.storage.providers.music.StorageMusicProvider;
import com.github.anrimian.musicplayer.data.utils.file.FileUtils;
import com.github.anrimian.musicplayer.domain.models.composition.FullComposition;
import com.github.anrimian.musicplayer.domain.models.composition.source.CompositionSourceTags;
import com.github.anrimian.musicplayer.domain.models.exceptions.EditorReadException;
import com.github.anrimian.musicplayer.domain.models.image.ImageSource;
import com.github.anrimian.musicplayer.domain.utils.functions.ThrowsCallback;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.jaudiotagger.tag.id3.valuepair.ImageFormats;
import org.jaudiotagger.tag.images.AndroidArtwork;
import org.jaudiotagger.tag.images.Artwork;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.annotation.Nullable;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

public class CompositionSourceEditor {

    private static final char GENRE_DIVIDER = '\u0000';
    private static final int MAX_COVER_SIZE = 1000;

    private final StorageMusicProvider storageMusicProvider;
    private final FileSourceProvider fileSourceProvider;

    public CompositionSourceEditor(StorageMusicProvider storageMusicProvider,
                                   FileSourceProvider fileSourceProvider) {
        this.storageMusicProvider = storageMusicProvider;
        this.fileSourceProvider = fileSourceProvider;
    }

    public Completable setCompositionTitle(FullComposition composition, String title) {
        return getPath(composition)
                .flatMapCompletable(path -> setCompositionTitle(path, composition.getStorageId(), title));
    }

    public Completable setCompositionAuthor(FullComposition composition, String author) {
        return getPath(composition)
                .flatMapCompletable(path -> setCompositionAuthor(path, composition.getStorageId(), author));
    }

    public Completable setCompositionAuthor(CompositionId composition, String author) {
        return getPath(composition)
                .flatMapCompletable(path -> setCompositionAuthor(path, composition.getStorageId(), author));
    }

    public Completable setCompositionAlbum(FullComposition composition, String author) {
        return getPath(composition)
                .flatMapCompletable(path -> setCompositionAlbum(path, composition.getStorageId(), author));
    }

    public Completable setCompositionAlbum(CompositionId composition, String author) {
        return getPath(composition)
                .flatMapCompletable(path -> setCompositionAlbum(path, composition.getStorageId(), author));
    }

    public Single<List<CompositionId>> setCompositionsAlbum(List<CompositionId> compositions, String album) {
        return Observable.fromIterable(compositions)
                .flatMapCompletable(composition -> setCompositionAlbum(composition, album))
                .onErrorResumeNext(throwable -> storageMusicProvider.processStorageError(throwable, compositions))
                .toSingleDefault(compositions);
    }

    public Completable setCompositionAlbumArtist(FullComposition composition, String artist) {
        return getPath(composition)
                .flatMapCompletable(path -> setCompositionAlbumArtist(path, composition.getStorageId(), artist));
    }

    public Single<List<CompositionId>> setCompositionsAlbumArtist(List<CompositionId> compositions, String artist) {
        return Observable.fromIterable(compositions)
                .flatMapCompletable(composition -> setCompositionAlbumArtist(composition, artist))
                .onErrorResumeNext(throwable -> storageMusicProvider.processStorageError(throwable, compositions))
                .toSingleDefault(compositions);
    }

    public Completable setCompositionAlbumArtist(CompositionId composition, String artist) {
        return getPath(composition)
                .flatMapCompletable(path -> setCompositionAlbumArtist(path, composition.getStorageId(), artist));
    }

    public Completable setCompositionLyrics(FullComposition composition, String text) {
        return getPath(composition)
                .flatMapCompletable(path -> setCompositionLyrics(path, composition.getStorageId(), text));
    }

    public Completable changeCompositionGenre(FullComposition composition,
                                              String oldGenre,
                                              String newGenre) {
        return getPath(composition)
                .flatMapCompletable(path -> changeCompositionGenre(path, composition.getStorageId(), oldGenre, newGenre));
    }

    public Completable changeCompositionGenre(CompositionId composition,
                                              String oldGenre,
                                              String newGenre) {
        return getPath(composition)
                .flatMapCompletable(path -> changeCompositionGenre(path, composition.getStorageId(), oldGenre, newGenre));
    }

    public Completable addCompositionGenre(FullComposition composition,
                                           String newGenre) {
        return getPath(composition)
                .flatMapCompletable(path -> addCompositionGenre(path, composition.getStorageId(), newGenre));
    }

    public Completable removeCompositionGenre(FullComposition composition, String genre) {
        return getPath(composition)
                .flatMapCompletable(path -> removeCompositionGenre(path, composition.getStorageId(), genre));
    }

    public Single<String[]> getCompositionGenres(FullComposition composition) {
        return getPath(composition)
                .flatMap(this::getCompositionGenres);
    }

    public Single<Long> changeCompositionAlbumArt(FullComposition composition,
                                                  ImageSource imageSource) {
        return getPath(composition)
                .flatMap(path -> changeCompositionAlbumArt(path, composition.getStorageId(), imageSource));
    }

    public Single<Long> removeCompositionAlbumArt(FullComposition composition) {
        return getPath(composition)
                .flatMap(path -> removeCompositionAlbumArt(path, composition.getStorageId()));
    }

    public Single<CompositionSourceTags> getFullTags(FullComposition composition) {
        return getPath(composition)
                .flatMap(this::getFullTags);
    }

    public Maybe<byte[]> getCompositionArtworkBinaryData(long storageId) {
        return getPath(storageId)
                .flatMapMaybe(this::getArtworkBinaryData);
    }

    //genre not found case
    Completable changeCompositionGenre(String filePath,
                                       Long storageId,
                                       String oldGenre,
                                       String newGenre) {
        return Completable.fromAction(() -> {
            String genres = getFileTag(filePath).getFirst(FieldKey.GENRE);
            genres = genres.replace(oldGenre, newGenre);
            editFile(filePath, storageId, FieldKey.GENRE, genres);
        });
    }

    Completable setCompositionAlbumArtist(String filePath,
                                          Long storageId,
                                          String artist) {
        return Completable.fromAction(() -> editFile(filePath, storageId, FieldKey.ALBUM_ARTIST, artist));
    }

    Completable setCompositionAlbum(String filePath,
                                    Long storageId,
                                    String author) {
        return Completable.fromAction(() -> editFile(filePath, storageId, FieldKey.ALBUM, author));
    }


    Completable setCompositionTitle(String filePath,
                                    Long storageId,
                                    String title) {
        return Completable.fromAction(() -> editFile(filePath, storageId, FieldKey.TITLE, title));
    }

    Completable setCompositionLyrics(String filePath,
                                     Long storageId,
                                     String text) {
        return Completable.fromAction(() -> editFile(filePath, storageId, FieldKey.LYRICS, text));
    }

    Completable addCompositionGenre(String filePath,
                                    Long storageId,
                                    String newGenre) {
        return Completable.fromAction(() -> {
            AudioFile file = AudioFileIO.read(new File(filePath));
            Tag tag = file.getTag();
            if (tag == null) {
                tag = new ID3v24Tag();
                file.setTag(tag);
            }
            tag.addField(FieldKey.GENRE, newGenre);
            AudioFileIO.write(file);
//            String genres = getFileTag(filePath).getFirst(FieldKey.GENRE);
//            StringBuilder sb = new StringBuilder(genres);
//            if (sb.length() != 0) {
//                sb.append(GENRE_DIVIDER);
//            }
//            sb.append(newGenre);
//            sb.append(GENRE_DIVIDER);
//            editFile(filePath, FieldKey.GENRE, sb.toString());
        });
    }

    Completable removeCompositionGenre(String filePath,
                                       Long storageId,
                                       String genre) {
        return Completable.fromAction(() -> {
            String genres = getFileTag(filePath).getFirst(FieldKey.GENRE);
            int startIndex = genres.indexOf(genre);
            if (startIndex == -1) {
                return;
            }
            int endIndex = startIndex + genre.length();
            StringBuilder sb = new StringBuilder(genres);

            //clear divider at start
            if (startIndex == 1 && sb.charAt(0) == GENRE_DIVIDER) {
                startIndex = 0;
            }
            //clear divider at end or next if genre is at start or has divider before
            if ((endIndex == sb.length() - 2 || startIndex == 0 || sb.charAt(startIndex - 1) == GENRE_DIVIDER)
                    && (endIndex < sb.length() && sb.charAt(endIndex) == GENRE_DIVIDER)) {
                endIndex++;
            }

            sb.delete(startIndex, endIndex);

            editFile(filePath, storageId, FieldKey.GENRE, sb.toString());
        });
    }

    Maybe<String> getCompositionTitle(String filePath) {
        return Maybe.fromCallable(() -> getFileTag(filePath).getFirst(FieldKey.TITLE));
    }

    Maybe<String> getCompositionAuthor(String filePath) {
        return Maybe.fromCallable(() -> getFileTag(filePath).getFirst(FieldKey.ARTIST));
    }

    Maybe<String> getCompositionAlbum(String filePath) {
        return Maybe.fromCallable(() -> getFileTag(filePath).getFirst(FieldKey.ALBUM));
    }

    Maybe<String> getCompositionAlbumArtist(String filePath) {
        return Maybe.fromCallable(() -> getFileTag(filePath).getFirst(FieldKey.ALBUM_ARTIST));
    }

    Maybe<String> getCompositionGenre(String filePath) {
        return Maybe.fromCallable(() -> getFileTag(filePath).getFirst(FieldKey.GENRE));
    }

    Maybe<String> getCompositionLyrics(String filePath) {
        return Maybe.fromCallable(() -> getFileTag(filePath).getFirst(FieldKey.LYRICS));
    }

    private Completable setCompositionAuthor(String filePath, Long storageId, String author) {
        return Completable.fromAction(() -> editFile(filePath, storageId, FieldKey.ARTIST, author));
    }

    private Single<String[]> getCompositionGenres(String filePath) {
        return Single.fromCallable(() -> {
            String genres =  getFileTag(filePath).getFirst(FieldKey.GENRE);
            if (genres == null) {
                return new String[0];
            }
            return genres.split(String.valueOf(GENRE_DIVIDER));
        });
    }

    private Single<CompositionSourceTags> getFullTags(String filePath) {
        return Single.fromCallable(() -> {
            try {
                Tag tag = getFileTag(filePath);
                return new CompositionSourceTags(tag.getFirst(FieldKey.TITLE),
                        tag.getFirst(FieldKey.ARTIST),
                        tag.getFirst(FieldKey.ALBUM),
                        tag.getFirst(FieldKey.ALBUM_ARTIST),
                        tag.getFirst(FieldKey.LYRICS));
            } catch (Exception e) {
                throw new TagReaderException("Unable to read: " + filePath, e);
            }
        });
    }

    private Maybe<byte[]> getArtworkBinaryData(String filePath) {
        return Maybe.fromCallable(() -> {
            Tag tag = getFileTag(filePath);
            if (tag == null) {
                return null;
            }
            Artwork artwork = tag.getFirstArtwork();
            if (artwork == null) {
                return null;
            }
            return artwork.getBinaryData();
        });
    }

    private Single<String> getPath(CompositionId composition) {
        return getPath(composition.getStorageId());
    }

    private Single<String> getPath(FullComposition composition) {
        return getPath(composition.getStorageId());
    }

    private Single<String> getPath(@Nullable Long storageId) {
        return Single.fromCallable(() -> {
            if (storageId == null) {
                throw new RuntimeException("composition not found");
            }
            String path = storageMusicProvider.getCompositionFilePath(storageId);
            if (path == null) {
                throw new RuntimeException("composition path not found in system media store");
            }
            return path;
        });
    }

    private Tag getFileTag(String filePath) throws Exception {
        AudioFile file = readFile(new File(filePath));
        return file.getTagOrCreateDefault();
    }

    private Single<Long> changeCompositionAlbumArt(String filePath, Long id, ImageSource imageSource) {
        return Single.fromCallable(() -> editAudioFileTag(filePath,
                id,
                tag -> {
                    try (InputStream stream = fileSourceProvider.getImageStream(imageSource)) {
                        if (stream == null) {
                            return;
                        }
                        byte[] data = FileUtils.getScaledBitmapByteArray(stream, MAX_COVER_SIZE);
                        Artwork artwork = new AndroidArtwork();
                        artwork.setBinaryData(data);
                        artwork.setMimeType(ImageFormats.getMimeTypeForBinarySignature(data));
                        tag.deleteArtworkField();
                        tag.setField(artwork);
                    }
                }
        ));
    }

    private Single<Long> removeCompositionAlbumArt(String filePath, Long id) {
        return Single.fromCallable(() -> editAudioFileTag(filePath, id, Tag::deleteArtworkField));
    }

    private void editFile(String filePath,
                          long id,
                          FieldKey genericKey,
                          String value) throws Exception {
        editAudioFileTag(filePath,
                id,
                tag -> tag.setField(genericKey, value == null? "" : value)
        );
    }

    private long editAudioFileTag(String filePath, Long id, ThrowsCallback<Tag> callback)
            throws Exception {
        File fileToEdit = new File(filePath);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || !fileToEdit.canWrite()) {//will see how it works
            return fileSourceProvider.useTempFile(getFileName(filePath), tempFile -> {
                copyFileUsingStream(fileToEdit, tempFile);
                runFileAction(tempFile, callback);
                copyFileToMediaStore(tempFile, id);
            });
        } else {
            runFileAction(fileToEdit, callback);
            return fileToEdit.length();
        }
    }

    private void runFileAction(File file, ThrowsCallback<Tag> callback) throws Exception {
        AudioFile audioFile = readFile(file);
        Tag tag = audioFile.getTagOrCreateAndSetDefault();
        callback.call(tag);
        AudioFileIO.write(audioFile);
    }

    private AudioFile readFile(File file) throws Exception {
        try {
            return AudioFileIO.read(file);
        } catch (CannotReadException e) {
            throw new EditorReadException(e.getMessage());
        }
    }

    private void copyFileToMediaStore(File source, Long id) throws IOException {
        try (InputStream is = new FileInputStream(source);
             OutputStream os = storageMusicProvider.openCompositionOutputStream(id)
        ) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }

    private static void copyFileUsingStream(File source, File dest) throws IOException {
        try (InputStream is = new FileInputStream(source);
             OutputStream os = new FileOutputStream(dest)
        ) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }
}
