package com.umiot.microclimate.history.repository;

import com.umiot.microclimate.history.entity.WeatherHistoryRecord;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
public class WeatherHistoryDao {

    private static final String DB_DIR = "weather_history";
    private static final String DB_NAME_PATTERN = "weather_history_%d_%02d.db";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── path helpers ──
    private String dbDirPath() {
        return DB_DIR;
    }

    private String dbFileName(int year, int month) {
        return String.format(DB_NAME_PATTERN, year, month);
    }

    private String dbUrl(int year, int month) {
        return "jdbc:sqlite:" + DB_DIR + "/" + dbFileName(year, month);
    }

    // ── ensure directory exists ──
    public void ensureDir() {
        File dir = new File(DB_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    // ── connection ──
    private Connection getConnection(int year, int month) throws SQLException {
        ensureDir();
        return DriverManager.getConnection(dbUrl(year, month));
    }

    // ── table init ──
    public void initTable(int year, int month) {
        String sql = """
            CREATE TABLE IF NOT EXISTS weather_history_record (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                station_id TEXT NOT NULL,
                temperature REAL,
                daily_high REAL,
                daily_low REAL,
                feels_like REAL,
                humidity REAL,
                dew_point REAL,
                rain_1min REAL,
                rain_1hour REAL,
                rain_2hour REAL,
                rain_daily REAL,
                wind_speed_10min REAL,
                wind_speed_60min REAL,
                wind_gust REAL,
                wind_direction TEXT,
                wind_direction_degrees REAL,
                record_time TEXT NOT NULL,
                UNIQUE(station_id, record_time)
            )
            """;
        try (Connection conn = getConnection(year, month);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to init table for " + year + "-" + month, e);
        }
    }

    // ── batch insert ──
    public void batchInsert(int year, int month, List<WeatherHistoryRecord> records) {
        if (records.isEmpty()) return;
        initTable(year, month);
        String sql = """
            INSERT OR IGNORE INTO weather_history_record
            (station_id, temperature, daily_high, daily_low, feels_like,
             humidity, dew_point, rain_1min, rain_1hour, rain_2hour,
             rain_daily, wind_speed_10min, wind_speed_60min, wind_gust,
             wind_direction, wind_direction_degrees, record_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = getConnection(year, month)) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (WeatherHistoryRecord r : records) {
                    ps.setString(1, r.getStationId());
                    setDouble(ps, 2, r.getTemperature());
                    setDouble(ps, 3, r.getDailyHigh());
                    setDouble(ps, 4, r.getDailyLow());
                    setDouble(ps, 5, r.getFeelsLike());
                    setDouble(ps, 6, r.getHumidity());
                    setDouble(ps, 7, r.getDewPoint());
                    setDouble(ps, 8, r.getRain1min());
                    setDouble(ps, 9, r.getRain1hour());
                    setDouble(ps, 10, r.getRain2hour());
                    setDouble(ps, 11, r.getRainDaily());
                    setDouble(ps, 12, r.getWindSpeed10min());
                    setDouble(ps, 13, r.getWindSpeed60min());
                    setDouble(ps, 14, r.getWindGust());
                    ps.setString(15, r.getWindDirection());
                    setDouble(ps, 16, r.getWindDirectionDegrees());
                    ps.setString(17, r.getRecordTime() != null ? r.getRecordTime().format(FMT) : null);
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to batch insert for " + year + "-" + month, e);
        }
    }

    // ── query single month ──
    private List<WeatherHistoryRecord> queryMonth(int year, int month, String sql, SqlParamSetter setter) {
        File f = new File(DB_DIR, dbFileName(year, month));
        if (!f.exists()) return Collections.emptyList();
        try (Connection conn = getConnection(year, month);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (setter != null) setter.apply(ps);
            List<WeatherHistoryRecord> results = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapRow(rs));
            }
            return results;
        } catch (SQLException e) {
            return Collections.emptyList();
        }
    }

    // ── iterate months ──
    private List<WeatherHistoryRecord> queryMonths(LocalDateTime start, LocalDateTime end,
                                                    String sqlTemplate, SqlParamSetter setter) {
        List<WeatherHistoryRecord> all = new ArrayList<>();
        YearMonth ym = YearMonth.from(start);
        YearMonth endYm = YearMonth.from(end);
        while (!ym.isAfter(endYm)) {
            all.addAll(queryMonth(ym.getYear(), ym.getMonthValue(), sqlTemplate, setter));
            ym = ym.plusMonths(1);
        }
        all.sort((a, b) -> {
            LocalDateTime ta = a.getRecordTime();
            LocalDateTime tb = b.getRecordTime();
            if (ta == null) return -1;
            if (tb == null) return 1;
            return ta.compareTo(tb);
        });
        return all;
    }

    // ── iterate all existing months ──
    private List<WeatherHistoryRecord> queryAllMonths(String sqlTemplate, SqlParamSetter setter) {
        File dir = new File(DB_DIR);
        if (!dir.exists() || !dir.isDirectory()) return Collections.emptyList();
        File[] files = dir.listFiles((d, name) -> name.startsWith("weather_history_") && name.endsWith(".db"));
        if (files == null) return Collections.emptyList();

        List<WeatherHistoryRecord> all = new ArrayList<>();
        for (File f : files) {
            String name = f.getName();
            // weather_history_2026_01.db → extract year and month
            String core = name.replace("weather_history_", "").replace(".db", "");
            String[] parts = core.split("_");
            if (parts.length != 2) continue;
            try {
                int y = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]);
                all.addAll(queryMonth(y, m, sqlTemplate, setter));
            } catch (NumberFormatException ignored) {}
        }
        all.sort((a, b) -> {
            LocalDateTime ta = a.getRecordTime();
            LocalDateTime tb = b.getRecordTime();
            if (ta == null) return -1;
            if (tb == null) return 1;
            return ta.compareTo(tb);
        });
        return all;
    }

    // ── public query methods ──

    public List<WeatherHistoryRecord> findByStation(String stationId) {
        return queryAllMonths(
            "SELECT * FROM weather_history_record WHERE station_id = ? ORDER BY record_time ASC",
            ps -> ps.setString(1, stationId));
    }

    public List<WeatherHistoryRecord> findByStationAndTimeRange(String stationId, LocalDateTime start, LocalDateTime end) {
        return queryMonths(start, end,
            "SELECT * FROM weather_history_record WHERE station_id = ? AND record_time BETWEEN ? AND ? ORDER BY record_time ASC",
            ps -> {
                ps.setString(1, stationId);
                ps.setString(2, start.format(FMT));
                ps.setString(3, end.format(FMT));
            });
    }

    public List<WeatherHistoryRecord> findByStationAndYear(String stationId, int year) {
        return queryMonths(
            LocalDateTime.of(year, 1, 1, 0, 0),
            LocalDateTime.of(year, 12, 31, 23, 59),
            "SELECT * FROM weather_history_record WHERE station_id = ? AND record_time BETWEEN ? AND ? ORDER BY record_time ASC",
            ps -> {
                ps.setString(1, stationId);
                ps.setString(2, year + "-01-01 00:00:00");
                ps.setString(3, year + "-12-31 23:59:59");
            });
    }

    public long countByStation(String stationId) {
        return queryAllMonths(
            "SELECT * FROM weather_history_record WHERE station_id = ?",
            ps -> ps.setString(1, stationId)).size();
    }

    public long countAll() {
        return queryAllMonths("SELECT * FROM weather_history_record", null).size();
    }

    public void deleteAll() {
        File dir = new File(DB_DIR);
        if (!dir.exists()) return;
        File[] files = dir.listFiles((d, name) -> name.startsWith("weather_history_") && name.endsWith(".db"));
        if (files == null) return;
        for (File f : files) {
            f.delete();
        }
    }

    public java.util.List<Integer> getAvailableYears() {
        java.util.Set<Integer> years = new java.util.TreeSet<>();
        File dir = new File(DB_DIR);
        if (!dir.exists() || !dir.isDirectory()) return java.util.Collections.emptyList();
        File[] files = dir.listFiles((d, name) -> name.startsWith("weather_history_") && name.endsWith(".db"));
        if (files == null) return java.util.Collections.emptyList();
        for (File f : files) {
            String core = f.getName().replace("weather_history_", "").replace(".db", "");
            String[] parts = core.split("_");
            if (parts.length >= 1) {
                try { years.add(Integer.parseInt(parts[0])); } catch (NumberFormatException ignored) {}
            }
        }
        return new java.util.ArrayList<>(years);
    }

    // ── row mapper ──
    private WeatherHistoryRecord mapRow(ResultSet rs) throws SQLException {
        WeatherHistoryRecord r = new WeatherHistoryRecord();
        r.setId(rs.getLong("id"));
        r.setStationId(rs.getString("station_id"));
        r.setTemperature(getDouble(rs, "temperature"));
        r.setDailyHigh(getDouble(rs, "daily_high"));
        r.setDailyLow(getDouble(rs, "daily_low"));
        r.setFeelsLike(getDouble(rs, "feels_like"));
        r.setHumidity(getDouble(rs, "humidity"));
        r.setDewPoint(getDouble(rs, "dew_point"));
        r.setRain1min(getDouble(rs, "rain_1min"));
        r.setRain1hour(getDouble(rs, "rain_1hour"));
        r.setRain2hour(getDouble(rs, "rain_2hour"));
        r.setRainDaily(getDouble(rs, "rain_daily"));
        r.setWindSpeed10min(getDouble(rs, "wind_speed_10min"));
        r.setWindSpeed60min(getDouble(rs, "wind_speed_60min"));
        r.setWindGust(getDouble(rs, "wind_gust"));
        r.setWindDirection(rs.getString("wind_direction"));
        r.setWindDirectionDegrees(getDouble(rs, "wind_direction_degrees"));
        String timeStr = rs.getString("record_time");
        if (timeStr != null) {
            r.setRecordTime(LocalDateTime.parse(timeStr, FMT));
        }
        return r;
    }

    private static void setDouble(PreparedStatement ps, int idx, Double val) throws SQLException {
        if (val == null) ps.setNull(idx, Types.REAL);
        else ps.setDouble(idx, val);
    }

    private static Double getDouble(ResultSet rs, String col) throws SQLException {
        double v = rs.getDouble(col);
        return rs.wasNull() ? null : v;
    }

    @FunctionalInterface
    private interface SqlParamSetter {
        void apply(PreparedStatement ps) throws SQLException;
    }
}
