package com.github.anrimian.musicplayer.ui.player_screen;

import static android.view.View.VISIBLE;
import static androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED;
import static com.github.anrimian.musicplayer.Constants.Arguments.OPEN_PLAY_QUEUE_ARG;
import static com.github.anrimian.musicplayer.Constants.Tags.CREATE_PLAYLIST_TAG;
import static com.github.anrimian.musicplayer.Constants.Tags.SELECT_PLAYLIST_TAG;
import static com.github.anrimian.musicplayer.domain.models.utils.CompositionHelper.formatCompositionName;
import static com.github.anrimian.musicplayer.ui.common.format.FormatUtils.formatCompositionAuthor;
import static com.github.anrimian.musicplayer.ui.common.format.FormatUtils.formatCompositionsCount;
import static com.github.anrimian.musicplayer.ui.common.format.FormatUtils.formatMilliseconds;
import static com.github.anrimian.musicplayer.ui.common.format.FormatUtils.getRepeatModeIcon;
import static com.github.anrimian.musicplayer.ui.common.format.FormatUtils.getRepeatModeText;
import static com.github.anrimian.musicplayer.ui.common.format.MessagesUtils.getAddToPlayListCompleteMessage;
import static com.github.anrimian.musicplayer.ui.common.format.MessagesUtils.getDeleteCompleteMessage;
import static com.github.anrimian.musicplayer.ui.common.format.MessagesUtils.makeSnackbar;
import static com.github.anrimian.musicplayer.ui.common.view.ViewUtils.setOnHoldListener;
import static com.github.anrimian.musicplayer.ui.utils.AndroidUtils.clearVectorAnimationInfo;
import static com.github.anrimian.musicplayer.ui.utils.AndroidUtils.getColorFromAttr;
import static com.github.anrimian.musicplayer.ui.utils.ViewUtils.animateVisibility;
import static com.github.anrimian.musicplayer.ui.utils.views.menu.ActionMenuUtil.setupMenu;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable;
import androidx.appcompat.widget.ActionMenuView;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.anrimian.musicplayer.R;
import com.github.anrimian.musicplayer.databinding.FragmentDrawerBinding;
import com.github.anrimian.musicplayer.databinding.PartialDetailedMusicBinding;
import com.github.anrimian.musicplayer.databinding.PartialDrawerHeaderBinding;
import com.github.anrimian.musicplayer.di.Components;
import com.github.anrimian.musicplayer.domain.interactors.sleep_timer.SleepTimerInteractorKt;
import com.github.anrimian.musicplayer.domain.models.Screens;
import com.github.anrimian.musicplayer.domain.models.composition.Composition;
import com.github.anrimian.musicplayer.domain.models.play_queue.PlayQueueItem;
import com.github.anrimian.musicplayer.domain.models.player.PlayerState;
import com.github.anrimian.musicplayer.domain.models.player.modes.RepeatMode;
import com.github.anrimian.musicplayer.domain.models.playlist.PlayList;
import com.github.anrimian.musicplayer.domain.models.scanner.FileScannerState;
import com.github.anrimian.musicplayer.domain.models.scanner.Running;
import com.github.anrimian.musicplayer.ui.ScreensMap;
import com.github.anrimian.musicplayer.ui.about.AboutAppFragment;
import com.github.anrimian.musicplayer.ui.common.compat.CompatUtils;
import com.github.anrimian.musicplayer.ui.common.dialogs.DialogUtils;
import com.github.anrimian.musicplayer.ui.common.error.ErrorCommand;
import com.github.anrimian.musicplayer.ui.common.format.FormatUtils;
import com.github.anrimian.musicplayer.ui.common.format.MessagesUtils;
import com.github.anrimian.musicplayer.ui.common.menu.PopupMenuWindow;
import com.github.anrimian.musicplayer.ui.common.toolbar.AdvancedToolbar;
import com.github.anrimian.musicplayer.ui.editor.common.DeleteErrorHandler;
import com.github.anrimian.musicplayer.ui.editor.common.ErrorHandler;
import com.github.anrimian.musicplayer.ui.editor.composition.CompositionEditorActivityKt;
import com.github.anrimian.musicplayer.ui.equalizer.EqualizerDialogFragment;
import com.github.anrimian.musicplayer.ui.library.albums.list.AlbumsListFragment;
import com.github.anrimian.musicplayer.ui.library.artists.list.ArtistsListFragment;
import com.github.anrimian.musicplayer.ui.library.compositions.LibraryCompositionsFragment;
import com.github.anrimian.musicplayer.ui.library.folders.root.LibraryFoldersRootFragment;
import com.github.anrimian.musicplayer.ui.library.genres.list.GenresListFragment;
import com.github.anrimian.musicplayer.ui.player_screen.view.adapter.PlayQueueAdapter;
import com.github.anrimian.musicplayer.ui.player_screen.view.drawer.DrawerLockStateProcessor;
import com.github.anrimian.musicplayer.ui.player_screen.view.wrappers.PlayerPanelWrapper;
import com.github.anrimian.musicplayer.ui.player_screen.view.wrappers.PlayerPanelWrapperImpl;
import com.github.anrimian.musicplayer.ui.player_screen.view.wrappers.TabletPlayerPanelWrapper;
import com.github.anrimian.musicplayer.ui.playlist_screens.choose.ChoosePlayListDialogFragment;
import com.github.anrimian.musicplayer.ui.playlist_screens.create.CreatePlayListDialogFragment;
import com.github.anrimian.musicplayer.ui.playlist_screens.playlist.PlayListFragmentKt;
import com.github.anrimian.musicplayer.ui.playlist_screens.playlists.PlayListsFragment;
import com.github.anrimian.musicplayer.ui.settings.SettingsFragment;
import com.github.anrimian.musicplayer.ui.sleep_timer.SleepTimerDialogFragment;
import com.github.anrimian.musicplayer.ui.start.StartFragment;
import com.github.anrimian.musicplayer.ui.utils.AndroidUtils;
import com.github.anrimian.musicplayer.ui.utils.fragments.BackButtonListener;
import com.github.anrimian.musicplayer.ui.utils.fragments.navigation.FragmentNavigation;
import com.github.anrimian.musicplayer.ui.utils.fragments.navigation.JugglerView;
import com.github.anrimian.musicplayer.ui.utils.views.drawer.SimpleDrawerListener;
import com.github.anrimian.musicplayer.ui.utils.views.recycler_view.RecyclerViewUtils;
import com.github.anrimian.musicplayer.ui.utils.views.recycler_view.touch_helper.drag_and_swipe.DragAndSwipeTouchHelperCallback;
import com.github.anrimian.musicplayer.ui.utils.views.seek_bar.SeekBarViewWrapper;
import com.github.anrimian.musicplayer.utils.Permissions;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import moxy.MvpAppCompatFragment;
import moxy.presenter.InjectPresenter;
import moxy.presenter.ProvidePresenter;

