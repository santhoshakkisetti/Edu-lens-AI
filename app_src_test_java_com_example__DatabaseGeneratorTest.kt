package com.example

import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter

class DatabaseGeneratorTest {

    data class TableColumn(
        val name: String,
        val dataType: String,
        val required: String,
        val defaultValue: String,
        val primaryKey: String,
        val foreignKey: String,
        val description: String,
        val exampleValue: String
    )

    data class TableDesign(
        val name: String,
        val description: String,
        val columns: List<TableColumn>
    )

    @Test
    fun generateDatabaseDesign() {
        val tables = getTablesSpecification()
        
        // Find workspace root dynamically
        var currentDir = File(".").absoluteFile
        while (currentDir.parentFile != null && !File(currentDir, "settings.gradle.kts").exists()) {
            currentDir = currentDir.parentFile
        }
        val workspaceRoot = if (File(currentDir, "settings.gradle.kts").exists()) currentDir else File(".")
        
        val outputDir = File(workspaceRoot, "database_design")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val csvDir = File(outputDir, "csv")
        if (!csvDir.exists()) {
            csvDir.mkdirs()
        }

        // 2. Generate CSV Files
        generateCsvFiles(tables, csvDir, outputDir)

        // 3. Generate XLSX Workbook
        generateXlsxWorkbook(tables, File(outputDir, "EduLens_AI_Database_Design.xlsx"))
        
        println("SUCCESS: Database design assets successfully generated in ${outputDir.absolutePath}!")
    }

