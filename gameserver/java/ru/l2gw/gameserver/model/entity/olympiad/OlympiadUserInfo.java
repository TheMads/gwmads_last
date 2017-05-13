package ru.l2gw.gameserver.model.entity.olympiad;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.model.L2Player;

import java.lang.ref.WeakReference;

/**
 * @author rage
 * @date 04.05.11 10:16
 */
public class OlympiadUserInfo
{
	private final WeakReference<L2Player> _player;
	private final int _objectId;
	private final int _clanId;
	private int _receivedDamage;
	private int _matchPoints;
	private final String _name;
	private final String _clanName;
	private final String HWID;

	public OlympiadUserInfo(L2Player player)
	{
		_player = new WeakReference<>(player);
		_objectId = player.getObjectId();
		_clanId = player.getClanId();
		_name = player.getName();
		_clanName = player.getClanId() > 0 ? player.getClan().getName() : "";
		HWID = player.getLastHWID();
	}

	public L2Player getPlayer()
	{
		return _player.get();
	}

	public int getObjectId()
	{
		return _objectId;
	}

	public String getName()
	{
		return _name;
	}

	public String getClanName()
	{
		return _clanName;
	}

	public int getDamage()
	{
		return _receivedDamage;
	}

	public void addDamage(int damage)
	{
		_receivedDamage += damage;
	}

	public int getMatchPoints()
	{
		return _matchPoints;
	}

	public void setMatchPoint(int points)
	{
		_matchPoints = points;
	}

	public int getPoints()
	{
		return Olympiad._nobles.get(_objectId).getInteger("points");
	}

	public String getHWID()
	{
		return HWID;
	}

	public void setPoints(int points)
	{
		Olympiad._nobles.get(_objectId).set("points", Math.min(points, Config.ALT_OLY_MAX_POINTS));
	}

	public int getClassId()
	{
		return Olympiad._nobles.get(_objectId).getInteger("class_id");
	}

	public int getClanId()
	{
		return _clanId;
	}

	public void updateMatches(int gameType)
	{
		if(gameType == 0)
			Olympiad._nobles.get(_objectId).set("team_matches", Olympiad._nobles.get(_objectId).getInteger("team_matches") + 1);
		else if(gameType == 1)
			Olympiad._nobles.get(_objectId).set("ncb_matches", Olympiad._nobles.get(_objectId).getInteger("ncb_matches") + 1);
		else
			Olympiad._nobles.get(_objectId).set("cb_matches", Olympiad._nobles.get(_objectId).getInteger("cb_matches") + 1);
	}
}
