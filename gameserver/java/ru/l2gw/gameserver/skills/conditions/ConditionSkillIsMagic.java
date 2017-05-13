package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.skills.Env;

/**
 * Created by IntelliJ IDEA.
 * User: rage
 * Date: 27.05.2009
 * Time: 10:01:51
 */
public class ConditionSkillIsMagic extends Condition
{
	private final boolean _isMagic;

	public ConditionSkillIsMagic(boolean isMagic)
	{
		_isMagic = isMagic;
	}

	@Override
	public boolean testImpl(Env env)
	{
		return env.skill != null && env.skill.isMagic() == _isMagic;
	}
}
