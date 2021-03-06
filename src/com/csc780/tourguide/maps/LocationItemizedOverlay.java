package com.csc780.tourguide.maps;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.csc780.tourguide.R;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

/**
 * This class is used to display note overlays on the map. It adds all the
 * overlay items to an array list which is used by FindLocationActivity class to
 * display each item on the map. It is also responsible for performing necessary
 * tasks when the user taps on each overlay item. Using two private classes and
 * ServerInterface class, it retrieves the note's information from the database
 * and displays it in an appropriate dialog. If the note belongs to the current
 * user, its information is displayed in a dialog that lets the user edit/delete
 * his note; otherwise, it displays the information in a dialog that does not
 * let the user modify the note.
 * 
 */

public class LocationItemizedOverlay extends ItemizedOverlay<OverlayItem> {

	public static final String TAG = "LocationItemizedOverlay";
	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	private Context context;
	private int x = 100, y = 100;
	private MapView mapview;
	private Handler mHandler;

	public LocationItemizedOverlay(Drawable defaultMarker, Context context,
			MapView mapview) {
		super(boundCenterBottom(defaultMarker));
		this.context = context;
		this.mapview = mapview;
	}

	public void setHandler(Handler handler) {
		this.mHandler = handler;

	}

	/**
	 * This method adds an overlay item to the array list that contains all the
	 * overlays.
	 * 
	 * @param overlay
	 *            an overlay item that should be displayed on the map
	 */
	public void addOverlay(OverlayItem overlay) {
		mOverlays.add(overlay);
		populate();
	}

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		try {
			shadow = false;
			super.draw(canvas, mapView, shadow);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method removes all the overlays from the array list.
	 */
	public void clearOverlay() {
		mOverlays.clear();
	}

	/**
	 * This method returns the overlay item at index i.
	 */
	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	/**
	 * This method removes an overlay item at index i.
	 * 
	 * @param index
	 *            the index whose element should be removed.
	 */

	public void removeOverlayItem(int index) {
		mOverlays.remove(index);
	}

	/**
	 * This method returns the size of the array list containing all overlay
	 * items.
	 */
	@Override
	public int size() {
		return mOverlays.size();
	}

	public void setDialogPosition(int x, int y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * This method overrides onTap() method for overlay items. After each note
	 * is tapped by the user, its coordinates on the map and the rowId of the
	 * note are retrieved from the overlay item's Geopoint and snippet,
	 * respectively. If the rowId is "-1", it means the note does not belong to
	 * the current user and he cannot edit o delete it. Hence, an async task is
	 * instantiated to retrieve the note's information from the database and
	 * display it in a note dialog that cannot be edited. If the rowId is not
	 * "-1" another task is instantiated, which retrieves the note's information
	 * and displays it in an editable dialog.
	 */
	@Override
	protected boolean onTap(int index) {
		OverlayItem item = mOverlays.get(index);
		GeoPoint geopoint = item.getPoint();
		geopoint2pos(geopoint);
		String id = item.getSnippet();
		// Viewing a memo
		if (id.equals("-1")) {
			new RetrieveMemoTask().execute(index);
		} else {

			String[] params = { id, item.getTitle() };
			new EditMemoTask().execute(params);
		}
		return true;
	}

	/**
	 * This method returns an overlay item at a specific index.
	 * 
	 * @param index
	 *            the index whose element should be returned
	 * @return an overlay item at index
	 */
	public OverlayItem getOverlayItem(int index) {
		return mOverlays.get(index);
	}

	private void geopoint2pos(GeoPoint geopoint) {
		Point point = new Point();
		mapview.getProjection().toPixels(geopoint, point);
		this.x = point.x;
		this.y = point.y;
	}

	/**
	 * This private class uses ServerInterface class to retrieve and process the
	 * note information for the note that has been tapped by the user and
	 * displays the note information in a note dialog that does not provide the
	 * user with edit/delete functionality. It uses a text-only dialog if the
	 * note does not contain a photo and an image-text dialog if the note has a
	 * photo.
	 * 
	 */
	class RetrieveMemoTask extends AsyncTask<Integer, Boolean, Boolean> {

		String[] memoInfo;

		@Override
		protected Boolean doInBackground(Integer... params) {
			String rowId = FindLocationActivity.nearbyLocation.get(params[0])
					.getRowId();
			Log.i(TAG, "rowId " + rowId);
			String result = ServerInterface.getMemo(rowId);
			if (result.equals(""))
				return false;
			memoInfo = ServerInterface.processMemoInfo(result);
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result) {
				if (memoInfo[3].equals("0")) {
					NoteDialog dialog = new NoteDialog(context,
							R.style.CustomDialogTheme, x, y, -1, false);
					dialog.setText(memoInfo[0], memoInfo[1], memoInfo[2]);
					dialog.setCanceledOnTouchOutside(true);
					dialog.show();
				} else {
					ImageNoteDialog dialog = new ImageNoteDialog(context,
							R.style.CustomDialogTheme, x, y, -1, false);
					dialog.setInfo(memoInfo);
					dialog.setCanceledOnTouchOutside(true);
					dialog.show();
				}
			}
		}

	}

	/**
	 * This private class uses ServerInterface class to retrieve and process the
	 * note information for the note that has been tapped by the user and
	 * displays the note information in a note dialog that provides the user
	 * with edit/delete functionality. It uses a text-only dialog if the note
	 * does not contain a photo and an image-text dialog if the note has a
	 * photo.
	 * 
	 */
	class EditMemoTask extends AsyncTask<String, Boolean, Boolean> {

		String[] memoInfo;
		String rowId;
		String index;

		@Override
		protected Boolean doInBackground(String... params) {
			rowId = params[0];
			index = params[1];
			try {
				String result = ServerInterface.getMemo(rowId);
				if (result.equals(""))
					return false;
				memoInfo = ServerInterface.processMemoInfo(result);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			Log.i(TAG, "memoInfo:" + memoInfo);
			return true;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result) {
				if (memoInfo[3].equals("0")) {
					NoteDialog dialog = new NoteDialog(context,
							R.style.CustomDialogTheme, x, y,
							Long.parseLong(rowId), true);
					dialog.setText(memoInfo[0], memoInfo[1], memoInfo[2]);
					dialog.setIndex(Integer.parseInt(index));
					dialog.setHandler(mHandler);
					dialog.setCanceledOnTouchOutside(true);
					dialog.show();
				} else {
					ImageNoteDialog dialog = new ImageNoteDialog(context,
							R.style.CustomDialogTheme, x, y,
							Long.parseLong(rowId), true);
					dialog.setInfo(memoInfo);
					dialog.setIndex(Integer.parseInt(index));
					dialog.setHandler(mHandler);
					dialog.setCanceledOnTouchOutside(true);
					dialog.show();
				}
			}

		}
	}

}
