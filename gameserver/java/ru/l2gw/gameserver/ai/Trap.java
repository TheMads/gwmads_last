package ru.l2gw.gameserver.ai;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.L2World;
import ru.l2gw.gameserver.model.instances.L2TrapInstance;

/**
 * Created by IntelliJ IDEA.
 * User: rage
 * Date: 16.01.2009
 * Time: 10:34:15
 */
public class Trap extends DefaultAI
{
	protected L2Skill trapSkill;
	protected L2TrapInstance _thisTrap;
	protected int trapRange;
	protected boolean isDetected;
	protected boolean castSkill;
	protected GArray<L2Skill> _skills;

	public Trap(L2Character actor)
	{
		super(actor);
		_isMobile = false;

		_skills = new GArray<>();
		for(L2Skill skill : _thisActor.getTemplate().getSkills().values())
			if(!skill.isPassive())
				_skills.add(skill);

		if(_skills.size() > 0)
			trapSkill = _skills.get(Rnd.get(_skills.size()));
		trapRange = getInt("trap_range", 100);
		_thisTrap = (L2TrapInstance) _thisActor;
		isDetected = _thisTrap.isDetected();
		castSkill = false;
	}

	@Override
	protected boolean maybeMoveToHome()
	{
		return false;
	}

	@Override
	protected boolean thinkActive()
	{
		if(_thisTrap.isDead() || castSkill)
			return true;

		if(_thisTrap.getTrapLifeTime() > 0 && _thisTrap.getTrapLifeTime() < System.currentTimeMillis())
		{
			_thisTrap.doDie(null);
			return true;
		}

		if(isDetected != _thisTrap.isDetected())
		{
			if(isDetected)
				for(L2Player player : L2World.getAroundPlayers(_thisTrap))
					if(_thisTrap.getPlayer() == null || player != _thisTrap.getPlayer())
						player.removeVisibleObject(_thisTrap);

			isDetected = _thisTrap.isDetected();
		}

		L2Character trapOwner = _thisTrap.getTrapOwner();

		if(trapSkill != null && trapOwner != null && !_thisTrap.isSkillDisabled(trapSkill.getId()))
			for(L2Character cha : _thisTrap.getKnownCharacters(trapRange))
				if((!cha.isPlayer() || !((L2Player) cha).isInvisible()) && trapSkill.checkTarget(trapOwner, trapSkill.getAimingTarget(_thisActor, cha), false, true) == null)
				{
					_thisTrap.setDetected(5);
					castSkill = true;

					ThreadPoolManager.getInstance().scheduleEffect(new CastSkill(_thisTrap, trapSkill, cha), 2000);
					break;
				}

		return true;
	}

	protected class CastSkill implements Runnable
	{
		L2TrapInstance _trap;
		L2Skill _skill;
		L2Character _target;

		public CastSkill(L2TrapInstance trap, L2Skill skill, L2Character target)
		{
			_trap = trap;
			_skill = skill;
			_target = target;
		}

		public void run()
		{
			if(!_trap.isDead())
				_trap.altUseSkill(_skill, _target, null);

			castSkill = false;
		}
	}
}