package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2MonsterInstance;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.math.Rnd;

/**
 * @author rage
 * @date 26.11.2009 8:59:15
 */
public class i_harvesting extends i_effect
{
	public i_harvesting(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		L2Player player = cha.getPlayer();
		if(player == null)
			return;

		for(Env env : targets)
			if(env.target.isMonster())
			{
				L2MonsterInstance monster = (L2MonsterInstance) env.target;

				// Не посеяно
				if(!monster.isSeeded())
				{
					player.sendPacket(Msg.THE_HARVEST_FAILED_BECAUSE_THE_SEED_WAS_NOT_SOWN);
					continue;
				}

				if(!monster.isSeeded(player))
				{
					player.sendPacket(Msg.YOU_ARE_NOT_AUTHORIZED_TO_HARVEST);
					continue;
				}

				double SuccessRate = Config.MANOR_HARVESTING_BASIC_SUCCESS;
				int diffPlayerTarget = Math.abs(player.getLevel() - monster.getLevel());

				// Штраф, на разницу уровней между мобом и игроком
				// 5% на каждый уровень при разнице >5 - по умолчанию
				if(diffPlayerTarget > Config.MANOR_DIFF_PLAYER_TARGET)
					SuccessRate -= (diffPlayerTarget - Config.MANOR_DIFF_PLAYER_TARGET) * Config.MANOR_DIFF_PLAYER_TARGET_PENALTY;

				// Минимальный шанс успеха всегда 1%
				if(SuccessRate < 1)
					SuccessRate = 1;

				if(Config.SKILLS_SHOW_CHANCE)
					player.sendMessage(new CustomMessage("ru.l2gw.gameserver.skills.skillclasses.Harvesting.Chance", player).addNumber((int) SuccessRate));

				if(!Rnd.chance(SuccessRate))
				{
					player.sendPacket(Msg.THE_HARVEST_HAS_FAILED);
					monster.takeHarvest();
					continue;
				}

				L2ItemInstance[] items = monster.takeHarvest();

				if(items == null)
					continue;

				for(L2ItemInstance item : items)
				{
					long itemCount = item.getCount();
					item = player.getInventory().addItem("Harvest", item, player, monster);

					SystemMessage sm = new SystemMessage(SystemMessage.S1_HARVESTED_S3_S2_S).addString(player.getName()).addNumber(itemCount).addItemName(item.getItemId());
					player.sendPacket(sm);

					if(player.isInParty())
						player.getParty().broadcastToPartyMembers(player, sm);
				}
			}
	}
}
