package com.college.lostfound;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;



public class LostFoundService {
    private ArrayList<Student> students = new ArrayList<>();
    private ArrayList<ItemReport> reports = new ArrayList<>();
    private int nextStudentId = 1;
    private int nextReportId = 1;

    // Load into temporary lists; accept the data only when every record is valid.
    public void load() throws IOException {
        ArrayList<Student> tempStudents = FileManager.loadStudents();
        ArrayList<ItemReport> tempReports = FileManager.loadReports(tempStudents);
        students = tempStudents;
        reports = tempReports;

        int maxStudentId = 0;
        for (Student s : students) {
            if (s.getId() > maxStudentId) maxStudentId = s.getId();
        }
        int maxReportId = 0;
        for (ItemReport r : reports) {
            if (r.getId() > maxReportId) maxReportId = r.getId();
        }
        nextStudentId = maxStudentId + 1;   // empty list starts at 1
        nextReportId = maxReportId + 1;
    }

    public ArrayList<ItemReport> getReports() { return reports; }

    public Student findStudent(int id) {
        for (Student s : students) {
            if (s.getId() == id) return s;
        }
        return null;
    }

    public ItemReport findReport(int id) {
        for (ItemReport r : reports) {
            if (r.getId() == id) return r;
        }
        return null;
    }

    public Student registerStudent(String name, String department) throws IOException {
        Student student = new Student(nextStudentId, name, department);
        students.add(student);
        try {
            FileManager.saveStudents(students);
        } catch (IOException e) {
            students.remove(student);          // cancel the change in memory
            throw e;
        }
        nextStudentId++;
        return student;
    }

    public ItemReport submitReport(int studentId, String type, String category,
                                   String description) throws IOException {
        ItemReport report = new ItemReport(nextReportId, studentId, type, category,
                                           description, ReportStatus.OPEN, "");
        reports.add(report);
        try {
            FileManager.saveReports(reports);
        } catch (IOException e) {
            reports.remove(report);            // cancel the change in memory
            throw e;
        }
        nextReportId++;
        return report;
    }

    public ArrayList<ItemReport> searchDescriptions(String keyword) {
        String key = keyword.toLowerCase(Locale.ROOT);
        ArrayList<ItemReport> found = new ArrayList<>();
        for (ItemReport r : reports) {
            if (r.getDescription().toLowerCase(Locale.ROOT).contains(key)) {
                found.add(r);
            }
        }
        return found;
    }

    public ArrayList<ItemReport> reportsOfStudent(int studentId) {
        ArrayList<ItemReport> mine = new ArrayList<>();
        for (ItemReport r : reports) {
            if (r.getStudentId() == studentId) mine.add(r);
        }
        return mine;
    }

    public void closeReport(ItemReport report, String note) throws IOException {
        int index = reports.indexOf(report);
        report.close(note);
        try {
            FileManager.saveReports(reports);
        } catch (IOException e) {
            // cancel the closure: put back an OPEN copy with an empty note
            reports.set(index, new ItemReport(report.getId(), report.getStudentId(),
                    report.getType(), report.getCategory(), report.getDescription(),
                    ReportStatus.OPEN, ""));
            throw e;
        }
    }

    // Prints the summary and returns the text so Main can offer to save it.
    public String showSummary() {
        int openLost = 0, openFound = 0, closed = 0;
        HashSet<String> reportedCategories = new HashSet<>();
        for (ItemReport r : reports) {
            reportedCategories.add(r.getCategory());
            if (r.getStatus() == ReportStatus.CLOSED) {
                closed++;
            } else if (r.getType().equals("Lost")) {
                openLost++;
            } else {
                openFound++;
            }
        }
        String summary = "Total reports: " + reports.size() + "\n"
                + "OPEN Lost reports: " + openLost + "\n"
                + "OPEN Found reports: " + openFound + "\n"
                + "CLOSED reports: " + closed + "\n"
                + "Distinct categories: " + reportedCategories.size();
        System.out.println(summary);
        return summary;
    }
}
