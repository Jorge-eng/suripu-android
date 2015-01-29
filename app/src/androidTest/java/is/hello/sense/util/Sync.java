package is.hello.sense.util;

import android.support.annotation.NonNull;

import java.util.Iterator;

import is.hello.sense.graph.PresenterSubject;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.observables.BlockingObservable;

/**
 * A wrapper around BlockingObservable to make it more or
 * less idiot-proof to use when writing tests.
 * <p/>
 * All operators throw exceptions emitted by the source observable.
 * @param <T>   The type emitted by the Sync wrapper.
 */
public final class Sync<T> implements Iterable<T> {
    /**
     * The wrapped observable.
     */
    private final BlockingObservable<T> observable;


    //region Creation

    /**
     * Wraps an unbounded source observable.
     * <p/>
     * This method <b>does not</b> work PresenterSubject.
     */
    public static <T> Sync<T> wrap(@NonNull Observable<T> source) {
        if (source instanceof PresenterSubject) {
            throw new IllegalArgumentException("of(Observable) cannot be used with PresenterSubject!");
        }

        return new Sync<>(source);
    }

    /**
     * Wraps a presenter subject, converting it to a bounded observable
     * by <code>take</code>ing a given number of emitted values from it.
     */
    public static <T> Sync<T> wrap(int limit, @NonNull PresenterSubject<T> source) {
        return new Sync<>(source.take(limit));
    }

    /**
     * Wraps a presenter subject, converting it to a bounded observable
     * by <code>take</code>ing one emitted value from it.
     */
    public static <T> Sync<T> wrap(@NonNull PresenterSubject<T> source) {
        return wrap(1, source);
    }

    /**
     * Wraps a given observable, <code>take</code>ing one emitted value from it,
     * and firing a given action block before returning.
     * <p/>
     * This method should be used with ValuePresenter subclasses when updating them:
     * <pre>
     *     Account account = Sync.wrapAfter(presenter::update, presenter.account).last();
     * </pre>
     */
    public static <T> Sync<T> wrapAfter(@NonNull Action0 action, @NonNull Observable<T> source) {
        Observable<T> next = source.take(1);
        Sync<T> sync = new Sync<>(next);
        action.call();
        return sync;
    }


    private Sync(@NonNull Observable<T> source) {
        this.observable = source.toBlocking();
    }

    //endregion


    //region Binding

    /**
     * Returns an iterator that yields any values the wrapped
     * observable has already emitted. <b>This method will
     * not block for values.</b>
     */
    @Override
    public Iterator<T> iterator() {
        return observable.getIterator();
    }

    /**
     * Calls a given action for each value emitted by the wrapped
     * observable, blocking until the observable completes.
     */
    public void forEach(@NonNull Action1<T> action) {
        observable.forEach(action);
    }

    /**
     * Blocks until the observable completes, then returns the last emitted value.
     */
    public T last() {
        return observable.last();
    }

    //endregion


    //region Convenience

    /**
     * Shorthand for <code>Sync.wrap(observable).last();</code>.
     *
     * @see #wrap(rx.Observable)
     * @see #last()
     */
    public static <T> T last(@NonNull Observable<T> source) {
        return wrap(source).last();
    }

    /**
     * Shorthand for <code>Sync.wrap(subject).last();</code>.
     *
     * @see #wrap(is.hello.sense.graph.PresenterSubject)
     * @see #last()
     */
    public static <T> T next(@NonNull PresenterSubject<T> source) {
        return wrap(source).last();
    }

    //endregion
}
