package biz;

import db.dao.DAO;
import model.Account;
import model.Operation;
import model.User;
import model.operations.Interest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InterestOperatorTest {

    @Mock
    private DAO dao;

    @Mock
    private AccountManager accountManager;

    @Mock
    private BankHistory bankHistory;

    @InjectMocks
    private InterestOperator interestOperator;

    private Account account;
    private User interestOperatorUser;

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setId(1);
        account.setAmmount(1000.0);

        interestOperatorUser = new User();
        interestOperatorUser.setId(2);
        interestOperatorUser.setName("InterestOperator");

        interestOperator = new InterestOperator(dao, accountManager);
        interestOperator.bankHistory = bankHistory;
    }

    @Test
    void testCountInterestForAccount_Success() throws SQLException {
        when(dao.findUserByName("InterestOperator")).thenReturn(interestOperatorUser);
        when(accountManager.paymentIn(eq(interestOperatorUser), anyDouble(), anyString(), eq(account.getId()))).thenReturn(true);

        interestOperator.countInterestForAccount(account);

        double expectedInterest = account.getAmmount() * 0.2;
        verify(accountManager, times(1)).paymentIn(eq(interestOperatorUser), eq(expectedInterest), eq("Interest ..."), eq(account.getId()));
        verify(bankHistory, times(1)).logOperation(any(Interest.class), eq(true));
    }

    @Test
    void testCountInterestForAccount_PaymentInFails() throws SQLException {
        when(dao.findUserByName("InterestOperator")).thenReturn(interestOperatorUser);
        when(accountManager.paymentIn(eq(interestOperatorUser), anyDouble(), anyString(), eq(account.getId()))).thenReturn(false);

        interestOperator.countInterestForAccount(account);

        double expectedInterest = account.getAmmount() * 0.2;
        verify(accountManager, times(1)).paymentIn(eq(interestOperatorUser), eq(expectedInterest), eq("Interest ..."), eq(account.getId()));
        verify(bankHistory, times(1)).logOperation(any(Interest.class), eq(false));
    }

    @Test
    void testCountInterestForAccount_SQLException() throws SQLException {
        when(dao.findUserByName("InterestOperator")).thenThrow(new SQLException());

        assertThrows(SQLException.class, () -> {
            interestOperator.countInterestForAccount(account);
        });

        verify(accountManager, never()).paymentIn(any(User.class), anyDouble(), anyString(), anyInt());
        verify(bankHistory, never()).logOperation(any(Operation.class), anyBoolean());
    }
}
