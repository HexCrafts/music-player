package com.github.anrimian.musicplayer.ui.playlist_screens.playlists;

import com.github.anrimian.musicplayer.domain.interactors.playlists.PlayListsInteractor;
import com.github.anrimian.musicplayer.domain.models.playlist.PlayList;
import com.github.anrimian.musicplayer.ui.common.error.ErrorCommand;
import com.github.anrimian.musicplayer.ui.common.error.parser.ErrorParser;

import java.util.List;

import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import moxy.InjectViewState;
import moxy.MvpPresenter;

@InjectViewState
public class PlayListsPresenter extends MvpPresenter<PlayListsView> {

    private final PlayListsInteractor playListsInteractor;
    private final Scheduler uiScheduler;
    private final ErrorParser errorParser;

    private final CompositeDisposable presenterDisposable = new CompositeDisposable();

    private PlayList playListInMenu;
    private PlayList playListToDelete;

    public PlayListsPresenter(PlayListsInteractor playListsInteractor,
                              Scheduler uiScheduler,
                              ErrorParser errorParser) {
        this.playListsInteractor = playListsInteractor;
        this.uiScheduler = uiScheduler;
        this.errorParser = errorParser;
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        subscribeOnPlayLists();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenterDisposable.dispose();
    }

    void onPlayListLongClick(PlayList playList) {
        playListInMenu = playList;
        getViewState().showPlayListMenu(playList);
    }

    void onDeletePlayListButtonClicked() {
        playListToDelete = playListInMenu;
        getViewState().showConfirmDeletePlayListDialog(playListToDelete);
        playListInMenu = null;
    }

    void onDeletePlayListDialogConfirmed() {
        playListsInteractor.deletePlayList(playListToDelete.getId())
                .observeOn(uiScheduler)
                .subscribe(this::onPlayListDeleted, this::onPlayListDeletingError);
    }

    void onFragmentMovedToTop() {
        playListsInteractor.setSelectedPlayListScreen(0);
    }

    void onChangePlayListNameButtonClicked() {
        getViewState().showEditPlayListNameDialog(playListInMenu);
    }

    private void onPlayListDeletingError(Throwable throwable) {
        ErrorCommand errorCommand = errorParser.parseError(throwable);
        getViewState().showDeletePlayListError(errorCommand);
        playListToDelete = null;
    }

    private void onPlayListDeleted() {
        getViewState().showPlayListDeleteSuccess(playListToDelete);
        playListToDelete = null;
    }

    private void subscribeOnPlayLists() {
        getViewState().showLoading();
        presenterDisposable.add(playListsInteractor.getPlayListsObservable()
                .observeOn(uiScheduler)
                .subscribe(this::onPlayListsReceived));
    }

    private void onPlayListsReceived(List<PlayList> list) {
        getViewState().updateList(list);
        if (list.isEmpty()) {
            getViewState().showEmptyList();
        } else {
            getViewState().showList();
        }
    }
}
