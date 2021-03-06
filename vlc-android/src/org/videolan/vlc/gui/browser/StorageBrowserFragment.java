/*
 * *************************************************************************
 *  StorageBrowserFragment.java
 * **************************************************************************
 *  Copyright © 2015 VLC authors and VideoLAN
 *  Author: Geoffrey Métais
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *  ***************************************************************************
 */

package org.videolan.vlc.gui.browser;

import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

import org.videolan.libvlc.Media;
import org.videolan.medialibrary.media.MediaWrapper;
import org.videolan.medialibrary.media.Storage;
import org.videolan.vlc.R;
import org.videolan.vlc.VLCApplication;
import org.videolan.vlc.util.AndroidDevices;
import org.videolan.vlc.util.CustomDirectories;

import java.io.File;

public class StorageBrowserFragment extends FileBrowserFragment {

    public static final String KEY_IN_MEDIALIB = "key_in_medialib";

    boolean mScannedDirectory = false;

    public StorageBrowserFragment(){
        mHandler = new BrowserFragmentHandler(this);
        ROOT = AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY;
    }

    @Override
    protected Fragment createFragment() {
        return new StorageBrowserFragment();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        mAdapter = new StorageBrowserAdapter(this);
        if (bundle == null)
            bundle = getArguments();
        if (bundle != null){
            mScannedDirectory = bundle.getBoolean(KEY_IN_MEDIALIB);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mRoot) {
            mFAB = (FloatingActionButton) getActivity().findViewById(R.id.fab);
            mFAB.setImageResource(R.drawable.ic_fab_add);
            mFAB.setVisibility(View.VISIBLE);
            mFAB.setOnClickListener(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mFAB.setVisibility(View.GONE);
        mFAB.setOnClickListener(null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (VLCApplication.showTvUi()) {
            if (mRoot)
                mFAB.requestFocus();
            else
                mRecyclerView.requestFocus();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IN_MEDIALIB, mScannedDirectory);
    }

    @Override
    protected void browseRoot() {
        String[] storages = mMediaLibrary.getDevices();
        String[] customDirectories = CustomDirectories.getCustomDirectories();
        Storage storage;
        for (String mediaDirLocation : storages) {
            if (TextUtils.isEmpty(mediaDirLocation))
                continue;
            storage = new Storage(Uri.fromFile(new File(mediaDirLocation)));
            if (TextUtils.equals(AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY, mediaDirLocation))
                storage.setName(getString(R.string.internal_memory));
            mAdapter.addItem(storage, false, false);
        }
        customLoop:
        for (String customDir : customDirectories) {
            for (String mediaDirLocation : storages) {
                if (TextUtils.isEmpty(mediaDirLocation))
                    continue;
                if (customDir.startsWith(mediaDirLocation))
                    continue customLoop;
            }
            storage = new Storage(Uri.parse(customDir));
            mAdapter.addItem(storage, false, false);
        }
        mHandler.sendEmptyMessage(BrowserFragmentHandler.MSG_HIDE_LOADING);
        if (mReadyToDisplay) {
            updateEmptyView();
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void update() {
        ((StorageBrowserAdapter)mAdapter).updateMediaDirs();
        super.update();
    }

    @Override
    public void onMediaAdded(int index, Media media) {
        if (media.getType() != Media.Type.Directory)
            return;
        super.onMediaAdded(index, media);
    }

    protected void updateDisplay() {
        if (isRootDirectory())
            ((StorageBrowserAdapter)mAdapter).updateMediaDirs();
        if (!mAdapter.isEmpty()) {
            if (mSavedPosition > 0) {
                mLayoutManager.scrollToPositionWithOffset(mSavedPosition, 0);
                mSavedPosition = 0;
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    public void browse (MediaWrapper media, int position, boolean scanned){
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment next = createFragment();
        Bundle args = new Bundle();
        args.putParcelable(KEY_MEDIA, media);
        args.putBoolean(KEY_IN_MEDIALIB, mScannedDirectory || scanned);
        next.setArguments(args);
        ft.replace(R.id.fragment_placeholder, next, media.getLocation());
        ft.addToBackStack(mMrl);
        ft.commit();
    }

    protected void setContextMenuItems(MenuInflater inflater, Menu menu, int position) {
        if (mRoot) {
            Storage storage = (Storage) mAdapter.getItem(position);
            boolean isCustom = CustomDirectories.contains(storage.getUri().getPath());
            if (isCustom)
                inflater.inflate(R.menu.directory_custom_dir, menu);
        } else
            super.setContextMenuItems(menu, position);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fab){
            showAddDirectoryDialog();
        }
    }

    @Override
    protected String getCategoryTitle() {
        return getString(R.string.directories_summary);
    }
}
