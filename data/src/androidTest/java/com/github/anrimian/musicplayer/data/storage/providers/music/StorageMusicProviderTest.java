package com.github.anrimian.musicplayer.data.storage.providers.music;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import android.Manifest;
import android.content.Context;
import android.util.Log;

import androidx.collection.LongSparseArray;
import androidx.test.rule.GrantPermissionRule;

import com.github.anrimian.musicplayer.data.storage.providers.albums.StorageAlbumsProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import hu.akarnokd.rxjava3.math.MathObservable;
import io.reactivex.rxjava3.core.Observable;

public class StorageMusicProviderTest {

    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);

    private StorageMusicProvider storageMusicProvider;

    @Before
    public void before() {
        Context appContext = getInstrumentation().getTargetContext();
        StorageAlbumsProvider storageAlbumsProvider = new StorageAlbumsProvider(appContext);
        storageMusicProvider = new StorageMusicProvider(appContext, storageAlbumsProvider);
    }

    @Test
    public void testRepositoryReturnValues() {
        LongSparseArray<StorageFullComposition> map = storageMusicProvider.getCompositions(0, false);
        if (map == null) {
            map = new LongSparseArray<>();
        }
        for(int i = 0, size = map.size(); i < size; i++) {
            StorageFullComposition composition = map.valueAt(i);
            System.out.println(composition);
            Assert.assertNotNull(composition.getRelativePath());
        }
    }

    @Test
    public void testLoadingPerformance() {
        Observable<Long> observable = Observable.range(0, 15)
                .map(o -> load())
                .doOnNext(time -> Log.d("TEST_TEST", "load: " + time + " ms"));

        MathObservable.averageDouble(observable)
                .subscribe((Double avg) -> Log.d("TEST_TEST", "average load: " + avg + " ms"));
    }

    private long load() {
        long startTime = System.currentTimeMillis();
        LongSparseArray<StorageFullComposition> map = storageMusicProvider.getCompositions( 0, false);
        if (map == null) {
            Log.d("KEK", "load failed");
        }
        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }
}