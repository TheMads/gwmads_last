package ru.l2gw.extensions.maintenance;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.database.mysql;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2ObjectsStorage;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.tables.ClanTable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author rage
 * @date 07.10.2009 10:10:19
 */
public class TransferCharacterTask extends MaintenanceTask
{
	private static Log _log = LogFactory.getLog("maintenance");
	private String _lastResult = "";

	/*
	 * params format:
	 * objectId
	 */
	@Override
	public boolean doTask(String params)
	{
		Connection con;
		PreparedStatement stmt = null;
		PreparedStatement stmt2;
		ResultSet rs = null;
		ResultSet rs2;
		String sql;
		int old_char_id;

		try
		{
			old_char_id = Integer.parseInt(params);
		}
		catch(NumberFormatException e)
		{
			_lastResult = "Param error";
			return false;
		}

		L2Player player = L2ObjectsStorage.getPlayer(old_char_id);
		if(player != null)
			player.logout(false, false, true);

		try
		{
			con = DatabaseFactory.getInstance().getConnection();

			stmt = con.prepareStatement("SELECT obj_id,char_name,account_name FROM trans_characters WHERE obj_id = " + old_char_id);
			rs = stmt.executeQuery();
			if(rs.next())
			{
				int new_char_id = IdFactory.getInstance().getNextId();
				String old_char_name = rs.getString("char_name");
				String new_char_name = old_char_name;
				String login = rs.getString("account_name");

				sql = "SELECT count(*) as cnt FROM characters WHERE account_name='" + login + "'";

				stmt2 = con.prepareStatement(sql);
				rs2 = stmt2.executeQuery();

				if(rs2.next())
				{
					int cnt = rs2.getInt("cnt");
					if(cnt > 6)
					{
						_log.info("TransferCharacter: char: " + old_char_name + "(" + old_char_id + "): has no free slots, transfer aborted.");
						_lastResult = "char: " + old_char_name + "(" + old_char_id + "): no free slots, transfer aborted.";
						stmt2.close();
						rs2.close();
						return false;
					}
				}

				stmt2.close();
				rs2.close();

				sql = "SELECT * FROM characters WHERE char_name like '" + old_char_name + "'";

				stmt2 = con.prepareStatement(sql);
				rs2 = stmt2.executeQuery();

				if(rs2.next())
					new_char_name = "x7-" + old_char_name;

				_log.info("TransferCharacter: old nick: " + old_char_name + " new nick: " + new_char_name);

				rs2.close();
				stmt2.close();

				sql = "INSERT INTO characters SELECT account_name, " + new_char_id + ", '" + new_char_name + "', face, hairStyle, hairColor, sex, heading, " +
													  "x, y, z, karma, pvpkills, pkkills, 0, deletetime, '', rec_have, rec_left, accesslevel, online, " +
													  "onlinetime, lastAccess, 0, 0, 0, 0, nochannel, noble, ketra, varka, ram , 0, 0, 0, 0, 0, " +
													  "pcBangPoints, logoutTime, vitPoints, UNIX_TIMESTAMP(), LastHWID, prPoints FROM trans_characters WHERE obj_id = " + old_char_id;
				stmt2 = con.prepareStatement(sql);
				stmt2.execute();
				stmt2.close();

				sql = "INSERT INTO character_hennas SELECT " + new_char_id + ", symbol_id, slot, class_index FROM trans_character_hennas WHERE char_obj_id = " + old_char_id;
				stmt2 = con.prepareStatement(sql);
				stmt2.execute();
				stmt2.close();

				sql = "INSERT INTO character_quests SELECT " + new_char_id + ", name, var, value FROM trans_character_quests WHERE char_id = " + old_char_id;
				stmt2 = con.prepareStatement(sql);
				stmt2.execute();
				stmt2.close();

				sql = "INSERt INTO character_recipebook SELECT " + new_char_id + ", id FROM trans_character_recipebook WHERE char_id = " + old_char_id;
				stmt2 = con.prepareStatement(sql);
				stmt2.execute();
				stmt2.close();

				sql = "INSERt INTO character_shortcuts SELECT " + new_char_id + ", slot, page, type, shortcut_id, level, class_index FROM trans_character_shortcuts WHERE char_obj_id = " + old_char_id;
				stmt2 = con.prepareStatement(sql);
				stmt2.execute();
				stmt2.close();

				sql = "INSERT INTO character_skills SELECT " + new_char_id + ", skill_id, skill_level, skill_name, class_index FROM trans_character_skills WHERE char_obj_id = " + old_char_id;
				stmt2 = con.prepareStatement(sql);
				stmt2.execute();
				stmt2.close();

				sql = "INSERT INTO character_subclasses SELECT " + new_char_id + ", class_id, level, exp, sp, curHp, curMp, curCp, maxHp, maxMp, maxCp, active, isBase, death_penalty, slot FROM trans_character_subclasses WHERE char_obj_id = " + old_char_id;
				stmt2 = con.prepareStatement(sql);
				stmt2.execute();
				stmt2.close();

				sql = "INSERT INTO character_variables SELECT " + new_char_id + ", type, name, `index`, value, expire_time FROM trans_character_variables WHERE obj_id = " + old_char_id;
				stmt2 = con.prepareStatement(sql);
				stmt2.execute();
				stmt2.close();

				sql = "SELECT * FROM trans_augmentations a INNER JOIN trans_items i on (a.item_id = i.object_id) WHERE i.owner_id = " + old_char_id;
				stmt2 = con.prepareStatement(sql);
				rs2 = stmt2.executeQuery();

				while(rs2.next())
				{
					int old_item_id = rs2.getInt("item_id");
					int new_item_id = IdFactory.getInstance().getNextId();
					mysql.set("INSERT INTO items SELECT " + new_item_id + ", " + new_char_id + ", item_id, name, count, enchant_level, enchant_attr, enchant_attr_value, class, loc, loc_data, price_sell, price_buy, time_of_use, custom_type1, custom_type2, shadow_life_time, flags FROM trans_items WHERE object_id = " + old_item_id);
					mysql.set("INSERT INTO augmentations VALUES(" + new_item_id + ", " + rs2.getInt("attributes") + ", " + rs2.getInt("skill") + ", " + rs2.getInt("level") + ")");
					mysql.set("DELETE FROM trans_items WHERE object_id = " + old_item_id);
					mysql.set("DELETE FROM trans_augmentations WHERE item_id = " + old_item_id);
				}

				sql = "SELECT * FROM trans_pets p INNER JOIN trans_items i on (p.item_obj_id=i.object_id) WHERE i.owner_id = " + old_char_id;
				stmt2 = con.prepareStatement(sql);
				rs2 = stmt2.executeQuery();

				while(rs2.next())
				{
					int old_item_id = rs2.getInt("item_obj_id");
					int new_item_id = IdFactory.getInstance().getNextId();
					int pet_obj_id = rs2.getInt("objId");
					int new_pet_id = IdFactory.getInstance().getNextId();
					mysql.set("INSERT INTO items SELECT " + new_item_id + ", " + new_char_id + ", item_id, name, count, enchant_level, enchant_attr, enchant_attr_value, class, loc, loc_data, price_sell, price_buy, time_of_use, custom_type1, custom_type2, shadow_life_time, flags FROM trans_items WHERE object_id = " + old_item_id);
					mysql.set("INSERT INTO pets SELECT " + new_item_id + ", " + new_pet_id + ", name, level, curHp, curMp, exp, sp, fed FROM trans_pets WHERE objId = " + pet_obj_id);
					mysql.set("DELETE FROM trans_items WHERE object_id = " + old_item_id);
					mysql.set("DELETE FROM trans_pets WHERE objId = " + pet_obj_id);
				}

				L2Clan clan = ClanTable.getInstance().getClanByMemberId(old_char_id);
				if(clan != null)
					clan.updateMemberId(old_char_id, new_char_id);

				sql = "SELECT object_id FROM trans_items WHERE owner_id = " + old_char_id;
				stmt2 = con.prepareStatement(sql);
				rs2 = stmt2.executeQuery();

				while(rs2.next())
				{
					int old_item_id = rs2.getInt("object_id");
					int new_item_id = IdFactory.getInstance().getNextId();
					mysql.set("INSERT INTO items SELECT " + new_item_id + ", " + new_char_id + ", item_id, name, count, enchant_level, enchant_attr, enchant_attr_value, class, loc, loc_data, price_sell, price_buy, time_of_use, custom_type1, custom_type2, shadow_life_time, flags FROM trans_items WHERE object_id = " + old_item_id);
				}

				mysql.set("DELETE FROM trans_characters WHERE obj_id = " + old_char_id);
				mysql.set("DELETE FROM trans_character_hennas WHERE char_obj_id = " + old_char_id);
				mysql.set("DELETE FROM trans_character_macroses WHERE char_obj_id = " + old_char_id);
				mysql.set("DELETE FROM trans_character_quests WHERE char_id = " + old_char_id);
				mysql.set("DELETE FROM trans_character_recipebook WHERE char_id = " + old_char_id);
				mysql.set("DELETE FROM trans_character_shortcuts WHERE char_obj_id = " + old_char_id);
				mysql.set("DELETE FROM trans_character_skills WHERE char_obj_id = " + old_char_id);
				mysql.set("DELETE FROM trans_character_subclasses WHERE char_obj_id = " + old_char_id);
				mysql.set("DELETE FROM trans_character_variables WHERE obj_id = " + old_char_id);
				mysql.set("DELETE FROM trans_items WHERE owner_id = " + old_char_id);
			}
		}
		catch(Exception e)
		{
			_lastResult = "SQL Error";
			_log.warn("TransferCharacterTask: can't update sql: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
		finally
		{
			DbUtils.closeQuietly(stmt, rs);
		}
		_lastResult = "Character transfered.";
		return true;
	}

	@Override
	public void addTask(String params)
	{
	}


	@Override
	public String getLastResult()
	{
		String ret = _lastResult;
		_lastResult = "";
		return ret;
	}
}
