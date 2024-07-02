package biz;

import db.dao.DAO;
import model.*;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class AuthenticationManagerTest {

    @Mock
    private DAO dao;

    @Mock
    private BankHistory history;

    @InjectMocks
    private AuthenticationManager authManager;

    private User user;
    private Password password;
    private Role adminRole;
    private Role userRole;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setName("TestUser");

        password = new Password();
        password.setUserId(1);
        password.setPasswd(AuthenticationManager.hashPassword(new char[]{'p', 'a', 's', 's'}));

        adminRole = new Role();
        adminRole.setId(1);
        adminRole.setName("Admin");

        userRole = new Role();
        userRole.setId(2);
        userRole.setName("User");

        user.setRole(userRole);
    }

    @Test
    void testLogIn_Success() throws SQLException, UserUnnkownOrBadPasswordException {
        when(dao.findUserByName("TestUser")).thenReturn(user);
        when(dao.findPasswordForUser(user)).thenReturn(password);

        User result = authManager.logIn("TestUser", new char[]{'p', 'a', 's', 's'});

        assertEquals(user, result);
        verify(history).logLoginSuccess(user);
    }

    @Test
    void testLogIn_BadPassword() throws SQLException {
        when(dao.findUserByName("TestUser")).thenReturn(user);
        when(dao.findPasswordForUser(user)).thenReturn(password);

        assertThrows(UserUnnkownOrBadPasswordException.class, () -> {
            authManager.logIn("TestUser", new char[]{'w', 'r', 'o', 'n', 'g'});
        });

        verify(history).logLoginFailure(eq(user), anyString());
    }

    @Test
    void testLogIn_UserNotFound() throws SQLException {
        when(dao.findUserByName("TestUser")).thenReturn(null);

        assertThrows(UserUnnkownOrBadPasswordException.class, () -> {
            authManager.logIn("TestUser", new char[]{'p', 'a', 's', 's'});
        });

        verify(history).logLoginFailure(isNull(), anyString());
    }

    @Test
    void testLogOut_Success() throws SQLException {
        doNothing().when(history).logLogOut(user);

        boolean result = authManager.logOut(user);

        assertTrue(result);
        verify(history).logLogOut(user);
    }

    @Test
    void testCanInvokeOperation_AdminRole() {
        user.setRole(adminRole);
        Operation operation = new Withdraw(user, 100.0, "Test Withdraw", new Account());

        boolean result = authManager.canInvokeOperation(operation, user);

        assertTrue(result);
    }

    @Test
    void testCanInvokeOperation_UserRole_PaymentIn() {
        Operation operation = new PaymentIn(user, 100.0, "Test PaymentIn", new Account());

        boolean result = authManager.canInvokeOperation(operation, user);

        assertTrue(result);
    }

    @Test
    void testCanInvokeOperation_UserRole_Withdraw_OwnAccount() {
        Account account = new Account();
        account.setOwner(user);
        Operation operation = new Withdraw(user, 100.0, "Test Withdraw", account);

        boolean result = authManager.canInvokeOperation(operation, user);

        assertTrue(result);
    }

    @Test
    void testCanInvokeOperation_UserRole_Withdraw_OtherAccount() {
        User otherUser = new User();
        otherUser.setId(2);
        otherUser.setName("OtherUser");
        otherUser.setRole(userRole);

        Account otherAccount = new Account();
        otherAccount.setOwner(otherUser);
        Operation operation = new Withdraw(otherUser, 100.0, "Test Withdraw", otherAccount);

        boolean result = authManager.canInvokeOperation(operation, user);

        assertFalse(result);
    }
}