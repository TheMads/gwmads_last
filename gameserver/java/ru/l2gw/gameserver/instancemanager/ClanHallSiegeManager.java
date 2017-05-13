package ru.l2gw.gameserver.instancemanager;

import ru.l2gw.commons.utils.DbUtils;
import ru.l2gw.database.DatabaseFactory;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.ClanHall;
import ru.l2gw.gameserver.model.entity.siege.ClanHall.ClanHallSiege;
import ru.l2gw.gameserver.model.entity.siege.Siege;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public final class ClanHallSiegeManager extends SiegeManager
{
	private static ClanHallSiegeManager _instance;

	public static ClanHallSiegeManager getInstance()
	{
		if(_instance == null)
			_instance = new ClanHallSiegeManager();

		return _instance;
	}

	public ClanHallSiegeManager()
	{
		load();
	}

	public static void reload()
	{
		load();
	}

	public static void load()
	{
		Statement statement = null;
		ResultSet rs = null;

		try
		{
			Connection con = null;
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.createStatement();

			for(ClanHall clanhall : ResidenceManager.getInstance().getClanHallList())
			{
				if(clanhall.getSiegeZone() == null)
					continue;
				ClanHallSiege s = clanhall.getSiege();
				s.setSiegeClanMinLevel(4);//Config.MIN_ClAN_LEVEL_FOR_CHSIEGE
				s.setSiegeLength(60);//Config.SIEGE_LEAGH in milliseconds
				s.setNextSiegePeriod(14);//Config.SIEGE_NEXT_SIEGE_PERIOD in days
				s.startAutoTask();
				//бред-снести как тока доделаем дп
				if(s.getZone() != null)
					s.getZone().setActive(false);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(statement, rs);
		}
	}

	public final boolean checkIfOkToSummon(L2Character cha, boolean isCheckOnly)
	{
		if(!(cha.isPlayer()))
			return false;

		SystemMessage sm = new SystemMessage(SystemMessage.S1);
		L2Player player = (L2Player) cha;
		ClanHall ch = ResidenceManager.getInstance().getClanHallByObjectInSiegeZone(player);

		if(ch == null || ch.getId() <= 0)
			sm.addString("You must be on Siege ground to summon this");
		else if(!ch.getSiege().isInProgress())
			sm.addString("You can only summon this during a siege.");
		else if(player.getClanId() != 0 && ch.getSiege().getAttackerClan(player.getClanId()) == null)
			sm.addString("You can only summon this as a registered attacker.");
		else
			return true;

		if(!isCheckOnly)
			player.sendPacket(sm);
		return false;
	}

	public static Siege getSiege(L2Object activeObject)
	{
		return getSiege(activeObject.getX(), activeObject.getY());
	}

	public static Siege getSiege(int x, int y)
	{
		ClanHall clanhall = ResidenceManager.getInstance().getClanHallBySiegeZoneCoord(x, y);
		if(clanhall != null)
			return clanhall.getSiege();
		return null;
	}
}