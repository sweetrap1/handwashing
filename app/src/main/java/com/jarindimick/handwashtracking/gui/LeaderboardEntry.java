package com.jarindimick.handwashtracking.gui;

// Define a simple data class to represent an employee's leaderboard entry
public class LeaderboardEntry {
    String employeeNumber;
    String employeeName;
    int handwashCount;

    public LeaderboardEntry(String employeeNumber, String employeeName, int handwashCount) {
        this.employeeNumber = employeeNumber;
        this.employeeName = employeeName;
        this.handwashCount = handwashCount;
    }
}
