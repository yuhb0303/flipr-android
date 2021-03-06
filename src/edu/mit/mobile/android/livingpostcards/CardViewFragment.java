package edu.mit.mobile.android.livingpostcards;
/*
 * Copyright (C) 2012-2013  MIT Mobile Experience Lab
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation version 2
 * of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import android.app.Activity;
import android.database.Cursor;
import android.extracted.widget.AdapterViewFlipper;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import edu.mit.mobile.android.content.ProviderUtils;
import edu.mit.mobile.android.flipr.BuildConfig;
import edu.mit.mobile.android.flipr.R;
import edu.mit.mobile.android.imagecache.ImageCache;
import edu.mit.mobile.android.imagecache.ImageCache.OnImageLoadListener;
import edu.mit.mobile.android.imagecache.ImageLoaderAdapter;
import edu.mit.mobile.android.imagecache.SimpleThumbnailCursorAdapter;
import edu.mit.mobile.android.livingpostcards.auth.Authenticator;
import edu.mit.mobile.android.livingpostcards.data.Card;
import edu.mit.mobile.android.livingpostcards.data.CardMedia;
import edu.mit.mobile.android.locast.app.LocastApplication;
import edu.mit.mobile.android.locast.net.NetworkClient;
import edu.mit.mobile.android.locast.sync.LocastSyncService;

/**
 * Views a single card.
 *
 */
public class CardViewFragment extends Fragment implements LoaderCallbacks<Cursor>,
        OnImageLoadListener {

    /**
     * The card URI
     */
    public static final String ARGUMENT_URI = "uri";

    private ImageCache mImageCache;

    private Uri mCard;

    private AdapterViewFlipper mCardImage;

    private int mTiming;

    private SimpleThumbnailCursorAdapter mAdapter;

    private Uri mCardMedia;

    private final static int LOADER_CARD = 100, LOADER_CARDMEDIA = 101;

    private static final String[] CARD_PROJECTION = new String[] { Card._ID,

    Card.COL_TIMING, Card.COL_MEDIA_URL };

    private static final String[] CARD_MEDIA_PROJECTION = new String[] { CardMedia._ID,
            CardMedia.COL_LOCAL_URL, CardMedia.COL_MEDIA_URL };

    private static final String[] CARD_MEDIA_FROM = new String[] { CardMedia.COL_LOCAL_URL,
            CardMedia.COL_MEDIA_URL };

    private static final int[] CARD_MEDIA_TO = new int[] { R.id.card_media_thumbnail,
            R.id.card_media_thumbnail };

    private static final int[] IMAGE_VIEW_IDS = new int[] { R.id.card_media_thumbnail };

    public static CardViewFragment newInstance(Uri card) {
        final CardViewFragment cmf = new CardViewFragment();

        final Bundle args = new Bundle();
        args.putParcelable(ARGUMENT_URI, card);
        cmf.setArguments(args);
        return cmf;
    }

    @Override
    public void onAttach(Activity activity) {

        super.onAttach(activity);

        mImageCache = ImageCache.getInstance(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mCard = getArguments().getParcelable(ARGUMENT_URI);

            if (mCard != null) {
                mCardMedia = Card.MEDIA.getUri(mCard);
                getLoaderManager().initLoader(LOADER_CARD, null, this);
                getLoaderManager().initLoader(LOADER_CARDMEDIA, null, this);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View v = inflater.inflate(R.layout.card_view_fragment, container, false);

        mCardImage = (AdapterViewFlipper) v.findViewById(R.id.card_image);

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        mAdapter = new SimpleThumbnailCursorAdapter(getActivity(), R.layout.card_media_fullsize,
                null, CARD_MEDIA_FROM, CARD_MEDIA_TO, IMAGE_VIEW_IDS, 0);

        mCardImage.setAdapter(new ImageLoaderAdapter(getActivity(), mAdapter, mImageCache,
                IMAGE_VIEW_IDS, 200, 200, ImageLoaderAdapter.UNIT_DIP));
    }

    @Override
    public void onPause() {
        super.onPause();

        mImageCache.unregisterOnImageLoadListener(this);
        mCardImage.stopFlipping();
    }

    @Override
    public void onResume() {
        super.onResume();
        mImageCache.registerOnImageLoadListener(this);
        if (mTiming != 0) {
            mCardImage.startFlipping();
        }
        LocastSyncService.startExpeditedAutomaticSync(getActivity(), mCard);

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
        switch (id) {
            case LOADER_CARD:
                return new CursorLoader(getActivity(), mCard, CARD_PROJECTION, null, null, null);

            case LOADER_CARDMEDIA:
                return new CursorLoader(getActivity(), mCardMedia, CARD_MEDIA_PROJECTION, null,
                        null, null);
            default:
                return null;

        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        switch (loader.getId()) {
            case LOADER_CARD:
                if (c.moveToFirst()) {
                    if (BuildConfig.DEBUG) {
                        ProviderUtils.dumpCursorToLog(c, CARD_PROJECTION);
                    }

                    mTiming = c.getInt(c.getColumnIndexOrThrow(Card.COL_TIMING));
                    mCardImage.setFlipInterval(mTiming);
                    mCardImage.startFlipping();

                    final String pubMediaUri = c.getString(c
                            .getColumnIndexOrThrow(Card.COL_MEDIA_URL));
                    if (pubMediaUri != null) {
                        final NetworkClient nc = LocastApplication.getNetworkClient(getActivity(),
                                Authenticator.getFirstAccount(getActivity()));
                        LocastSyncService.startSync(getActivity(), nc.getFullUrl(pubMediaUri),
                                mCardMedia, false);
                    }

                }

                break;

            case LOADER_CARDMEDIA:
                mAdapter.setExpectedCount(c.getCount());
                if (BuildConfig.DEBUG) {
                    if (c.moveToFirst()) {
                        ProviderUtils.dumpCursorToLog(c, CARD_MEDIA_PROJECTION);
                    }
                }
                mAdapter.swapCursor(c);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_CARDMEDIA:
                mAdapter.swapCursor(null);
                break;
        }
    }

    @Override
    public void onImageLoaded(int id, Uri imageUri, Drawable image) {
        if (id == R.id.card_image) {
            // TODO
        }
    }

    public boolean checkAllCached() {
        final boolean allCached = true;
        if (mAdapter == null) {
            return false;
        }
        final Cursor c = mAdapter.getCursor();
        final int imgLocCol = c.getColumnIndexOrThrow(CardMedia.COL_LOCAL_URL);
        final int imgPubCol = c.getColumnIndexOrThrow(CardMedia.COL_MEDIA_URL);
        if (c == null || c.isClosed()) {
            return false;
        }
        for (c.moveToFirst(); c.isAfterLast(); c.moveToNext()) {
            String url = c.getString(imgLocCol);
            if (url == null) {
                url = c.getString(imgPubCol);
            }

        }
        return false;
    }

    @Override
    public void onImageLoaded(long id, Uri imageUri, Drawable image) {
        // XXX

    }
}
