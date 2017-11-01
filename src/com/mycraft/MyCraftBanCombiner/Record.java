package com.mycraft.MyCraftBanCombiner;

import java.util.ArrayList;

public class Record {
	String name;
	ArrayList<String> uuids;
	long lastseen;
	long ip;
	String newest;

	public Record(String name, long ip) {
		this.name = name;
		this.ip = ip;
		uuids = new ArrayList<>();
		lastseen = 0;
		newest = null;
	}

	public void add(String name, String uuid, long time) {
		if (time > lastseen) {
			lastseen = time;
			newest = uuid;
			this.name = name;
		}
		uuids.add(uuid);
	}
}
