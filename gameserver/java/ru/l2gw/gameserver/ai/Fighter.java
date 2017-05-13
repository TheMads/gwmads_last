package ru.l2gw.gameserver.ai;

import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Skill;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.l2gw.gameserver.ai.CtrlIntention.AI_INTENTION_ACTIVE;

/**
 * This class manages AI of L2NpcInstance.<BR><BR>
 */
public class Fighter extends DefaultAI
{
	public Fighter(L2Character actor)
	{
		super(actor);
	}

	@Override
	protected boolean thinkActive()
	{
		if(super.thinkActive())
			return true;

		// BUFF
		if(_selfbuff_skills.length > 0 && Rnd.chance(10) && !_thisActor.isMoving)
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
			addUseSkillDesire(_thisActor, _ud, 1, 1, DEFAULT_DESIRE * 100);
			return true;
		}

		// Базовые параметры
		int phys_per = 25;
		int debuff_per = 30;
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
			if(rnd_per < pdam_per && _pdam_skills.length > 0)
			{
				t_skill.clear();
				for(L2Skill sk : _pdam_skills)
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

			if(_cancel_skills.length > 0 && _thisActor.getCurrentHp() / _thisActor.getMaxHp() > 0.40 && Rnd.chance(0.25))
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
			return true;
		}

		// Добавить новое задание
		addAttackDesire(_temp_attack_target, 1, DEFAULT_DESIRE);
		return true;
	}
}