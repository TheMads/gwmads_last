package ru.l2gw.gameserver.skills.conditions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.skills.Env;

public abstract class Condition implements ConditionListener
{
	private ConditionListener _listener;

	private String _msg;
	private int _msgId = 0;
	private boolean _result;

	public final void setMessage(String msg)
	{
		_msg = msg;
	}

	public final void setMessageId(int msgId)
	{
		_msgId = msgId;
	}

	public final String getMessage()
	{
		return _msg;
	}

	public final int getMessageId()
	{
		return _msgId;
	}

	public void setListener(ConditionListener listener)
	{
		_listener = listener;
		notifyChanged();
	}

	public final ConditionListener getListener()
	{
		return _listener;
	}

	public final boolean test(Env env)
	{
		boolean res = testImpl(env);
		if(_listener != null && res != _result)
		{
			_result = res;
			notifyChanged();
		}
		return res;
	}

	public abstract boolean testImpl(Env env);

	public void notifyChanged()
	{
		if(_listener != null)
			_listener.notifyChanged();
	}
}
