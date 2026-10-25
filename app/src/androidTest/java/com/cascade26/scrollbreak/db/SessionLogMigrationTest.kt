package com.cascade26.scrollbreak.db

import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionLogMigrationTest {
    @Test
    fun migration18To19CreatesSessionLogsAndPreservesLegacyRows() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null)
            .callback(object : SupportSQLiteOpenHelper.Callback(18) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE legacy_usage (id INTEGER PRIMARY KEY NOT NULL, value TEXT NOT NULL)")
                    db.execSQL("INSERT INTO legacy_usage (id, value) VALUES (1, 'preserved')")
                }

                override fun onUpgrade(
                    db: androidx.sqlite.db.SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) = Unit
            })
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)

        try {
            val database = helper.writableDatabase
            ScrollBreakRoomDatabase.MIGRATION_18_19.migrate(database)

            database.query("SELECT value FROM legacy_usage WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("preserved", cursor.getString(0))
            }
            database.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'session_logs'"
            ).use { cursor -> assertTrue(cursor.moveToFirst()) }
        } finally {
            helper.close()
        }
    }
}
