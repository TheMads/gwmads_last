package ru.l2gw.gameserver.model.entity.vehicle;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Object;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.MyTargetSelected;

/**
 * @author rage
 * @date 08.09.2010 14:45:13
 */
public class L2AirShipHelm extends L2Object
{
	private final L2ClanAirship _airship;

	public L2AirShipHelm(Integer objectId, L2ClanAirship cas)
	{
		super(objectId);
		_airship = cas;
	}

	public L2ClanAirship getClanAirship()
	{
		return _airship;
	}

	@Override
	public boolean isAttackable(L2Character attacker, boolean forceUse, boolean sendMessage)
	{
		return false;
	}

	@Override
	public void onAction(L2Player player, boolean dontMove)
	{
		if(player.setTarget(this))
			player.sendPacket(new MyTargetSelected(getObjectId(), 0));
		else
			player.sendActionFailed();
	}
}
