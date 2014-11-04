package is.hello.sense.bluetooth.stacks.transmission;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.UUID;

import is.hello.sense.util.Logger;
import rx.functions.Action1;

public abstract class PacketDataHandler<T> {
    public static final String LOG_TAG = "Bluetooth/" + PacketDataHandler.class.getSimpleName();

    //region Primitive Methods

    public abstract boolean shouldProcessCharacteristic(@NonNull UUID charUUID);
    public abstract void processPacket(final @NonNull SequencedPacket blePacket);

    //endregion


    //region Propagating Data

    public @Nullable Action1<Throwable> onError;
    public @Nullable Action1<T> onFinished;

    public void clearHandlers() {
        this.onError = null;
        this.onFinished = null;
    }

    protected void onError(@NonNull Throwable e) {
        Logger.error(LOG_TAG, "Data error " + e);

        if (this.onError != null) {
            this.onError.call(e);
        } else {
            throw new IllegalStateException("PacketDataHandler received error without a terminal receiver");
        }

        clearHandlers();
    }

    protected void onFinished(@Nullable final T data) {
        Logger.info(LOG_TAG, "Finished decoding packets into " + data);

        if (this.onFinished != null) {
            this.onFinished.call(data);
        } else {
            throw new IllegalStateException("PacketDataHandler received data without a terminal receiver");
        }

        clearHandlers();
    }

    //endregion
}
