package com.mycraft.MyCraftBanCombiner;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.configuration.file.YamlConfiguration;

public class Database {
	// Config & DataBase
	protected Connection connection;
	protected String host;
	protected String database;
	protected String tableName;
	protected String username;
	protected String password;
	protected int port = 3306;
	protected Statement statement;

	public Database(String host, String database, String tableName, String username, String password, int port) {
		this.host = host;
		this.database = database;
		this.tableName = tableName;
		this.username = username;
		this.password = password;
		this.port = port;
	}

	public Database(YamlConfiguration config) {
		host = config.getString("mysql.host");
		database = config.getString("mysql.database");
		tableName = config.getString("mysql.tablename");
		username = config.getString("mysql.username");
		password = config.getString("mysql.password");
		port = config.getInt("mysql.port");
	}

	public Statement getStatement() {
		return statement;
	}

	public Connection getConnection() {
		return connection;
	}

	public ResultSet runSQL(String arg) {
		try {
			Statement statement = connection.createStatement();
			return statement.executeQuery(arg);
		} catch (SQLException e) {
			System.out.println(arg);
			e.printStackTrace();
			return null;
		}
	}

	public void updateSQL(String arg) {
		try {
			Statement statement = connection.createStatement();
			statement.executeUpdate(arg);
		} catch (SQLException e) {
			System.out.println(arg);
			e.printStackTrace();
		}
	}

	public int insertSQL(String arg) {
		try {
			PreparedStatement pstmt = connection.prepareStatement(arg, Statement.RETURN_GENERATED_KEYS);
			pstmt.executeUpdate();
			ResultSet keys = pstmt.getGeneratedKeys();
			if (keys.next()) {
				return keys.getInt(1);
			}
		} catch (SQLException e) {
			System.out.println(arg);
			e.printStackTrace();
		}
		return -1;
	}

	public String getTableName() {
		return tableName;
	}

	public Statement openConnection() {
		try {
			synchronized (this) {
				if (connection != null && !connection.isClosed()) {
					return null;
				}
				connection = DriverManager.getConnection(
						"jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + "?user=" + this.username
								+ "&password=" + this.password + "&useUnicode=true&characterEncoding=utf-8");
				statement = connection.createStatement();
				return statement;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

}
