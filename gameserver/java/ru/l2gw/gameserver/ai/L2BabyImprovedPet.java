package ru.l2gw.gameserver.ai;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.L2Summon;
import ru.l2gw.gameserver.tables.PetDataTable;
import ru.l2gw.gameserver.tables.SkillTable;

/**
 * @author rage
 * @date 01.03.2010 11:34:42
 */
public class L2BabyImprovedPet extends L2BabyPet
{
	private static final int heal1 = 5195;
	private static final int heal2 = 5590;
	private static final int recharge = 5200;

	private static final int buffControl = 5771;

	private static final int petHaste = 5186; // 1-2
	private static final int petVampiricRage = 5187; // 1-4
	private static final int petRegeneration = 5188; // 1-3
	private static final int petBlessedBody = 5189; // 1-6
	private static final int petBlessedSoul = 5190; // 1-6
	private static final int petGuidance = 5191; // 1-3
	private static final int petWindWalk = 5192; // 1-2
	private static final int petAcumen = 5193; // 1-3
	private static final int petEmpower = 5194; // 1-3
	private static final int petConcentration = 5201; // 1-3
	private static final int petMight = 5586; // 1-3
	private static final int petShield = 5587; // 1-3
	private static final int petFocus = 5588; // 1-3
	private static final int petDeathWisper = 5589; // 1-3

	private static final L2Skill[][] BUFFALO_BUFFS = {
			{SkillTable.getInstance().getInfo(petMight, 3), SkillTable.getInstance().getInfo(petBlessedBody, 6)},
			{SkillTable.getInstance().getInfo(petMight, 3), SkillTable.getInstance().getInfo(petBlessedBody, 6), SkillTable.getInstance().getInfo(petShield, 3), SkillTable.getInstance().getInfo(petGuidance, 3)},
			{SkillTable.getInstance().getInfo(petMight, 3), SkillTable.getInstance().getInfo(petBlessedBody, 6), SkillTable.getInstance().getInfo(petShield, 3), SkillTable.getInstance().getInfo(petGuidance, 3), SkillTable.getInstance().getInfo(petVampiricRage, 4), SkillTable.getInstance().getInfo(petHaste, 2)},
			{SkillTable.getInstance().getInfo(petMight, 3), SkillTable.getInstance().getInfo(petBlessedBody, 6), SkillTable.getInstance().getInfo(petShield, 3), SkillTable.getInstance().getInfo(petGuidance, 3), SkillTable.getInstance().getInfo(petVampiricRage, 4), SkillTable.getInstance().getInfo(petHaste, 2), SkillTable.getInstance().getInfo(petFocus, 3), SkillTable.getInstance().getInfo(petDeathWisper, 3)}
	};

	private static final L2Skill[][] KOOKABURRA_BUFFS = {
			{SkillTable.getInstance().getInfo(petEmpower, 3), SkillTable.getInstance().getInfo(petBlessedSoul, 6)},
			{SkillTable.getInstance().getInfo(petEmpower, 3), SkillTable.getInstance().getInfo(petBlessedSoul, 6), SkillTable.getInstance().getInfo(petBlessedBody, 6), SkillTable.getInstance().getInfo(petShield, 3)},
			{SkillTable.getInstance().getInfo(petEmpower, 3), SkillTable.getInstance().getInfo(petBlessedSoul, 6), SkillTable.getInstance().getInfo(petBlessedBody, 6), SkillTable.getInstance().getInfo(petShield, 3), SkillTable.getInstance().getInfo(petAcumen, 3), SkillTable.getInstance().getInfo(petConcentration, 6)},
			{SkillTable.getInstance().getInfo(petEmpower, 3), SkillTable.getInstance().getInfo(petBlessedSoul, 6), SkillTable.getInstance().getInfo(petBlessedBody, 6), SkillTable.getInstance().getInfo(petShield, 3), SkillTable.getInstance().getInfo(petAcumen, 3), SkillTable.getInstance().getInfo(petConcentration, 6), SkillTable.getInstance().getInfo(petRegeneration, 3), SkillTable.getInstance().getInfo(petWindWalk, 2)} //FIXME: need info about last two skills
	};

	private static final L2Skill[][] COUGAR_BUFFS = {
			{SkillTable.getInstance().getInfo(petEmpower, 3), SkillTable.getInstance().getInfo(petMight, 3)},
			{SkillTable.getInstance().getInfo(petEmpower, 3), SkillTable.getInstance().getInfo(petMight, 3), SkillTable.getInstance().getInfo(petShield, 3), SkillTable.getInstance().getInfo(petBlessedBody, 6)},
			{SkillTable.getInstance().getInfo(petEmpower, 3), SkillTable.getInstance().getInfo(petMight, 3), SkillTable.getInstance().getInfo(petShield, 3), SkillTable.getInstance().getInfo(petBlessedBody, 6), SkillTable.getInstance().getInfo(petAcumen, 3), SkillTable.getInstance().getInfo(petHaste, 2)},
			{SkillTable.getInstance().getInfo(petEmpower, 3), SkillTable.getInstance().getInfo(petMight, 3), SkillTable.getInstance().getInfo(petShield, 3), SkillTable.getInstance().getInfo(petBlessedBody, 6), SkillTable.getInstance().getInfo(petAcumen, 3), SkillTable.getInstance().getInfo(petHaste, 2), SkillTable.getInstance().getInfo(petVampiricRage, 4), SkillTable.getInstance().getInfo(petFocus, 3)}
	};

