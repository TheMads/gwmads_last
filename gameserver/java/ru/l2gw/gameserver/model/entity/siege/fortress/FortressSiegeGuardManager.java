package ru.l2gw.gameserver.model.entity.siege.fortress;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.model.L2Spawn;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.model.entity.siege.reinforce.GuardReinforce;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

/**
 * @author rage
 * @date 30.06.2009 10:07:34
 */
public class FortressSiegeGuardManager
{
	private static Log _log = LogFactory.getLog(FortressSiegeGuardManager.class.getName());

	private SiegeUnit _fortress;
	private Map<Integer, FastList<L2Spawn>> _siegeGuardSpawn = new FastMap<Integer, FastList<L2Spawn>>();

	public FortressSiegeGuardManager(SiegeUnit fortress)
	{
		_fortress = fortress;
		loadSiegeGuards();
	}

	private void loadSiegeGuards()
	{
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT * FROM siege_guards WHERE unitId = ? ORDER BY isHired");
			statement.setInt(1, _fortress.getId());
			rset = statement.executeQuery();

			L2Spawn spawn;
			L2NpcTemplate template;

			while(rset.next())
			{
				template = NpcTable.getTemplate(rset.getInt("npcId"));
				if(template != null)
				{
					FastList<L2Spawn> list;
					if(_siegeGuardSpawn.containsKey(rset.getInt("isHired")))
						list = _siegeGuardSpawn.get(rset.getInt("isHired"));
					else
						list = new FastList<L2Spawn>();

					spawn = new L2Spawn(template);
					spawn.setId(rset.getInt("id"));
					spawn.setAmount(1);
					spawn.setLocx(rset.getInt("x"));
					spawn.setLocy(rset.getInt("y"));
					spawn.setLocz(rset.getInt("z"));
					spawn.setHeading(rset.getInt("heading"));
					spawn.setRespawnDelay(rset.getInt("respawnDelay"));
					spawn.setLocation(0);

					list.add(spawn);

					_siegeGuardSpawn.put(rset.getInt("isHired"), list);
				}
				else
					_log.warn("Missing npc data in npc table for id: " + rset.getInt("npcId"));
			}
		}
		catch(Exception e1)
		{
			_log.warn("Error loading siege guard for " + _fortress + ":" + e1);
			e1.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public void unspawnSiegeGuard()
	{
		for(FastList<L2Spawn> list : _siegeGuardSpawn.values())
		{
			for(L2Spawn spawn : list)
			{
				spawn.stopRespawn();
				spawn.despawnAll();
			}
		}
	}

	public void spawnSiegeGuard()
	{
		unspawnSiegeGuard();

		for(L2Spawn spawn : _siegeGuardSpawn.get(0))
		{
			spawn.init();
			if(spawn.getRespawnTime() == 0)
				spawn.stopRespawn();
		}

		if(_fortress.getOwnerId() == 0)
			for(L2Spawn spawn : _siegeGuardSpawn.get(5))
			{
				spawn.init();
				if(spawn.getRespawnTime() == 0)
					spawn.stopRespawn();
			}

		for(GuardReinforce gr : _fortress.getGuardReinforce())
		{
			if(gr.getLevel() > 0 || _fortress.getOwnerId() == 0)
			{
				for(int lvl = 1; lvl <= (_fortress.getOwnerId() > 0 ? gr.getLevel() : gr.getMaxLevel()); lvl++)
				{
					int group = Integer.parseInt(gr.getId() + "" + lvl);
					if(_siegeGuardSpawn.containsKey(group))
					{
						for(L2Spawn spawn : _siegeGuardSpawn.get(group))
						{
							spawn.init();
							if(spawn.getRespawnTime() == 0)
								spawn.stopRespawn();
						}
					}
				}
			}
		}
	}

}