/**
 * Created on 19.10.2017.
 */

public class PlayerFragment extends MvpAppCompatFragment implements BackButtonListener, PlayerView {

    private static final int NO_ITEM = -1;
    private static final String SELECTED_DRAWER_ITEM = "selected_drawer_item";

    @InjectPresenter
    PlayerPresenter presenter;

    private FragmentDrawerBinding viewBinding;
    private PartialDetailedMusicBinding panelBinding;
    private PartialDrawerHeaderBinding drawerHeaderBinding;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private RecyclerView rvPlayList;
    private ImageView ivPlayPause;
    private ImageView ivSkipToPrevious;
    private ImageView ivSkipToNext;
    private JugglerView fragmentContainer;
    private TextView tvCurrentComposition;
    private ImageView btnRepeatMode;
    private ImageView btnRandomPlay;
    private TextView tvPlayedTime;
    private TextView tvTotalTime;
    private SeekBar sbTrackState;
    private View bottomSheetTopShadow;
    private View topBottomSheetPanel;
    private ImageView ivMusicIcon;
    private ImageView btnActionsMenu;
    private TextView tvCurrentCompositionAuthor;
    private CoordinatorLayout clPlayQueueContainer;
    @Nullable
    private MotionLayout mlBottomSheet;
    private AdvancedToolbar toolbar;
    private ActionMenuView acvPlayQueueMenu;
    private TextView tvQueueSubtitle;

    private PlayQueueAdapter playQueueAdapter;

    private int selectedDrawerItemId = NO_ITEM;
    private int itemIdToStart = NO_ITEM;

    private final Handler secondScrollHandler = new Handler(Looper.getMainLooper());
    private int currentPosition = -2;//for immediate first scroll

    private LinearLayoutManager playQueueLayoutManager;
    private SeekBarViewWrapper seekBarViewWrapper;

    private DrawerLockStateProcessor drawerLockStateProcessor;
    private final CompositeDisposable viewDisposable = new CompositeDisposable();

    private FragmentNavigation navigation;

    private PlayerPanelWrapper playerPanelWrapper;

    @Nullable
    private Composition previousCoverComposition;

    private ErrorHandler deletingErrorHandler;

    public static PlayerFragment newInstance() {
        return newInstance(false);
    }

