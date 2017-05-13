package ru.l2gw.gameserver.ai;

import javolution.util.FastList;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Skill;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static ru.l2gw.gameserver.ai.CtrlIntention.AI_INTENTION_ACTIVE;

/**
 * @author: rage
 */
public class Priest extends DefaultAI
{
	private ConcurrentLinkedQueue<L2Character> _friends;

	public Priest(L2Character actor)
	{
		super(actor);
		_friends = new ConcurrentLinkedQueue<L2Character>();

/*
		if(_heal.length == 0 && _hot.length == 0 && _healpercent.length == 0)
			_log.info("Warning: PriestAI has no heal skills! " + _thisActor);
*/
	}

	@Override
	protected void onEvtClanAttacked(L2Character attacked_member, L2Character attacker, int damage)
	{
		if(_thisActor == null || !_thisActor.isInRange(attacked_member, _thisActor.getFactionRange()))
			return;
		if(Math.abs(attacker.getZ() - _thisActor.getZ()) > 400)
			return;

		_friends.add(attacked_member);
	}

	@Override
	protected boolean thinkActive()
	{
		if(super.thinkActive())
			return true;

		if(_task_list.size() < 1 && (_friends.size() > 0 || _thisActor.getCurrentHp() / _thisActor.getMaxHp() < 0.70))
		{
			createNewTask();
			return true;
		}

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

	private L2Character getTopDesireTarget()
	{
		ConcurrentLinkedQueue<L2Character> list = new ConcurrentLinkedQueue<L2Character>();
		for(L2Character friend : _friends)
			if(friend != null && !friend.isDead() && friend.getCurrentHp() / friend.getMaxHp() < 0.90)
				list.add(friend);

		if(list.size() > 0)
		{
			_friends = list;
			return (L2Character)list.toArray()[Rnd.get(list.size())];
		}

		return null;
	}

	@Override
	protected boolean createNewTask()
	{
		// Удаляем все задания
		clearTasks();

		L2Character _temp_target = getTopDesireTarget();

		if(_temp_target == null && _thisActor.getCurrentHp() / _thisActor.getMaxHp() < 0.25)
			_temp_target = _thisActor;

		if(_temp_target == null)
		{
			_thisActor.setAttackTimeout(Integer.MAX_VALUE);
			setAttackTarget(null);
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
		int buff_per = 20;
		int heal_per = 70;

		List<L2Skill> d_skill = new FastList<L2Skill>();
		L2Skill r_skill = null;

		int distance = (int)_thisActor.getDistance(_temp_target);
		double _def_mp = _thisActor.getCurrentMp();

		Map<L2Skill, Integer> skill_chances = new HashMap<>();

		// HEAL
		if(_heal_skills.length > 0)
		{
			List<L2Skill> skills = getEnabledSkills(_heal_skills);
			if(skills.size() > 0)
				skill_chances.put(skills.get(Rnd.get(skills.size())), heal_per);
		}

		if(_buff_skills.length > 0)
		{
			List<L2Skill> skills = getEnabledSkills(_buff_skills);
			if(skills.size() > 0)
				skill_chances.put(skills.get(Rnd.get(skills.size())), buff_per);
		}

		if(_debuff_skills.length > 0)
		{
			List<L2Skill> skills = getEnabledSkills(_debuff_skills);
			if(skills.size() > 0)
				skill_chances.put(skills.get(Rnd.get(skills.size())), 30);
		}

		// Self explosion
		if(_selfexplosion_skills.length > 0 && Rnd.chance(5))
		{
			List<L2Skill> skills = getEnabledSkills(_selfexplosion_skills);
			if(skills.size() > 0)
			{
				int chance = (int)(10 - _thisActor.getCurrentHp() / _thisActor.getMaxHp() * 100. / 10.);
				skill_chances.put(skills.get(Rnd.get(skills.size())), chance);
			}
		}
		
		if(_cancel_skills.length > 0)
		{
			List<L2Skill> skills = getEnabledSkills(_cancel_skills);
			if(skills.size() > 0)
				skill_chances.put(skills.get(Rnd.get(skills.size())), 1);
		}

		// Mana burn
		if(_manaburn_skills.length > 0)
		{
			List<L2Skill> skills = getEnabledSkills(_manaburn_skills);
			if(skills.size() > 0)
				skill_chances.put(skills.get(Rnd.get(skills.size())), 10);
		}

		if(skill_chances.size() > 0)
		{
			for(L2Skill skill : skill_chances.keySet())
				if(Rnd.chance(skill_chances.get(skill)))
					d_skill.add(skill);

			r_skill = getSkillByRange(d_skill.toArray(new L2Skill[d_skill.size()]), distance);
		}

		// Использовать скилл если можно
		if(r_skill != null && !r_skill.isMuted(_thisActor) && _def_mp >= r_skill.getMpConsume())
		{
			// Проверка таргета
			if(r_skill.getAimingTarget(_thisActor) == _thisActor)
				_temp_target = _thisActor;

			// Добавить новое задание
			addUseSkillDesire(_temp_target, r_skill, 1, 1, DEFAULT_DESIRE * 2);
			return true;
		}

		return true;
	}
}