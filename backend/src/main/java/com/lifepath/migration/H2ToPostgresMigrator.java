package com.lifepath.migration;

import java.nio.file.Path;
import java.sql.*;

/**
 * One-time migration: copy users, city_state, quests, habits, calendar from H2 file DB to PostgreSQL.
 * Run with: mvn compile exec:java -Dexec.mainClass="com.lifepath.migration.H2ToPostgresMigrator"
 *
 * Requires: PostgreSQL running (e.g. docker compose up postgres), H2 file at backend/data/lifepath.mv.db
 * Env: POSTGRES_PASSWORD (default: lifepath), H2_PATH (default: data/lifepath)
 */
public class H2ToPostgresMigrator {

    private static final String H2_URL;
    private static final String PG_URL = "jdbc:postgresql://localhost:5432/lifepath";
    private static final String PG_USER = "lifepath";
    private static final String PG_PASS = System.getenv().getOrDefault("POSTGRES_PASSWORD", "lifepath");

    static {
        String h2Path = System.getenv().getOrDefault("H2_PATH", "data/lifepath");
        Path resolved = Path.of(h2Path).toAbsolutePath();
        H2_URL = "jdbc:h2:file:" + resolved + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";
    }

    public static void main(String[] args) throws Exception {
        System.out.println("H2 source: " + H2_URL);
        System.out.println("Postgres target: " + PG_URL);

        try (Connection h2 = DriverManager.getConnection(H2_URL, "sa", "");
             Connection pg = DriverManager.getConnection(PG_URL, PG_USER, PG_PASS)) {

            int usersMigrated = migrateUsers(h2, pg);
            int cityMigrated = migrateCityState(h2, pg);
            int questsMigrated = safeMigrate(h2, pg, "quests", H2ToPostgresMigrator::migrateQuests);
            int blockedMigrated = safeMigrate(h2, pg, "blocked_quests", H2ToPostgresMigrator::migrateBlockedQuests);
            int habitsMigrated = safeMigrate(h2, pg, "habits", H2ToPostgresMigrator::migrateHabits);
            int habitComplMigrated = safeMigrate(h2, pg, "habit_completions", H2ToPostgresMigrator::migrateHabitCompletions);
            int streaksMigrated = safeMigrate(h2, pg, "streaks", H2ToPostgresMigrator::migrateStreaks);
            int calTokensMigrated = safeMigrate(h2, pg, "google_calendar_tokens", H2ToPostgresMigrator::migrateGoogleCalendarTokens);
            int calEventsMigrated = safeMigrate(h2, pg, "calendar_events", H2ToPostgresMigrator::migrateCalendarEvents);

            System.out.println("Done. users: " + usersMigrated + ", city_state: " + cityMigrated +
                    ", quests: " + questsMigrated + ", blocked_quests: " + blockedMigrated +
                    ", habits: " + habitsMigrated + ", habit_completions: " + habitComplMigrated +
                    ", streaks: " + streaksMigrated + ", google_calendar_tokens: " + calTokensMigrated +
                    ", calendar_events: " + calEventsMigrated);
        }
    }

    private static int migrateUsers(Connection h2, Connection pg) throws SQLException {
        try (Statement st = h2.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, email, password_hash, display_name, timezone, total_xp, created_at, updated_at FROM users")) {

            String insert = "INSERT INTO users (id, email, password_hash, display_name, timezone, total_xp, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (email) DO NOTHING";
            int count = 0;
            try (PreparedStatement ps = pg.prepareStatement(insert)) {
                while (rs.next()) {
                    ps.setObject(1, rs.getObject("id"));
                    ps.setString(2, rs.getString("email"));
                    ps.setString(3, rs.getString("password_hash"));
                    ps.setString(4, rs.getString("display_name"));
                    ps.setString(5, rs.getString("timezone"));
                    ps.setInt(6, rs.getInt("total_xp"));
                    ps.setTimestamp(7, rs.getTimestamp("created_at"));
                    ps.setTimestamp(8, rs.getTimestamp("updated_at"));
                    int n = ps.executeUpdate();
                    if (n > 0) {
                        count++;
                        System.out.println("  Migrated user: " + rs.getString("email"));
                    }
                }
            }
            return count;
        }
    }

