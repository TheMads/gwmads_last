package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.Config;
import ru.l2gw.extensions.multilang.CustomMessage;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Manor;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2MonsterInstance;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;

/**
 * @author: rage
 * @date: 13.07.2010 14:45:02
 */
public class i_sowing extends i_effect
{
	public i_sowing(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		if(!cha.isPlayer())
			return;

		L2Player player = (L2Player) cha;
		int seed_id = player.getUseSeed();
		L2ItemInstance seedItem = player.getInventory().getItemByItemId(seed_id);

		if(seedItem == null || !player.destroyItem("Sowing", seedItem.getObjectId(), 1, null, true))
			return;

		for(Env env : targets)
		{
			if(!(env.target instanceof L2MonsterInstance))
				continue;

			L2MonsterInstance target = (L2MonsterInstance) env.target;

			// обработка
			double SuccessRate = Config.MANOR_SOWING_BASIC_SUCCESS;

			double diffPlayerTarget = Math.abs(player.getLevel() - target.getLevel());
			double diffSeedTarget = Math.abs(L2Manor.getInstance().getSeedLevel(seed_id) - target.getLevel());

			// Штраф, на разницу уровней между мобом и игроком
			// 5% на каждый уровень при разнице >5 - по умолчанию
			if(diffPlayerTarget > Config.MANOR_DIFF_PLAYER_TARGET)
				SuccessRate -= (diffPlayerTarget - Config.MANOR_DIFF_PLAYER_TARGET) * Config.MANOR_DIFF_PLAYER_TARGET_PENALTY;

			// Штраф, на разницу уровней между семечкой и мобом
			// 5% на каждый уровень при разнице >5 - по умолчанию
			if(diffSeedTarget > Config.MANOR_DIFF_SEED_TARGET)
				SuccessRate -= (diffSeedTarget - Config.MANOR_DIFF_SEED_TARGET) * Config.MANOR_DIFF_SEED_TARGET_PENALTY;

			if(ItemTable.getInstance().getTemplate(seed_id).isAltSeed())
			{
				SuccessRate *= Config.MANOR_SOWING_ALT_BASIC_SUCCESS / Config.MANOR_SOWING_BASIC_SUCCESS;
				SuccessRate = 100 - 1. / (100 - SuccessRate);
			}

			// Минимальный шанс успеха всегда 1%
			if(SuccessRate < 1)
				SuccessRate = 1;

			if(Config.SKILLS_SHOW_CHANCE)
				player.sendMessage(new CustomMessage("ru.l2gw.gameserver.skills.skillclasses.Sowing.Chance", player).addNumber((int) SuccessRate));

			if(Rnd.chance((int) SuccessRate))
			{
				target.setSeeded((short) seed_id, player);
				player.sendPacket(new SystemMessage(SystemMessage.THE_SEED_WAS_SUCCESSFULLY_SOWN));
			}
			else
				player.sendPacket(new SystemMessage(SystemMessage.THE_SEED_WAS_NOT_SOWN));
		}
	}
}
