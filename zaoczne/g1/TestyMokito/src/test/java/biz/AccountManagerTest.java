package biz;

import db.dao.DAO;
import model.Account;
import model.Operation;
import model.User;
import model.exceptions.OperationIsNotAllowedException;
import model.exceptions.UserUnnkownOrBadPasswordException;
import model.operations.PaymentIn;
import model.operations.Withdraw;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountManagerTest {

    @Mock
    private DAO dao;

    @Mock
    private BankHistory history;

    @Mock
    private AuthenticationManager auth;

    @InjectMocks
    private AccountManager accountManager;

    private User user;
    private Account account;
    private int accountId = 1;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setName("TestUser");

        account = new Account();
        account.setId(accountId);
        account.setAmmount(1000.0);
    }

    @Test
    void testPaymentIn_Success() throws SQLException {
        when(dao.findAccountById(accountId)).thenReturn(account);
        when(dao.updateAccountState(account)).thenReturn(true);

        boolean result = accountManager.paymentIn(user, 100.0, "Deposit", accountId);

        assertTrue(result);
        verify(dao).updateAccountState(account);
        verify(history).logOperation(any(PaymentIn.class), eq(true));
    }

    @Test
    void testPaymentOut_Success() throws SQLException, OperationIsNotAllowedException {
        when(dao.findAccountById(accountId)).thenReturn(account);
        when(auth.canInvokeOperation(any(Operation.class), eq(user))).thenReturn(true);
        when(dao.updateAccountState(account)).thenReturn(true);

        boolean result = accountManager.paymentOut(user, 100.0, "Withdraw", accountId);

        assertTrue(result);
        verify(dao).updateAccountState(account);
        verify(history).logOperation(any(Withdraw.class), eq(true));
    }

    @Test
    void testPaymentOut_Unauthorized() throws SQLException {
        when(dao.findAccountById(accountId)).thenReturn(account);
        when(auth.canInvokeOperation(any(Operation.class), eq(user))).thenReturn(false);

        assertThrows(OperationIsNotAllowedException.class, () -> accountManager.paymentOut(user, 100.0, "Withdraw", accountId));

        verify(history).logUnauthorizedOperation(any(Operation.class), eq(false));
    }

    @Test
    void testLogIn_Success() throws SQLException, UserUnnkownOrBadPasswordException {
        when(auth.logIn(eq("TestUser"), any(char[].class))).thenReturn(user);

        boolean result = accountManager.logIn("TestUser", new char[]{'p', 'a', 's', 's'});

        assertTrue(result);
        assertEquals(user, accountManager.getLoggedUser());
    }

    @Test
    void testLogIn_Failure() throws SQLException, UserUnnkownOrBadPasswordException {
        when(auth.logIn(eq("TestUser"), any(char[].class))).thenThrow(new UserUnnkownOrBadPasswordException(""));

        assertThrows(UserUnnkownOrBadPasswordException.class, () -> accountManager.logIn("TestUser", new char[]{'p', 'a', 's', 's'}));

        assertNull(accountManager.getLoggedUser());
    }

    @Test
    void testLogOut_Success() throws SQLException, UserUnnkownOrBadPasswordException {
        when(auth.logOut(user)).thenReturn(true);

        accountManager.logIn("TestUser", new char[]{'p', 'a', 's', 's'}); // Mock logIn first
        boolean result = accountManager.logOut(user);

        assertTrue(result);
        assertNull(accountManager.getLoggedUser());
    }

    @Test
    void testInternalPayment_Success() throws SQLException, OperationIsNotAllowedException {
        Account destAccount = new Account();
        destAccount.setId(2);
        destAccount.setAmmount(500.0);

        when(dao.findAccountById(accountId)).thenReturn(account);
        when(dao.findAccountById(2)).thenReturn(destAccount);
        when(auth.canInvokeOperation(any(Operation.class), eq(user))).thenReturn(true);
        when(dao.updateAccountState(account)).thenReturn(true);
        when(dao.updateAccountState(destAccount)).thenReturn(true);

        boolean result = accountManager.internalPayment(user, 100.0, "Transfer", accountId, 2);

        assertTrue(result);
        verify(dao).updateAccountState(account);
        verify(dao).updateAccountState(destAccount);
        verify(history, times(2)).logOperation(any(Operation.class), eq(true));
    }

    @Test
    void testPaymentIn_AccountNotFound() throws SQLException {
        when(dao.findAccountById(accountId)).thenReturn(null);

        boolean result = accountManager.paymentIn(user, 100.0, "Deposit", accountId);

        assertFalse(result);
        verify(history).logOperation(any(PaymentIn.class), eq(false));
    }

    @Test
    void testPaymentOut_AccountNotFound() throws SQLException {
        when(dao.findAccountById(accountId)).thenReturn(null);

        assertThrows(OperationIsNotAllowedException.class, () -> {
            accountManager.paymentOut(user, 100.0, "Withdraw", accountId);
        });

        verify(history).logUnauthorizedOperation(any(Operation.class), eq(false));
    }

    @Test
    void testInternalPayment_SourceAccountNotFound() throws SQLException, OperationIsNotAllowedException {
        Account destAccount = new Account();
        destAccount.setId(2);
        destAccount.setAmmount(500.0);

        when(dao.findAccountById(accountId)).thenReturn(null);
        when(dao.findAccountById(2)).thenReturn(destAccount);

        assertThrows(IllegalArgumentException.class, () -> {
            accountManager.internalPayment(user, 100.0, "Transfer", accountId, 2);
        });

        verify(history, never()).logUnauthorizedOperation(any(Operation.class), anyBoolean());
        verify(history, never()).logOperation(any(Operation.class), anyBoolean());
    }

    @Test
    void testInternalPayment_DestinationAccountNotFound() throws SQLException, OperationIsNotAllowedException {
        when(dao.findAccountById(accountId)).thenReturn(account);
        when(dao.findAccountById(2)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> {
            accountManager.internalPayment(user, 100.0, "Transfer", accountId, 2);
        });

        verify(history, never()).logUnauthorizedOperation(any(Operation.class), anyBoolean());
        verify(history, never()).logOperation(any(Operation.class), anyBoolean());
    }

    @Test
    void testPaymentIn_SQLException() throws SQLException {
        when(dao.findAccountById(accountId)).thenThrow(new SQLException());

        assertThrows(SQLException.class, () -> {
            accountManager.paymentIn(user, 100.0, "Deposit", accountId);
        });

        verify(history, never()).logOperation(any(Operation.class), anyBoolean());
    }

    @Test
    void testPaymentOut_SQLException() throws SQLException {
        when(dao.findAccountById(accountId)).thenThrow(new SQLException());

        assertThrows(SQLException.class, () -> {
            accountManager.paymentOut(user, 100.0, "Withdraw", accountId);
        });

        verify(history, never()).logOperation(any(Operation.class), anyBoolean());
    }

    @Test
    void testInternalPayment_SQLException() throws SQLException, OperationIsNotAllowedException {
        when(dao.findAccountById(accountId)).thenThrow(new SQLException());

        assertThrows(SQLException.class, () -> {
            accountManager.internalPayment(user, 100.0, "Transfer", accountId, 2);
        });

        verify(history, never()).logOperation(any(Operation.class), anyBoolean());
    }


    @Test
    void testPaymentOut_InsufficientFunds() throws SQLException, OperationIsNotAllowedException {
        account.setAmmount(50.0); // Set insufficient funds
        when(dao.findAccountById(accountId)).thenReturn(account);
        when(auth.canInvokeOperation(any(Operation.class), eq(user))).thenReturn(true);

        boolean result = accountManager.paymentOut(user, 100.0, "Withdraw", accountId);

        assertFalse(result);
        verify(dao, never()).updateAccountState(account);
        verify(history).logOperation(any(Withdraw.class), eq(false));
    }


    @Test
    void testPaymentOut_NegativeAmount() throws SQLException {
        assertThrows(IllegalArgumentException.class, () -> {
            accountManager.paymentOut(user, -100.0, "Negative Withdraw", accountId);
        });
        verify(history).logOperation(any(Operation.class), anyBoolean());
        verify(dao, never()).updateAccountState(any(Account.class));
    }

    @Test
    void testInternalPayment_InsufficientFunds() throws SQLException, OperationIsNotAllowedException {
        account.setAmmount(50.0); // Set insufficient funds in source account
        Account destAccount = new Account();
        destAccount.setId(2);
        destAccount.setAmmount(500.0);

        when(dao.findAccountById(accountId)).thenReturn(account);
        when(dao.findAccountById(2)).thenReturn(destAccount);
        when(auth.canInvokeOperation(any(Operation.class), eq(user))).thenReturn(true);

        boolean result = accountManager.internalPayment(user, 100.0, "Transfer", accountId, 2);

        assertFalse(result);
        verify(dao, never()).updateAccountState(account);
        verify(dao, never()).updateAccountState(destAccount);
        verify(history).logOperation(any(Withdraw.class), eq(false));
        verify(history).logOperation(any(PaymentIn.class), eq(false));
    }

    @Test
    void testBuildBank_Success() {
        AccountManager accountManager = AccountManager.buildBank();

        assertNotNull(accountManager);
        assertNotNull(accountManager.dao);
        assertNotNull(accountManager.history);
        assertNotNull(accountManager.auth);
        assertNotNull(accountManager.interestOperator);
    }
}