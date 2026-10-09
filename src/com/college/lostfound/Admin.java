package com.college.lostfound;
public class Admin extends User {
    public Admin() { super(0, "Admin"); }
    @Override public void showProfile() { System.out.println("Admin (ID 0)"); }
}
