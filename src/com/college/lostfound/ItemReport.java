package com.college.lostfound;

public class ItemReport {
    private int id, studentId;
    private String type, category, description, closureNote;
    private ReportStatus status;

    public ItemReport(int id, int studentId, String type, String category,
                      String description, ReportStatus status, String closureNote) {
        this.id = id;
        this.studentId = studentId;
        this.type = type;
        this.category = category;
        this.description = description;
        this.status = status;
        this.closureNote = closureNote;
    }

    public int getId() { return id; }
    public int getStudentId() { return studentId; }
    public String getType() { return type; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public ReportStatus getStatus() { return status; }
    public String getClosureNote() { return closureNote; }

    // Status and note change together; there is no reopen method.
    public void close(String note) {
        if (status == ReportStatus.CLOSED) {
            throw new IllegalStateException("Report already closed.");
        }
        if (note == null || note.isBlank()) {
            throw new IllegalArgumentException("Closure note required.");
        }
        closureNote = note;
        status = ReportStatus.CLOSED;
    }

    @Override
    public String toString() {
        return id + " | " + type + " | " + description + " | " + status;
    }
}