    private static int migrateCityState(Connection h2, Connection pg) throws SQLException {
        try (Statement st = h2.createStatement();
             ResultSet rs = st.executeQuery("SELECT user_id, coins, premium_currency, city_name, population, created_at, updated_at FROM city_state")) {

            String insert = "INSERT INTO city_state (user_id, coins, premium_currency, city_name, population, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (user_id) DO NOTHING";
            int count = 0;
            try (PreparedStatement ps = pg.prepareStatement(insert)) {
                while (rs.next()) {
                    ps.setObject(1, rs.getObject("user_id"));
                    ps.setInt(2, rs.getInt("coins"));
                    ps.setInt(3, rs.getInt("premium_currency"));
                    ps.setString(4, rs.getString("city_name"));
                    ps.setInt(5, rs.getInt("population"));
                    ps.setTimestamp(6, rs.getTimestamp("created_at"));
                    ps.setTimestamp(7, rs.getTimestamp("updated_at"));
                    int n = ps.executeUpdate();
                    if (n > 0) {
                        count++;
                        System.out.println("  Migrated city_state for user: " + rs.getObject("user_id"));
                    }
                }
            }
            return count;
        }
    }

    private static int migrateQuests(Connection h2, Connection pg) throws SQLException {
        String insert = "INSERT INTO quests (id, user_id, title, notes, priority_level, parent_id, status, time_of_day, context_tag, created_at, updated_at, completed_at, deleted_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (id) DO NOTHING";
        return migrateOrdered(h2, pg, "SELECT id, user_id, title, notes, priority_level, parent_id, status, time_of_day, context_tag, created_at, updated_at, completed_at, deleted_at FROM quests ORDER BY parent_id NULLS FIRST",
                insert, (rs, ps) -> {
                    ps.setObject(1, rs.getObject("id"));
                    ps.setObject(2, rs.getObject("user_id"));
                    ps.setString(3, rs.getString("title"));
                    ps.setString(4, rs.getString("notes"));
                    ps.setString(5, rs.getString("priority_level"));
                    ps.setObject(6, rs.getObject("parent_id"));
                    ps.setString(7, rs.getString("status"));
                    ps.setString(8, rs.getString("time_of_day"));
                    ps.setString(9, rs.getString("context_tag"));
                    ps.setTimestamp(10, rs.getTimestamp("created_at"));
                    ps.setTimestamp(11, rs.getTimestamp("updated_at"));
                    ps.setTimestamp(12, rs.getTimestamp("completed_at"));
                    ps.setTimestamp(13, rs.getTimestamp("deleted_at"));
                }, "quests");
    }

    private static int migrateBlockedQuests(Connection h2, Connection pg) throws SQLException {
        String insert = "INSERT INTO blocked_quests (id, quest_id, reason, unblock_trigger, unblock_at, unblock_location, created_at, unblocked_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (id) DO NOTHING";
        return migrateSimple(h2, pg, "SELECT id, quest_id, reason, unblock_trigger, unblock_at, unblock_location, created_at, unblocked_at FROM blocked_quests",
                insert, (rs, ps) -> {
                    ps.setObject(1, rs.getObject("id"));
                    ps.setObject(2, rs.getObject("quest_id"));
                    ps.setString(3, rs.getString("reason"));
                    ps.setString(4, rs.getString("unblock_trigger"));
                    ps.setTimestamp(5, rs.getTimestamp("unblock_at"));
                    ps.setString(6, rs.getString("unblock_location"));
                    ps.setTimestamp(7, rs.getTimestamp("created_at"));
                    ps.setTimestamp(8, rs.getTimestamp("unblocked_at"));
                }, "blocked_quests");
    }

    private static int migrateHabits(Connection h2, Connection pg) throws SQLException {
        String insert = "INSERT INTO habits (id, user_id, title, currency_per_completion, is_streak_tracked, created_at, updated_at, deleted_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (id) DO NOTHING";
        return migrateSimple(h2, pg, "SELECT id, user_id, title, currency_per_completion, is_streak_tracked, created_at, updated_at, deleted_at FROM habits",
                insert, (rs, ps) -> {
                    ps.setObject(1, rs.getObject("id"));
                    ps.setObject(2, rs.getObject("user_id"));
                    ps.setString(3, rs.getString("title"));
                    ps.setInt(4, rs.getInt("currency_per_completion"));
                    ps.setBoolean(5, rs.getBoolean("is_streak_tracked"));
                    ps.setTimestamp(6, rs.getTimestamp("created_at"));
                    ps.setTimestamp(7, rs.getTimestamp("updated_at"));
                    ps.setTimestamp(8, rs.getTimestamp("deleted_at"));
                }, "habits");
    }

