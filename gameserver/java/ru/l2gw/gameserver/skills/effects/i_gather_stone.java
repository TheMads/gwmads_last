package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;

/**
 * @author ic
 */
public class i_gather_stone extends i_effect
{
	private final double chance;
	private final int min;
	private final int max;

	private static final int RED_EXTRACTED = 14009;
	private static final int BLUE_EXTRACTED = 14010;
	private static final int GREEN_EXTRACTED = 14011;

	public i_gather_stone(EffectTemplate template)
	{
		super(template);
		chance = _template._attrs.getDouble("chance", 0);
		min = _template._attrs.getInteger("min", 0);
		max = _template._attrs.getInteger("max", 0);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		if(!cha.isPlayer())
			return;

		L2Player player = (L2Player) cha;

		for(Env env : targets)
			if(env.target instanceof L2NpcInstance && env.target.getNpcId() >= 18684 && env.target.getNpcId() <= 18692)
			{
				int count = Rnd.get(min, max);
				boolean success = Rnd.chance(chance);
				if(!success || count < 1)
				{
					player.sendPacket(new SystemMessage(SystemMessage.THE_COLLECTION_HAS_FAILED));
					env.target.doDie(null);
					continue;
				}

				if(env.target.getNpcId() <= 18686) // RED
				{
					player.addItem("StarStoneGathering", RED_EXTRACTED, count, null, true);
				}
				else if(env.target.getNpcId() >= 18690) // GREEN
				{
					player.addItem("StarStoneGathering", GREEN_EXTRACTED, count, null, true);
				}
				else // BLUE
				{
					player.addItem("StarStoneGathering", BLUE_EXTRACTED, count, null, true);
				}

				env.target.doDie(null);
			}
	}
}
