package is.hello.sense.graph.presenters;

import org.joda.time.DateTime;

import javax.inject.Inject;

import is.hello.sense.api.model.Account;
import is.hello.sense.graph.InjectionTestCase;
import is.hello.sense.util.SyncObserver;

public class AccountPresenterTests extends InjectionTestCase {
    @Inject AccountPresenter accountPresenter;

    public void testUpdate() throws Exception {
        accountPresenter.update();

        SyncObserver<Account> account = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, accountPresenter.account);
        account.await();

        assertNull(account.getError());
        assertNotNull(account.getSingle());
    }

    public void testSaveAccount() throws Exception {
        Account updatedAccount = new Account();
        updatedAccount.setWeight(120L);
        updatedAccount.setHeight(2000L);
        updatedAccount.setBirthDate(DateTime.now());

        SyncObserver<Account> account = SyncObserver.subscribe(SyncObserver.WaitingFor.NEXT, accountPresenter.account);
        accountPresenter.saveAccount(updatedAccount);
        account.await();

        assertNull(account.getError());
        assertNotNull(account.getSingle());

        Account savedAccount = account.getLast();
        assertEquals(updatedAccount.getWeight(), savedAccount.getWeight());
        assertEquals(updatedAccount.getHeight(), savedAccount.getHeight());
        assertEquals(updatedAccount.getBirthDate(), savedAccount.getBirthDate());
    }
}
