package is.hello.sense.bluetooth.stacks;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import is.hello.sense.bluetooth.errors.PeripheralServiceDiscoveryFailedError;
import is.hello.sense.bluetooth.stacks.transmission.PacketHandler;
import is.hello.sense.bluetooth.stacks.util.AdvertisingData;
import is.hello.sense.functional.Either;
import rx.Observable;

public class TestPeripheral implements Peripheral {
    final BluetoothStack stack;
    final TestPeripheralBehavior behavior;

    PacketHandler dataHandler;

    public TestPeripheral(@NonNull BluetoothStack stack,
                          @NonNull TestPeripheralBehavior peripheralBehavior) {
        this.stack = stack;
        this.behavior = peripheralBehavior;
    }

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "UnusedParameters"})
    private <T> Observable<T> createResponseWith(@NonNull Either<T, Throwable> value, @Nullable OperationTimeout timeout) {
        Observable<T> observable = value.<Observable<T>>map(Observable::just, Observable::error);
        return observable.delay(behavior.latency, TimeUnit.MILLISECONDS);
    }

    @NonNull
    @Override
    public OperationTimeout createOperationTimeout(@NonNull String name, long duration, @NonNull TimeUnit timeUnit) {
        return TestOperationTimeout.acquire(name);
    }

    @Override
    public int getScanTimeRssi() {
        return behavior.scanTimeRssi;
    }

    @Override
    public String getAddress() {
        return behavior.address;
    }

    @Override
    public String getName() {
        return behavior.name;
    }

    @NonNull
    @Override
    public AdvertisingData getAdvertisingData() {
        return behavior.advertisingData;
    }

    @Override
    public BluetoothStack getStack() {
        return stack;
    }

    @NonNull
    @Override
    public Observable<Peripheral> connect(@NonNull OperationTimeout timeout) {
        behavior.trackMethodCall(TestPeripheralBehavior.Method.CONNECT);
        return createResponseWith(behavior.connectResponse, null);
    }

    @NonNull
    @Override
    public Observable<Peripheral> disconnect() {
        behavior.trackMethodCall(TestPeripheralBehavior.Method.DISCONNECT);
        return createResponseWith(behavior.disconnectResponse, null);
    }

    @Override
    public @ConnectivityStatus int getConnectionStatus() {
        return behavior.connectionStatus;
    }

    @NonNull
    @Override
    public Observable<Peripheral> createBond() {
        behavior.trackMethodCall(TestPeripheralBehavior.Method.CREATE_BOND);
        return createResponseWith(behavior.createBondResponse, null);
    }

    @Override
    public @BondStatus int getBondStatus() {
        return behavior.bondStatus;
    }

    @NonNull
    @Override
    public Observable<Map<UUID, PeripheralService>> discoverServices(@NonNull OperationTimeout timeout) {
        behavior.trackMethodCall(TestPeripheralBehavior.Method.DISCOVER_SERVICES);
        return createResponseWith(behavior.servicesResponse, timeout);
    }

    @NonNull
    @Override
    public Observable<PeripheralService> discoverService(@NonNull UUID serviceIdentifier, @NonNull OperationTimeout timeout) {
        return discoverServices(timeout).flatMap(ignored -> {
            if (behavior.servicesResponse != null && behavior.servicesResponse.isLeft()) {
                PeripheralService service = behavior.servicesResponse.getLeft().get(serviceIdentifier);
                if (service != null) {
                    return Observable.just(service);
                }
            }
            return Observable.error(new PeripheralServiceDiscoveryFailedError());
        });
    }

    @NonNull
    @Override
    public Observable<UUID> subscribeNotification(@NonNull PeripheralService onPeripheralService,
                                                  @NonNull UUID characteristicIdentifier,
                                                  @NonNull UUID descriptorIdentifier,
                                                  @NonNull OperationTimeout timeout) {
        behavior.trackMethodCall(TestPeripheralBehavior.Method.SUBSCRIBE);
        return createResponseWith(behavior.subscriptionResponse, timeout);
    }

    @NonNull
    @Override
    public Observable<UUID> unsubscribeNotification(@NonNull PeripheralService onPeripheralService,
                                                    @NonNull UUID characteristicIdentifier,
                                                    @NonNull UUID descriptorIdentifier,
                                                    @NonNull OperationTimeout timeout) {
        behavior.trackMethodCall(TestPeripheralBehavior.Method.UNSUBSCRIBE);
        return createResponseWith(behavior.unsubscriptionResponse, timeout);
    }

    @NonNull
    @Override
    public Observable<Void> writeCommand(@NonNull PeripheralService onPeripheralService,
                                         @NonNull UUID identifier,
                                         @NonNull WriteType writeType, @NonNull byte[] payload,
                                         @NonNull OperationTimeout timeout) {
        behavior.trackMethodCall(TestPeripheralBehavior.Method.WRITE_COMMAND);
        return createResponseWith(behavior.writeCommandResponse, timeout);
    }

    @Override
    public void setPacketHandler(@Nullable PacketHandler dataHandler) {
        this.dataHandler = dataHandler;
    }

}