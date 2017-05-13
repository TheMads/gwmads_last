package ru.l2gw.gameserver.ai;

import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.templates.L2Weapon;
import ru.l2gw.gameserver.templates.L2Weapon.WeaponType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.l2gw.gameserver.ai.CtrlIntention.AI_INTENTION_ACTIVE;

/**
 * This class manages AI of L2NpcInstance.<BR><BR>
 */
public class Ranger extends DefaultAI
{
	private L2Skill[] _dam_skills;

	public Ranger(L2Character actor)
	{
		super(actor);

		_dam_skills = msum(_pdam_skills, _mdam_skills);
	}

	@Override
	protected boolean thinkActive()
	{
		if(super.thinkActive())
			return true;

		// BUFF
		if(_selfbuff_skills.length > 0 && Rnd.chance(10))
		{
			L2Skill r_skill = _selfbuff_skills[Rnd.get(_selfbuff_skills.length)];
			if(_thisActor.getEffectBySkill(r_skill) == null)
			{
				// Добавить новое задание
				addUseSkillDesire(_thisActor, r_skill, 1, 1, DEFAULT_DESIRE * 2);
				return true;
			}
		}

		return false;
	}

	@Override
	protected boolean doTask()
	{
		try
		{
			if(_task_list.size() == 0 || _task_list.first() == null)
				clearTasks();
		}
		catch(Exception e)
		{
			clearTasks();
		}
		
		if(_task_list.size() < 1)
			return true;

		L2Character RunAndAtkT = null;

		Task currentTask = _task_list.first();
		if(currentTask != null && currentTask.type == TaskType.MOVE)
		{
			RunAndAtkT = currentTask.getTarget();
			return CanIMove(RunAndAtkT) && super.doTask();
		}
		return super.doTask();
	}

	private boolean CanIMove(L2Character attaker)
	{
		if(attaker == null || attaker == _thisActor || attaker.isAlikeDead() || !_thisActor.isInRange(attaker, 2000))
			return true;

		if(!_thisActor.getAggroList().containsKey(attaker.getObjectId()))
			return true;

		if(attaker.isActionsBlocked() || attaker.isStunned() || attaker.isParalyzed() || attaker.isMageClass())
			return false;
		L2Weapon weap = attaker.getActiveWeaponItem();
		if(weap != null)
		{
			//TODO убрать этот try оно для дебага
			try
			{
				//на случай если во время выполнения этого кода человек снимет оружие или поменяет
				synchronized (weap)
				{
					if(weap.getItemType() == WeaponType.BOW || weap.getItemType() == WeaponType.CROSSBOW)
						return false;
				}
			}
			catch(Exception NPE)
			{
				_log.warn("find NPE in Ranger");
				if(attaker == null)
					_log.info("attacker is null");
				if(weap == null)
					_log.info("weapon is null for " + attaker.toString());
				if(weap.getItemType() == null)
					_log.info("weaponType is null for " + weap.toString());
				return true;
			}
		}
		return true;
	}

