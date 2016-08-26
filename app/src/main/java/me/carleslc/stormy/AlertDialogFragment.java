package me.carleslc.stormy;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;

public class AlertDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();

        String defaultMessage = context.getString(R.string.error_message);
        Bundle args = getArguments();
        String alertMessage = (args != null) ?
                args.getString(context.getString(R.string.error_message_key), defaultMessage)
                : defaultMessage;

        return new AlertDialog.Builder(context)
                .setTitle(R.string.error_title)
                .setMessage(alertMessage)
                .setPositiveButton(R.string.error_ok_button_text, null)
                .create();
    }

}
