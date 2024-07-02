package biz;

import model.Account;
import model.User;
import model.Operation;
import model.operations.LogIn;
import model.operations.LogOut;
import db.dao.DAO;
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
public class BankHistoryTest {

    @Mock
    private DAO dao;

    @InjectMocks
    private BankHistory bankHistory;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setName("TestUser");
    }

    @Test
    void testLogLoginSuccess() throws SQLException {
        doNothing().when(dao).logOperation(any(Operation.class), eq(true));

        bankHistory.logLoginSuccess(user);

        verify(dao, times(1)).logOperation(any(LogIn.class), eq(true));
    }

    @Test
    void testLogLoginFailure() throws SQLException {
        doNothing().when(dao).logOperation(any(Operation.class), eq(false));

        bankHistory.logLoginFailure(user, "Login failed");

        verify(dao, times(1)).logOperation(any(LogIn.class), eq(false));
    }

    @Test
    void testLogLogOut() throws SQLException {
        doNothing().when(dao).logOperation(any(Operation.class), eq(true));

        bankHistory.logLogOut(user);

        verify(dao, times(1)).logOperation(any(LogOut.class), eq(true));
    }

    @Test
    void testLogOperation() throws SQLException {
        Operation operation = new LogIn(user, "Logowanie ");
        doNothing().when(dao).logOperation(operation, true);

        bankHistory.logOperation(operation, true);

        verify(dao, times(1)).logOperation(operation, true);
    }

    @Test
    void testLogUnauthorizedOperation_NotImplemented() {
        Operation operation = new LogIn(user, "Unauthorized");

        assertThrows(RuntimeException.class, () -> {
            bankHistory.logUnauthorizedOperation(operation, false);
        });
    }

    @Test
    void testLogPaymentIn_NotImplemented() {
        Account account = new Account();
        account.setId(1);
        account.setAmmount(1000.0);
        account.setOwner(user);

        assertThrows(RuntimeException.class, () -> {
            bankHistory.logPaymentIn(account, 100.0, true);
        });
    }

    @Test
    void testLogPaymentOut_NotImplemented() {
        Account account = new Account();
        account.setId(1);
        account.setAmmount(1000.0);
        account.setOwner(user);

        assertThrows(RuntimeException.class, () -> {
            bankHistory.logPaymentOut(account, 100.0, true);
        });
    }
}