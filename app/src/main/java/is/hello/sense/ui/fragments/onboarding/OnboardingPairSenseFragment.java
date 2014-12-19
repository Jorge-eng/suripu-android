package is.hello.sense.ui.fragments.onboarding;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.bluetooth.devices.HelloPeripheral;
import is.hello.sense.bluetooth.devices.SensePeripheral;
import is.hello.sense.bluetooth.devices.transmission.protobuf.MorpheusBle;
import is.hello.sense.bluetooth.errors.PeripheralConnectionError;
import is.hello.sense.bluetooth.errors.PeripheralNotFoundError;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.HardwarePresenter;
import is.hello.sense.ui.activities.OnboardingActivity;
import is.hello.sense.ui.common.FragmentNavigation;
import is.hello.sense.ui.common.InjectionFragment;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.dialogs.LoadingDialogFragment;
import is.hello.sense.ui.fragments.UnstableBluetoothFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.BuildValues;
import is.hello.sense.util.Logger;
import rx.Observable;

public class OnboardingPairSenseFragment extends InjectionFragment {
    private static int REQUEST_CODE_PAIR_HELP = 0x19;

    @Inject HardwarePresenter hardwarePresenter;
    @Inject BuildValues buildValues;

    private Button nextButton;
    private LoadingDialogFragment loadingDialogFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Analytics.trackEvent(Analytics.EVENT_ONBOARDING_PAIR_SENSE, null);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_pair_sense, container, false);

        this.nextButton = (Button) view.findViewById(R.id.fragment_onboarding_step_continue);
        Views.setSafeOnClickListener(nextButton, this::next);

        Button helpButton = (Button) view.findViewById(R.id.fragment_onboarding_step_help);
        Views.setSafeOnClickListener(helpButton, this::next);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        subscribe(hardwarePresenter.bluetoothEnabled, nextButton::setEnabled, Functions.LOG_ERROR);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PAIR_HELP && resultCode == OnboardingPairHelpFragment.RESULT_NEW_SENSE) {
            TroubleshootSenseDialogFragment dialogFragment = new TroubleshootSenseDialogFragment();
            dialogFragment.show(getFragmentManager(), TroubleshootSenseDialogFragment.TAG);
        }
    }

    private void beginPairing() {
        this.loadingDialogFragment = LoadingDialogFragment.show(getFragmentManager(), getString(R.string.title_scanning_for_sense), true);
    }

    private void finishedPairing() {
        loadingDialogFragment.setTitle(getString(R.string.title_checking_connectivity));
        bindAndSubscribe(hardwarePresenter.currentWifiNetwork(), network -> {
            if (network.connectionState == MorpheusBle.wifi_connection_state.IP_RETRIEVED) {
                linkAccount();
            } else {
                LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(),
                        () -> ((OnboardingActivity) getActivity()).showSelectWifiNetwork(true));
            }
        }, e -> {
            Logger.error(OnboardingPairSenseFragment.class.getSimpleName(), "Could not get Sense's wifi network", e);

            LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(),
                    () -> ((OnboardingActivity) getActivity()).showSelectWifiNetwork(true));
        });
    }

    @SuppressWarnings("CodeBlock2Expr")
    private void linkAccount() {
        loadingDialogFragment.setTitle(getString(R.string.title_linking_account));
        bindAndSubscribe(hardwarePresenter.linkAccount(), ignored -> {
            LoadingDialogFragment.closeWithDoneTransition(getFragmentManager(),
                    () -> ((OnboardingActivity) getActivity()).showPairPill());
        }, error -> {
            Logger.error(OnboardingPairSenseFragment.class.getSimpleName(), "Could not link Sense to account", error);
            pairingFailed(error);
        });
    }

    public void next(@NonNull View sender) {
        beginPairing();

        if (hardwarePresenter.getPeripheral() == null) {
            Observable<SensePeripheral> device = hardwarePresenter.scanForDevices().map(hardwarePresenter::getClosestPeripheral);
            bindAndSubscribe(device, this::tryToPairWith, this::pairingFailed);
        } else {
            finishedPairing();
        }
    }

    public void tryToPairWith(@Nullable SensePeripheral device) {
        if (device != null) {
            if (buildValues.isDebugBuild()) {
                SenseAlertDialog dialog = new SenseAlertDialog(getActivity());
                dialog.setTitle(R.string.debug_title_confirm_sense_pair);
                dialog.setMessage(getString(R.string.debug_message_confirm_sense_pair_fmt, device.getName()));
                dialog.setPositiveButton(android.R.string.ok, (sender, which) -> pairWith(device));
                dialog.setNegativeButton(android.R.string.cancel, null);
                dialog.show();
            } else {
                pairWith(device);
            }
        } else {
            pairingFailed(new PeripheralConnectionError());
        }
    }

    public void pairWith(@NonNull SensePeripheral device) {
        bindAndSubscribe(hardwarePresenter.connectToPeripheral(device), status -> {
            if (status == HelloPeripheral.ConnectStatus.CONNECTED) {
                finishedPairing();
            } else {
                loadingDialogFragment.setTitle(getString(status.messageRes));
            }
        }, this::pairingFailed);
    }

    public void pairingFailed(Throwable e) {
        LoadingDialogFragment.close(getFragmentManager());

        if (e instanceof PeripheralNotFoundError) {
            OnboardingPairHelpFragment pairHelpFragment = new OnboardingPairHelpFragment();
            pairHelpFragment.setTargetFragment(this, REQUEST_CODE_PAIR_HELP);
            ((FragmentNavigation) getActivity()).showFragment(pairHelpFragment, null, true);
        } else if (hardwarePresenter.isErrorFatal(e)) {
            UnstableBluetoothFragment fragment = new UnstableBluetoothFragment();
            fragment.show(getFragmentManager(), R.id.activity_onboarding_container);
        } else {
            ErrorDialogFragment.presentBluetoothError(getFragmentManager(), getActivity(), e);
        }
    }


    public static class TroubleshootSenseDialogFragment extends DialogFragment {
        public static final String TAG = TroubleshootSenseDialogFragment.class.getSimpleName();

        public static final int RESULT_HELP = 0x505;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Analytics.trackEvent(Analytics.EVENT_ONBOARDING_SECOND_PILL_CHECK, null);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            SenseAlertDialog dialog = new SenseAlertDialog(getActivity());

            dialog.setTitle(R.string.dialog_title_troubleshoot_sense);
            dialog.setMessage(R.string.dialog_message_troubleshoot_sense);

            dialog.setPositiveButton(android.R.string.ok, null);
            dialog.setNegativeButton(R.string.action_help, (sender, which) -> {
                if (getTargetFragment() != null) {
                    getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_HELP, null);
                }
            });

            return dialog;
        }
    }
}
