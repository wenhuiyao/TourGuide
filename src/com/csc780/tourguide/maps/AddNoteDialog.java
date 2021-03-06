package com.csc780.tourguide.maps;

import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.csc780.tourguide.R;

/**
 * This class is responsible for displaying a dialog (small window) when a user
 * wants to add a note that only contains text or when he wants to edit the note
 * that he just added to the database. If the user wants to add a note, an empty
 * dialog is displayed and the user can enter his name and the note text in the
 * dialog. The date is automatically set.By clicking on a save button, all the
 * above information along with the user's current location (longitude,
 * latitude) will be stored in the database. If the user wants to edit the note
 * he just added, a dialog with the stored information is displayed and the user
 * can change the name and the text of the note. By clicking on the save button,
 * the database is updated for that note.
 * 
 * 
 */
public class AddNoteDialog extends Dialog {

	public static final String TAG = "AddNoteDialog";
	private EditText editBody;
	private EditText editAuthor;
	private TextView textDate;
	private long now;
	// private Context context;
	private double latitude;
	private double longitude;
	private Button saveButton;
	private Button cancelButton;
	private long rowId = -1;
	private Handler mHandler;

	/**
	 * This is the constructor for a dialog that is shown when the user wants to
	 * edit his own note.
	 * 
	 * @param context
	 * @param theme
	 * @param rowId
	 *            the rowId of the user's note that has been retrieved from the
	 *            database to be edited. It is needed to update the note that
	 *            has been edited.
	 * @param author
	 *            the author of the note (either anonymous or the user's name),
	 *            which has been retrieved from the database
	 * @param body
	 *            the body of the note, which has been retrieved from the
	 *            database
	 * @param date
	 *            the creation date of the note, retrieved from the database
	 */
	public AddNoteDialog(Context context, int theme, Long rowId, String author,
			String body, String date) {
		super(context, theme);
		this.rowId = rowId;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.y = -150;
		getWindow().setAttributes(lp);
		setContentView(R.layout.add_note_dialog);
		editBody = (EditText) findViewById(R.id.editText_body);
		editAuthor = (EditText) findViewById(R.id.editText_author);
		textDate = (TextView) findViewById(R.id.textView_date1);
		saveButton = (Button) findViewById(R.id.button_save);
		cancelButton = (Button) findViewById(R.id.button_cancel);
		// display the note information. Body and author can be editted.
		editBody.setText(body);
		editAuthor.setText(author);
		textDate.setText(date);
		editAuthor.setFocusable(true);
		// a listener for save button to update the edited note in the database
		saveButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				saveMemo();
				dismiss();
			}
		});
		// a listener for cancel button to leave the edit note dialog and go
		// back to the application main screen
		cancelButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
	}

	/**
	 * This is the constructor for a dialog that is shown when the user wants to
	 * add a new note.
	 * 
	 * @param context
	 * @param theme
	 * @param latitude
	 *            latitude of the user's current location
	 * @param longitude
	 *            longitude of the user's current location
	 * @param handler
	 *            a handler to pass a message
	 */
	public AddNoteDialog(Context context, int theme, double latitude,
			double longitude, Handler handler) {
		super(context, theme);
		this.latitude = latitude;
		this.longitude = longitude;
		this.mHandler = handler;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.y = -150;
		getWindow().setAttributes(lp);
		setContentView(R.layout.add_note_dialog);
		editBody = (EditText) findViewById(R.id.editText_body);
		editAuthor = (EditText) findViewById(R.id.editText_author);
		textDate = (TextView) findViewById(R.id.textView_date1);
		saveButton = (Button) findViewById(R.id.button_save);
		cancelButton = (Button) findViewById(R.id.button_cancel);
		now = System.currentTimeMillis();
		String nowText = (String) DateFormat.format("MM/dd/yy", now);
		textDate.setText(nowText);
		editAuthor.setFocusable(true);

		// a listener for save button to store the new note in the database
		saveButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				saveMemo();
				dismiss();
			}
		});
		// a listener for cancel button to leave the add note dialog and go
		// back to the application main screen
		cancelButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

	}

	/**
	 * On clicking on save, this method is called, which instantiates a task to
	 * either insert the new note in the database or to update the database with
	 * the edited note.
	 */

	private void saveMemo() {
		if (editBody.getText().toString().length() == 0)
			return;
		new InsertMemoTask().execute();
	}

	/**
	 * On pressing back button, the current activity will be exited.
	 */

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		this.dismiss();
	}

	/**
	 * This class checks the value of the rowId and calls the appropriate
	 * methods in ServerInterface class to either store a new note or update an
	 * edited note. rowId of -1 indicates that a new note should be inserted to
	 * the database. non-negative values of rowId indicate that the note has
	 * been edited and it should be updated in the database. If a new note is
	 * being added, it also stores the rowId and current location information of
	 * the new note in an array that contains all the notes the user has just
	 * added. This array is used to display note icons on the map for the new
	 * notes.
	 * 
	 */
	private class InsertMemoTask extends AsyncTask<String, Boolean, Boolean> {

		@Override
		protected Boolean doInBackground(String... params) {
			String author = editAuthor.getText().toString();
			String body = editBody.getText().toString();

			String result = "";
			// insert a new note
			if (rowId < 0) {
				// use a default name if the user does not provide his name
				if (author.length() == 0)
					author = "Anonymous";
				result = ServerInterface.insertMemo(author, body,
						Long.toString(now), Double.toString(latitude),
						Double.toString(longitude));
				if (!result.equals("-1"))
					FindLocationActivity.ownMemo.add(new NearbyLocationInfo(
							result, latitude, longitude));

			} else {
				// update the edited note
				result = ServerInterface.editMemo(Long.toString(rowId), author,
						body);
			}
			return true;
		}

		/**
		 * sends appropriate message to the main activity
		 */
		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result && rowId < 0) {
				Message msg = mHandler
						.obtainMessage(FindLocationActivity.MESSAGE_ADD_MEMO);
				msg.sendToTarget();
			}

		}

	}
}
