package edu.mit.mobile.android.livingpostcards;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.Toast;
import edu.mit.mobile.android.content.ProviderUtils;

public class DeleteDialogFragment extends DialogFragment implements OnClickListener {

    private static final String ARG_ITEM_URI = "uri";
    private static final String ARG_MESSAGE = "message";

    private OnDeleteListener mOnDeleteListener;

    private Uri mItem;
    private CharSequence mMessage;

    public static DeleteDialogFragment newInstance(Uri item, CharSequence message) {
        final DeleteDialogFragment f = new DeleteDialogFragment();
        final Bundle args = new Bundle();
        args.putParcelable(ARG_ITEM_URI, item);
        args.putCharSequence(ARG_MESSAGE, message);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle args = getArguments();
        mMessage = args.getCharSequence(ARG_MESSAGE);
        final Uri item = args.getParcelable(ARG_ITEM_URI);
        mItem = item;


        final String type = getActivity().getContentResolver().getType(mItem);
        if (type == null || !type.startsWith(ProviderUtils.TYPE_ITEM_PREFIX)) {
            Toast.makeText(getActivity().getApplicationContext(),
                    "Cannot handle the requested content type",
                    Toast.LENGTH_LONG).show();
            dismiss();
            return;
        }
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        return new AlertDialog.Builder(getActivity()).setIcon(R.drawable.ic_launcher)
                .setTitle("Delete item").setPositiveButton("Delete", this).setMessage(mMessage)
                .setNegativeButton(android.R.string.cancel, this).setCancelable(true).create();
    }

    public void registerOnDeleteListener(OnDeleteListener listener) {
        mOnDeleteListener = listener;
    }

    public void unregisterOnDeleteListener(OnDeleteListener listener) {
        mOnDeleteListener = null;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case AlertDialog.BUTTON_POSITIVE:
                final int count = getActivity().getContentResolver().delete(mItem, null, null);
                dialog.dismiss();
                mOnDeleteListener.onDelete(mItem, count >= 1); // it should only ever be 1, but...

                break;

            case AlertDialog.BUTTON_NEGATIVE:
                dialog.cancel();
                mOnDeleteListener.onDelete(mItem, false);
                break;
        }

    }

    public static interface OnDeleteListener {
        public void onDelete(Uri item, boolean deleted);
    }
}