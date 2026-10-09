package com.college.lostfound;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    static String[] types = {"Lost", "Found"};
    static String[] categories = {"Books", "ID Cards", "Electronics", "Other"};

    static Scanner scanner = new Scanner(System.in);
    static LostFoundService service = new LostFoundService();
    static boolean echo = false;   // --echo prints typed input (used for demo output)

    static String readLine(String prompt) {
        System.out.print(prompt);
        String text = scanner.nextLine();
        if (echo) System.out.println(text);
        return text.trim();
    }

    static int readNumber(String prompt, int min, int max) {
        while (true) {
            try {
                int number = Integer.parseInt(readLine(prompt));
                if (number >= min && number <= max) return number;
            } catch (NumberFormatException e) {
                // fall through to the error message
            }
            System.out.println("Invalid input. Enter a number from " + min + " to " + max + ".");
        }
    }

    static String readText(String prompt, int maxLength) {
        while (true) {
            String text = readLine(prompt);
            if (text.isEmpty()) {
                System.out.println("This field cannot be empty.");
            } else if (text.length() > maxLength) {
                System.out.println("Maximum " + maxLength + " characters.");
            } else if (text.contains("|")) {
                System.out.println("The | character is not allowed.");
            } else {
                return text;
            }
        }
    }

    public static void main(String[] args) {
        echo = args.length > 0 && args[0].equals("--echo");
        try {
            service.load();
        } catch (IOException e) {
            System.out.println("FILE ERROR: " + e.getMessage());
            System.out.println("Fix the file and restart.");
            return;
        }

        while (true) {
            System.out.println("\nMAIN MENU");
            System.out.println("1. Register student");
            System.out.println("2. Admin menu");
            System.out.println("3. Student menu");
            System.out.println("0. Exit");
            int choice = readNumber("Choice: ", 0, 3);
            if (choice == 0) {
                System.out.println("Goodbye.");
                break;
            } else if (choice == 1) {
                registerStudent();
            } else if (choice == 2) {
                showAdminMenu();
            } else {
                showStudentMenu();
            }
        }
    }

    static void registerStudent() {
        String name = readText("Name: ", 200);
        String department = readText("Department: ", 200);
        try {
            Student student = service.registerStudent(name, department);
            System.out.println("Registered. Your student ID is " + student.getId());
        } catch (IOException e) {
            System.out.println("Save failed: " + e.getMessage());
        }
    }

    static void showAdminMenu() {
        User user = new Admin();
        user.showProfile();
        while (true) {
            System.out.println("\nADMIN MENU");
            System.out.println("1. View all item reports");
            System.out.println("2. Search item descriptions");
            System.out.println("3. Close report");
            System.out.println("4. View summary reports");
            System.out.println("0. Back");
            int choice = readNumber("Choice: ", 0, 4);
            if (choice == 0) {
                return;
            } else if (choice == 1) {
                printReports(service.getReports());
            } else if (choice == 2) {
                String keyword = readText("Keyword: ", 200);
                printReports(service.searchDescriptions(keyword));
            } else if (choice == 3) {
                closeReport();
            } else {
                showSummary();
            }
        }
    }

    static void printReports(ArrayList<ItemReport> list) {
        if (list.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (ItemReport report : list) {
            System.out.println(report);
        }
    }

    static void closeReport() {
        if (service.getReports().isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        ItemReport report;
        while (true) {
            int reportId = readNumber("Report ID: ", 1, Integer.MAX_VALUE);
            report = service.findReport(reportId);
            if (report != null) break;
            System.out.println("Unknown report ID. Try again.");
        }
        if (report.getStatus() != ReportStatus.OPEN) {
            System.out.println("Report " + report.getId() + " is already CLOSED. Closure refused.");
            return;
        }
        String note = readText("Closure note: ", 200);
        try {
            service.closeReport(report, note);
            System.out.println("Report saved. Status: CLOSED");
        } catch (IOException e) {
            System.out.println("Save failed: " + e.getMessage());
        }
    }

    static void showSummary() {
        String summary = service.showSummary();
        int save = readNumber("Save report? 1. Yes  2. No: ", 1, 2);
        if (save == 1) {
            try {
                FileManager.saveReport(summary);
                System.out.println("Saved to data/report.txt");
            } catch (IOException e) {
                System.out.println("Save failed: " + e.getMessage());
            }
        }
    }

    static void showStudentMenu() {
        Student student;
        while (true) {
            int studentId = readNumber("Enter student ID (0 to go back): ", 0, Integer.MAX_VALUE);
            if (studentId == 0) return;
            student = service.findStudent(studentId);
            if (student != null) break;
            System.out.println("Unknown student ID. Try again.");
        }
        User user = student;
        user.showProfile();
        while (true) {
            System.out.println("\nSTUDENT MENU");
            System.out.println("1. Submit item report");
            System.out.println("2. View my reports");
            System.out.println("0. Back");
            int choice = readNumber("Choice: ", 0, 2);
            if (choice == 0) {
                return;
            } else if (choice == 1) {
                submitReport(student);
            } else {
                viewMyReports(student);
            }
        }
    }

    static void submitReport(Student student) {
        System.out.println("Type: 1. Lost  2. Found");
        int typeChoice = readNumber("Choice: ", 1, types.length);
        System.out.println("Category:");
        for (int i = 0; i < categories.length; i++) {
            System.out.println((i + 1) + ". " + categories[i]);
        }
        int categoryChoice = readNumber("Choice: ", 1, categories.length);
        String description = readText("Item description: ", 200);

        String type = types[typeChoice - 1];
        String category = categories[categoryChoice - 1];
        try {
            ItemReport report = service.submitReport(student.getId(), type, category, description);
            System.out.println("Report " + report.getId() + " saved. Status: OPEN");
        } catch (IOException e) {
            System.out.println("Save failed: " + e.getMessage());
        }
    }

    static void viewMyReports(Student student) {
        ArrayList<ItemReport> mine = service.reportsOfStudent(student.getId());
        if (mine.isEmpty()) {
            System.out.println("No records found.");
            return;
        }
        for (ItemReport report : mine) {
            System.out.println(report);
            if (report.getStatus() == ReportStatus.CLOSED) {
                System.out.println("   Closure note: " + report.getClosureNote());
            }
        }
    }
}
