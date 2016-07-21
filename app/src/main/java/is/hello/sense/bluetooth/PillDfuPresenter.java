package is.hello.sense.bluetooth;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Singleton;

import is.hello.buruberi.bluetooth.stacks.BluetoothStack;
import is.hello.buruberi.bluetooth.stacks.GattPeripheral;
import is.hello.buruberi.bluetooth.stacks.util.PeripheralCriteria;
import is.hello.buruberi.util.Rx;
import is.hello.sense.bluetooth.exceptions.PillNotFoundException;
import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.graph.presenters.ValuePresenter;
import rx.Observable;

//todo move to commonsense or commonpill??? after this is working

@Singleton
public class PillDfuPresenter extends ValuePresenter<PillPeripheral> {
    public static final String PILL_DFU_NAME = "PillDFU";

    private final Context context;
    private final BluetoothStack bluetoothStack;

    public final PresenterSubject<PillPeripheral> sleepPill = this.subject;
    private String desiredPillName = null;


    @Inject
    public PillDfuPresenter(@NonNull final Context context,
                            @NonNull final BluetoothStack bluetoothStack) {
        this.context = context;
        this.bluetoothStack = bluetoothStack;
    }


    public void setDesiredPillName(@Nullable final String desiredPillName) {
        this.desiredPillName = desiredPillName;
    }

    @Override
    protected boolean isDataDisposable() {
        return false;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<PillPeripheral> provideUpdateObservable() {
        final PeripheralCriteria criteria = new PeripheralCriteria();
        criteria.setDuration(PeripheralCriteria.DEFAULT_DURATION_MS * 2);
        criteria.addPredicate(ad -> (PillPeripheral.isPillNormal(ad) || PillPeripheral.isPillDfu(ad)));
        return bluetoothStack.discoverPeripherals(criteria)
                             .map(gattPeripherals -> {
                                 Log.d(getClass().getName(), "Desired Pill: " + desiredPillName);
                                 PillPeripheral closestDFUPill = null;
                                 for (final GattPeripheral peripheral : gattPeripherals) {
                                     final String pillName = peripheral.getName();
                                     if (desiredPillName != null && desiredPillName.equals(pillName)) {
                                         return new PillPeripheral(peripheral);
                                     }
                                     if (PILL_DFU_NAME.equals(pillName)) {
                                         if (closestDFUPill == null) {
                                             closestDFUPill = new PillPeripheral(peripheral);
                                         } else if (closestDFUPill.getScanTimeRssi() < peripheral.getScanTimeRssi()) {
                                             closestDFUPill = new PillPeripheral(peripheral);
                                         }
                                     }
                                 }
                                 return closestDFUPill;
                             });

    }


    @Nullable
    @Override
    public Bundle onSaveState() {
        return null;
    }

    //region Pill Interactions

    public void reset() {
        sleepPill.forget();
    }

    public Observable<ComponentName> startDfuService(@NonNull final File file) {
        return Observable.<ComponentName>create(subscriber -> {
            if (sleepPill.getValue() == null) {
                subscriber.onError(new PillNotFoundException());
            }
            Intent intent = new Intent(context, DfuService.class);
            intent.putExtra(DfuService.EXTRA_DEVICE_NAME, sleepPill.getValue().getName());
            intent.putExtra(DfuService.EXTRA_DEVICE_ADDRESS, sleepPill.getValue().getAddress());
            intent.putExtra(DfuService.EXTRA_FILE_TYPE, DfuService.TYPE_APPLICATION);
            intent.putExtra(DfuService.EXTRA_FILE_MIME_TYPE, DfuService.MIME_TYPE_OCTET_STREAM);
            intent.putExtra(DfuService.EXTRA_KEEP_BOND, false);
            intent.putExtra(DfuService.EXTRA_FILE_PATH, file.getPath());
            intent.putExtra(DfuService.EXTRA_DISABLE_NOTIFICATION, true);

            try {
                context.stopService(intent);
                ComponentName componentName = context.startService(intent);
                subscriber.onNext(componentName);
                subscriber.onCompleted();
            } catch (SecurityException e) {
                subscriber.onError(e);
            }
        }).subscribeOn(Rx.mainThreadScheduler());
    }
    //endregion
}
