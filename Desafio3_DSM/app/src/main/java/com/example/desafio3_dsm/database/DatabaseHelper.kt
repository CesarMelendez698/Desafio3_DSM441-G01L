package com.example.desafio3_dsm.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.desafio3_dsm.model.Book
import com.example.desafio3_dsm.model.YearStats

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "reading_tracker.db"
        private const val DATABASE_VERSION = 1

        // Tabla Libros
        private const val TABLE_BOOKS = "books"
        private const val COL_BOOK_ID = "id"
        private const val COL_BOOK_USER_ID = "user_id"
        private const val COL_BOOK_TITLE = "title"
        private const val COL_BOOK_AUTHOR = "author"
        private const val COL_BOOK_GENRE = "genre"
        private const val COL_BOOK_TOTAL_PAGES = "total_pages"
        private const val COL_BOOK_READ_PAGES = "read_pages"
        private const val COL_BOOK_STATUS = "status"
        private const val COL_BOOK_RATING = "rating"
        private const val COL_BOOK_FINISH_DATE = "finish_date"
        private const val COL_BOOK_FINISH_YEAR = "finish_year"

        // Tabla Metas Anuales
        private const val TABLE_GOALS = "annual_goals"
        private const val COL_GOAL_ID = "id"
        private const val COL_GOAL_USER_ID = "user_id"
        private const val COL_GOAL_YEAR = "year"
        private const val COL_GOAL_COUNT = "goal_count"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createBooksTable = """
            CREATE TABLE $TABLE_BOOKS (
                $COL_BOOK_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_BOOK_USER_ID TEXT NOT NULL,
                $COL_BOOK_TITLE TEXT NOT NULL,
                $COL_BOOK_AUTHOR TEXT NOT NULL,
                $COL_BOOK_GENRE TEXT NOT NULL,
                $COL_BOOK_TOTAL_PAGES INTEGER NOT NULL,
                $COL_BOOK_READ_PAGES INTEGER NOT NULL,
                $COL_BOOK_STATUS TEXT NOT NULL,
                $COL_BOOK_RATING REAL NOT NULL DEFAULT 0,
                $COL_BOOK_FINISH_DATE INTEGER,
                $COL_BOOK_FINISH_YEAR INTEGER
            )
        """.trimIndent()

        val createGoalsTable = """
            CREATE TABLE $TABLE_GOALS (
                $COL_GOAL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_GOAL_USER_ID TEXT NOT NULL,
                $COL_GOAL_YEAR INTEGER NOT NULL,
                $COL_GOAL_COUNT INTEGER NOT NULL,
                UNIQUE($COL_GOAL_USER_ID, $COL_GOAL_YEAR)
            )
        """.trimIndent()

        db.execSQL(createBooksTable)
        db.execSQL(createGoalsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BOOKS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GOALS")
        onCreate(db)
    }

    // --- Operaciones de Libros ---

    fun insertBook(book: Book): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_BOOK_USER_ID, book.userId)
            put(COL_BOOK_TITLE, book.title.trim())
            put(COL_BOOK_AUTHOR, book.author.trim())
            put(COL_BOOK_GENRE, book.genre)
            put(COL_BOOK_TOTAL_PAGES, book.totalPages)
            put(COL_BOOK_READ_PAGES, book.readPages)
            put(COL_BOOK_STATUS, book.status)
            put(COL_BOOK_RATING, book.rating)
            put(COL_BOOK_FINISH_DATE, book.finishDate)
            put(COL_BOOK_FINISH_YEAR, book.finishYear)
        }
        val id = db.insert(TABLE_BOOKS, null, values)
        book.id = id
        return id
    }

    fun updateBook(book: Book): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_BOOK_TITLE, book.title.trim())
            put(COL_BOOK_AUTHOR, book.author.trim())
            put(COL_BOOK_GENRE, book.genre)
            put(COL_BOOK_TOTAL_PAGES, book.totalPages)
            put(COL_BOOK_READ_PAGES, book.readPages)
            put(COL_BOOK_STATUS, book.status)
            put(COL_BOOK_RATING, book.rating)
            put(COL_BOOK_FINISH_DATE, book.finishDate)
            put(COL_BOOK_FINISH_YEAR, book.finishYear)
        }
        return db.update(
            TABLE_BOOKS,
            values,
            "$COL_BOOK_ID = ? AND $COL_BOOK_USER_ID = ?",
            arrayOf(book.id.toString(), book.userId)
        )
    }

    fun deleteBook(bookId: Long, userId: String): Int {
        val db = writableDatabase
        return db.delete(
            TABLE_BOOKS,
            "$COL_BOOK_ID = ? AND $COL_BOOK_USER_ID = ?",
            arrayOf(bookId.toString(), userId)
        )
    }

    fun getBookById(bookId: Long, userId: String): Book? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_BOOKS,
            null,
            "$COL_BOOK_ID = ? AND $COL_BOOK_USER_ID = ?",
            arrayOf(bookId.toString(), userId),
            null,
            null,
            null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                cursorToBook(it)
            } else {
                null
            }
        }
    }

    fun getAllBooks(userId: String): List<Book> {
        val books = mutableListOf<Book>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_BOOKS,
            null,
            "$COL_BOOK_USER_ID = ?",
            arrayOf(userId),
            null,
            null,
            "$COL_BOOK_ID DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                books.add(cursorToBook(it))
            }
        }
        return books
    }

    fun getCurrentlyReadingBooks(userId: String): List<Book> {
        val books = mutableListOf<Book>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_BOOKS,
            null,
            "$COL_BOOK_USER_ID = ? AND $COL_BOOK_STATUS = ?",
            arrayOf(userId, "Leyendo"),
            null,
            null,
            "$COL_BOOK_ID DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                books.add(cursorToBook(it))
            }
        }
        return books
    }

    fun getFinishedBooksCountInYear(userId: String, year: Int): Int {
        val db = readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT COUNT(*) FROM $TABLE_BOOKS 
            WHERE $COL_BOOK_USER_ID = ? 
              AND $COL_BOOK_STATUS = 'Terminado' 
              AND $COL_BOOK_FINISH_YEAR = ?
            """.trimIndent(),
            arrayOf(userId, year.toString())
        )
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    // --- Metas Anuales ---

    fun getAnnualGoal(userId: String, year: Int): Int {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_GOALS,
            arrayOf(COL_GOAL_COUNT),
            "$COL_GOAL_USER_ID = ? AND $COL_GOAL_YEAR = ?",
            arrayOf(userId, year.toString()),
            null,
            null,
            null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                it.getInt(0)
            } else {
                10 // Meta por defecto si aún no ha definido
            }
        }
    }

    fun setAnnualGoal(userId: String, year: Int, goal: Int): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_GOAL_USER_ID, userId)
            put(COL_GOAL_YEAR, year)
            put(COL_GOAL_COUNT, goal)
        }
        val result = db.insertWithOnConflict(
            TABLE_GOALS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        return result != -1L
    }

    // --- Estadísticas ---

    fun getStatsForYear(userId: String, year: Int): YearStats {
        val db = readableDatabase

        // 1. Conteo por género para libros terminados en el año
        var countNovel = 0
        var countSciFi = 0
        var countHistory = 0
        var countSelfHelp = 0

        val genreQuery = """
            SELECT $COL_BOOK_GENRE, COUNT(*) as qty, SUM($COL_BOOK_TOTAL_PAGES) as pages
            FROM $TABLE_BOOKS
            WHERE $COL_BOOK_USER_ID = ?
              AND $COL_BOOK_STATUS = 'Terminado'
              AND $COL_BOOK_FINISH_YEAR = ?
            GROUP BY $COL_BOOK_GENRE
        """.trimIndent()

        var totalPages = 0
        var topGenre = "Ninguno"
        var topGenreCount = 0

        val genreCursor = db.rawQuery(genreQuery, arrayOf(userId, year.toString()))
        genreCursor.use {
            while (it.moveToNext()) {
                val genre = it.getString(0)
                val count = it.getInt(1)
                val pages = it.getInt(2)
                totalPages += pages

                when (genre) {
                    "Novela" -> countNovel = count
                    "Ciencia ficción" -> countSciFi = count
                    "Historia" -> countHistory = count
                    "Desarrollo personal" -> countSelfHelp = count
                }

                if (count > topGenreCount) {
                    topGenreCount = count
                    topGenre = genre
                }
            }
        }

        // Si no hubo terminados, también podemos sumar páginas de libros leídos del año
        if (totalPages == 0) {
            val pagesCursor = db.rawQuery(
                """
                SELECT SUM($COL_BOOK_READ_PAGES) FROM $TABLE_BOOKS
                WHERE $COL_BOOK_USER_ID = ?
                  AND ($COL_BOOK_FINISH_YEAR = ? OR $COL_BOOK_FINISH_YEAR IS NULL)
                """.trimIndent(),
                arrayOf(userId, year.toString())
            )
            pagesCursor.use {
                if (it.moveToFirst()) {
                    totalPages = it.getInt(0)
                }
            }
        }

        // 2. Libro mejor calificado del año
        // En caso de empate, el que se terminó primero (finish_date ASC)
        val bestRatedQuery = """
            SELECT * FROM $TABLE_BOOKS
            WHERE $COL_BOOK_USER_ID = ?
              AND $COL_BOOK_STATUS = 'Terminado'
              AND $COL_BOOK_FINISH_YEAR = ?
              AND $COL_BOOK_RATING > 0
            ORDER BY $COL_BOOK_RATING DESC, $COL_BOOK_FINISH_DATE ASC
            LIMIT 1
        """.trimIndent()

        var bestBook: Book? = null
        val bestCursor = db.rawQuery(bestRatedQuery, arrayOf(userId, year.toString()))
        bestCursor.use {
            if (it.moveToFirst()) {
                bestBook = cursorToBook(it)
            }
        }

        return YearStats(
            year = year,
            countNovel = countNovel,
            countSciFi = countSciFi,
            countHistory = countHistory,
            countSelfHelp = countSelfHelp,
            mostReadGenre = topGenre,
            mostReadGenreCount = topGenreCount,
            totalPagesReadInYear = totalPages,
            bestRatedBook = bestBook
        )
    }

    private fun cursorToBook(cursor: Cursor): Book {
        val finishDateIndex = cursor.getColumnIndexOrThrow(COL_BOOK_FINISH_DATE)
        val finishDate = if (cursor.isNull(finishDateIndex)) null else cursor.getLong(finishDateIndex)

        val finishYearIndex = cursor.getColumnIndexOrThrow(COL_BOOK_FINISH_YEAR)
        val finishYear = if (cursor.isNull(finishYearIndex)) null else cursor.getInt(finishYearIndex)

        return Book(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_BOOK_ID)),
            userId = cursor.getString(cursor.getColumnIndexOrThrow(COL_BOOK_USER_ID)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(COL_BOOK_TITLE)),
            author = cursor.getString(cursor.getColumnIndexOrThrow(COL_BOOK_AUTHOR)),
            genre = cursor.getString(cursor.getColumnIndexOrThrow(COL_BOOK_GENRE)),
            totalPages = cursor.getInt(cursor.getColumnIndexOrThrow(COL_BOOK_TOTAL_PAGES)),
            readPages = cursor.getInt(cursor.getColumnIndexOrThrow(COL_BOOK_READ_PAGES)),
            status = cursor.getString(cursor.getColumnIndexOrThrow(COL_BOOK_STATUS)),
            rating = cursor.getFloat(cursor.getColumnIndexOrThrow(COL_BOOK_RATING)),
            finishDate = finishDate,
            finishYear = finishYear
        )
    }
}
