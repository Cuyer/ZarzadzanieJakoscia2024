package biz;

import db.dao.DAO;
import db.dao.impl.DAOImpl;
import db.dao.impl.SQLiteDB;
import model.Account;
import model.Operation;
import model.User;
import model.exceptions.OperationIsNotAllowedException;
import model.exceptions.UserUnnkownOrBadPasswordException;
import model.operations.PaymentIn;
import model.operations.Withdraw;

import java.sql.SQLException;

/**
 * Created by Krzysztof Podlaski on 04.03.2018.
 */
public class AccountManager {
    DAO dao;
    BankHistory history;
    AuthenticationManager auth;
    InterestOperator interestOperator;
    User loggedUser=null;

    /*
    Brak sprawdzenia, czy użytkownik nie jest nullem
     */
    public boolean paymentIn(User user, double ammount, String description, int accountId) throws SQLException {
        if (ammount < 0) {
            throw new IllegalArgumentException("Amount to pay in");
        }
        if (user == null) {
            throw new IllegalArgumentException("User should not be null");
        }
        Account account = dao.findAccountById(accountId);
        Operation operation = new PaymentIn(user, ammount,description, account);
        boolean success = false;
        if (account != null) {
            success = account.income(ammount);
            success = dao.updateAccountState(account);
        }
        history.logOperation(operation, success);
        return success;
    }

    /*
    Metoda nie sprawdza nadpisania success przez account.outcome(ammount),
    dlatego zawsze zrobi update stanu konta i zaloguje operację
    Brak sprawdzenia czy amount > 0
    Brak sprawdzenia czy account istnieje
    Brak sprawdzenia, czy użytkownik nie jest nullem
     */
    public boolean paymentOut(User user, double amount, String description, int accountId) throws OperationIsNotAllowedException, SQLException {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to withdraw cannot be negative");
        }
        if (user == null) {
            throw new IllegalArgumentException("User should not be null");
        }
        Account account = dao.findAccountById(accountId);
        if (account == null) {
            throw new IllegalArgumentException("Account does not exist");
        }

        Operation operation = new Withdraw(user, amount, description, account);
        boolean success = auth.canInvokeOperation(operation, user);
        if (!success) {
            history.logUnauthorizedOperation(operation, success);
            throw new OperationIsNotAllowedException("Unauthorized operation");
        }
        success = account.outcome(amount);
        if (success) {
            success = dao.updateAccountState(account);
        }
        history.logOperation(operation, success);
        return success;
    }

    /*
    Brak sprawdzenia, czy amount > 0, brak sprawdzenia czy source i dest account istnieją
    Brak sprawdzenia, czy user != null
     */
    public boolean internalPayment(User user, double amount, String description, int sourceAccountId, int destAccountId) throws OperationIsNotAllowedException, SQLException {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to pay cannot be negative");
        }
        if (user == null) {
            throw new IllegalArgumentException("User should not be null");
        }
        Account sourceAccount = dao.findAccountById(sourceAccountId);
        Account destAccount = dao.findAccountById(destAccountId);
        if (sourceAccount == null || destAccount == null) {
            throw new IllegalArgumentException("Source or destination account does not exist");
        }

        Operation withdraw = new Withdraw(user, amount, description, sourceAccount);
        Operation payment = new PaymentIn(user, amount, description, destAccount);
        boolean success = auth.canInvokeOperation(withdraw, user);
        if (!success) {
            history.logUnauthorizedOperation(withdraw, success);
            throw new OperationIsNotAllowedException("Unauthorized operation");
        }
        success = sourceAccount.outcome(amount);
        success = success && destAccount.income(amount);
        if (success) {
            success = dao.updateAccountState(sourceAccount);
            if (success) dao.updateAccountState(destAccount);
        }
        history.logOperation(withdraw, success);
        history.logOperation(payment, success);
        return success;
    }

    public static AccountManager buildBank() {
        try {
            DAO dao = SQLiteDB.createDAO();
            BankHistory history = new BankHistory(dao);
            AuthenticationManager am = new AuthenticationManager(dao, history);
            AccountManager aManager = new AccountManager();
            InterestOperator io = new InterestOperator(dao, aManager);
            aManager.dao = dao;
            aManager.auth = am;
            aManager.history = history;
            aManager.interestOperator = io;
            return aManager;
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean logIn(String userName, char[] password) throws UserUnnkownOrBadPasswordException, SQLException {
        loggedUser =  auth.logIn(userName, password);
        return loggedUser!=null;
    }

    public boolean logOut(User user) throws SQLException {
        if (auth.logOut(user)) {
            loggedUser = null;
            return true;
        }
        return false;
    }

    public User getLoggedUser() {
        return loggedUser;
    }
}
