package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;

public class ConditionLogicAnd extends Condition
{
	private final static Condition[] emptyConditions = new Condition[0];

	public Condition[] _conditions = emptyConditions;

	public ConditionLogicAnd()
	{
		super();
	}

	public void add(Condition condition)
	{
		if(condition == null)
			return;
		if(getListener() != null)
			condition.setListener(this);
		final int len = _conditions.length;
		final Condition[] tmp = new Condition[len + 1];
		System.arraycopy(_conditions, 0, tmp, 0, len);
		tmp[len] = condition;
		_conditions = tmp;
	}

	@Override
	public void setListener(ConditionListener listener)
	{
		if(listener != null)
			for(Condition c : _conditions)
				c.setListener(this);
		else
			for(Condition c : _conditions)
				c.setListener(null);
		super.setListener(listener);
	}

	@Override
	public boolean testImpl(Env env)
	{
		for(Condition c : _conditions)
			if(!c.test(env))
			{
				if(env.character == null)
					return false;
				if(env.skill != null)
				{
					String condMsg = c.getMessage();
					int condMsgId = c.getMessageId();
					if(condMsgId != 0)
					{
						SystemMessage sm = new SystemMessage(condMsgId);
						sm.addSkillName(env.skill.getDisplayId(), env.skill.getLevel());
						env.character.sendPacket(sm);
					}
					else if(condMsg != null)
						env.character.sendMessage(condMsg);
				}
				else if(env.item != null)
				{
					int condMsgId = c.getMessageId();
					if(condMsgId != 0)
						env.character.sendPacket(new SystemMessage(condMsgId).addItemName(env.item.getItemId()));
				}
				return false;
			}
		return true;
	}
}