    private fun generateCsvFiles(tables: List<TableDesign>, csvDir: File, outputDir: File) {
        // Individual table CSVs
        for (table in tables) {
            val file = File(csvDir, "${table.name.replace(" ", "_")}.csv")
            FileWriter(file).use { writer ->
                // Write Header
                writer.write("Column Name,Data Type,Required (Yes/No),Default Value,Primary Key,Foreign Key,Description,Example Value\n")
                // Write Columns
                for (col in table.columns) {
                    val row = listOf(
                        escapeCsv(col.name),
                        escapeCsv(col.dataType),
                        escapeCsv(col.required),
                        escapeCsv(col.defaultValue),
                        escapeCsv(col.primaryKey),
                        escapeCsv(col.foreignKey),
                        escapeCsv(col.description),
                        escapeCsv(col.exampleValue)
                    ).joinToString(",")
                    writer.write(row + "\n")
                }
            }
        }

        // Documentation CSV
        val docFile = File(csvDir, "Documentation.csv")
        FileWriter(docFile).use { writer ->
            writer.write("Section,Content\n")
            writer.write("Architectural Overview,\"EduLens AI is an advanced AI-powered learning application providing smart notes, automated quiz generation, conversational AI doubts solving, and spaced-repetition flashcards.\"\n")
            writer.write("Relationships,\"A comprehensive 1:N relational map exists from Users to Study History, Notes, Flashcards, Quiz Results, Achievements, Notifications, Progress, Settings, Conversations, Bookmarks, and Study Goals.\"\n")
            writer.write("SQL Equivalent Schema,\"Available in SQL DDL file or inside the XLSX workbook.\"\n")
            writer.write("Firestore Design,\"Available in JSON format inside the XLSX workbook.\"\n")
            writer.write("Index Recommendations,\"Primary, Composite, and Full-Text index strategies are documented to optimize querying speeds.\"\n")
        }
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\"", "\"\"")
        return if (clean.contains(",") || clean.contains("\n") || clean.contains("\"")) {
            "\"$clean\""
        } else {
            clean
        }
    }

    private fun generateXlsxWorkbook(tables: List<TableDesign>, xlsxFile: File) {
        val wb = XSSFWorkbook()

        // Styles
        val headerFont = wb.createFont().apply {
            bold = true
            color = IndexedColors.WHITE.index
            fontHeightInPoints = 11.toShort()
        }

        val headerStyle = wb.createCellStyle().apply {
            setFont(headerFont)
            fillForegroundColor = IndexedColors.BLUE_GREY.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
            borderBottom = BorderStyle.MEDIUM
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }

        val cellStyle = wb.createCellStyle().apply {
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            verticalAlignment = VerticalAlignment.CENTER
        }

        val titleFont = wb.createFont().apply {
            bold = true
            fontHeightInPoints = 16.toShort()
            color = IndexedColors.DARK_BLUE.index
        }

        val sectionFont = wb.createFont().apply {
            bold = true
            fontHeightInPoints = 13.toShort()
            color = IndexedColors.BLUE_GREY.index
        }

        // --- SHEET 1: DOCUMENTATION ---
        val docSheet = wb.createSheet("Documentation")
        docSheet.isDisplayGridlines = true

        var docRowIdx = 0
        
        // Title Row
        val r0 = docSheet.createRow(docRowIdx++)
        val c0 = r0.createCell(0)
        c0.setCellValue("EduLens AI - Complete Database Design & Architecture")
        val titleStyle = wb.createCellStyle().apply { setFont(titleFont) }
        c0.cellStyle = titleStyle
        
        docRowIdx++ // blank row

        // Helper to write documentation block
        fun writeDocBlock(sectionTitle: String, contentLines: List<String>) {
            val rSec = docSheet.createRow(docRowIdx++)
            val cSec = rSec.createCell(0)
            cSec.setCellValue(sectionTitle)
            val secStyle = wb.createCellStyle().apply { setFont(sectionFont) }
            cSec.cellStyle = secStyle

            for (line in contentLines) {
                val row = docSheet.createRow(docRowIdx++)
                val cell = row.createCell(1) // Indented to column B
                cell.setCellValue(line)
                cell.cellStyle = wb.createCellStyle().apply {
                    wrapText = true
                    verticalAlignment = VerticalAlignment.CENTER
                }
            }
            docRowIdx++ // blank spacer
        }

        writeDocBlock("1. System Overview", listOf(
            "EduLens AI is a modern, responsive, and robust educational ecosystem powered by generative AI.",
            "This document presents the complete production database architectural blueprint for the platform.",
            "It outlines a multi-engine persistence strategy combining Relational Databases (SQL) for transactional",
            "integrity (Users, Progress, Quizzes) and NoSQL (Firestore) for fast real-time synchronization,",
            "offline support, and flexible message-payload storage."
        ))

        writeDocBlock("2. Table Relationships (Relational Map)", listOf(
            "• Users (1) -> Study History (N) [Cascade Delete]",
            "• Users (1) -> Notes (N) [Cascade Delete]",
            "• Users (1) -> Flashcards (N) [Cascade Delete]",
            "• Users (1) -> Quiz Results (N) [Cascade Delete]",
            "• Users (1) -> Notifications (N) [Cascade Delete]",
            "• Users (1) -> User Progress (N) [Cascade Delete]",
            "• Users (1) -> Settings (1) [Cascade Delete]",
            "• Users (1) -> AI Conversations (N) [Cascade Delete]",
            "• Users (1) -> Bookmarks (N) [Cascade Delete]",
            "• Users (1) -> Study Goals (N) [Cascade Delete]",
            "• Subjects (1) -> Chapters (N) [Restrict Delete]",
            "• Chapters (1) -> Notes (N) [Set Null on Delete]",
            "• Chapters (1) -> Flashcards (N) [Set Null on Delete]",
            "• Chapters (1) -> Quizzes (N) [Set Null on Delete]",
            "• Quizzes (1) -> Quiz Results (N) [Cascade Delete]"
        ))

        writeDocBlock("3. Entity-Relationship Diagram (ERD Context Map)", listOf(
            "                        +---------------+",
            "                        |     Users     |",
            "                        +---------------+",
            "                           |   |   |   |",
            "        +------------------+   |   |   +-------------------+",
            "        |                      |   |                       |",
            "        v                      v   v                       v",
            "+---------------+      +---------------+       +---------------+",
            "|   Settings    |      |  Study Goals  |       |   Progress    |",
            "+---------------+      +---------------+       +---------------+",
            "        |                      |",
            "        v                      v",
            "+---------------+      +---------------+",
            "| Notifications |      | Study History |",
            "+---------------+      +---------------+",
            "        |                      |",
            "        v                      v",
            "+---------------+      +---------------+       +---------------+",
            "|     Notes     | <--- |   Chapters    | ----> |  Flashcards   |",
            "+---------------+      +---------------+       +---------------+",
            "                               |",
            "                               v",
            "                       +---------------+       +---------------+",
            "                       |    Quizzes    | ----> | Quiz Results  |",
            "                       +---------------+       +---------------+",
            "                               ^",
            "                               |",
            "                       +---------------+",
            "                       | AI Convers.   |",
            "                       +---------------+"
        ))

        writeDocBlock("4. NoSQL / Firestore Collection Schema Layout", listOf(
            "• users/ {user_id}  <-- Document",
            "  ├── email: string",
            "  ├── first_name: string",
            "  ├── last_name: string",
            "  └── settings/ default (Subcollection)  <-- Config parameters",
            "• subjects/ {subject_id}",
            "  ├── title: string",
            "  ├── category: string",
            "  └── chapters/ {chapter_id} (Subcollection)",
            "      ├── title: string",
            "      └── sequence_number: int",
            "• notes/ {note_id}",
            "  ├── user_id: string (Index)",
            "  ├── chapter_id: string",
            "  ├── title: string",
            "  └── content: string",
            "• flashcards/ {flashcard_id}",
            "  ├── user_id: string (Index)",
            "  ├── front_text: string",
            "  ├── back_text: string",
            "  └── next_review_at: timestamp (Index for spaced repetition)",
            "• conversations/ {conversation_id}",
            "  ├── user_id: string (Index)",
            "  ├── context_type: string",
            "  └── messages/ [ {sender: string, text: string, timestamp: timestamp} ] (Array of Maps)"
        ))

        writeDocBlock("5. Indexing Strategy & Recommendations", listOf(
            "• Primary Keys (UUIDs): Auto-indexed by relational systems (B-Tree).",
            "• Compound Indices (SQL):",
            "  - idx_study_history_user_completed: ON study_history(user_id, completed_at DESC)",
            "  - idx_flashcards_user_review: ON flashcards(user_id, next_review_at ASC)",
            "  - idx_quiz_results_user_percentage: ON quiz_results(user_id, percentage_score DESC)",
            "• Single-Field Indices (NoSQL):",
            "  - conversations: user_id ASC, updated_at DESC",
            "  - flashcards: user_id ASC, next_review_at ASC",
            "  - notifications: user_id ASC, is_read ASC, created_at DESC",
            "• Full-Text Search Indices:",
            "  - PostgreSQL: CREATE INDEX idx_notes_content_fts ON notes USING gin(to_tsvector('english', content))",
            "  - Elasticsearch / Algolia: Recommended syncing of Notes and Subject Titles for instant query response."
        ))

        writeDocBlock("6. Security Rules (Firestore & PostgreSQL Row-Level Security)", listOf(
            "■ FIRESTORE SECURITY RULES:",
            "rules_version = '2';",
            "service cloud.firestore {",
            "  match /databases/{database}/documents {",
            "    match /users/{userId} {",
            "      allow read, write: if request.auth != null && request.auth.uid == userId;",
            "    }",
            "    match /notes/{noteId} {",
            "      allow read: if resource.data.is_public == true || (request.auth != null && request.auth.uid == resource.data.user_id);",
            "      allow write: if request.auth != null && request.auth.uid == request.resource.data.user_id;",
            "    }",
            "    match /flashcards/{cardId} {",
            "      allow read, write: if request.auth != null && request.auth.uid == resource.data.user_id;",
            "    }",
            "  }",
            "}",
            "■ POSTGRESQL ROW-LEVEL SECURITY (RLS):",
            "ALTER TABLE notes ENABLE ROW LEVEL SECURITY;",
            "CREATE POLICY user_notes_policy ON notes",
            "  FOR ALL TO authenticated",
            "  USING (user_id = auth.uid())",
            "  WITH CHECK (user_id = auth.uid());"
        ))

        writeDocBlock("7. SQL Schema Definition (PostgreSQL / MySQL DDL)", listOf(
            "CREATE TABLE users (",
            "  user_id VARCHAR(36) PRIMARY KEY,",
            "  email VARCHAR(255) UNIQUE NOT NULL,",
            "  password_hash VARCHAR(255) NOT NULL,",
            "  first_name VARCHAR(100) NOT NULL,",
            "  last_name VARCHAR(100) NOT NULL,",
            "  avatar_url VARCHAR(2083),",
            "  auth_provider VARCHAR(50) DEFAULT 'local' NOT NULL,",
            "  is_verified BOOLEAN DEFAULT FALSE NOT NULL,",
            "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,",
            "  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL",
            ");",
            "CREATE TABLE subjects (",
            "  subject_id VARCHAR(36) PRIMARY KEY,",
            "  title VARCHAR(100) NOT NULL,",
            "  description VARCHAR(500),",
            "  category VARCHAR(100) DEFAULT 'General' NOT NULL,",
            "  icon_url VARCHAR(2083),",
            "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL",
            ");",
            "CREATE TABLE chapters (",
            "  chapter_id VARCHAR(36) PRIMARY KEY,",
            "  subject_id VARCHAR(36) REFERENCES subjects(subject_id) ON DELETE RESTRICT,",
            "  title VARCHAR(150) NOT NULL,",
            "  sequence_number INT NOT NULL DEFAULT 1,",
            "  content_url VARCHAR(2083),",
            "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL",
            ");",
            "CREATE TABLE study_history (",
            "  history_id VARCHAR(36) PRIMARY KEY,",
            "  user_id VARCHAR(36) REFERENCES users(user_id) ON DELETE CASCADE,",
            "  subject_id VARCHAR(36) REFERENCES subjects(subject_id) ON DELETE CASCADE,",
            "  chapter_id VARCHAR(36) REFERENCES chapters(chapter_id) ON DELETE SET NULL,",
            "  session_type VARCHAR(50) DEFAULT 'reading' NOT NULL,",
            "  duration_seconds INT DEFAULT 0 NOT NULL,",
            "  completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,",
            "  metadata JSON",
            ");"
        ))

        writeDocBlock("8. Data Validation & Business Sanitization Rules", listOf(
            "• Email addresses must match regex validation RFC 5322 (e.g., ^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$).",
            "• Passwords must undergo Argon2id or BCrypt hashing with strength cost factor >= 12 before database commit.",
            "• Durations (seconds), points, total questions, and scores must have constraint checks enforcing positive values >= 0.",
            "• Decimal variables (percentage_score, average_quiz_score) must be bounded strictly between 0.00 and 100.00.",
            "• Sequence numbers of chapters must be greater than or equal to 1 to ensure standard numerical progression order.",
            "• All datetime/timestamp metrics must be stored strictly in Coordinated Universal Time (UTC) to eliminate multi-region offsets."
        ))

        // Set documentation column widths
        docSheet.setColumnWidth(0, 10000)
        docSheet.setColumnWidth(1, 24000)

        // --- SHEETS 2-16: THE 15 TABLES ---
        for (table in tables) {
            val sheet = wb.createSheet(table.name)
            sheet.isDisplayGridlines = true

            // Description Header
            val descRow = sheet.createRow(0)
            val descCell = descRow.createCell(0)
            descCell.setCellValue("Table Name: ${table.name}  |  Description: ${table.description}")
            descCell.cellStyle = wb.createCellStyle().apply {
                setFont(wb.createFont().apply {
                    bold = true
                    color = IndexedColors.DARK_BLUE.index
                })
            }

            // Headers
            val headerRow = sheet.createRow(2)
            val headersList = listOf(
                "Column Name", "Data Type", "Required (Yes/No)", "Default Value", 
                "Primary Key", "Foreign Key", "Description", "Example Value"
            )
            for (i in headersList.indices) {
                val cell = headerRow.createCell(i)
                cell.setCellValue(headersList[i])
                cell.cellStyle = headerStyle
            }

            // Add Columns
            var rowIdx = 3
            for (col in table.columns) {
                val row = sheet.createRow(rowIdx++)
                
                val cName = row.createCell(0).apply { setCellValue(col.name) }
                val cType = row.createCell(1).apply { setCellValue(col.dataType) }
                val cReq = row.createCell(2).apply { setCellValue(col.required) }
                val cDef = row.createCell(3).apply { setCellValue(col.defaultValue) }
                val cPK = row.createCell(4).apply { setCellValue(col.primaryKey) }
                val cFK = row.createCell(5).apply { setCellValue(col.foreignKey) }
                val cDesc = row.createCell(6).apply { setCellValue(col.description) }
                val cEx = row.createCell(7).apply { setCellValue(col.exampleValue) }

                listOf(cName, cType, cReq, cDef, cPK, cFK, cDesc, cEx).forEach { c ->
                    c.cellStyle = cellStyle
                }
            }

            // Enable Filters
            sheet.setAutoFilter(org.apache.poi.ss.util.CellRangeAddress(2, rowIdx - 1, 0, 7))

            // Auto-size Columns
            for (i in 0..7) {
                sheet.autoSizeColumn(i)
                // Add a bit of extra padding to prevent truncation of autofilter buttons
                val currentWidth = sheet.getColumnWidth(i)
                sheet.setColumnWidth(i, currentWidth + 1200)
            }
        }

        // Save Workbook
        FileOutputStream(xlsxFile).use { fos ->
            wb.write(fos)
        }
        wb.close()
    }

    private fun getTablesSpecification(): List<TableDesign> {
        return listOf(
            TableDesign(
                "Users",
                "Core table storing user authentication credentials and basic profiles.",
                listOf(
                    TableColumn("user_id", "VARCHAR(36)", "Yes", "NULL", "Yes", "No", "Unique identifier for the user (UUID)", "usr_9f82a1b3"),
                    TableColumn("email", "VARCHAR(255)", "Yes", "NULL", "No", "No", "User's unique verified email address", "santhosh.akkisetti@gmail.com"),
                    TableColumn("password_hash", "VARCHAR(255)", "Yes", "NULL", "No", "No", "Securely hashed password (BCrypt)", "\$2a\$12\$K3vH8v7a9b0c1d2e3f4g5h..."),
                    TableColumn("first_name", "VARCHAR(100)", "Yes", "NULL", "No", "No", "User's given first name", "Santhosh"),
                    TableColumn("last_name", "VARCHAR(100)", "Yes", "NULL", "No", "No", "User's surname or family name", "Akkisetti"),
                    TableColumn("avatar_url", "VARCHAR(2083)", "No", "NULL", "No", "No", "URL linking to hosted profile avatar", "https://example.com/avatars/user123.png"),
                    TableColumn("auth_provider", "VARCHAR(50)", "Yes", "'local'", "No", "No", "Auth platform (local, google, apple)", "google"),
                    TableColumn("is_verified", "BOOLEAN", "Yes", "FALSE", "No", "No", "Flag indicating if user email is verified", "TRUE"),
                    TableColumn("created_at", "TIMESTAMP", "Yes", "CURRENT_TIMESTAMP", "No", "No", "Timestamp of account creation (UTC)", "2026-06-30 22:28:28"),
                    TableColumn("updated_at", "TIMESTAMP", "Yes", "CURRENT_TIMESTAMP", "No", "No", "Timestamp of last account modification (UTC)", "2026-06-30 22:28:28")
                )
            ),
            TableDesign(
                "Study History",
                "Transaction log documenting user study sessions, types, and duration.",
                listOf(
                    TableColumn("history_id", "VARCHAR(36)", "Yes", "NULL", "Yes", "No", "Unique identifier for study session log (UUID)", "sth_3a8b9c2d"),
                    TableColumn("user_id", "VARCHAR(36)", "Yes", "NULL", "No", "Users.user_id", "Foreign key referencing the studying user", "usr_9f82a1b3"),
                    TableColumn("subject_id", "VARCHAR(36)", "Yes", "NULL", "No", "Subjects.subject_id", "Foreign key referencing the subject studied", "sub_8f2d1c4e"),
                    TableColumn("chapter_id", "VARCHAR(36)", "No", "NULL", "No", "Chapters.chapter_id", "Foreign key referencing specific chapter studied", "chp_5e3f2a1b"),
                    TableColumn("session_type", "VARCHAR(50)", "Yes", "'reading'", "No", "No", "Type of study session (reading, quiz, flashcard, ai_chat)", "quiz"),
                    TableColumn("duration_seconds", "INT", "Yes", "0", "No", "No", "Duration of the study session in seconds", "1200"),
                    TableColumn("completed_at", "TIMESTAMP", "Yes", "CURRENT_TIMESTAMP", "No", "No", "Timestamp when study session ended (UTC)", "2026-06-30 18:30:00"),
                    TableColumn("metadata", "JSON", "No", "NULL", "No", "No", "Flexible JSON log parameters (e.g. read pages)", "{\"pages_read\": [1,2,3,4]}")
                )
            ),
            TableDesign(
                "Notes",
                "User created notes with summaries, linked to chapters.",
                listOf(
                    TableColumn("note_id", "VARCHAR(36)", "Yes", "NULL", "Yes", "No", "Unique identifier for the note (UUID)", "not_7f2b1a4d"),
                    TableColumn("user_id", "VARCHAR(36)", "Yes", "NULL", "No", "Users.user_id", "Foreign key referencing note creator", "usr_9f82a1b3"),
                    TableColumn("chapter_id", "VARCHAR(36)", "No", "NULL", "No", "Chapters.chapter_id", "Foreign key referencing associated chapter", "chp_5e3f2a1b"),
                    TableColumn("title", "VARCHAR(255)", "Yes", "NULL", "No", "No", "Title of the note", "Quantum Computing Basics"),
                    TableColumn("content", "TEXT", "Yes", "NULL", "No", "No", "Main markdown content text of the note", "Quantum mechanics is a fundamental theory..."),
                    TableColumn("ai_summary", "TEXT", "No", "NULL", "No", "No", "AI-generated rich text summary of note body", "Summarizes key quantum principles..."),
                    TableColumn("is_public", "BOOLEAN", "Yes", "FALSE", "No", "No", "Flag indicating if note is shared publicly", "FALSE"),
                    TableColumn("created_at", "TIMESTAMP", "Yes", "CURRENT_TIMESTAMP", "No", "No", "Timestamp when note was created", "2026-06-30 10:15:00"),
                    TableColumn("updated_at", "TIMESTAMP", "Yes", "CURRENT_TIMESTAMP", "No", "No", "Timestamp of last note update", "2026-06-30 11:20:00")
                )
            ),
            TableDesign(
                "Flashcards",
                "Spaced repetition cards linked to study chapters for quick reviewing.",
                listOf(
                    TableColumn("flashcard_id", "VARCHAR(36)", "Yes", "NULL", "Yes", "No", "Unique identifier for the flashcard (UUID)", "fls_8b2c1d4e"),
                    TableColumn("user_id", "VARCHAR(36)", "Yes", "NULL", "No", "Users.user_id", "Foreign key referencing card owner", "usr_9f82a1b3"),
                    TableColumn("chapter_id", "VARCHAR(36)", "No", "NULL", "No", "Chapters.chapter_id", "Foreign key referencing associated chapter", "chp_5e3f2a1b"),
                    TableColumn("front_text", "TEXT", "Yes", "NULL", "No", "No", "Text of the question displayed on the front", "What is Superposition?"),
                    TableColumn("back_text", "TEXT", "Yes", "NULL", "No", "No", "Text of the answer displayed on the back", "A principle where a system can exist in multiple states..."),
                    TableColumn("ease_factor", "FLOAT", "Yes", "2.5", "No", "No", "SM-2 spaced learning dynamic difficulty factor", "2.6"),
                    TableColumn("interval_days", "INT", "Yes", "0", "No", "No", "Days to wait before scheduled review according to SM-2", "4"),
                    TableColumn("repetitions", "INT", "Yes", "0", "No", "No", "Number of consecutive successful recall cycles", "2"),
                    TableColumn("next_review_at", "TIMESTAMP", "Yes", "CURRENT_TIMESTAMP", "No", "No", "Calculated timestamp for next card review (UTC)", "2026-07-04 09:00:00"),
                    TableColumn("created_at", "TIMESTAMP", "Yes", "CURRENT_TIMESTAMP", "No", "No", "Timestamp when card was created", "2026-06-28 14:00:00")
                )
            ),
            TableDesign(
                "Quizzes",
                "Exams and micro-quizzes created by AI or manually.",
                listOf(
                    TableColumn("quiz_id", "VARCHAR(36)", "Yes", "NULL", "Yes", "No", "Unique identifier for the quiz (UUID)", "qiz_2e3f4a1b"),
                    TableColumn("creator_id", "VARCHAR(36)", "No", "NULL", "No", "Users.user_id", "Foreign key referencing quiz author (NULL if system-generated)", "usr_9f82a1b3"),
                    TableColumn("chapter_id", "VARCHAR(36)", "No", "NULL", "No", "Chapters.chapter_id", "Foreign key referencing associated chapter", "chp_5e3f2a1b"),
                    TableColumn("title", "VARCHAR(255)", "Yes", "NULL", "No", "No", "Title of the quiz", "Midterm Physics Quiz 1"),
                    TableColumn("quiz_type", "VARCHAR(50)", "Yes", "'system_generated'", "No", "No", "Type of quiz (ai_generated, manual_creation, system_generated)", "ai_generated"),
                    TableColumn("total_questions", "INT", "Yes", "0", "No", "No", "Count of questions contained in this quiz", "10"),
                    TableColumn("time_limit_minutes", "INT", "No", "NULL", "No", "No", "Duration limit in minutes (NULL if untimed)", "15"),
                    TableColumn("questions_data", "JSON", "Yes", "NULL", "No", "No", "JSON containing questions array, options, and key", "[{\"q_id\": 1, \"text\": \"Solve 2+2\", \"options\": [\"3\",\"4\"], \"correct\": \"4\"}]"),
                    TableColumn("created_at", "TIMESTAMP", "Yes", "CURRENT_TIMESTAMP", "No", "No", "Timestamp when quiz was created", "2026-06-30 15:00:00")
                )
            ),
            TableDesign(
                "Quiz Results",
                "Summary and metrics of user attempts and scores in quizzes.",
                listOf(
                    TableColumn("result_id", "VARCHAR(36)", "Yes", "NULL", "Yes", "No", "Unique identifier for result record (UUID)", "qrs_9a8b7c6d"),
                    TableColumn("user_id", "VARCHAR(36)", "Yes", "NULL", "No", "Users.user_id", "Foreign key referencing candidate user", "usr_9f82a1b3"),
                    TableColumn("quiz_id", "VARCHAR(36)", "Yes", "NULL", "No", "Quizzes.quiz_id", "Foreign key referencing the quiz completed", "qiz_2e3f4a1b"),
                    TableColumn("score_obtained", "INT", "Yes", "NULL", "No", "No", "Total raw score achieved by candidate", "8"),
                    TableColumn("percentage_score", "DECIMAL(5,2)", "Yes", "NULL", "No", "No", "Calculated percentage correct score", "80.00"),
                    TableColumn("time_taken_seconds", "INT", "Yes", "NULL", "No", "No", "Completion duration in seconds", "450"),
                    TableColumn("answers_data", "JSON", "Yes", "NULL", "No", "No", "JSON details mapping submitted choices vs correct key", "{\"1\": \"4\", \"2\": \"5\"}"),
                    TableColumn("completed_at", "TIMESTAMP", "Yes", "CURRENT_TIMESTAMP", "No", "No", "Timestamp when attempt was submitted", "2026-06-30 15:15:00")
                )
            ),
            TableDesign(
                "Subjects",
                "Top-level modules of learning categorization.",
                listOf(
                    TableColumn("subject_id", "VARCHAR(36)", "Yes", "NULL", "Yes", "No", "Unique identifier for the subject (UUID)", "sub_8f2d1c4e"),
                    TableColumn("title", "VARCHAR(100)", "Yes", "NULL", "No", "No", "Header name of the subject", "Computer Science"),
                    TableColumn("description", "VARCHAR(500)", "No", "NULL", "No", "No", "Short description of subject content and scope", "Core computer science principles including algorithms and data structures."),
                    TableColumn("category", "VARCHAR(100)", "Yes", "'General'", "No", "No", "Educational categorization class", "Engineering"),
                    TableColumn("icon_url", "VARCHAR(2083)", "No", "NULL", "No", "No", "URL linking to subject's vector asset", "https://example.com/icons/cs.svg"),
                    TableColumn("created_at", "TIMESTAMP", "Yes", "CURRENT_TIMESTAMP", "No", "No", "Timestamp when subject was initialized", "2026-06-25 08:00:00")
                )
            ),
            TableDesign(
                "Chapters",
                "Detailed submodules or segments under parent subjects.",
                listOf(
                    TableColumn("chapter_id", "VARCHAR(36)", "Yes", "NULL", "Yes", "No", "Unique identifier for the chapter (UUID)", "chp_5e3f2a1b"),
                    TableColumn("subject_id", "VARCHAR(36)", "Yes", "NULL", "No", "Subjects.subject_id", "Foreign key linking to parent subject module", "sub_8f2d1c4e"),
                    TableColumn("title", "VARCHAR(150)", "Yes", "NULL", "No", "No", "Title description of the chapter", "Quantum Algorithms"),
                    TableColumn("sequence_number", "INT", "Yes", "1", "No", "No", "Sort order value under parent subject sequence", "3"),
                    TableColumn("content_url", "VARCHAR(2083)", "No", "NULL", "No", "No", "URL linking to reference document (PDF/HTML)", "https://example.com/books/cs_quantum_ch3.pdf"),
                    TableColumn("created_at", "TIMESTAMP", "Yes", "CURRENT_TIMESTAMP", "No", "No", "Creation timestamp of the chapter", "2026-06-25 08:30:00")
                )
            ),
            TableDesign(
                "Achievements",
                "Gamified badges and points unlocked through study triggers.",
                listOf(
                    TableColumn("achievement_id", "VARCHAR(36)", "Yes", "NULL", "Yes", "No", "Unique identifier for achievement category (UUID)", "ach_1a2b3c4d"),
                    TableColumn("title", "VARCHAR(100)", "Yes", "NULL", "No", "No", "Header title of the achievement badge", "Night Owl"),
                    TableColumn("description", "VARCHAR(255)", "Yes", "NULL", "No", "No", "Description of requirements to unlock", "Complete a quiz or study session between 12:00 AM and 4:00 AM."),
                    TableColumn("points", "INT", "Yes", "10", "No", "No", "Gamification points awarded on unlock", "50"),
                    TableColumn("badge_url", "VARCHAR(2083)", "Yes", "NULL", "No", "No", "URL linking to vector graphic illustration", "https://example.com/badges/night_owl.svg"),
                    TableColumn("criteria_type", "VARCHAR(50)", "Yes", "NULL", "No", "No", "Metrics parameter class analyzed (streak, score, time)", "quiz_score"),
                    TableColumn("criteria_value", "INT", "Yes", "1", "No", "No", "Required numerical value to trigger lock release", "95")
                )
            ),
            TableDesign(
                "Notifications",
                "User alerts, updates, goals reached, and warnings log.",
                listOf(
                    TableColumn("notification_id", "VARCHAR(36)", "Yes", "NULL", "Yes", "No", "Unique identifier for notification log (UUID)", "ntf_3c4d5e6f"),
                    TableColumn("user_id", "VARCHAR(36)", "Yes", "NULL", "No", "Users.user_id", "Foreign key referencing recipient user", "usr_9f82a1b3"),
                    TableColumn("title", "VARCHAR(150)", "Yes", "NULL", "No", "No", "Alert notification header", "Daily Goal Achieved!"),
                    TableColumn("message", "VARCHAR(500)", "Yes", "NULL", "No", "No", "Alert notification body text", "Congratulations! You have completed your daily study goal of 30 minutes."),
                    TableColumn("notification_type", "VARCHAR(50)", "Yes", "'system'", "No", "No", "Type of alert (system, reminder, achievement, social)", "achievement"),
                    TableColumn("is_read", "BOOLEAN", "Yes", "FALSE", "No", "No", "Boolean state indicating reader checked", "FALSE"),
                    TableColumn("created_at", "TIMESTAMP", "Yes", "CURRENT_TIMESTAMP", "No", "No", "Triggered timestamp", "2026-06-30 20:00:00")
                )
            ),
            TableDesign(
                "User Progress",
                "Cumulative study progress stats segmented by subjects.",
                listOf(
                    TableColumn("progress_id", "VARCHAR(36)", "Yes", "NULL", "Yes", "No", "Unique identifier for tracking record (UUID)", "prg_7e8f9a1b"),
                    TableColumn("user_id", "VARCHAR(36)", "Yes", "NULL", "No", "Users.user_id", "Foreign key referencing candidate user", "usr_9f82a1b3"),
                    TableColumn("subject_id", "VARCHAR(36)", "Yes", "NULL", "No", "Subjects.subject_id", "Foreign key referencing target subject", "sub_8f2d1c4e"),
                    TableColumn("chapters_completed", "INT", "Yes", "0", "No", "No", "Completed chapters count under subject parameter", "2"),
                    TableColumn("total_study_time_seconds", "INT", "Yes", "0", "No", "No", "Cumulative seconds studied in this subject", "3600"),
                    TableColumn("average_quiz_score", "DECIMAL(5,2)", "No", "NULL", "No", "No", "Average percentage achieved in exams (NULL if no history)", "82.50"),
                    TableColumn("last_activity_at", "TIMESTAMP", "Yes", "CURRENT_TIMESTAMP", "No", "No", "Timestamp of last interaction with subject content (UTC)", "2026-06-30 18:30:00")
                )
            ),
            TableDesign(
                "Settings",
                "User customization parameters and preference options.",
                listOf(
                    TableColumn("setting_id", "VARCHAR(36)", "Yes", "NULL", "Yes", "No", "Unique identifier for settings log (UUID)", "set_9a8b1c2d"),
                    TableColumn("user_id", "VARCHAR(36)", "Yes", "NULL", "No", "Users.user_id", "Foreign key referencing user owner", "usr_9f82a1b3"),
                    TableColumn("theme", "VARCHAR(20)", "Yes", "'system'", "No", "No", "Visual theme option (light, dark, system, geometric)", "geometric"),
                    TableColumn("push_enabled", "BOOLEAN", "Yes", "TRUE", "No", "No", "Alert push permissions toggle", "TRUE"),
                    TableColumn("email_digests_enabled", "BOOLEAN", "Yes", "TRUE", "No", "No", "Weekly digest delivery toggle", "FALSE"),
                    TableColumn("daily_goal_minutes", "INT", "Yes", "30", "No", "No", "Target study minutes per day", "45"),
                    TableColumn("updated_at", "TIMESTAMP", "Yes", "CURRENT_TIMESTAMP", "No", "No", "Timestamp of last preferences save (UTC)", "2026-06-30 21:00:00")
                )
            ),
            TableDesign(
                "AI Conversations",
                "History of context-aware interaction with learning AI models.",
                listOf(
                    TableColumn("conversation_id", "VARCHAR(36)", "Yes", "NULL", "Yes", "No", "Unique identifier for conversational session (UUID)", "aic_5e3f2a1b"),
                    TableColumn("user_id", "VARCHAR(36)", "Yes", "NULL", "No", "Users.user_id", "Foreign key referencing user participant", "usr_9f82a1b3"),
                    TableColumn("context_type", "VARCHAR(50)", "Yes", "'general'", "No", "No", "Core focus area of conversation (general, chapter_doubt, image_ocr, note_helper)", "chapter_doubt"),
                    TableColumn("context_id", "VARCHAR(36)", "No", "NULL", "No", "No", "ID linking to targeted module context (e.g., chapter_id, note_id)", "chp_5e3f2a1b"),
                    TableColumn("messages_history", "JSON", "Yes", "NULL", "No", "No", "Structured payload array holding chat exchanges", "[{\"sender\": \"user\", \"text\": \"Explain qubit superposition.\"}, {\"sender\": \"ai\", \"text\": \"Superposition is...\"}]"),
                    TableColumn("created_at", "TIMESTAMP", "Yes", "CURRENT_TIMESTAMP", "No", "No", "Timestamp conversational session opened", "2026-06-30 18:00:00"),
                    TableColumn("updated_at", "TIMESTAMP", "Yes", "CURRENT_TIMESTAMP", "No", "No", "Timestamp of most recent message received", "2026-06-30 18:15:00")
                )
            ),
            TableDesign(
                "Bookmarks",
                "Saved notes, chapters, flashcards, or chats flagged by users.",
                listOf(
                    TableColumn("bookmark_id", "VARCHAR(36)", "Yes", "NULL", "Yes", "No", "Unique identifier for bookmark log (UUID)", "bmk_2a3b4c5d"),
                    TableColumn("user_id", "VARCHAR(36)", "Yes", "NULL", "No", "Users.user_id", "Foreign key referencing creator", "usr_9f82a1b3"),
                    TableColumn("reference_type", "VARCHAR(50)", "Yes", "NULL", "No", "No", "Bookmark object class (chapter, note, flashcard, ai_convo)", "note"),
                    TableColumn("reference_id", "VARCHAR(36)", "Yes", "NULL", "No", "No", "Primary key target referencing bookmarked object", "not_7f2b1a4d"),
                    TableColumn("note_markup", "VARCHAR(500)", "No", "NULL", "No", "No", "User customized custom note/tags attached to bookmark", "Important for exams"),
                    TableColumn("created_at", "TIMESTAMP", "Yes", "CURRENT_TIMESTAMP", "No", "No", "Bookmark log timestamp (UTC)", "2026-06-30 19:30:00")
                )
            ),
            TableDesign(
                "Study Goals",
                "Custom learning objectives configured by users to track streaks.",
                listOf(
                    TableColumn("goal_id", "VARCHAR(36)", "Yes", "NULL", "Yes", "No", "Unique identifier for configured goal (UUID)", "gol_1e2f3a4b"),
                    TableColumn("user_id", "VARCHAR(36)", "Yes", "NULL", "No", "Users.user_id", "Foreign key referencing target user owner", "usr_9f82a1b3"),
                    TableColumn("title", "VARCHAR(150)", "Yes", "NULL", "No", "No", "Objective description title", "Master Quantum Computing concepts before final exams"),
                    TableColumn("target_type", "VARCHAR(50)", "Yes", "'time_spent'", "No", "No", "Metrics constraint evaluated (time_spent, quizzes_taken, chapters_read)", "time_spent"),
                    TableColumn("target_value", "INT", "Yes", "NULL", "No", "No", "Target value constraint count to achieve completion", "18000"),
                    TableColumn("current_value", "INT", "Yes", "0", "No", "No", "Current progression value registered", "7200"),
                    TableColumn("start_date", "TIMESTAMP", "Yes", "CURRENT_TIMESTAMP", "No", "No", "Timestamp target tracks activated", "2026-06-25 00:00:00"),
                    TableColumn("end_date", "TIMESTAMP", "Yes", "NULL", "No", "No", "Timestamp deadline targeting limit", "2026-07-15 23:59:59"),
                    TableColumn("status", "VARCHAR(20)", "Yes", "'active'", "No", "No", "Goal phase descriptor (active, completed, failed, paused)", "active"),
                    TableColumn("created_at", "TIMESTAMP", "Yes", "CURRENT_TIMESTAMP", "No", "No", "Configuration log logged", "2026-06-25 09:00:00")
                )
            )
        )
    }
}
