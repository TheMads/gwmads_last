package ru.l2gw.gameserver.ai;

import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.L2Summon;
import ru.l2gw.gameserver.model.instances.L2PetBabyInstance;
import ru.l2gw.gameserver.tables.SkillTable;

/**
 * @author rage
 * @date 01.03.2010 11:33:51
 */
public class L2BabyPet extends L2SummonAI
{
	protected L2PetBabyInstance _babyPet;
	private static final int heal1 = 4717;
	private static final int heal2 = 4718;
	protected boolean active;

	public L2BabyPet(L2Summon actor)
	{
		super(actor);
		if(actor instanceof L2PetBabyInstance)
		{
			_babyPet = (L2PetBabyInstance) actor;
			active = false;
			startAITask();
		}
	}

	@Override
	protected void onEvtTimer(int timerId, Object arg1, Object arg2)
	{
		if(active && timerId == 1 && _babyPet != null)
		{
			L2Player owner = _babyPet.getPlayer();
			if(owner != null && !owner.isDead() && !_babyPet.isDead() && !_babyPet.isCastingNow() && _babyPet.isInRange(owner, 1000))
			{
				if(Rnd.chance(25))
				{
					L2Skill heal = SkillTable.getInstance().getInfo(heal1, getSkillLevel());
					if(heal != null && !_babyPet.isSkillDisabled(heal1) && heal.getMpConsume() < _babyPet.getCurrentMp() && heal.getHpConsume() < _babyPet.getCurrentHp() && !owner.isDead() && owner.getCurrentHp() < owner.getMaxHp() * 0.80)
					{
						if(_babyPet.isInRange(owner, heal.getCastRange()) || _babyPet.getFollowStatus())
							Cast(heal, owner, null, false, false);
					}
				}
				else if(Rnd.chance(75))
				{
					L2Skill heal = SkillTable.getInstance().getInfo(heal2, getSkillLevel());
					if(heal != null && owner.isInCombat() && !_babyPet.isSkillDisabled(heal2) && heal.getMpConsume() < _babyPet.getCurrentMp() && heal.getHpConsume() < _babyPet.getCurrentHp() && !owner.isDead() && owner.getCurrentHp() < owner.getMaxHp() * 0.30)
						if(_babyPet.isInRange(owner, heal.getCastRange()) || _babyPet.getFollowStatus())
							Cast(heal, owner, null, false, false);
				}
			}
			addTimer(1, 1000);
		}
	}

	@Override
	public void stopAITask()
	{
		active = false;
	}

	@Override
	public void startAITask()
	{
		if(!active)
		{
			active = true;
			addTimer(1, 1000);
		}
	}

	protected int getSkillLevel()
	{
		int lvl;
		if(_actor.getLevel() < 70)
		{
			lvl = _actor.getLevel() / 10;
			if(lvl < 1)
				lvl = 1;
		}
		else
			lvl = 8 + (_actor.getLevel() - 70) / 5;

		if(lvl > 12)
			lvl = 12;

		return lvl;
	}
}
