package com.jarindimick.handwashtracking.gui;

public class LeaderboardEntry {
    String employeeNumber;
    String employeeName; // This will effectively store the first name
    String lastName;     // NEW field
    int handwashCount;

    public LeaderboardEntry(String employeeNumber, String employeeName, String lastName, int handwashCount) { // Constructor updated
        this.employeeNumber = employeeNumber;
        this.employeeName = employeeName; // This is the first name
        this.lastName = lastName;         // Store the last name
        this.handwashCount = handwashCount;
    }
}