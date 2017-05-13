package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.L2Playable;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Formulas;
import ru.l2gw.util.EffectsComparator;
import ru.l2gw.commons.math.Rnd;

import java.util.Arrays;

/**
 * @author: rage
 * @date: 15.07.2010 11:42:52
 */
public class i_steal_buff extends i_effect
{
	private final double chance;
	private final int max_count;

	public i_steal_buff(EffectTemplate template)
	{
		super(template);
		chance = template._attrs.getDouble("chance", 5);
		max_count = template._attrs.getInteger("max_count", 3);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		//byte mLevel = getSkill().getMagicLevel() == 0 ? cha.getLevel() : getSkill().getMagicLevel();
		for(Env env : targets)
		{
			if(!env.target.isPlayable() || env.target.getPlayer().isGM())
				continue;

			L2Effect[] effects = env.target.getAllEffectsArray();
			Arrays.sort(effects, EffectsComparator.getInstance());
			int count = 0;
			//double chance = getSkill().getActivateRate();
			//chance *= 0.01 * cha.calcStat(Stats.CANCEL_POWER, 100, null, null);
			//chance *= 0.01 * env.target.calcStat(Stats.CANCEL_RECEPTIVE, 100, null, null);
			//chance *= Formulas.getLevelMod(mLevel, env.target.getLevel());
			//if(Config.SKILLS_SHOW_CHANCE && cha.getPlayer() != null && cha.getPlayer().isGM())
			//	cha.sendMessage("Base chance: " + getSkill().getActivateRate() + " mod chance: " + chance);

			((L2Playable) env.target).setMassUpdating(true);
			for(int i = effects.length - 1; i >= 0; i--)
			{
				L2Effect e = effects[i];

				double ch = Formulas.calcCancelChance(cha, env.target, chance, getSkill().getMagicLevel(), e);

				if(!Rnd.chance(ch) || e.getSkill().getSkillTargetType() == L2Skill.TargetType.summon || e.getSkill().isDebuff()
						|| e.getSkill().isHeroSkill() || e.getSkill().isToggle()
						|| e.getSkill().isTriggered() || e.getSkill().getAbnormalTypes().contains("transformation")
						|| e.getSkillId() == 5104 || e.getSkillId() == 5105
						|| !e.getSkill().isCancelable())
					continue;

				count++;

				L2Effect stealedEffect = cloneEffect(cha, e);
				e.exit();

				if(stealedEffect != null)
					cha.addEffect(stealedEffect);

				if(count >= max_count)
					break;
			}
			((L2Playable) env.target).setMassUpdating(false);
			if(count > 0)
				env.target.updateEffectIcons();
		}
	}

	private L2Effect cloneEffect(L2Character cha, L2Effect eff)
	{
		L2Skill skill = eff.getSkill();
		L2Effect effect = skill.getTimedEffectTemplate().getEffect(new Env(cha, cha, skill));

		if(effect != null)
		{
			effect.setAbnormalTime(eff.getTimeLeft());
			return effect;
		}
		return null;
	}
}
