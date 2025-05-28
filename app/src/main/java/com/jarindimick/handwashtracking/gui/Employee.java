// Make sure this package declaration is correct
package com.jarindimick.handwashtracking.gui;
// Or if you created a models package:
// package com.jarindimick.handwashtracking.models;

public class Employee {
    private long id;
    private String employeeNumber;
    private String firstName;
    private String lastName;
    private String department;
    private boolean isActive;

    public Employee(long id, String employeeNumber, String firstName, String lastName, String department, boolean isActive) {
        this.id = id;
        this.employeeNumber = employeeNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.department = department;
        this.isActive = isActive;
    }

    // Getters
    public long getId() { return id; }
    public String getEmployeeNumber() { return employeeNumber; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName).trim(); }
    public String getDepartment() { return department; }
    public boolean isActive() { return isActive; }

    // Setters
    public void setId(long id) { this.id = id; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setDepartment(String department) { this.department = department; }
    public void setActive(boolean active) { isActive = active; }
}