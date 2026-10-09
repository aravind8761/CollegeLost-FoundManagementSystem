package com.college.lostfound;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;


public class FileManager {
    private static final Path DATA_FOLDER = Paths.get("data");
    private static final Path STUDENTS_FILE = DATA_FOLDER.resolve("students.txt");
    private static final Path REPORTS_FILE = DATA_FOLDER.resolve("reports.txt");
    private static final Path REPORT_FILE = DATA_FOLDER.resolve("report.txt");

    // ---------- saving: temp file first, then replace the original ----------
    private static void writeLines(Path file, List<String> lines) throws IOException {
        Files.createDirectories(DATA_FOLDER);
        Path temp = DATA_FOLDER.resolve(file.getFileName() + ".tmp");
        try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            Files.deleteIfExists(temp);
            throw e;
        }
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void saveStudents(List<Student> students) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Student s : students) {
            lines.add(s.getId() + "|" + s.getName() + "|" + s.getDepartment());
        }
        writeLines(STUDENTS_FILE, lines);
    }

    public static void saveReports(List<ItemReport> reports) throws IOException {
        List<String> lines = new ArrayList<>();
        for (ItemReport r : reports) {
            lines.add(r.getId() + "|" + r.getStudentId() + "|" + r.getType() + "|"
                    + r.getCategory() + "|" + r.getDescription() + "|"
                    + r.getStatus() + "|" + r.getClosureNote());
        }
        writeLines(REPORTS_FILE, lines);
    }

    public static void saveReport(String summary) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String line : summary.split("\n")) {
            lines.add(line);
        }
        writeLines(REPORT_FILE, lines);
    }

    // ---------- loading ----------
    private static IOException error(String file, int lineNumber, String message) {
        return new IOException(file + " line " + lineNumber + ": " + message);
    }

    private static IOException readError(String file, IOException e) {
        if (e.getMessage() != null && e.getMessage().startsWith(file)) return e;
        return new IOException(file + " could not be read (" + e + ")");
    }

    private static int parseId(String text, String file, int lineNumber) throws IOException {
        int id;
        try {
            id = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw error(file, lineNumber, "ID is not a number");
        }
        if (id <= 0) throw error(file, lineNumber, "ID must be positive");
        return id;
    }

    private static boolean isOneOf(String[] choices, String value) {
        for (String choice : choices) {
            if (choice.equals(value)) return true;
        }
        return false;
    }

    public static ArrayList<Student> loadStudents() throws IOException {
        Files.createDirectories(DATA_FOLDER);
        ArrayList<Student> students = new ArrayList<>();
        if (!Files.exists(STUDENTS_FILE)) return students;   // first run: empty list

        try (BufferedReader reader = Files.newBufferedReader(STUDENTS_FILE, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] fields = line.split("\\|", -1);
                if (fields.length != 3) throw error("students.txt", lineNumber, "expected 3 fields");
                int id = parseId(fields[0], "students.txt", lineNumber);
                for (Student s : students) {
                    if (s.getId() == id) throw error("students.txt", lineNumber, "duplicate ID");
                }
                if (fields[1].isBlank() || fields[1].length() > 200
                        || fields[2].isBlank() || fields[2].length() > 200) {
                    throw error("students.txt", lineNumber, "name and department must be 1-200 characters");
                }
                students.add(new Student(id, fields[1], fields[2]));
            }
        } catch (IOException e) {
            throw readError("students.txt", e);
        }
        return students;
    }

    public static ArrayList<ItemReport> loadReports(List<Student> students) throws IOException {
        ArrayList<ItemReport> reports = new ArrayList<>();
        if (!Files.exists(REPORTS_FILE)) return reports;     // first run: empty list

        try (BufferedReader reader = Files.newBufferedReader(REPORTS_FILE, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String[] fields = line.split("\\|", -1);     // -1 keeps an empty last field
                if (fields.length != 7) throw error("reports.txt", lineNumber, "expected 7 fields");

                int id = parseId(fields[0], "reports.txt", lineNumber);
                int studentId = parseId(fields[1], "reports.txt", lineNumber);
                for (ItemReport r : reports) {
                    if (r.getId() == id) throw error("reports.txt", lineNumber, "duplicate report ID");
                }
                boolean studentExists = false;
                for (Student s : students) {
                    if (s.getId() == studentId) studentExists = true;
                }
                if (!studentExists) throw error("reports.txt", lineNumber, "unknown student ID " + studentId);

                if (!isOneOf(Main.types, fields[2])) throw error("reports.txt", lineNumber, "invalid type");
                if (!isOneOf(Main.categories, fields[3])) throw error("reports.txt", lineNumber, "invalid category");
                if (fields[4].isBlank() || fields[4].length() > 200) {
                    throw error("reports.txt", lineNumber, "description must be 1-200 characters");
                }

                ReportStatus status;
                try {
                    status = ReportStatus.valueOf(fields[5]);
                } catch (IllegalArgumentException e) {
                    throw error("reports.txt", lineNumber, "invalid status");
                }
                String closureNote = fields[6];
                if (closureNote.length() > 200) throw error("reports.txt", lineNumber, "closure note too long");
                if (status == ReportStatus.OPEN && !closureNote.isEmpty()) {
                    throw error("reports.txt", lineNumber, "OPEN report must have an empty note");
                }
                if (status == ReportStatus.CLOSED && closureNote.isBlank()) {
                    throw error("reports.txt", lineNumber, "CLOSED report needs a note");
                }
                reports.add(new ItemReport(id, studentId, fields[2], fields[3],
                                           fields[4], status, closureNote));
            }
        } catch (IOException e) {
            throw readError("reports.txt", e);
        }
        return reports;
    }
}
