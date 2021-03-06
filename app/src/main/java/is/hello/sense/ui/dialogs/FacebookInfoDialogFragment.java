package is.hello.sense.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;

import is.hello.sense.R;
import is.hello.sense.ui.common.SenseDialogFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Styles;

public class FacebookInfoDialogFragment extends SenseDialogFragment {

    public static final String TAG = FacebookInfoDialogFragment.class.getSimpleName() + "_TAG";

    public static FacebookInfoDialogFragment newInstance(){
        return new FacebookInfoDialogFragment();
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final SenseAlertDialog dialog = SenseAlertDialog.newBottomSheetInstance(getActivity());
        final CharSequence clickableLink = getResources().getText(R.string.facebook_oauth_description);
        dialog.setMessage(Styles.resolveSupportLinks(getActivity(),
                                                     clickableLink));
        dialog.setTitle(R.string.facebook_oauth_title);
        dialog.setNegativeButton(R.string.action_close, (dialogInterface, which) -> {
            dismiss();
        });
        dialog.setCanceledOnTouchOutside(true);
        dialog.setCancelable(true);
        return dialog;
    }
}
