package biz;

import db.dao.DAO;
import model.Account;
import model.Operation;
import model.User;
import model.operations.LogIn;
import model.operations.LogOut;

import java.sql.SQLException;

/**
 * Created by Krzysztof Podlaski on 04.03.2018.
 */
public class BankHistory {
    DAO dao;

    public BankHistory(DAO dao) {
        this.dao = dao;
    }

    public void logLoginSuccess(User user) throws SQLException {
        Operation o = new LogIn(user,"Logowanie ");
        logOperation(o,true);
    }

    public void logLoginFailure(User user, String info) throws SQLException {
        Operation o = new LogIn(null,info);
        logOperation(o,false);
    }

    /*
    Jeśli user == null to powinniśy mieć false, w przeciwnym wypadku true
     */
    public void logLogOut(User user) throws SQLException {
        Operation o = new LogOut(user,"Logowanie ");
        logOperation(o,user != null);
    }

    public void logPaymentIn(Account account, double ammount, boolean success) {
        throw new RuntimeException("Not implemented");
    }

    public void logPaymentOut(Account account, double ammount, boolean success) {
        throw new RuntimeException("Not implemented");
    }

    public void logOperation(Operation operation, boolean success) throws SQLException {

        dao.logOperation(operation,success);
    }

    public void logUnauthorizedOperation(Operation operation, boolean success) {

        throw new RuntimeException("Not implemented");
    }
}
