package com.sorwe.store.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AppItemEntity::class, RepoSourceEntity::class],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun repoDao(): RepoDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE apps ADD COLUMN rating REAL NOT NULL DEFAULT 0.0"
                )
                database.execSQL(
                    "ALTER TABLE apps ADD COLUMN featured INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE apps ADD COLUMN banner TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE apps ADD COLUMN sourceType TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE apps ADD COLUMN lastUpdated INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE apps ADD COLUMN platform TEXT NOT NULL DEFAULT 'Android'"
                )
            }
        }

        // Force re-fetch so all apps get the correct platform from apps.json
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "UPDATE apps SET lastUpdated = 0"
                )
            }
        }
        // Add repoUrl column and force re-fetch so apps get their repo URL
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE apps ADD COLUMN repoUrl TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "UPDATE apps SET lastUpdated = 0"
                )
            }
        }

        // Add repos table for multiple custom repositories
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `repos` (`url` TEXT NOT NULL, `name` TEXT NOT NULL, `isEnabled` INTEGER NOT NULL, PRIMARY KEY(`url`))"
                )
                // Insert the default repository
                database.execSQL(
                    "INSERT INTO `repos` (`url`, `name`, `isEnabled`) VALUES ('https://raw.githubusercontent.com/ifyoucanfindme973-netizen/sorwe-store-data/main/apps.json', 'Default Sorwe Repo', 1)"
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE apps ADD COLUMN releaseKeyword TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        // Fix old default repo URL (without refs/heads/) to canonical URL
        // Also reset lastUpdated so all apps get fresh download URLs from GitHub API
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Update the old URL variant to the canonical refs/heads form
                database.execSQL(
                    "UPDATE `repos` SET `url` = 'https://raw.githubusercontent.com/ifyoucanfindme973-netizen/sorwe-store-data/refs/heads/main/apps.json' " +
                    "WHERE `url` = 'https://raw.githubusercontent.com/ifyoucanfindme973-netizen/sorwe-store-data/main/apps.json'"
                )
                // Force re-fetch of all apps to get proper download URLs
                database.execSQL("UPDATE apps SET lastUpdated = 0")
            }
        }

        // Migrate from old repo to new Developer-For-Git repo
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Remove old repo urls if they exist
                database.execSQL("DELETE FROM `repos` WHERE `url` LIKE '%ifyoucanfindme973-netizen/sorwe-store-data%'")
                // Insert new core repo
                database.execSQL(
                    "INSERT OR REPLACE INTO `repos` (`url`, `name`, `isEnabled`) " +
                    "VALUES ('https://raw.githubusercontent.com/Developer-For-Git/MOD-STORE-DATA-/refs/heads/main/apps.json', 'Default Sorwe Repo', 1)"
                )
                // Force re-fetch
                database.execSQL("UPDATE apps SET lastUpdated = 0")
            }
        }
    }
}
