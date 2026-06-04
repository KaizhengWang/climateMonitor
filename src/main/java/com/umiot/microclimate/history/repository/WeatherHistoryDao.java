package com.umiot.microclimate.history.repository;

import com.umiot.microclimate.history.entity.WeatherHistoryRecord;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Repository
public class WeatherHistoryDao {

    private static final String DB_URL = "jdbc:sqlite:weather_history.db";

    private static final String CREATE_TABLE_SQL = """
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

    private static final String INSERT_SQL = """
        INSERT OR IGNORE INTO weather_history_record
        (station_id, temperature, daily_high, daily_low, feels_like,
         humidity, dew_point, rain_1min, rain_1hour, rain_2hour,
         rain_daily, wind_speed_10min, wind_speed_60min, wind_gust,
         wind_direction, wind_direction_degrees, record_time)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public void initTable() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_TABLE_SQL);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to init history DB table", e);
        }
    }

    public void batchInsert(List<WeatherHistoryRecord> records) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
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
            throw new RuntimeException("Failed to batch insert weather history", e);
        }
    }

    public List<WeatherHistoryRecord> findByStation(String stationId) {
        String sql = "SELECT * FROM weather_history_record WHERE station_id = ? ORDER BY record_time ASC";
        return queryList(sql, stmt -> stmt.setString(1, stationId));
    }

    public List<WeatherHistoryRecord> findByStationAndTimeRange(String stationId, LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT * FROM weather_history_record WHERE station_id = ? AND record_time BETWEEN ? AND ? ORDER BY record_time ASC";
        return queryList(sql, stmt -> {
            stmt.setString(1, stationId);
            stmt.setString(2, start.format(FMT));
            stmt.setString(3, end.format(FMT));
        });
    }

    public long countByStation(String stationId) {
        String sql = "SELECT COUNT(*) FROM weather_history_record WHERE station_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, stationId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count records", e);
        }
    }

    public long countAll() {
        String sql = "SELECT COUNT(*) FROM weather_history_record";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count records", e);
        }
    }

    public List<WeatherHistoryRecord> findByStationAndYear(String stationId, int year) {
        String start = year + "-01-01 00:00:00";
        String end = year + "-12-31 23:59:59";
        String sql = "SELECT * FROM weather_history_record WHERE station_id = ? AND record_time BETWEEN ? AND ? ORDER BY record_time ASC";
        return queryList(sql, stmt -> {
            stmt.setString(1, stationId);
            stmt.setString(2, start);
            stmt.setString(3, end);
        });
    }

    public void deleteAll() {
        String sql = "DELETE FROM weather_history_record";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete records", e);
        }
    }

    private List<WeatherHistoryRecord> queryList(String sql, SqlParamSetter setter) {
        List<WeatherHistoryRecord> results = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setter.apply(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query weather history", e);
        }
        return results;
    }

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
