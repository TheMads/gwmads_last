package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.instances.L2DoorInstance;
import ru.l2gw.gameserver.model.instances.L2SiegeHeadquarterInstance;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Formulas;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author rage
 * @date 24.12.2009 11:01:02
 */
public class i_hp_drain extends i_effect
{
	private final double _absorbPart;
	public i_hp_drain(EffectTemplate template)
	{
		super(template);
		_absorbPart = template._attrs.getInteger("absorbPart", 0) / 100.;
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
		{
			if(env.target.isDead())
				continue;

			double hp = 0.;

			boolean shield = Formulas.calcShldUse(cha, env.target);
			double damage = Formulas.calcMagicDam(cha, env.target, getSkill(), shield, ss);
			double targetCP = env.target.getCurrentCp();

			if(damage > targetCP || !env.target.isPlayer())
				hp = (damage - targetCP) * _absorbPart;

			if(damage < 1)
				damage = 1;

			boolean blockHp = env.target.isStatActive(Stats.BLOCK_HP);
			if(blockHp)
				damage = 0;

			if(hp > env.target.getCurrentHp())
				hp = env.target.getCurrentHp();

			env.target.reduceHp(damage, cha, false, false);
			cha.sendDamageMessage(env.target, (int) damage, false, false, blockHp);

			hp += cha.getCurrentHp();

			if(!(env.target instanceof L2DoorInstance || env.target instanceof L2SiegeHeadquarterInstance) && !blockHp)
				cha.setCurrentHp(hp);
		}
	}
}
