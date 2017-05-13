package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2PetInstance;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.tables.PetDataTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

/**
 * @author rage
 * @date 23.11.2009 14:10:54
 */
public class i_summon_pet extends i_effect
{
	public i_summon_pet(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		L2Player player = cha.getPlayer();

		if(player == null)
			return;

		L2ItemInstance item = targets.get(0).item;

		if(player.isPetSummoned() || player.getMountEngine().isMounted() || item == null)
			return;

		int npcId = PetDataTable.getSummonId(item);

		if(npcId == 0)
			return;

		L2NpcTemplate petTemplate = NpcTable.getTemplate(npcId);
		L2PetInstance newpet = L2PetInstance.spawnPet(petTemplate, player, item);

		if(newpet == null)
			return;

		newpet.setTitle(player.getName());

		if(!newpet.isRespawned())
			try
			{
				newpet.setCurrentHp(newpet.getMaxHp());
				newpet.setCurrentMp(newpet.getMaxMp());
				newpet.setExp(newpet.getExpForThisLevel());
				newpet.setCurrentFed(newpet.getMaxMeal());
				newpet.store(player.getObjectId());
			}
			catch(NullPointerException e)
			{
				_log.warn("PetSummon: failed set stats for summon " + npcId + ".");
				return;
			}

		newpet.setRunning();
		player.setPet(newpet);

		if(newpet.getCurrentFed() < newpet.getMaxMeal() * 0.36)
			player.sendPacket(Msg.YOUR_PET_IS_VERY_HUNGRY_PLEASE_BE_CAREFUL);

		newpet.spawnMe(GeoEngine.findPointToStay(player.getX(), player.getY(), player.getZ(), 40, 40, player.getReflection()));
		newpet.broadcastPetInfo();
		newpet.setShowSpawnAnimation(false);
		newpet.startFeed();
		newpet.setFollowStatus(true);
	}
}
