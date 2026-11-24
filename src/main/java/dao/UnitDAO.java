package dao;

import config.DatabaseConfig;
import exception.DataAccessException;
import model.Unit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UnitDAO {

    public List<Unit> findAll() {
        String sql = "SELECT unit_code, unit_name FROM unit ORDER BY unit_name";
        List<Unit> units = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                units.add(new Unit(rs.getString("unit_code"), rs.getString("unit_name")));
            }
            return units;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load units", e);
        }
    }
}
