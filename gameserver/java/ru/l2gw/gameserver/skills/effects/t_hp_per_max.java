package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.entity.olympiad.Olympiad;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.serverpackets.ExRegenMax;
import ru.l2gw.gameserver.skills.Stats;

/**
 * @author rage
 * @date 02.06.11 15:19
 */
public class t_hp_per_max extends t_effect
{
	public t_hp_per_max(L2Effect effect, EffectTemplate template)
	{
		super(effect, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();

		if(getEffected().isPlayer() && getSkill().isPotion())
			getEffected().sendPacket(new ExRegenMax((int)(_effect.getAbnormalTime() / 1000), _template._ticks, calc()));
		startActionTask(3000);
	}

	@Override
	public boolean onActionTime()
	{
		if(getEffected().isDead())
			return false;

		double hp = calcTickVal();

		if(hp > 0) // heal
		{
			if(getEffected().isStatActive(Stats.BLOCK_HEAL))
				return true;

			int hpLimit = (int)getEffected().calcStat(Stats.HP_LIMIT, getEffected().getMaxHp(), null, null);
			int newHp = (int) (getEffected().getMaxHp() * hp * 0.01);

			if(getEffected().getCurrentHp() +  newHp > hpLimit)
				newHp = (int)(hpLimit - getEffected().getCurrentHp());

			if(newHp < 0)
				newHp = 0;

			getEffected().setCurrentHp(getEffected().getCurrentHp() + newHp);
			return true;
		}
		else // dot
		{
			if(getEffected().isStatActive(Stats.BLOCK_HP) || getEffected().isInvul())
				return true;

			boolean pvp = getEffected().isPlayable() && (getEffector().isPlayable() || getEffector().isCubic()) && getEffected() != getEffector();
			double damage = -getEffected().getMaxHp() * hp * 0.01;
			if(pvp)
			{
				damage = getEffected().getCurrentCp() - damage;
				hp = damage < 0 ? -damage : 0;

				if(damage < 0)
					damage = 0;

				getEffected().setCurrentCp(damage);

				if(getEffector().getPlayer().isOlympiadStart() && getEffected().getPlayer().isOlympiadStart() && getEffector().getPlayer().getOlympiadGameId() == getEffected().getPlayer().getOlympiadGameId())
					Olympiad.addReceivedDamage(getEffected().getPlayer().getOlympiadGameId(), getEffected().getPlayer().getObjectId(), (int) (-getEffected().getMaxHp() * calcTickVal() * 0.01));
			}
			else
				hp = damage;

			if(hp > 0)
			{
				if(getEffected().isNpc())
					((L2NpcInstance) getEffected()).addDamage(getEffector(), (int) hp);

				hp = Math.max(getEffected().getCurrentHp() - hp, 1);
				getEffected().setCurrentHp(hp);
			}

			getEffected().stopEffects("sleep", "meditation", "hide");
		}
		return true;
	}
}
