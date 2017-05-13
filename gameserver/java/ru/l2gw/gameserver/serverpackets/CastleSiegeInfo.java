package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Alliance;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;

import java.util.Calendar;

/**
 * Shows the Siege Info<BR>
 * <BR>
 * packet type id 0xc9<BR>
 * format: cdddSSdSdd<BR>
 * <BR>
 * c = c9<BR>
 * d = CastleID<BR>
 * d = Show Owner Controls (0x00 default || >=0x02(mask?) owner)<BR>
 * d = Owner ClanID<BR>
 * S = Owner ClanName<BR>
 * S = Owner Clan LeaderName<BR>
 * d = Owner AllyID<BR>
 * S = Owner AllyName<BR>
 * d = current time (seconds)<BR>
 * d = Siege time (seconds) (0 for selectable)<BR>
 * d = (UNKNOW) Siege Time Select Related
 */
public class CastleSiegeInfo extends L2GameServerPacket
{
	private SiegeUnit _siegeUnit;
	private long _startTime;
	private L2Player player;
	private int _id;
	private int _isOwner;
	private int _owner;
	private String _ownerName;
	private String _leaderName;
	private String ally_name;
	private int ally_id;

	public CastleSiegeInfo(SiegeUnit castle)
	{
		_siegeUnit = castle;
	}

	@Override
	final public void runImpl()
	{
		player = getClient().getPlayer();
		if(player == null)
			return;

		_ownerName = "";
		_leaderName = "";
		ally_name = "";
		ally_id = 0;

		if(_siegeUnit != null)
		{
			_id = _siegeUnit.getId();
			_isOwner = !_siegeUnit.isClanHall && _siegeUnit.getOwnerId() == player.getClanId() && player.isClanLeader() ? 0x01 : 0x00;
			_owner = _siegeUnit.getOwnerId();
			if(_siegeUnit.getOwnerId() > 0)
			{
				L2Clan owner = _siegeUnit.getOwner();
				if(owner != null)
				{
					_ownerName = owner.getName();
					_leaderName = owner.getLeaderName();
					L2Alliance ally = owner.getAlliance();
					if(ally != null)
					{
						ally_id = ally.getAllyId();
						ally_name = ally.getAllyName();
					}
				}
				else
					_log.warn("Null owner for castle: " + _siegeUnit.getName());
			}
		}

		if(_siegeUnit != null)
			_startTime = (int) (_siegeUnit.getSiege().getSiegeDate().getTimeInMillis() / 1000);
	}

	@Override
	protected final void writeImpl()
	{
		if(player == null)
			return;

		writeC(0xC9);
		writeD(_id);
		writeD(_isOwner);
		writeD(_owner);

		writeS(_ownerName); // Clan Name
		writeS(_leaderName); // Clan Leader Name
		writeD(ally_id); // Ally ID
		writeS(ally_name); // Ally Name
		writeD((int) (Calendar.getInstance().getTimeInMillis() / 1000));
		writeD((int) _startTime);
		writeD(0x00); //number of choices?
	}
}