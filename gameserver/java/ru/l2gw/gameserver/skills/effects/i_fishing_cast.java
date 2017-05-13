package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.controllers.GameTimeController;
import ru.l2gw.gameserver.model.FishData;
import ru.l2gw.gameserver.model.Inventory;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.ExFishingStart;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.tables.FishTable;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;

import java.util.List;

/**
 * @author: rage
 * @date: 15.07.2010 12:13:48
 */
public class i_fishing_cast extends i_effect
{
	public i_fishing_cast(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		if(!cha.isPlayer())
			return;

		L2Player player = (L2Player) cha;

		L2ItemInstance lure = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if(lure == null || lure.getCount() < 1)
		{
			player.sendPacket(new SystemMessage(SystemMessage.BAITS_ARE_NOT_PUT_ON_A_HOOK));
			return;
		}

		L2ItemInstance lure2 = player.getInventory().destroyItem("Consume", player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND), 1, player, null);
		if(lure2 == null || lure2.getCount() == 0)
			player.sendPacket(new SystemMessage(SystemMessage.NOT_ENOUGH_BAIT));

		int lvl = FishTable.getInstance().GetRandomFishLvl(player);
		int group = FishTable.getInstance().GetGroupForLure(lure.getItemId());
		int type = FishTable.getInstance().GetRandomFishType(group, lure.getItemId());

		List<FishData> fishs = FishTable.getInstance().getfish(lvl, type, group);
		if(fishs == null || fishs.size() == 0)
		{
			player.sendMessage("Error: Fishes are not definied, report admin please.");
			player.endFishing(false);
			return;
		}

		int check = Rnd.get(fishs.size());
		FishData fish = fishs.get(check);

		if(!GameTimeController.getInstance().isNowNight() && lure.isNightLure())
			fish.setType(-1);

		player.stopMove();
		player.setImobilised(true);
		player.setFishing(true);
		player.setFish(fish);
		player.setLure(lure);
		player.broadcastUserInfo(true);
		player.broadcastPacket(new ExFishingStart(player, fish.getType(), player.getFishLoc(), lure.isNightLure()));
		player.sendPacket(new SystemMessage(SystemMessage.STARTS_FISHING));
		player.startLookingForFishTask();
	}
}
