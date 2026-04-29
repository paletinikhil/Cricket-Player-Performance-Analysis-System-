package model;

public abstract class Person {

    protected int id;
    protected String name;
    protected String username;
    protected String password;

    public Person(int id, String name, String username, String password) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.password = password;
    }

    public String getName() {
        return name;
    }
}
