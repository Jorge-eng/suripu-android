package is.hello.sense.ui.fragments.onboarding;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import is.hello.sense.R;
import is.hello.sense.functional.Functions;
import is.hello.sense.ui.common.InjectionFragment;
import rx.Observable;

import static rx.android.observables.AndroidObservable.fromBroadcast;

public class OnboardingPairSenseFragment extends InjectionFragment {
    private static final int BLE_SETTINGS_REQUEST_CODE = 0x66;

    private BluetoothAdapter bluetoothAdapter;

    private Button nextButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.bluetoothAdapter = ((BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding_pair_sense, container, false);

        this.nextButton = (Button) view.findViewById(R.id.fragment_onboarding_step_continue);
        nextButton.setOnClickListener(this::next);
        updateNextButton();

        Button helpButton = (Button) view.findViewById(R.id.fragment_onboarding_step_help);
        helpButton.setOnClickListener(this::next);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        Observable<Intent> bluetoothStateChanged = fromBroadcast(getActivity(), new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        subscribe(bluetoothStateChanged, ignored -> updateNextButton(), Functions::ignoreError);
    }

    private void updateNextButton() {
        if (bluetoothAdapter.isEnabled())
            nextButton.setText(R.string.action_continue);
        else
            nextButton.setText(R.string.action_turn_on_ble);
    }

    public void next(View ignored) {
        if (bluetoothAdapter.isEnabled()) {

        } else {
            startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
        }
    }
}