    public static PlayerFragment newInstance(boolean openPlayQueue) {
        Bundle args = new Bundle();
        args.putBoolean(OPEN_PLAY_QUEUE_ARG, openPlayQueue);
        PlayerFragment fragment = new PlayerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @ProvidePresenter
    PlayerPresenter providePresenter() {
        return Components.getLibraryComponent().playerPresenter();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        viewBinding = FragmentDrawerBinding.inflate(inflater, container, false);
        toolbar = viewBinding.toolbar.getRoot();
        fragmentContainer = viewBinding.drawerFragmentContainer;
        mlBottomSheet = viewBinding.getRoot().findViewById(R.id.ml_bottom_sheet);
        navigationView = viewBinding.navigationView;
        drawer = viewBinding.drawer;
        acvPlayQueueMenu = viewBinding.toolbarPlayQueue.acvPlayQueue;

        panelBinding = viewBinding.clMusicPanel;
        assert panelBinding != null;
        ivSkipToPrevious = panelBinding.ivSkipToPrevious;
        ivSkipToNext = panelBinding.ivSkipToNext;
        btnRepeatMode = panelBinding.btnInfinitePlay;
        rvPlayList = viewBinding.rvPlaylist;
        btnActionsMenu = panelBinding.btnActionsMenu;
        topBottomSheetPanel = panelBinding.topPanel;
        sbTrackState = panelBinding.sbTrackState;
        ivPlayPause = panelBinding.ivPlayPause;
        btnRandomPlay = panelBinding.btnRandomPlay;
        bottomSheetTopShadow = viewBinding.bottomSheetTopShadow;
        tvCurrentComposition = panelBinding.tvCurrentComposition;
        tvPlayedTime = panelBinding.tvPlayedTime;
        tvTotalTime = panelBinding.tvTotalTime;
        ivMusicIcon = panelBinding.ivMusicIcon;
        tvCurrentCompositionAuthor = panelBinding.tvCurrentCompositionAuthor;
        clPlayQueueContainer = viewBinding.clPlayQueueContainer;
        tvQueueSubtitle = viewBinding.toolbarPlayQueue.tvQueueSubtitle;
        return viewBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AndroidUtils.setNavigationBarColorAttr(requireActivity(), R.attr.playerPanelBackground);

        toolbar.initializeViews(requireActivity().getWindow());
        toolbar.setupWithActivity((AppCompatActivity) requireActivity());

        navigation = FragmentNavigation.from(getChildFragmentManager());
        navigation.initialize(fragmentContainer, savedInstanceState);
        navigation.checkForEqualityOnReplace(true);
        navigation.setExitAnimation(R.anim.anim_slide_out_right);
        navigation.setEnterAnimation(R.anim.anim_slide_in_right);
        navigation.setRootExitAnimation(R.anim.anim_alpha_disappear);

        drawerLockStateProcessor = new DrawerLockStateProcessor(viewBinding.drawer);
        drawerLockStateProcessor.setupWithNavigation(navigation);
        viewDisposable.add(toolbar.getSearchModeObservable()
                .subscribe(drawerLockStateProcessor::onSearchModeChanged)
        );
        viewDisposable.add(toolbar.getSelectionModeObservable()
                .subscribe(drawerLockStateProcessor::onSelectionModeChanged)
        );

        if (mlBottomSheet == null) {
            playerPanelWrapper = new TabletPlayerPanelWrapper(view,
                    drawerLockStateProcessor::onBottomSheetOpened);
        } else {
            playerPanelWrapper = new PlayerPanelWrapperImpl(view,
                    viewBinding,
                    panelBinding,
                    mlBottomSheet,
                    requireActivity(),
                    savedInstanceState,
                    presenter::onBottomPanelCollapsed,
                    presenter::onBottomPanelExpanded,
                    drawerLockStateProcessor::onBottomSheetOpened);
        }


        navigationView.setNavigationItemSelectedListener(this::onNavigationItemSelected);
        View headerView = navigationView.inflateHeaderView(R.layout.partial_drawer_header);
        drawerHeaderBinding = PartialDrawerHeaderBinding.bind(headerView);

        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(requireActivity(), drawer, R.string.open_drawer, R.string.close_drawer);
        DrawerArrowDrawable drawerArrowDrawable = createDrawerArrowDrawable();
        drawerToggle.setDrawerArrowDrawable(drawerArrowDrawable);

        drawer.addDrawerListener(new SimpleDrawerListener(this::onDrawerClosed));

        setupMenu(acvPlayQueueMenu, R.menu.play_queue_menu, this::onPlayQueueMenuItemClicked);

        toolbar.setupWithNavigation(navigation,
                drawerArrowDrawable,
                () -> playerPanelWrapper.isBottomPanelExpanded());

        ivSkipToPrevious.setOnClickListener(v -> presenter.onSkipToPreviousButtonClicked());
        setOnHoldListener(ivSkipToPrevious, presenter::onFastSeekBackwardCalled);
        ivSkipToNext.setOnClickListener(v -> presenter.onSkipToNextButtonClicked());
        setOnHoldListener(ivSkipToNext, presenter::onFastSeekForwardCalled);
        btnRepeatMode.setOnClickListener(this::onRepeatModeButtonClicked);

        playQueueLayoutManager = new LinearLayoutManager(requireContext());
        rvPlayList.setLayoutManager(playQueueLayoutManager);

        playQueueAdapter = new PlayQueueAdapter(rvPlayList);
        playQueueAdapter.setOnCompositionClickListener(presenter::onQueueItemClicked);
        playQueueAdapter.setMenuClickListener(this::onPlayItemMenuClicked);
        playQueueAdapter.setIconClickListener(presenter::onQueueItemIconClicked);
        rvPlayList.setAdapter(playQueueAdapter);

        DragAndSwipeTouchHelperCallback callback = FormatUtils.withSwipeToDelete(rvPlayList,
                getColorFromAttr(requireContext(), R.attr.listItemBottomBackground),
                presenter::onItemSwipedToDelete,
                ItemTouchHelper.START,
                R.drawable.ic_remove_from_queue,
                R.string.delete_from_queue);
        callback.setOnMovedListener(presenter::onItemMoved);
        callback.setOnStartDragListener(position -> presenter.onDragStarted());
        callback.setOnEndDragListener(position -> presenter.onDragEnded());
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(rvPlayList);


        if (savedInstanceState != null) {
            selectedDrawerItemId = savedInstanceState.getInt(SELECTED_DRAWER_ITEM, NO_ITEM);
        } else {
            presenter.onCurrentScreenRequested();
        }

        btnActionsMenu.setOnClickListener(this::onCompositionMenuClicked);
        topBottomSheetPanel.setOnClickListener(v -> openPlayQueue());

        seekBarViewWrapper = new SeekBarViewWrapper(sbTrackState);
        seekBarViewWrapper.setProgressChangeListener(presenter::onTrackRewoundTo);
        seekBarViewWrapper.setOnSeekStartListener(presenter::onSeekStart);
        seekBarViewWrapper.setOnSeekStopListener(presenter::onSeekStop);

        CompatUtils.setOutlineTextButtonStyle(panelBinding.tvPlaybackSpeed);
        CompatUtils.setOutlineTextButtonStyle(panelBinding.tvSleepTime);

        deletingErrorHandler = new DeleteErrorHandler(getChildFragmentManager(),
                presenter::onRetryFailedDeleteActionClicked,
                this::showEditorRequestDeniedMessage);

        ChoosePlayListDialogFragment fragment = (ChoosePlayListDialogFragment) getChildFragmentManager()
                .findFragmentByTag(SELECT_PLAYLIST_TAG);
        if (fragment != null) {
            fragment.setOnCompleteListener(presenter::onPlayListForAddingSelected);
        }

        CreatePlayListDialogFragment createPlayListFragment = (CreatePlayListDialogFragment) getChildFragmentManager()
                .findFragmentByTag(CREATE_PLAYLIST_TAG);
        if (createPlayListFragment != null) {
            createPlayListFragment.setOnCompleteListener(presenter::onPlayListForAddingCreated);
        }

        if (requireArguments().getBoolean(OPEN_PLAY_QUEUE_ARG)) {
            requireArguments().remove(OPEN_PLAY_QUEUE_ARG);
            openPlayQueue();
        }

        if (!Permissions.hasFilePermission(requireContext())) {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_activity_container, new StartFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (drawer.getDrawerLockMode(GravityCompat.START) != LOCK_MODE_LOCKED_CLOSED) {
                drawer.openDrawer(GravityCompat.START);
            } else {
                onBackPressed();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        navigation.onSaveInstanceState(outState);
        outState.putInt(SELECTED_DRAWER_ITEM, selectedDrawerItemId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        toolbar.release();
        drawerLockStateProcessor.release();
        viewDisposable.clear();
    }

    @Override
    public boolean onBackPressed() {
        if (playerPanelWrapper.isBottomPanelExpanded()) {
            playerPanelWrapper.collapseBottomPanelSmoothly();
            return true;
        }
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }

        Fragment fragment = navigation.getFragmentOnTop();
        boolean processed = fragment instanceof BackButtonListener
                && ((BackButtonListener) fragment).onBackPressed();
        if (!processed) {
            processed = FragmentNavigation.from(getChildFragmentManager()).goBack();
        }
        return processed;
    }

    @Override
    public void onStart() {
        super.onStart();
        presenter.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        //battery saving
        presenter.onStop();

        clearVectorAnimationInfo(ivPlayPause);
    }

    @Override
    public void setButtonPanelState(boolean expanded) {
        if (expanded) {
            playerPanelWrapper.expandBottomPanel();
        } else {
            playerPanelWrapper.collapseBottomPanel();
        }
    }

    @Override
    public void showDrawerScreen(int screenId, long selectedPlayListScreenId) {
        int itemId = ScreensMap.getMenuId(screenId);
        selectedDrawerItemId = itemId;
        navigationView.setCheckedItem(itemId);

        switch (screenId) {
            case Screens.LIBRARY: {
                presenter.onLibraryScreenSelected();
                break;
            }
            case Screens.PLAY_LISTS: {
                List<Fragment> fragments = new ArrayList<>();
                fragments.add(new PlayListsFragment());
                if (selectedPlayListScreenId != 0) {
                    fragments.add(PlayListFragmentKt.newPlayListFragment(selectedPlayListScreenId));
                }
                navigation.newRootFragmentStack(fragments, 0, R.anim.anim_alpha_appear);
                break;
            }
        }
    }

    @Override
    public void showLibraryScreen(int selectedLibraryScreen) {
        Fragment fragment;
        switch (selectedLibraryScreen) {
            case Screens.LIBRARY_COMPOSITIONS: {
                fragment = new LibraryCompositionsFragment();
                break;
            }
            case Screens.LIBRARY_FOLDERS: {
                fragment = new LibraryFoldersRootFragment();
                break;
            }
            case Screens.LIBRARY_ARTISTS: {
                fragment = new ArtistsListFragment();
                break;
            }
            case Screens.LIBRARY_ALBUMS: {
                fragment = new AlbumsListFragment();
                break;
            }
            case Screens.LIBRARY_GENRES: {
                fragment = new GenresListFragment();
                break;
            }
            default: {
                fragment = new LibraryCompositionsFragment();
            }
        }
        startFragment(fragment);
    }

    @Override
    public void showPlayerState(PlayerState state) {
        if (state == PlayerState.PLAY) {
            AndroidUtils.setAnimatedVectorDrawable(ivPlayPause, R.drawable.anim_play_to_pause);
            ivPlayPause.setContentDescription(getString(R.string.pause));
            ivPlayPause.setOnClickListener(v -> presenter.onStopButtonClicked());
            playQueueAdapter.showPlaying(true);
        } else {
            AndroidUtils.setAnimatedVectorDrawable(ivPlayPause, R.drawable.anim_pause_to_play);
            ivPlayPause.setContentDescription(getString(R.string.play));
            ivPlayPause.setOnClickListener(v -> presenter.onPlayButtonClicked());
            playQueueAdapter.showPlaying(false);
        }
    }

    @Override
    public void setMusicControlsEnabled(boolean show) {
        ivSkipToNext.setEnabled(show);
        ivSkipToPrevious.setEnabled(show);
        ivPlayPause.setEnabled(show);
        btnRepeatMode.setEnabled(show);
        btnRandomPlay.setEnabled(show);
        sbTrackState.setEnabled(show);
        acvPlayQueueMenu.getMenu().findItem(R.id.menu_save_as_playlist).setEnabled(show);
        panelBinding.tvPlaybackSpeed.setEnabled(show);
    }

    @Override
    public void showCurrentQueueItem(@Nullable PlayQueueItem item, boolean showCover) {
        animateVisibility(bottomSheetTopShadow, VISIBLE);
        animateVisibility(rvPlayList, VISIBLE);

        btnActionsMenu.setEnabled(item != null);
        if (item == null) {
            tvPlayedTime.setText(formatMilliseconds(0));
            tvTotalTime.setText(formatMilliseconds(0));
            sbTrackState.setProgress(0);

            tvCurrentComposition.setText(R.string.no_current_composition);
            tvCurrentCompositionAuthor.setText(R.string.unknown_author);
            ivMusicIcon.setImageResource(R.drawable.ic_music_placeholder);
            String noCompositionMessage = getString(R.string.no_current_composition);
            topBottomSheetPanel.setContentDescription(getString(R.string.now_playing_template, noCompositionMessage));
            rvPlayList.setContentDescription(noCompositionMessage);
            sbTrackState.setContentDescription(noCompositionMessage);
            previousCoverComposition = null;
        } else {
            Composition composition = item.getComposition();
            String compositionName = formatCompositionName(composition);
            tvCurrentComposition.setText(compositionName);
            tvTotalTime.setText(formatMilliseconds(composition.getDuration()));
            tvCurrentCompositionAuthor.setText(formatCompositionAuthor(composition, requireContext()));
            seekBarViewWrapper.setMax(composition.getDuration());
            topBottomSheetPanel.setContentDescription(getString(R.string.now_playing_template, compositionName));
            sbTrackState.setContentDescription(null);

            if (showCover) {
                Components.getAppComponent()
                        .imageLoader()
                        .displayImageInReusableTarget(ivMusicIcon, composition, previousCoverComposition, R.drawable.ic_music_placeholder);
                previousCoverComposition = composition;
            } else {
                ivMusicIcon.setImageResource(R.drawable.ic_music_placeholder);
                previousCoverComposition = null;
            }

            playQueueAdapter.onCurrentItemChanged(item);
        }
    }

    //check by:
    //switch order mode(working)
    //new queue(so-so, but pass)
    //remove queue item(working)
    @Override
    public void scrollQueueToPosition(int position) {
        secondScrollHandler.removeCallbacksAndMessages(null);
        int positionDiff = Math.abs(position - currentPosition);
        currentPosition = position;
        if (RecyclerViewUtils.isPositionVisible(playQueueLayoutManager, position)) {
            return;
        }

        boolean smooth = positionDiff == 1
                || position == playQueueLayoutManager.findFirstVisibleItemPosition()
                || position == playQueueLayoutManager.findLastVisibleItemPosition();

        RecyclerViewUtils.scrollToPosition(rvPlayList,
                playQueueLayoutManager,
                position,
                smooth);

        //sometimes can not scroll, check twice
        secondScrollHandler.postDelayed(() -> {
            if (!RecyclerViewUtils.isPositionVisible(playQueueLayoutManager, position)) {
                RecyclerViewUtils.scrollToPosition(rvPlayList,
                        playQueueLayoutManager,
                        position,
                        false);
            }
        }, 300);
    }

    @Override
    public void updatePlayQueue(List<PlayQueueItem> items) {
        playQueueAdapter.submitList(items);
    }

    @Override
    public void showRepeatMode(int mode) {
        @DrawableRes int iconRes = getRepeatModeIcon(mode);
        btnRepeatMode.setImageResource(iconRes);
        String description = getString(getRepeatModeText(mode));
        btnRepeatMode.setContentDescription(description);
    }

    @Override
    public void showRandomPlayingButton(boolean active) {
        btnRandomPlay.setSelected(active);
        if (active) {
            btnRandomPlay.setOnClickListener(v -> presenter.onRandomPlayingButtonClicked(false));
        } else {
            btnRandomPlay.setOnClickListener(v -> presenter.onRandomPlayingButtonClicked(true));
        }
    }

    @Override
    public void showTrackState(long currentPosition, long duration) {
        seekBarViewWrapper.setProgress(currentPosition);
        String formattedTime = formatMilliseconds(currentPosition);
        sbTrackState.setContentDescription(getString(R.string.position_template, formattedTime));
        tvPlayedTime.setText(formattedTime);
    }

    @Override
    public void showShareMusicDialog(Composition composition) {
        DialogUtils.shareComposition(requireContext(), composition);
    }

    @Override
    public void showPlayQueueSubtitle(int size) {
        tvQueueSubtitle.setText(formatCompositionsCount(requireContext(), size));
    }

    @Override
    public void notifyItemMoved(int from, int to) {
        playQueueAdapter.notifyItemMoved(from, to);
    }

    @Override
    public void setPlayQueueCoversEnabled(boolean isCoversEnabled) {
        playQueueAdapter.setCoversEnabled(isCoversEnabled);
    }

    @Override
    public void startEditCompositionScreen(long id) {
        startActivity(CompositionEditorActivityKt.newCompositionEditorIntent(requireContext(), id));
    }

    @Override
    public void showErrorMessage(ErrorCommand errorCommand) {
        MessagesUtils.makeSnackbar(clPlayQueueContainer, errorCommand.getMessage()).show();
    }

    @Override
    public void showDeletedItemMessage() {
        MessagesUtils.makeSnackbar(clPlayQueueContainer, R.string.queue_item_removed, Snackbar.LENGTH_LONG)
                .setAction(R.string.cancel, presenter::onRestoreDeletedItemClicked)
                .show();
    }

    @Override
    public void showAddingToPlayListError(ErrorCommand errorCommand) {
        MessagesUtils.makeSnackbar(clPlayQueueContainer,
                getString(R.string.add_to_playlist_error_template, errorCommand.getMessage()),
                Snackbar.LENGTH_SHORT)
                .show();
    }

    @Override
    public void showAddingToPlayListComplete(PlayList playList, List<Composition> compositions) {
        String text = getAddToPlayListCompleteMessage(requireActivity(), playList, compositions);
        MessagesUtils.makeSnackbar(clPlayQueueContainer, text, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void showSelectPlayListDialog() {
        ChoosePlayListDialogFragment dialog = new ChoosePlayListDialogFragment();
        dialog.setOnCompleteListener(presenter::onPlayListForAddingSelected);
        dialog.show(getChildFragmentManager(), SELECT_PLAYLIST_TAG);
    }

    @Override
    public void showConfirmDeleteDialog(List<Composition> compositionsToDelete) {
        DialogUtils.showConfirmDeleteDialog(requireContext(),
                compositionsToDelete,
                () -> presenter.onDeleteCompositionsDialogConfirmed(compositionsToDelete));
    }

    @Override
    public void showDeleteCompositionError(ErrorCommand errorCommand) {
        deletingErrorHandler.handleError(errorCommand, () ->
                makeSnackbar(clPlayQueueContainer,
                        getString(R.string.delete_composition_error_template, errorCommand.getMessage()),
                        Snackbar.LENGTH_SHORT)
                        .show()
        );
    }

    @Override
    public void showDeleteCompositionMessage(List<Composition> compositionsToDelete) {
        String text = getDeleteCompleteMessage(requireActivity(), compositionsToDelete);
        MessagesUtils.makeSnackbar(clPlayQueueContainer, text, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void displayPlaybackSpeed(float speed) {
        panelBinding.tvPlaybackSpeed.setText(getString(R.string.playback_speed_template, speed));
        panelBinding.tvPlaybackSpeed.setOnClickListener(v ->
                DialogUtils.showSpeedSelectorDialog(requireContext(),
                        speed,
                        presenter::onPlaybackSpeedSelected)
        );
    }

    @Override
    public void showSpeedChangeFeatureVisible(boolean visible) {
        panelBinding.tvPlaybackSpeed.setVisibility(visible? VISIBLE: View.GONE);
    }

    @Override
    public void showSleepTimerRemainingTime(long remainingMillis) {
        //setVisibility() don't work in motion layout
        if (remainingMillis == SleepTimerInteractorKt.NO_TIMER) {
            panelBinding.tvSleepTime.setText("");
            panelBinding.tvSleepTime.setBackground(null);
            panelBinding.tvSleepTime.setCompoundDrawables(null, null, null, null);
            panelBinding.tvSleepTime.setOnClickListener(null);
            return;
        }
        if (!panelBinding.tvSleepTime.hasOnClickListeners()) {
            //initialize, set visible
            Drawable icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_timer);
            Resources resources = requireContext().getResources();
            int iconSize = resources.getDimensionPixelSize(R.dimen.sleep_timer_icon_size);
            icon.setBounds(0, 0, iconSize, iconSize);
            icon.setTint(getColorFromAttr(requireContext(), android.R.attr.textColorSecondary));
            panelBinding.tvSleepTime.setCompoundDrawables(icon, null, null, null);
            int iconPadding = resources.getDimensionPixelSize(R.dimen.sleep_timer_icon_padding);
            panelBinding.tvSleepTime.setCompoundDrawablePadding(iconPadding);
            panelBinding.tvSleepTime.setBackgroundResource(R.drawable.bg_outline_text_button);
            panelBinding.tvSleepTime.setOnClickListener(v ->
                    new SleepTimerDialogFragment().show(getChildFragmentManager(), null)
            );
        }

        panelBinding.tvSleepTime.setText(FormatUtils.formatMilliseconds(remainingMillis));
    }

    @Override
    public void showFileScannerState(FileScannerState state) {
        if (state instanceof Running) {
            String fileName = ((Running) state).getComposition().getFileName();
            drawerHeaderBinding.tvFileScannerState.setText(getString(R.string.file_scanner_state, fileName));
            drawerHeaderBinding.tvFileScannerState.setVisibility(VISIBLE);
        } else {
            drawerHeaderBinding.tvFileScannerState.setVisibility(View.GONE);
        }
    }

    public void openPlayQueue() {
        presenter.onOpenPlayQueueClicked();
        playerPanelWrapper.openPlayQueue();
    }

    private void onPlayQueueMenuItemClicked(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_save_as_playlist: {
                CreatePlayListDialogFragment fragment = new CreatePlayListDialogFragment();
                fragment.setOnCompleteListener(presenter::onPlayListForAddingCreated);
                fragment.show(getChildFragmentManager(), CREATE_PLAYLIST_TAG);
                break;
            }
            case R.id.menu_sleep_timer: {
                new SleepTimerDialogFragment().show(getChildFragmentManager(), null);
                break;
            }
            case R.id.menu_equalizer: {
                new EqualizerDialogFragment().show(getChildFragmentManager(), null);
                break;
            }
            case R.id.menu_clear_play_queue: {
                presenter.onClearPlayQueueClicked();
                break;
            }
        }
    }

    private void onNavigationIconClicked() {
        if (drawer.getDrawerLockMode(GravityCompat.START) != LOCK_MODE_LOCKED_CLOSED) {
            drawer.openDrawer(GravityCompat.START);
        } else {
            onBackPressed();
        }
    }

    private void startFragment(Fragment fragment) {
        navigation.newRootFragment(fragment, 0, R.anim.anim_alpha_appear);
    }

    private void clearFragment() {
        navigation.clearFragmentStack(R.anim.anim_alpha_disappear);
    }

    private void onCompositionMenuClicked(View view) {
        PopupMenuWindow.showPopup(view,
                R.menu.composition_short_actions_menu,
                item -> {
                    switch (item.getItemId()) {
                        case R.id.menu_add_to_playlist: {
                            presenter.onAddCurrentCompositionToPlayListButtonClicked();
                            break;
                        }
                        case R.id.menu_share: {
                            presenter.onShareCompositionButtonClicked();
                            break;
                        }
                        case R.id.menu_delete: {
                            presenter.onDeleteCurrentCompositionButtonClicked();
                            break;
                        }
                        case R.id.menu_edit: {
                            presenter.onEditCompositionButtonClicked();
                            break;
                        }
                    }
                });
    }

    private DrawerArrowDrawable createDrawerArrowDrawable() {
        DrawerArrowDrawable drawerArrowDrawable = new DrawerArrowDrawable(requireActivity());
        drawerArrowDrawable.setColor(getColorFromAttr(requireActivity(), R.attr.toolbarTextColorPrimary));
        return drawerArrowDrawable;
    }

    private boolean onNavigationItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        boolean selected = false;
        if (itemId == R.id.menu_settings) {
            navigation.addNewFragment(new SettingsFragment());
        } else if (itemId == R.id.menu_about) {
            navigation.addNewFragment(new AboutAppFragment());
        } else if (selectedDrawerItemId != itemId) {
            selectedDrawerItemId = itemId;
            itemIdToStart = itemId;
            clearFragment();
            selected = true;
        }
        drawer.closeDrawer(GravityCompat.START);
        return selected;
    }

    private void onDrawerClosed() {
        if (itemIdToStart != NO_ITEM) {
            int screenId = ScreensMap.getScreenId(itemIdToStart);
            presenter.onDrawerScreenSelected(screenId);
            itemIdToStart = NO_ITEM;
        }
    }

    private void onPlayItemMenuClicked(View view, PlayQueueItem playQueueItem) {
        Composition composition = playQueueItem.getComposition();

        PopupMenuWindow.showPopup(view,
                R.menu.play_queue_item_menu,
                item -> {
                    switch (item.getItemId()) {
                        case R.id.menu_add_to_playlist: {
                            presenter.onAddQueueItemToPlayListButtonClicked(composition);
                            break;
                        }
                        case R.id.menu_edit: {
                            startActivity(CompositionEditorActivityKt.newCompositionEditorIntent(requireContext(), composition.getId()));
                            break;
                        }
                        case R.id.menu_share: {
                            onShareCompositionClicked(composition);
                            break;
                        }
                        case R.id.menu_delete_from_queue: {
                            presenter.onDeleteQueueItemClicked(playQueueItem);
                            break;
                        }
                        case R.id.menu_delete: {
                            presenter.onDeleteCompositionButtonClicked(composition);
                            break;
                        }
                    }
                });
    }

    private void onRepeatModeButtonClicked(View view) {
        PopupMenuWindow.showPopup(view,
                R.menu.repeat_mode_menu,
                item -> {
                    int repeatMode = RepeatMode.NONE;
                    switch (item.getItemId()) {
                        case R.id.menu_repeat_playlist: {
                            repeatMode = RepeatMode.REPEAT_PLAY_LIST;
                            break;
                        }
                        case R.id.menu_repeat_composition: {
                            repeatMode = RepeatMode.REPEAT_COMPOSITION;
                            break;
                        }
                        case R.id.menu_do_not_repeat: {
                            repeatMode = RepeatMode.NONE;
                            break;
                        }
                    }
                    presenter.onRepeatModeChanged(repeatMode);
                });
    }

    private void onShareCompositionClicked(Composition composition) {
        DialogUtils.shareComposition(requireContext(), composition);
    }

    private void showEditorRequestDeniedMessage() {
        makeSnackbar(clPlayQueueContainer, R.string.android_r_edit_file_permission_denied, Snackbar.LENGTH_LONG).show();
    }

}
