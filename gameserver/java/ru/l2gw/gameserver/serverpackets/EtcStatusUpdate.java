package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;

/**
 * @Author: Death
 * @Date: 31/1/2007
 * @Time: 16:40:20
 */
public class EtcStatusUpdate extends L2GameServerPacket
{
	/**
	 *
	 * Packet for lvl 3 client buff line
	 *
	 * Example:(C4)
	 * F9 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 - empty statusbar
	 * F9 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 - increased force lvl 1
	 * F9 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 - weight penalty lvl 1
	 * F9 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 - chat banned
	 * F9 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 - Danger Area lvl 1
	 * F9 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 - lvl 1 grade penalty
	 *
	 * packet format: cdd //and last three are ddd???
	 *
	 * Some test results:
	 * F9 07 00 00 00 04 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 - lvl 7 increased force lvl 4 weight penalty
	 *
	 * Example:(C5 709)
	 * F9 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 00 00 00 0F 00 00 00 - lvl 1 charm of courage lvl 15 Death Penalty
	 *
	 *
	 * NOTE:
	 * End of buff:
	 * You must send empty packet
	 * F9 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
	 * to remove the statusbar or just empty value to remove some icon.
	 */

	private int increasedForce, weightPenalty, messageRefusal, dangerArea;
	private int weaponPenalty, armorPenalty, charmOfCourage, deathPenaltyLevel, consumedSouls;
	private boolean can_writeImpl = false;
	private L2Player _player;

	public EtcStatusUpdate(L2Player player)
	{
		if(player == null)
			return;
		_player = player;
	}

	@Override
	final public void runImpl()
	{
		if(_player == null)
			return;

		increasedForce = _player.getIncreasedForce();
		weightPenalty = _player.getWeightPenalty();
		messageRefusal = _player.getMessageRefusal() || _player.getNoChannel() != 0 ? 1 : 0;
		dangerArea = _player.isInDangerArea() ? 1 : 0;
		weaponPenalty = _player.getWeaponPenalty();
		armorPenalty = _player.getArmorPenalty();
		charmOfCourage = _player.isCharmOfCourage() ? 1 : 0;
		if(_player.getDeathPenalty() != null)
			deathPenaltyLevel = _player.getDeathPenalty().getLevel();
		else
			deathPenaltyLevel = 0;
		consumedSouls = _player.getConsumedSouls();
		can_writeImpl = true;
	}

	@Override
	protected final void writeImpl()
	{
		if(!can_writeImpl)
			return;

		// dddddddd
		writeC(0xF9); //Packet type
		writeD(increasedForce); // skill id 4271, 7 lvl
		writeD(weightPenalty); // skill id 4270, 4 lvl
		writeD(messageRefusal); //skill id 4269, 1 lvl
		writeD(dangerArea); // skill id 4268, 1 lvl
		writeD(weaponPenalty);
		writeD(armorPenalty);
		writeD(charmOfCourage); //Charm of Courage, "Prevents experience value decreasing if killed during a siege war".
		writeD(deathPenaltyLevel); //Death Penalty max lvl 15, "Combat ability is decreased due to death."
		writeD(consumedSouls);
	}
}