package com.mycraft.MyCraftBanCombiner;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class MyCraftBanCombiner extends JavaPlugin implements Listener {
	// Config & DataBase
	private YamlConfiguration config;
	private Database db;
	private HashMap<String, Record> map = new HashMap<>();
	private List<String> tables = new ArrayList<>();

	// -------- Bukkit related
	@Override
	public void onEnable() {
		initCommands();
		loadConfig();

		super.onEnable();
	}

	@Override
	public void onDisable() {
		super.onDisable();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		// ------ 指令
		if (sender instanceof Player && sender.hasPermission("MyCraftBanCombiner.admin")) {
			sender.sendMessage("权限不足！");
			return false;
		}
		switch (cmd.getName()) {
		case "MyCraftBanCombiner":
			combineBan(sender);
			break;
		default:
			return false;
		}
		return true;
	}

	private void loadConfig() {
		try {
			if (!getDataFolder().exists()) {
				getDataFolder().mkdirs();
			}
			if (config == null) {
				File fileConfig = new File(getDataFolder(), "config.yml");
				if (!fileConfig.exists()) {
					saveResource("config.yml", false);
				}
				config = new YamlConfiguration();
				config.load(fileConfig);
			}

			tables = config.getStringList("tables");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void loadSQLData() {
		if (db == null) {
			db = new Database(config);
		}
		try {
			Statement statement = db.openConnection();
			String tableName = db.getTableName();
			String sqlCreate1 = "CREATE TABLE IF NOT EXISTS " + tableName + "_new LIKE " + tableName + ";";
			statement.execute(sqlCreate1);

			ResultSet resultSet = db.runSQL("SELECT * FROM `" + tableName + "`;");
			while (resultSet.next()) {
				StringBuilder uuid = new StringBuilder();
				byte[] bytes = resultSet.getBytes("id");
				for (byte b : bytes) {
					uuid.append(String.format("%02x", b));
				}
				String name = resultSet.getString("name");
				long ip = resultSet.getLong("ip");
				long lastSeen = resultSet.getLong("lastSeen");

				if (!map.containsKey(name.toLowerCase())) {
					map.put(name.toLowerCase(), new Record(name, ip));
				}
				Record record = map.get(name.toLowerCase());
				record.add(name, uuid.toString(), lastSeen);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// -------- private methods | initialization
	private void initCommands() {
		this.getCommand("MyCraftBanCombiner").setExecutor(this);
	}

	// -------- private detail methods | response to commands
	private void combineBan(CommandSender sender) {
		loadSQLData();
		Bukkit.getScheduler().runTaskAsynchronously(this, new Runnable() {
			public void run() {
				int count = 0;
				for (Record record : map.values()) {
					String newest = "0x" + record.newest;
					String query = "Insert into " + db.getTableName() + "_new (`id`, `name`, `ip`, `lastSeen`) VALUES ("
							+ newest + ", '"
							+ record.name.replace("\\", "\\\\").replace("'", "\\'").replace("`", "\\`")
									.replace("*", "\\*").replace("%", "\\%")
							+ "'," + record.ip + "," + record.lastseen + ");";
					db.insertSQL(query);

					for (String uuid : record.uuids) {
						if (uuid.equals(record.newest))
							continue;
						for (String table : tables) {
							query = "UPDATE " + table + " SET `player_id` = " + newest + " WHERE `player_id` = " + "0x"
									+ uuid;
							db.updateSQL(query);
							count++;
						}
					}
				}
				sender.sendMessage("[!] 共计 " + map.size() + " 个用户. 发现了 " + count + " 条重复数据.");
			}
		});
	}
}