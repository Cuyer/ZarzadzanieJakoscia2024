package model;

/**
 * Created by Krzysztof Podlaski on 04.03.2018.
 */
public class Account {
    private int id;
    private double ammount;
    private User owner;

    public boolean income(double ammount){
        this.ammount+=ammount;
        return true;
    }

    /*
    Brak sprawdzenia, czy ammount jest > 0
     */

    public boolean outcome(double ammount){
        if (this.ammount<ammount) return false;
        this.ammount-=ammount;
        return true;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getAmmount() {
        return ammount;
    }

    public void setAmmount(double ammount) {
        this.ammount = ammount;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }
}
