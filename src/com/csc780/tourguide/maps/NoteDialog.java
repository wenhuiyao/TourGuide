package com.csc780.tourguide.maps;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.csc780.tourguide.R;

/**
 * This class displays the note information (author, date and body) in a dialog.
 * When a user taps on a note icon on the map, a dialog containing the note's
 * information, which has been retrieved from the database, is shown. If the
 * note has just been added by the user , then edit and delete icons are
 * available to the user to enable him to modify his note.
 * 
 */

public class NoteDialog extends Dialog {

	public static final String TAG = "NoteDialog";
	private TextView note;
	private TextView textAuthor;
	private TextView textDate;
	private ImageView imageEdit, imageRemove;
	private String author, body, date;
	private Context context;
	private long rowId = -1;
	private int index;
	private Handler handler;

	/**
	 * This is the class constructor.
	 * 
	 * @param context
	 * @param theme
	 * @param x the x coordinate the dialog is going to position
	 *            
	 * @param y the y coordinate the dialog is going to position
	 *            
	 * @param rowId
	 *            the rowId of the note whose information should be displayed
	 * @param isOwn
	 *            determines if the note has just been added by the user
	 */
	public NoteDialog(Context context, int theme, int x, int y, long rowId,
			boolean isOwn) {
		super(context, theme);
		this.context = context;
		this.rowId = rowId;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		int width = FindLocationActivity.LAYOUT_WIDTH / 2;
		int height = FindLocationActivity.LAYOUT_HEIGHT / 2;
		if (x > width)
			lp.x = x - width;

		if (y > height)
			lp.y = y - height - FindLocationActivity.LAYOUT_HEIGHT / 4;
		else
			lp.y = y - FindLocationActivity.LAYOUT_HEIGHT / 4;

		getWindow().setAttributes(lp);
		setContentView(R.layout.note_dialog);
		note = (TextView) findViewById(R.id.textView_note);
		textAuthor = (TextView) findViewById(R.id.textView_author);
		textDate = (TextView) findViewById(R.id.textView_date);
		// enables the user to modify the note he just added.
		if (isOwn) {
			textDate.setVisibility(View.GONE);
			imageEdit = (ImageView) findViewById(R.id.imageView_edit);
			imageRemove = (ImageView) findViewById(R.id.imageView_remove);
			imageEdit.setVisibility(View.VISIBLE);
			imageRemove.setVisibility(View.VISIBLE);
			imageEdit.setClickable(true);
			imageRemove.setClickable(true);
			imageEdit.setOnClickListener(editListener);
			imageRemove.setOnClickListener(removeListener);
		}
	}

	/**
	 * a listener for the edit icon in the user's own note which lets the user
	 * edit his note.
	 */
	private View.OnClickListener editListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			dismiss();
			Log.i(TAG, "rowId:" + Long.toString(rowId));
			AddNoteDialog dialog = new AddNoteDialog(context,
					R.style.CustomDialogTheme, rowId, author, body, date);
			dialog.show();
		}
	};

	/**
	 * A listener for the delete icon in the user's own note which lets the user
	 * delete his note. It also asks the main activity to remove the relevant
	 * icon from the map.
	 * 
	 */
	private View.OnClickListener removeListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			Log.i(TAG, "remove rowId:" + Long.toString(rowId));
			String result = ServerInterface.deleteMemo(Long.toString(rowId));
			if (result.equals("1")) {
				Message msg = handler.obtainMessage();
				msg.what = FindLocationActivity.MESSAGE_REMOVE_OVERLAY;
				msg.obj = index;
				msg.sendToTarget();
			}
			dismiss();
		}
	};

	/**
	 * This method sets the index
	 * @param index 
	 */
	public void setIndex(int index) {
		this.index = index;
	}

	public void setHandler(Handler handler) {
		this.handler = handler;
	}

	/**
	 * This method populates the dialog with the note's information retrieved
	 * from the database
	 * 
	 * @param author
	 *            the author of the note
	 * @param date
	 *            the creation date of the note
	 * @param body
	 *            the body of the note
	 */
	public void setText(String author, String date, String body) {
		this.author = author;
		this.body = body;
		this.date = date;
		textAuthor.setText(author);
		textDate.setText(date);
		note.setText(body);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		this.dismiss();
	}

}
