package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.tables.PetDataTable;
import ru.l2gw.gameserver.templates.L2EtcItem;

/**
 * @author rage
 * @date 23.11.2009 14:01:10
 */
public class ConditionCanSummonPet extends Condition
{
	@Override
	public boolean testImpl(Env env)
	{
		if(!env.first)
			return true;
		
		if(env.item == null || !env.character.isPlayer())
			return false;
		
		if(env.item.getItemType().equals(L2EtcItem.EtcItemType.PET_COLLAR))
		{
			L2Player player = env.character.getPlayer();

			if(player.isInBoat())
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_MAY_NOT_CALL_FORTH_A_PET_OR_SUMMONED_CREATURE_FROM_THIS_LOCATION));
				player.sendActionFailed();
				return false;
			}

			if(player.isInCombat())
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_SUMMON_DURING_COMBAT));
				return false;
			}

			if(player.isTradeInProgress() || player.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_SUMMON_DURING_A_TRADE_OR_WHILE_USING_THE_PRIVATE_SHOPS));
				return false;
			}

			if(player.isPetSummoned() || player.getMountEngine().isMounted())
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_MAY_NOT_USE_MULTIPLE_PETS_OR_SERVITORS_AT_THE_SAME_TIME));
				return false;
			}

			int npcId = PetDataTable.getSummonId(env.item);

			if(npcId == 0)
				return false;

			int petIdForItem = PetDataTable.getSummonId(env.item);
			if(petIdForItem  == PetDataTable.RED_STRIDER_STAR_ID || petIdForItem  == PetDataTable.RED_STRIDER_TWILIGHT_ID || petIdForItem  == PetDataTable.RED_STRIDER_WIND_ID || petIdForItem  == PetDataTable.WFENRIR_WOLF_ID || petIdForItem  == PetDataTable.WGREAT_WOLF_ID)
			{
				SiegeUnit ch = ResidenceManager.getInstance().getResidenceByOwner(player.getClanId(), true);
				if(ch == null || !(ch.getLocation().equals("Aden") || ch.getLocation().equals("Rune")) || ch.getSiegeZone() != null)
					return false;
			}

			return true;
		}

		return false;
	}
}
