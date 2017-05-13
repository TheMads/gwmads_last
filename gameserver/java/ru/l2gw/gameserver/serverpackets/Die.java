package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.instancemanager.SiegeManager;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.gmaccess.AdminTemplateManager;
import ru.l2gw.gameserver.model.instances.L2MonsterInstance;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.templates.L2Item;

/**
 * Пример:
 * 00
 * 8b 22 90 48 objectId
 * 01 00 00 00
 * 00 00 00 00
 * 00 00 00 00
 * 00 00 00 00
 * 00 00 00 00
 * 00 00 00 00
 * 00 00 00 00
 * format  dddddddd   rev 828
 */
public class Die extends L2GameServerPacket
{
	private int _chaId;
	private boolean _fake;
	private boolean _sweepable;
	private int _access;
	private L2Clan _clan;
	private int to_hideaway, to_castle, to_siege_HQ, to_fortress, to_village;

	public Die(L2Character cha)
	{
		L2Player player = null;
		if(cha.isPlayer())
		{
			player = (L2Player) cha;
			if(player.isInOlympiadMode() || player.isActionBlocked(L2Zone.BLOCKED_SKILL_RESURRECT) || player.getSessionVar("event_no_res") != null)
			{
				_chaId = cha.getObjectId();
				_fake = false;
				to_castle = 0;
				to_hideaway = 0;
				to_siege_HQ = 0;
				to_fortress = 0;
				to_village = 0;
				return;
			}
			_access = AdminTemplateManager.checkBoolean("resurrectFixed", player) || player.getItemCountByItemId(L2Item.ITEM_ID_PHOENIX_FEATHER) > 0 ? 0x01 : 0x00;
			_clan = player.getClan();
		}
		_chaId = cha.getObjectId();
		_fake = !cha.isDead();
		if(cha.isMonster())
			_sweepable = ((L2MonsterInstance) cha).isSweepActive();

		if(_clan != null && cha.getX() > -166168)
		{
			to_hideaway = _clan.getHasUnit(1) ? 0x01 : 0x00;
			to_castle = _clan.getHasUnit(2) || SiegeManager.getCastleDefenderSiegeUnit(_clan.getClanId()) != null || (TerritoryWarManager.getWar().isInProgress() && player.getTerritoryId() > 0) ? 0x01 : 0x00;
			to_siege_HQ = _clan.getCamp() != null && _clan.getCamp().isInZone(L2Zone.ZoneType.headquarters) || (TerritoryWarManager.getWar().isInProgress() && player.getTerritoryId() > 0 && ResidenceManager.getInstance().getBuildingById(player.getTerritoryId() - 80).getOwner().getCamp() != null) ? 0x01 : 0x00;
			to_fortress = _clan.getHasUnit(3) ? 0x01 : 0x00;
		}
		else
		{
			to_hideaway = 0;
			to_castle = 0;
			to_siege_HQ = 0;
			to_fortress = 0;
		}
		to_village = 1;
	}

	@Override
	protected final void writeImpl()
	{
		if(_fake)
			return;

		writeC(0x00);
		writeD(_chaId);
		writeD(to_village); // to nearest village
		writeD(to_hideaway); // to hide away
		writeD(to_castle); // to castle
		writeD(to_siege_HQ); // to siege HQ
		writeD(_sweepable ? 0x01 : 0x00); // sweepable  (blue glow)
		writeD(_access); // Use Feather
		writeD(to_fortress); // fortress
		// Freya
		writeC(0); //show die animation
		writeD(0x00); //agathion ress button
		writeD(0x00); // additional free space
	}
}