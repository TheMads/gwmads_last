package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.extensions.listeners.MethodCollection;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.instances.L2DoorInstance;
import ru.l2gw.gameserver.model.instances.L2SiegeHeadquarterInstance;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Formulas;
import ru.l2gw.gameserver.skills.Stats;

/**
 * User: ic
 * Date: 23.04.2010
 */
public class i_heal extends i_effect
{
	public i_heal(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
		{
			if(env.target == null || env.target.isDead() || env.target instanceof L2DoorInstance || env.target instanceof L2SiegeHeadquarterInstance)
				continue;

			int mLevel = getSkill().getMagicLevel() > 0 ? getSkill().getMagicLevel() : cha.getLevel();
			double skillPower = getSkill().getPower(cha, env.target);
			int mAtkRequired = (int) ((mLevel * mLevel * mLevel * mLevel * -0.000033975f) + (mLevel * mLevel * mLevel * 0.007f) - (mLevel * mLevel * 0.225f) + (mLevel * 3.722f) + 2.437f);
			int spsMaxBonus = (int) (0.028f * mLevel * mLevel + 1.485f * mLevel + 15.44f);
			int mAtk = cha.getMAtk(null, null);
			int mAtkTest = mAtkRequired - mAtk;
			int spsEffect = 0;

			if(ss == 2)
			{
				mAtk *= 4;
				spsEffect = spsMaxBonus;
			}
			else if(ss == 1)
			{
				mAtk *= 2;
				spsEffect = (int) (spsMaxBonus * 0.41f);
			}

			if(mAtkTest > 0)
				spsEffect = (int) (spsEffect - mAtkTest * 0.48f);
			if(spsEffect < 0)
				spsEffect = 0;

			int mAtkBonus = (int) Math.sqrt(mAtk);

			//_log.info("mLevel: " + mLevel + " skillPower: " + skillPower + " mAtkReq: " + mAtkRequired + " spsMaxBonus: " + spsMaxBonus + " mAtkBonus: " + mAtkBonus);
			//_log.info("ss: " + ss + " mAtk: " + mAtk + " spsEffect: " + spsEffect + " mAtkTest: " + mAtkTest);


			double hp = mAtkBonus + skillPower + spsEffect;

			hp *= cha.calcStat(Stats.HEAL_POWER, 100, null, null) / 100;

			double newHp = Formulas.calcMCrit(cha.getCriticalMagic(env.target, getSkill())) ? hp * 3 : hp;

			newHp *= env.target.calcStat(Stats.HEAL_EFFECTIVNESS, 100, null, null) / 100;

			newHp += cha.calcStat(Stats.HEAL_POWER_STATIC, 0, null, null);
			newHp += env.target.calcStat(Stats.HEAL_EFFECTIVNESS_STATIC, 0, null, null);

			if(env.target.isStatActive(Stats.BLOCK_HP))
				newHp = 0;

			// Player holding a cursed weapon can't be healed and can't heal
			if(env.target != cha)
				if(env.target.isPlayer() && env.target.isCursedWeaponEquipped())
					newHp = 0;
				else if(cha.isPlayer() && cha.isCursedWeaponEquipped())
					newHp = 0;

			int hpLimit = (int) env.target.calcStat(Stats.HP_LIMIT, env.target.getMaxHp(), null, null);

			if(env.target.getCurrentHp() + newHp > hpLimit)
				newHp = hpLimit - env.target.getCurrentHp();

			if(newHp < 0)
				newHp = 0;

			env.target.setCurrentHp(newHp + env.target.getCurrentHp());
			if(env.target == cha && newHp >= 0)
				env.target.sendPacket(new SystemMessage(SystemMessage.S1_HP_HAVE_BEEN_RESTORED).addNumber((int) newHp));
			else if(newHp >= 0)
				env.target.sendPacket(new SystemMessage(SystemMessage.S2_HP_HAS_BEEN_RESTORED_BY_C1).addCharName(cha).addNumber((int) newHp));

			if(newHp > 0)
				cha.fireMethodInvoked(MethodCollection.onHeal, new Object[]{env.target, newHp});
		}
	}
}
