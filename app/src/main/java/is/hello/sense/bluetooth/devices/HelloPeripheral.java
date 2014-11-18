package is.hello.sense.bluetooth.devices;

import android.support.annotation.NonNull;

import java.util.UUID;

import is.hello.sense.bluetooth.stacks.OperationTimeout;
import is.hello.sense.bluetooth.stacks.Peripheral;
import is.hello.sense.bluetooth.stacks.PeripheralService;
import is.hello.sense.bluetooth.stacks.util.TakesOwnership;
import is.hello.sense.util.Logger;
import rx.Observable;
import rx.functions.Action1;

/**
 * Semi-high level wrapper around a Peripheral. Provides generic connection and subscription functionality.
 */
public abstract class HelloPeripheral<TSelf extends HelloPeripheral<TSelf>> {
    protected final Peripheral peripheral;
    protected PeripheralService peripheralService;

    protected HelloPeripheral(@NonNull Peripheral peripheral) {
        this.peripheral = peripheral;
    }

    //region Properties

    public int getScannedRssi() {
        return peripheral.getScanTimeRssi();
    }

    public String getAddress() {
        return peripheral.getAddress();
    }

    public String getName() {
        return peripheral.getName();
    }

    //endregion


    //region Connectivity

    /**
     * Connects to the peripheral, ensures a bond is present, and performs service discovery.
     * This method should be wrapped by subclasses so that the caller does not have to provide
     * an operation timeout object.
     * @param timeout   A timeout object to apply to the service discovery portion of connection.
     */
    protected Observable<ConnectStatus> connect(@NonNull @TakesOwnership OperationTimeout timeout) {
        return peripheral.getStack().newConfiguredObservable(s -> {
            Logger.info(Peripheral.LOG_TAG, "connect to " + toString());

            s.onNext(ConnectStatus.CONNECTING);
            Action1<Throwable> onError = s::onError;
            peripheral.connect().subscribe(peripheral -> {
                Logger.info(Peripheral.LOG_TAG, "connected to " + toString());

                s.onNext(ConnectStatus.BONDING);
                peripheral.createBond().subscribe(ignored -> {
                    Logger.info(Peripheral.LOG_TAG, "bonded to " + toString());

                    s.onNext(ConnectStatus.DISCOVERING_SERVICES);
                    peripheral.discoverServices(timeout).subscribe(services -> {
                        Logger.info(Peripheral.LOG_TAG, "discovered services for " + toString());

                        this.peripheralService = peripheral.getService(getTargetServiceIdentifier());
                        s.onNext(ConnectStatus.CONNECTED);
                        s.onCompleted();
                    }, onError);
                }, onError);
            }, onError);
        });
    }

    public Observable<TSelf> disconnect() {
        //noinspection unchecked
        return peripheral.disconnect().map(ignored -> (TSelf) this);
    }

    public boolean isConnected() {
        return peripheral.getConnectionStatus() == Peripheral.STATUS_CONNECTED;
    }

    public int getBondStatus() {
        return peripheral.getBondStatus();
    }

    //endregion


    //region Internal

    protected abstract UUID getTargetServiceIdentifier();
    protected abstract UUID getDescriptorIdentifier();

    protected PeripheralService getTargetService() {
        return peripheralService;
    }

    protected Observable<UUID> subscribe(@NonNull UUID characteristicIdentifier,
                                         @NonNull @TakesOwnership OperationTimeout timeout) {
        Logger.info(Peripheral.LOG_TAG, "Subscribing to " + characteristicIdentifier);

        return peripheral.subscribeNotification(getTargetService(),
                                                characteristicIdentifier,
                                                getDescriptorIdentifier(),
                                                timeout);
    }

    protected Observable<UUID> unsubscribe(@NonNull UUID characteristicIdentifier,
                                           @NonNull @TakesOwnership OperationTimeout timeout) {
        Logger.info(Peripheral.LOG_TAG, "Unsubscribing from " + characteristicIdentifier);

        return peripheral.unsubscribeNotification(getTargetService(),
                                                  characteristicIdentifier,
                                                  getDescriptorIdentifier(),
                                                  timeout);
    }

    //endregion


    @Override
    public String toString() {
        return '{' + getClass().getSimpleName() + ' ' + getName() + '@' + getAddress() + '}';
    }


    public static enum ConnectStatus {
        CONNECTING,
        BONDING,
        DISCOVERING_SERVICES,
        CONNECTED,
    }
}