    private static int migrateHabitCompletions(Connection h2, Connection pg) throws SQLException {
        String insert = "INSERT INTO habit_completions (id, habit_id, completed_at, completion_date) " +
                "VALUES (?, ?, ?, ?) ON CONFLICT (id) DO NOTHING";
        return migrateSimple(h2, pg, "SELECT id, habit_id, completed_at, completion_date FROM habit_completions",
                insert, (rs, ps) -> {
                    ps.setObject(1, rs.getObject("id"));
                    ps.setObject(2, rs.getObject("habit_id"));
                    ps.setTimestamp(3, rs.getTimestamp("completed_at"));
                    ps.setDate(4, rs.getDate("completion_date"));
                }, "habit_completions");
    }

    private static int migrateStreaks(Connection h2, Connection pg) throws SQLException {
        String insert = "INSERT INTO streaks (habit_id, user_id, current_streak_days, longest_streak_days, last_completed_date) " +
                "VALUES (?, ?, ?, ?, ?) ON CONFLICT (habit_id, user_id) DO NOTHING";
        return migrateSimple(h2, pg, "SELECT habit_id, user_id, current_streak_days, longest_streak_days, last_completed_date FROM streaks",
                insert, (rs, ps) -> {
                    ps.setObject(1, rs.getObject("habit_id"));
                    ps.setObject(2, rs.getObject("user_id"));
                    ps.setInt(3, rs.getInt("current_streak_days"));
                    ps.setInt(4, rs.getInt("longest_streak_days"));
                    ps.setDate(5, rs.getDate("last_completed_date"));
                }, "streaks");
    }

    private static int migrateGoogleCalendarTokens(Connection h2, Connection pg) throws SQLException {
        String insert = "INSERT INTO google_calendar_tokens (id, user_id, access_token, refresh_token, token_expiry, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (user_id) DO NOTHING";
        return migrateSimple(h2, pg, "SELECT id, user_id, access_token, refresh_token, token_expiry, created_at, updated_at FROM google_calendar_tokens",
                insert, (rs, ps) -> {
                    ps.setObject(1, rs.getObject("id"));
                    ps.setObject(2, rs.getObject("user_id"));
                    ps.setString(3, rs.getString("access_token"));
                    ps.setString(4, rs.getString("refresh_token"));
                    ps.setTimestamp(5, rs.getTimestamp("token_expiry"));
                    ps.setTimestamp(6, rs.getTimestamp("created_at"));
                    ps.setTimestamp(7, rs.getTimestamp("updated_at"));
                }, "google_calendar_tokens");
    }

    private static int migrateCalendarEvents(Connection h2, Connection pg) throws SQLException {
        String insert = "INSERT INTO calendar_events (id, user_id, external_id, title, start_time, end_time, completed, synced_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (user_id, external_id) DO NOTHING";
        return migrateSimple(h2, pg, "SELECT id, user_id, external_id, title, start_time, end_time, completed, synced_at FROM calendar_events",
                insert, (rs, ps) -> {
                    ps.setObject(1, rs.getObject("id"));
                    ps.setObject(2, rs.getObject("user_id"));
                    ps.setString(3, rs.getString("external_id"));
                    ps.setString(4, rs.getString("title"));
                    ps.setTimestamp(5, rs.getTimestamp("start_time"));
                    ps.setTimestamp(6, rs.getTimestamp("end_time"));
                    ps.setBoolean(7, rs.getBoolean("completed"));
                    ps.setTimestamp(8, rs.getTimestamp("synced_at"));
                }, "calendar_events");
    }

    @FunctionalInterface
    private interface RsSetter {
        void set(ResultSet rs, PreparedStatement ps) throws SQLException;
    }

    private static int migrateSimple(Connection h2, Connection pg, String select, String insert, RsSetter setter, String name) throws SQLException {
        try (Statement st = h2.createStatement(); ResultSet rs = st.executeQuery(select)) {
            int count = 0;
            try (PreparedStatement ps = pg.prepareStatement(insert)) {
                while (rs.next()) {
                    setter.set(rs, ps);
                    if (ps.executeUpdate() > 0) count++;
                }
            }
            if (count > 0) System.out.println("  Migrated " + name + ": " + count);
            return count;
        }
    }

    private static int migrateOrdered(Connection h2, Connection pg, String select, String insert, RsSetter setter, String name) throws SQLException {
        return migrateSimple(h2, pg, select, insert, setter, name);
    }

    @FunctionalInterface
    private interface Migrator {
        int migrate(Connection h2, Connection pg) throws SQLException;
    }

    private static int safeMigrate(Connection h2, Connection pg, String name, Migrator m) {
        try {
            return m.migrate(h2, pg);
        } catch (SQLException e) {
            System.out.println("  Skipped " + name + " (table may not exist): " + e.getMessage());
            return 0;
        }
    }

}
