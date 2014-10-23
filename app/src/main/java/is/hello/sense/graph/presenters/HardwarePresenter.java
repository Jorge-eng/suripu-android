package is.hello.sense.graph.presenters;

import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.common.primitives.Ints;
import com.hello.ble.devices.Morpheus;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.util.BleObserverCallback;
import is.hello.sense.util.Constants;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

@Singleton public class HardwarePresenter extends Presenter {
    private final PreferencesPresenter preferencesPresenter;
    private final ApiSessionManager apiSessionManager;

    private Observable<Morpheus> repairingTask;
    private Morpheus device;

    @Inject public HardwarePresenter(@NonNull PreferencesPresenter preferencesPresenter,
                                     @NonNull ApiSessionManager apiSessionManager) {
        this.preferencesPresenter = preferencesPresenter;
        this.apiSessionManager = apiSessionManager;
    }


    public void setPairedDeviceAddress(@Nullable String address) {
        logEvent("saving paired device address: " + address);

        SharedPreferences.Editor editor = preferencesPresenter.edit();
        if (address != null) {
            editor.putString(Constants.GLOBAL_PREF_PAIRED_DEVICE_ADDRESS, address);
        } else {
            editor.remove(Constants.GLOBAL_PREF_PAIRED_DEVICE_ADDRESS);
        }
        editor.apply();
    }

    public void setPairedPillId(@Nullable String pillId) {
        logEvent("saving paired pill id: " + pillId);

        SharedPreferences.Editor editor = preferencesPresenter.edit();
        if (pillId != null) {
            editor.putString(Constants.GLOBAL_PREF_PAIRED_PILL_ID, pillId);
        } else {
            editor.remove(Constants.GLOBAL_PREF_PAIRED_PILL_ID);
        }
        editor.apply();
    }


    public Morpheus getDevice() {
        return device;
    }

    public Observable<Set<Morpheus>> scanForDevices() {
        logEvent("scanForDevices()");

        return Observable.create((Observable.OnSubscribe<Set<Morpheus>>) s -> Morpheus.discover(new BleObserverCallback<>(s), Constants.BLE_SCAN_TIMEOUT_MS))
                         .subscribeOn(AndroidSchedulers.mainThread());
    }

    public @Nullable Morpheus bestDeviceForPairing(@NonNull Set<Morpheus> devices) {
        logEvent("bestDeviceForPairing(" + devices + ")");

        if (devices.isEmpty()) {
            return null;
        } else {
            return Collections.max(devices, (l, r) -> Ints.compare(l.getScanTimeRssi(), r.getScanTimeRssi()));
        }
    }

    public Observable<Morpheus> rediscoverDevice() {
        logEvent("rediscoverDevice()");

        if (device != null) {
            logEvent("device already rediscovered " + device);

            return Observable.just(device);
        }

        if (repairingTask != null) {
            return repairingTask;
        }

        String deviceAddress = preferencesPresenter.getSharedPreferences().getString(Constants.GLOBAL_PREF_PAIRED_DEVICE_ADDRESS, null);
        if (TextUtils.isEmpty(deviceAddress)) {
            return Observable.error(new Exception(""));
        } else {
            this.repairingTask = Observable.create((Observable.OnSubscribe<Morpheus>) s -> Morpheus.discover(deviceAddress, new BleObserverCallback<>(s), Constants.BLE_SCAN_TIMEOUT_MS))
                    .flatMap(device -> {
                        if (device != null) {
                            logEvent("rediscoveredDevice(" + device + ")");
                            this.device = device;
                            this.repairingTask = null;

                            return Observable.just(device);
                        } else {
                            return Observable.error(new Exception("Could not rediscover device."));
                        }
                    })
                    .subscribeOn(AndroidSchedulers.mainThread());
            return repairingTask;
        }
    }

    public Observable<Void> connectToDevice(@NonNull Morpheus device) {
        logEvent("connectToDevice(" + device + ")");

        if (device.isConnected() && device.getBondState() != BluetoothDevice.BOND_NONE) {
            logEvent("already paired with device " + device);

            return Observable.just(null);
        }

        return Observable.create((Observable.OnSubscribe<Void>) s -> device.connect(new BleObserverCallback<>(s)))
                         .doOnNext(ignored -> {
                             logEvent("pairedWithDevice(" + device + ")");
                             setPairedDeviceAddress(device.getAddress());
                             this.device = device;
                         })
                         .subscribeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Void> sendWifiCredentials(String bssid, String ssid, String password) {
        logEvent("sendWifiCredentials()");

        return Observable.create((Observable.OnSubscribe<Void>) s -> device.setWIFIConnection(bssid, ssid, password, new BleObserverCallback<>(s)))
                         .subscribeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Void> linkAccount() {
        logEvent("linkAccount()");

        return Observable.create((Observable.OnSubscribe<Void>) s -> device.linkAccount(apiSessionManager.getAccessToken(), new BleObserverCallback<>(s)))
                         .subscribeOn(AndroidSchedulers.mainThread());
    }

    public Observable<String> linkPill() {
        logEvent("linkPill()");

        return Observable.create((Observable.OnSubscribe<String>) s -> device.pairPill(apiSessionManager.getAccessToken(), new BleObserverCallback<>(s)))
                         .doOnNext(pillId -> {
                             logEvent("linkedWithPill(" + pillId+ ")");
                             setPairedPillId(pillId);
                         })
                         .subscribeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Void> putIntoPairingMode() {
        logEvent("putIntoPairingMode()");

        return Observable.create((Observable.OnSubscribe<Void>) s -> device.switchToPairingMode(new BleObserverCallback<>(s)))
                         .subscribeOn(AndroidSchedulers.mainThread());
    }

    public void clearDevice() {
        logEvent("clearDevice()");

        if (device != null) {
            if (device.isConnected()) {
                logEvent("disconnect from paired device");
                device.disconnect();
            }

            this.device = null;
        }
    }
}
