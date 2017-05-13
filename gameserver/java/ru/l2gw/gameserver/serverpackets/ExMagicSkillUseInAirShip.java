package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;

/**
 * Format: ddddddddddh[h]h[ddd]
 */
public class ExMagicSkillUseInAirShip extends MagicSkillUse
{
	private int _airShipId;

	public ExMagicSkillUseInAirShip(L2Character cha, L2Character target, int skillId, int skillLevel, int hitTime, long reuseDelay, boolean isBuff)
	{
		super(cha, target, skillId, skillLevel, hitTime, reuseDelay, isBuff);
		if(cha.isPlayer() && cha.isInAirShip())
		{
			L2Player player = cha.getPlayer();
			_airShipId = player.getVehicle().getObjectId();
			_x = player.getLocInVehicle().getX();
			_y = player.getLocInVehicle().getY();
			_z = player.getLocInVehicle().getZ();
		}
	}

	public ExMagicSkillUseInAirShip(L2Character cha, int skillId, int skillLevel, int hitTime, long reuseDelay)
	{
		super(cha, skillId, skillLevel, hitTime, reuseDelay);
		if(cha.isPlayer() && cha.isInAirShip())
		{
			L2Player player = cha.getPlayer();
			_airShipId = player.getVehicle().getObjectId();
			_x = player.getLocInVehicle().getX();
			_y = player.getLocInVehicle().getY();
			_z = player.getLocInVehicle().getZ();
		}
	}

	@Override
	protected void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x74);
		writeD(_chaId);
		writeD(_targetId);
		writeD(_skillId);
		writeD(_skillLevel);
		writeD(_hitTime);
		writeD((int) _reuseDelay);
		writeD(_x);
		writeD(_y);
		writeD(_z);
		writeD(_airShipId);
		writeH(0x00);
		writeH(0x00);
	}
}