	private L2Skill[][] buffs;

	public L2BabyImprovedPet(L2Summon actor)
	{
		super(actor);
		if(actor.getNpcId() == PetDataTable.IMPROVED_BABY_BUFFALO_ID)
			buffs = BUFFALO_BUFFS;
		else if(actor.getNpcId() == PetDataTable.IMPROVED_BABY_KOOKABURRA_ID)
			buffs = KOOKABURRA_BUFFS;
		else if(actor.getNpcId() == PetDataTable.IMPROVED_BABY_COUGAR_ID)
			buffs = COUGAR_BUFFS;
	}

	@Override
	protected void onEvtTimer(int timerId, Object arg1, Object arg2)
	{
		if(active && timerId == 1 && _babyPet != null)
		{
			L2Player owner = _babyPet.getPlayer();
			if(owner != null && !owner.isDead() && !_babyPet.isCastingNow() && !_babyPet.isDead() && _babyPet.isInRange(owner, 1000))
			{
				if(owner.isInCombat() && !_babyPet.isCastingNow())
				{
					if(_babyPet.getNpcId() == PetDataTable.IMPROVED_BABY_KOOKABURRA_ID || _babyPet.getNpcId() == PetDataTable.IMPROVED_BABY_COUGAR_ID)
					{
						if(owner.getCurrentHp() < owner.getMaxHp() * 0.30)
						{
							L2Skill heal = SkillTable.getInstance().getInfo(heal2, getSkillLevel());
							if(heal != null && !_babyPet.isSkillDisabled(heal2) && heal.getMpConsume() < _babyPet.getCurrentMp() && heal.getHpConsume() < _babyPet.getCurrentHp() && owner.getCurrentHp() < owner.getMaxHp() * 0.80)
								if(_babyPet.isInRange(owner, heal.getCastRange()) || _babyPet.getFollowStatus())
									Cast(heal, owner, null, false, false);
						}
						else if(owner.getCurrentMp() < owner.getMaxMp() * 0.60)
						{
							L2Skill heal = SkillTable.getInstance().getInfo(recharge, _babyPet.getRechargeLevel());
							if(heal != null && owner.isInCombat() && !_babyPet.isSkillDisabled(recharge) && heal.getMpConsume() < _babyPet.getCurrentMp() && heal.getHpConsume() < _babyPet.getCurrentHp() && owner.getCurrentMp() < owner.getMaxMp() * 0.60)
								if(_babyPet.isInRange(owner, heal.getCastRange()) || _babyPet.getFollowStatus())
									Cast(heal, owner, null, false, false);
						}
					}
					else
					{
						if(owner.getCurrentHp() < owner.getMaxHp() * 0.70 && owner.getCurrentHp() >= owner.getMaxHp() * 0.30)
						{
							L2Skill heal = SkillTable.getInstance().getInfo(heal1, getSkillLevel());
							if(heal != null && !_babyPet.isSkillDisabled(heal1) && heal.getMpConsume() < _babyPet.getCurrentMp() && heal.getHpConsume() < _babyPet.getCurrentHp() && owner.getCurrentHp() < owner.getMaxHp() * 0.80)
								if(_babyPet.isInRange(owner, heal.getCastRange()) || _babyPet.getFollowStatus())
									Cast(heal, owner, null, false, false);
						}
						else if(owner.getCurrentHp() < owner.getMaxHp() * 0.30)
						{
							L2Skill heal = SkillTable.getInstance().getInfo(heal2, getSkillLevel());
							if(heal != null && owner.isInCombat() && !_babyPet.isSkillDisabled(heal2) && heal.getMpConsume() < _babyPet.getCurrentMp() && heal.getHpConsume() < _babyPet.getCurrentHp() && owner.getCurrentHp() < owner.getMaxHp() * 0.30)
							{
								if(_babyPet.isInRange(owner, heal.getCastRange()) || _babyPet.getFollowStatus())
									Cast(heal, owner, null, false, false);
							}
						}
					}
				}

				if(!_babyPet.isCastingNow() && _babyPet.getEffectBySkillId(buffControl) == null)
				{
					L2Skill[] buff = buffs[_babyPet.getBuffLevel()];
					for(L2Skill skill : buff)
						if(!_babyPet.isSkillDisabled(skill.getId()) && skill.getMpConsume() < _babyPet.getCurrentMp() && skill.getHpConsume() < _babyPet.getCurrentHp() && owner.getEffectByAbnormalType(skill.getAbnormalTypes().get(0)) == null)
						{
							if(_babyPet.isInRange(owner, skill.getCastRange()) || _babyPet.getFollowStatus())
								Cast(skill, owner, null, false, false);
							break;
						}
				}
			}
			addTimer(1, 1000);
		}
	}
}
