package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Formulas;
import ru.l2gw.gameserver.skills.Stats;

/**
 * author rage
 * date 25.12.2009 10:20:09
 */
public class i_fatal_blow extends i_effect
{
	private final double baseChance;
	private final double critChance;

	public i_fatal_blow(EffectTemplate template)
	{
		super(template);
		baseChance = _template._attrs.getDouble("p2");
		critChance = _template._attrs.getDouble("p3");
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
		{
			if(env.target == null || env.target.isDead() || Formulas.calcDodge(cha, env.target, getSkill()))
				continue;

			if(!counter)
				env.target.doCounterAttack(getSkill(), cha);

			boolean success = Formulas.calcBlowChance(cha, env.target, baseChance);

			if(success)
			{
				double damage = (int) Formulas.calcBlowDamage(cha, env.target, getSkill(), ss > 0, false, critChance);

				boolean blockHp = env.target.isStatActive(Stats.BLOCK_HP);
				if(blockHp)
					damage = 0;

				if(damage > 0)
				{
					if(!env.target.isRaid() && Formulas.calcCastBreak(cha, env.target, damage))
					{
						env.target.breakAttack();
						env.target.breakCast(false, true);
					}

					if(env.target.isStunned() && Formulas.calcStunBreak(true))
						env.target.stopEffects("stun");
				}

				L2Character reflector = null;
				L2Character tgt = env.target;

				if(getSkill().getCastRange() <= 100 && tgt.checkReflectMeleeSkill(getSkill()))
				{
					reflector = tgt;
					tgt = cha.isCubic() ? cha.getPlayer() : cha;
				}

				if(!blockHp)
					tgt.reduceHp(damage, reflector != null ? reflector : cha, false, false);

				if(reflector != null)
					reflector.sendDamageMessage(tgt, (int) damage, false, true, blockHp);
				else
					cha.sendDamageMessage(tgt, (int) damage, false, true, blockHp);
			}
		}
	}
}
