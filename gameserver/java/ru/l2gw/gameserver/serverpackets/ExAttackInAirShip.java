package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;

public class ExAttackInAirShip extends Attack
{
	/*
	 * Format: dddcddddh[ddc]
	 */
	private int airShipId;

	public ExAttackInAirShip(L2Character attacker, L2Character target, boolean ss, int grade)
	{
		super(attacker, target, ss, grade);
		L2Player player = attacker.getPlayer();
		if(player != null)
		{
			airShipId = player.getVehicle() != null ? player.getVehicle().getObjectId() : 0;
			_x = player.getLocInVehicle().getX();
			_y = player.getLocInVehicle().getY();
			_z = player.getLocInVehicle().getZ();
		}
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x73);

		writeD(_attackerId);
		writeD(hits[0]._targetId);
		writeD(hits[0]._damage);
		writeC(hits[0]._flags);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeD(airShipId);
		writeH(hits.length - 1);
		for(int i = 1; i < hits.length; i++)
		{
			writeD(hits[i]._targetId);
			writeD(hits[i]._damage);
			writeC(hits[i]._flags);
		}
	}
}