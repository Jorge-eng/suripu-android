package is.hello.sense.ui.fragments.onboarding;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import javax.inject.Inject;

import is.hello.sense.R;
import is.hello.sense.api.ApiService;
import is.hello.sense.api.model.SenseTimeZone;
import is.hello.sense.bluetooth.devices.HelloPeripheral;
import is.hello.sense.bluetooth.devices.SensePeripheral;
import is.hello.sense.bluetooth.devices.transmission.protobuf.SenseCommandProtos;
import is.hello.sense.bluetooth.errors.PeripheralNotFoundError;
import is.hello.sense.functional.Functions;
import is.hello.sense.graph.presenters.PreferencesPresenter;
import is.hello.sense.ui.common.OnboardingToolbar;
import is.hello.sense.ui.common.UserSupport;
import is.hello.sense.ui.dialogs.ErrorDialogFragment;
import is.hello.sense.ui.fragments.HardwareFragment;
import is.hello.sense.ui.fragments.UnstableBluetoothFragment;
import is.hello.sense.ui.widget.SenseAlertDialog;
import is.hello.sense.ui.widget.util.Views;
import is.hello.sense.util.Analytics;
import is.hello.sense.util.BuildValues;
import is.hello.sense.util.Logger;
import rx.Observable;

public class OnboardingPairSenseFragment extends HardwareFragment {
    @Inject ApiService apiService;
    @Inject BuildValues buildValues;
    @Inject PreferencesPresenter preferences;

    private Button nextButton;

    private boolean hasLinkedAccount = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.hasLinkedAccount = savedInstanceState.getBoolean("hasLinkedAccount", false);
        }

        Analytics.trackEvent(Analytics.EVENT_ONBOARDING_PAIR_SENSE, null);

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_pair_sense, container, false);

        this.nextButton = (Button) view.findViewById(R.id.fragment_onboarding_step_continue);
        Views.setSafeOnClickListener(nextButton, this::next);

        Button pairingModeHelp = (Button) view.findViewById(R.id.fragment_onboarding_pair_sense_mode_help);
        Views.setSafeOnClickListener(pairingModeHelp, this::showPairingModeHelp);

        OnboardingToolbar.of(this, view)
                .setWantsBackButton(true)
                .setOnHelpClickListener(ignored -> UserSupport.showForOnboardingStep(getActivity(), UserSupport.OnboardingStep.SETUP_SENSE));

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        subscribe(hardwarePresenter.bluetoothEnabled, nextButton::setEnabled, Functions.LOG_ERROR);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("hasLinkedAccount", hasLinkedAccount);
    }


    private void checkConnectivityAndContinue() {
        showBlockingActivity(R.string.title_checking_connectivity);
        showHardwareActivity(() -> {
            bindAndSubscribe(hardwarePresenter.currentWifiNetwork(), network -> {
                if (network.connectionState == SenseCommandProtos.wifi_connection_state.IP_RETRIEVED) {
                    preferences.edit()
                               .putString(PreferencesPresenter.PAIRED_DEVICE_SSID, network.ssid)
                               .apply();

                    linkAccount();
                } else {
                    hideAllActivity(true, () -> getOnboardingActivity().showSelectWifiNetwork(true));
                }
            }, e -> {
                Logger.error(OnboardingPairSenseFragment.class.getSimpleName(), "Could not get Sense's wifi network", e);
                hideAllActivity(true, () -> getOnboardingActivity().showSelectWifiNetwork(true));
            });
        });
    }

    private void linkAccount() {
        if (hasLinkedAccount) {
            setDeviceTimeZone();
        } else {
            showBlockingActivity(R.string.title_linking_account);

            bindAndSubscribe(hardwarePresenter.linkAccount(),
                             ignored -> {
                                 this.hasLinkedAccount = true;
                                 setDeviceTimeZone();
                             },
                             error -> {
                                 Logger.error(OnboardingPairSenseFragment.class.getSimpleName(), "Could not link Sense to account", error);
                                 pairingFailed(error);
                             });
        }
    }

    private void setDeviceTimeZone() {
        showBlockingActivity(R.string.title_setting_time_zone);

        SenseTimeZone timeZone = SenseTimeZone.fromDefault();
        bindAndSubscribe(apiService.updateTimeZone(timeZone),
                         ignored -> {
                             Logger.info(OnboardingSignIntoWifiFragment.class.getSimpleName(), "Time zone updated.");

                             preferences.edit()
                                        .putString(PreferencesPresenter.PAIRED_DEVICE_TIME_ZONE, timeZone.timeZoneId)
                                        .apply();

                             pushDeviceData();
                         },
                         this::pairingFailed);
    }

    private void pushDeviceData() {
        showBlockingActivity(R.string.title_pushing_data);

        bindAndSubscribe(hardwarePresenter.pushData(),
                         ignored -> finished(),
                         error -> {
                             Logger.error(getClass().getSimpleName(), "Could not push data from Sense, ignoring.", error);
                             finished();
                         });
    }

    private void finished() {
        hideAllActivity(true, () -> getOnboardingActivity().showPairPill());
    }

    public void showPairingModeHelp(@NonNull View sender) {
        getOnboardingActivity().showFragment(new OnboardingSensePairingModeHelpFragment(), null, true);
    }

    public void next(@NonNull View sender) {
        showBlockingActivity(R.string.title_pairing);

        Observable<SensePeripheral> device = hardwarePresenter.closestPeripheral();
        bindAndSubscribe(device, this::tryToPairWith, this::pairingFailed);
    }

    public void tryToPairWith(@NonNull SensePeripheral device) {
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
    }

    public void pairWith(@NonNull SensePeripheral device) {
        bindAndSubscribe(hardwarePresenter.connectToPeripheral(device), status -> {
            if (status == HelloPeripheral.ConnectStatus.CONNECTED) {
                checkConnectivityAndContinue();
            } else {
                showBlockingActivity(status.messageRes);
            }
        }, this::pairingFailed);
    }

    public void pairingFailed(Throwable e) {
        hideAllActivity(false, () -> {
            if (e instanceof PeripheralNotFoundError) {
                TroubleshootSenseDialogFragment dialogFragment = new TroubleshootSenseDialogFragment();
                dialogFragment.show(getFragmentManager(), TroubleshootSenseDialogFragment.TAG);
            } else if (hardwarePresenter.isErrorFatal(e)) {
                UnstableBluetoothFragment fragment = new UnstableBluetoothFragment();
                fragment.show(getFragmentManager(), R.id.activity_onboarding_container);
            } else {
                ErrorDialogFragment.presentBluetoothError(getFragmentManager(), getActivity(), e);
            }
        });
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
