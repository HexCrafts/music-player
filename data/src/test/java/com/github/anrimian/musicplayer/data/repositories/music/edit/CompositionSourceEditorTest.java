package com.github.anrimian.musicplayer.data.repositories.music.edit;

import com.github.anrimian.musicplayer.data.utils.files.ResourceFile;

import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class CompositionSourceEditorTest {

    @Rule
    public ResourceFile res = new ResourceFile("/VersuS - Warriors (Instrumental Kizomba).mp3");

    private CompositionSourceEditor sourceEditor = new CompositionSourceEditor();

    @Test
    public void testEditor() throws IOException {
        String filePath = res.getFile().getPath();
        System.out.println("title: " + sourceEditor.getCompositionTitle(filePath).blockingGet());
        System.out.println("author: " + sourceEditor.getCompositionAuthor(filePath).blockingGet());
        System.out.println("album: " + sourceEditor.getCompositionAlbum(filePath).blockingGet());
        System.out.println("genre: " + sourceEditor.getCompositionGenre(filePath).blockingGet());
    }

    @Test
    public void changeTitleTest() throws IOException {
        String filePath = res.getFile().getPath();
        System.out.println("title: " + sourceEditor.getCompositionTitle(filePath).blockingGet());

        String testTitle = "Test title";
        sourceEditor.setCompositionTitle(filePath, testTitle).subscribe();
        String newTitle = sourceEditor.getCompositionTitle(filePath).blockingGet();
        System.out.println("new title: " + sourceEditor.getCompositionTitle(filePath).blockingGet());
        assertEquals(testTitle, newTitle);
    }

    @Test
    public void changeAlbumTest() throws IOException {
        String filePath = res.getFile().getPath();
        System.out.println("album: " + sourceEditor.getCompositionAlbum(filePath).blockingGet());

        String testAlbum = "Test album";
        sourceEditor.setCompositionAlbum(filePath, testAlbum).subscribe();
        String newTitle = sourceEditor.getCompositionAlbum(filePath).blockingGet();
        System.out.println("new album: " + sourceEditor.getCompositionAlbum(filePath).blockingGet());
        assertEquals(testAlbum, newTitle);
    }

    @Test
    public void setGenreTest() throws IOException {
        String filePath = res.getFile().getPath();
        System.out.println("genre: " + sourceEditor.getCompositionGenre(filePath).blockingGet());

        String testGenre = "Test genre";
        sourceEditor.setCompositionGenre(filePath, testGenre).subscribe();
        String newGenre = sourceEditor.getCompositionGenre(filePath).blockingGet();
        System.out.println("new genre: " + sourceEditor.getCompositionGenre(filePath).blockingGet());
        assertEquals(testGenre, newGenre);
    }

    @Test
    public void addGenreTest() throws IOException {
        String filePath = res.getFile().getPath();
        String genres = sourceEditor.getCompositionGenre(filePath).blockingGet();
        System.out.println("genres: " + genres);

        String testGenre1 = "Test genre1";
        String testGenre2 = "Test genre2";
        sourceEditor.addCompositionGenre(filePath, testGenre1).subscribe();
        sourceEditor.addCompositionGenre(filePath, testGenre2).subscribe();
        String newGenres = sourceEditor.getCompositionGenre(filePath).blockingGet();
        System.out.println("new genres: " + newGenres);
        assertEquals(testGenre1 + "; " + testGenre2, newGenres);
    }

    @Test
    public void removeGenreTest() throws IOException {
        String filePath = res.getFile().getPath();
        String genres = sourceEditor.getCompositionGenre(filePath).blockingGet();
        System.out.println("genres: " + genres);

        String testGenre1 = "Test genre1";
        String testGenre2 = "Test genre2";
        sourceEditor.addCompositionGenre(filePath, testGenre1).subscribe();
        sourceEditor.addCompositionGenre(filePath, testGenre2).subscribe();
        sourceEditor.removeCompositionGenre(filePath, testGenre1).subscribe();
        String newGenres = sourceEditor.getCompositionGenre(filePath).blockingGet();
        System.out.println("new genres: " + newGenres);
        assertEquals(testGenre2, newGenres);
    }

    @Test
    public void changeGenreTest() throws IOException {
        String filePath = res.getFile().getPath();
        String genres = sourceEditor.getCompositionGenre(filePath).blockingGet();
        System.out.println("genres: " + genres);

        String testGenre1 = "Test genre1";
        String testGenre2 = "Test genre2";
        String testGenre3 = "Test genre3";
        sourceEditor.addCompositionGenre(filePath, testGenre1).subscribe();
        sourceEditor.addCompositionGenre(filePath, testGenre2).subscribe();
        sourceEditor.changeCompositionGenre(filePath, testGenre1, testGenre3).subscribe();
        String newGenres = sourceEditor.getCompositionGenre(filePath).blockingGet();
        System.out.println("new genres: " + newGenres);
        assertEquals(testGenre3 + "; " + testGenre2, newGenres);
    }

    @Test
    public void changeAlbumArtistTest() throws IOException {
        String filePath = res.getFile().getPath();
        System.out.println("album artist: " + sourceEditor.getCompositionAlbumArtist(filePath).blockingGet());

        String testName = "Test album artist";
        sourceEditor.setCompositionAlbumArtist(filePath, testName).subscribe();
        String newGenre = sourceEditor.getCompositionAlbumArtist(filePath).blockingGet();
        System.out.println("new album artist: " + sourceEditor.getCompositionAlbumArtist(filePath).blockingGet());
        assertEquals(testName, newGenre);
    }
}