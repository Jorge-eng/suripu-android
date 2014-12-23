package is.hello.sense.bluetooth.stacks;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

import static is.hello.sense.AssertExtensions.assertThrows;

public class SchedulerOperationTimeoutTests extends TestCase {
    private static final Scheduler TEST_SCHEDULER = AndroidSchedulers.mainThread();

    public void testExhaustionAndRecycling() throws Exception {
        SchedulerOperationTimeout.Pool pool = new SchedulerOperationTimeout.Pool("Test", OperationTimeout.Pool.RECOMMENDED_CAPACITY);
        List<OperationTimeout> operations = new ArrayList<>();
        for (int i = 0; i < OperationTimeout.Pool.RECOMMENDED_CAPACITY; i++) {
            operations.add(pool.acquire("Test", 1, TimeUnit.SECONDS));
        }

        assertThrows(() -> pool.acquire("Test", 1, TimeUnit.SECONDS));

        for (OperationTimeout timeout : operations) {
            timeout.recycle();
        }
        operations.clear();

        OperationTimeout timeout = pool.acquire("Test", 1, TimeUnit.SECONDS);
        timeout.recycle();
    }

    public void testScheduling() throws Exception {
        OperationTimeout timeout = null;
        try {
            timeout = new SchedulerOperationTimeout.Pool("Test", 1).acquire("Test", 500, TimeUnit.MILLISECONDS);
            AtomicBoolean called = new AtomicBoolean();
            timeout.setTimeoutAction(() -> called.set(true), TEST_SCHEDULER);

            timeout.schedule();
            timeout.unschedule();
            assertFalse(called.get());

            timeout.schedule();
            Thread.sleep(800, 0);
            timeout.unschedule();
            assertTrue(called.get());
        } finally {
            if (timeout != null) {
                timeout.recycle();
            }
        }
    }
}
