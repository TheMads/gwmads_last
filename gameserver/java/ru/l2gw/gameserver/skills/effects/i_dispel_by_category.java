package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.L2Playable;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Formulas;
import ru.l2gw.util.EffectsComparator;
import ru.l2gw.commons.arrays.GArray;

import java.util.Arrays;

/**
 * @author ic
 * @date 23.12.2009
 */
public class i_dispel_by_category extends i_effect
{
	private final String _category;
	private final double _chance;
	private final int _maxSlots;

	public i_dispel_by_category(EffectTemplate template)
	{
		super(template);
		_category = template._attrs.getString("category", "");
		_chance = template._attrs.getInteger("chance", 0);
		_maxSlots = template._attrs.getInteger("maxSlots", 0);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		if(_category.equals(""))
			return;

		for(Env env : targets)
		{
			if(env.target == null || env.target.isDead())
				continue;


			if(_category.equalsIgnoreCase("slot_buff"))
			{
				int count = 0;
				//double chance = _chance;
				//chance *= 0.01 * cha.calcStat(Stats.CANCEL_POWER, 100, null, null);
				//chance *= 0.01 * env.target.calcStat(Stats.CANCEL_RECEPTIVE, 100, null, null);

				if(env.target instanceof L2Playable)
					((L2Playable) env.target).setMassUpdating(true);

				L2Effect[] effects = env.target.getAllEffectsArray();
				Arrays.sort(effects, EffectsComparator.getInstance());

				for(int i = effects.length - 1; i >= 0; i--)
				{
					L2Effect e = effects[i];

					if(e != null && !e.getSkill().isDebuff() && !e.getSkill().isToggle() && e.getSkill().isCancelable() && e.getSkill().getMagicLevel() > 0)
					{
						//double levelMod = Formulas.getLevelMod(getSkill().getMagicLevel(), e.getSkill().getMagicLevel()) > 1 ? 1 : Formulas.getLevelMod(getSkill().getMagicLevel(), e.getSkill().getMagicLevel());
						double chance = Formulas.calcCancelChance(cha, env.target, _chance, getSkill().getMagicLevel(), e);

						if(Rnd.chance(chance))
						{
							if(count < _maxSlots)
							{
								env.target.stopEffect(e.getSkillId());
								count++;
								if(count >= _maxSlots)
									break;
							}
						}
					}
				}
				if(env.target instanceof L2Playable)
				{
					((L2Playable) env.target).setMassUpdating(false);
					if(count > 0)
					{
						env.target.updateEffectIcons();
						env.target.sendChanges();
					}
				}
			}
			else if(_category.equalsIgnoreCase("slot_debuff"))
			{
				int count = 0;
				if(env.target instanceof L2Playable)
					((L2Playable) env.target).setMassUpdating(true);
				for(L2Effect e : env.target.getAllEffects())
					if(e != null && e.getSkill().isDebuff() && e.getSkill().getMagicLevel() > 0 && e.getSkill().isCancelable())
						if(Rnd.chance(_chance))
						{
							env.target.stopEffect(e.getSkillId());
							count++;
							if(_maxSlots > 0 && count == _maxSlots)
								break;
						}
				if(env.target instanceof L2Playable)
				{
					((L2Playable) env.target).setMassUpdating(false);
					if(count > 0)
					{
						env.target.updateEffectIcons();
						env.target.sendChanges();
					}
				}
			}
			else if(_category.equalsIgnoreCase("dance_song"))
			{
				int count = 0;
				if(env.target instanceof L2Playable)
					((L2Playable) env.target).setMassUpdating(true);
				for(L2Effect e : env.target.getAllEffects())
					if(e != null && e.getSkill().isSongDance())
						if(Rnd.chance(_chance))
						{
							env.target.stopEffect(e.getSkillId());
							count++;
						}
				if(env.target instanceof L2Playable)
				{
					((L2Playable) env.target).setMassUpdating(false);
					if(count > 0)
					{
						env.target.updateEffectIcons();
						env.target.sendChanges();
					}
				}
			}
		}
	}
}