	@Override
	protected boolean createNewTask()
	{
		// Удаляем все задания
		clearTasks();

		L2Character _temp_attack_target = getAttackTarget();

		// Новая цель исходя из агрессивности
		L2Character hated = _thisActor.isConfused() ? _temp_attack_target : _thisActor.getMostHated();

		if(hated != null && hated != _thisActor)
			_temp_attack_target = hated;
		else
		{
			_thisActor.setAttackTimeout(Integer.MAX_VALUE);
			setAttackTarget(null);
			_temp_attack_target = null;
			clientStopMoving();
			setIntention(AI_INTENTION_ACTIVE);
			return false;
		}

		if(_useUD)
		{
			_useUD = false;
			addUseSkillDesire(_temp_attack_target, _ud, 1, 1, DEFAULT_DESIRE * 100);
			return true;
		}
		
		// Базовые параметры
		int phys_per = 25;
		int debuff_per = 50;
		int pdam_per = 50;
		int manadam_per = 10;
		int heal_per = 10;

		ArrayList<L2Skill> d_skill = new ArrayList<L2Skill>();
		ArrayList<L2Skill> t_skill = new ArrayList<L2Skill>();
		L2Skill r_skill = null;

		double distance = _thisActor.getDistance(_temp_attack_target);
		double _att_hp = _temp_attack_target.getCurrentHp();
		int _att_max_hp = _temp_attack_target.getMaxHp();
		double _def_mp = _thisActor.getCurrentMp();

		if(!Rnd.chance(phys_per))
		{
			int rnd_per = Rnd.get(100);

			// DEBUFF
			if(rnd_per < debuff_per && _att_hp / _att_max_hp > 0.25 && _debuff_skills.length > 0)
			{
				t_skill.clear();
				for(L2Skill sk : _debuff_skills)
				{
					if(sk == null || _temp_attack_target.getEffectBySkill(sk) != null)
						continue;
					if(sk.getCastRange() > 200 && distance <= 200)
						continue;
					if(sk.getCastRange() <= 200 && distance > 200)
						continue;
					t_skill.add(sk);
				}
				if(t_skill.size() > 0)
					d_skill.add(t_skill.get(Rnd.get(t_skill.size())));
			}

			// PDAM
			if(rnd_per < pdam_per && _dam_skills.length > 0)
			{
				t_skill.clear();
				for(L2Skill sk : _dam_skills)
				{
					if(sk == null || sk.getCastRange() > 200 && distance <= 200)
						continue;
					if(sk.getCastRange() <= 200 && distance > 200)
						continue;
					t_skill.add(sk);
				}
				if(t_skill.size() > 0)
					d_skill.add(t_skill.get(Rnd.get(t_skill.size())));
			}

			if(_cancel_skills.length > 0 && _thisActor.getCurrentHp() / _thisActor.getMaxHp() > 0.40 && Rnd.chance(1))
			{
				List<L2Skill> skills = getEnabledSkills(_cancel_skills);
				if(skills.size() > 0)
					d_skill.add(skills.get(Rnd.get(skills.size())));
			}

			// HEAL
			if(_heal_skills.length > 0 && Rnd.chance(heal_per) && _thisActor.getCurrentHp() / _thisActor.getMaxHp() < 0.30)
			{
				t_skill.clear();
				t_skill.addAll(Arrays.asList(_heal_skills));
				if(t_skill.size() > 0)
					d_skill.add(t_skill.get(Rnd.get(t_skill.size())));
			}

			// Mana burn
			if(rnd_per < manadam_per && _manaburn_skills.length > 0)
			{
				List<L2Skill> skills = getEnabledSkills(_manaburn_skills);
				if(skills.size() > 0)
					d_skill.add(skills.get(Rnd.get(skills.size())));
			}
			
			// Self explosion
			if(_selfexplosion_skills.length > 0 && Rnd.chance(5))
			{
				List<L2Skill> skills = getEnabledSkills(_selfexplosion_skills);
				if(skills.size() > 0 && Rnd.chance((10 - _thisActor.getCurrentHp() / _thisActor.getMaxHp() * 100. / 10.)))
				{
					d_skill.clear();
					d_skill.add(skills.get(Rnd.get(skills.size())));
				}
			}

			// Выбрать 1 скилл из полученных
			if(d_skill.size() > 0)
				r_skill = d_skill.get(Rnd.get(d_skill.size()));

		}

		// Использовать скилл если можно, иначе атаковать
		if(r_skill != null && !r_skill.isMuted(_thisActor) && _def_mp >= r_skill.getMpConsume() && !_thisActor.isSkillDisabled(r_skill.getId()))
		{
			// Проверка таргета
			if(r_skill.getAimingTarget(_thisActor) == _thisActor)
				_temp_attack_target = _thisActor;
			else if(!r_skill.isOffensive())
				_temp_attack_target = getFriendTarget(r_skill);

			// Добавить новое задание
			addUseSkillDesire(_temp_attack_target, r_skill, 1, 1, DEFAULT_DESIRE * 2);
		}
		else
		{
			// Добавить новое задание
			addAttackDesire(_temp_attack_target, 1, DEFAULT_DESIRE);
		}

		_def_think = true;
		return true;
	}

	@Override
	protected void onEvtAttacked(L2Character attacker, int damage, L2Skill skill)
	{
		super.onEvtAttacked(attacker, damage, skill);

		if(attacker != null && CanIMove(attacker) && _thisActor.getDistance(attacker) <= 200)
		{
			for(Task task : _task_list)
				if(task != null && task.type == TaskType.MOVE)
					return;

			int posX = _thisActor.getX();
			int posY = _thisActor.getY();
			int posZ = _thisActor.getZ();

			int old_posX = posX;
			int old_posY = posY;
			int old_posZ = posZ;

			int signx = -1;
			int signy = -1;

			if(posX > attacker.getX())
				signx = 1;
			if(posX > attacker.getY())
				signy = 1;

			// int range = (int) ((_thisActor.calculateAttackSpeed()  /1000 * _thisActor.getWalkSpeed() )* 0.71); // was "_thisActor.getPhysicalAttackRange()"    0.71 = sqrt(2) / 2

			int range = (int) (0.71 * _thisActor.calculateAttackSpeed() / 1000 * _thisActor.getMoveSpeed());

			posX += signx * range;
			posY += signy * range;
			posZ = GeoEngine.getHeight(posX, posY, posZ, _thisActor.getReflection());

			if(GeoEngine.canMoveToCoord(old_posX, old_posY, old_posZ, posX, posY, posZ, _thisActor.getReflection()))
			{
				addMoveToDesire(posX, posY, posZ, DEFAULT_DESIRE * 10);
				addAttackDesire(attacker, 1, DEFAULT_DESIRE);
			}
		}
	}
}