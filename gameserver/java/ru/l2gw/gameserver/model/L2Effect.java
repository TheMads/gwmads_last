package ru.l2gw.gameserver.model;

import javolution.util.FastList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.serverpackets.AbstractAbnormalStatus;
import ru.l2gw.gameserver.serverpackets.AbnormalStatusUpdate;
import ru.l2gw.gameserver.serverpackets.ExOlympiadSpelledInfo;
import ru.l2gw.gameserver.serverpackets.PartySpelled;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.serverpackets.IconEffectPacket;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.effects.c_force_buff;
import ru.l2gw.gameserver.skills.effects.t_effect;
import ru.l2gw.gameserver.skills.funcs.Func;
import ru.l2gw.gameserver.skills.funcs.FuncTemplate;

import java.text.SimpleDateFormat;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author: rage
 * @date: 12.08.2010 15:48:16
 */
public class L2Effect
{
	protected static final Log _log = LogFactory.getLog(L2Effect.class.getName());

	public static enum EffectState
	{
		CREATED,
		ACTING,
		FINISHING,
		FINISHED
	}

	private static final Func[] _emptyFunctionSet = new Func[0];

	public final static L2Effect[] EMPTY_L2EFFECT_ARRAY = new L2Effect[0];
	
	protected final L2Character _effector;
	protected final L2Character _effected;
	protected final L2Skill _skill;
	private final int _skillId;
	private final int _skillLvl;

	// the current state
	public EffectState _state;

	// period, milliseconds
	private long _abormalTime;
	private long _effectStartTime;

	// function templates
	protected FuncTemplate[] _funcTemplates;
	private static final SimpleDateFormat _format = new SimpleDateFormat("HH:mm:ss.SSS");
	public boolean _finished = false;
	private ScheduledFuture<EffectEndTask> _effectEndTask;
	private boolean _inUse = false;
	private L2Effect _next = null;

	protected Env _env;
	private t_effect[] _effects;

	private FastList<String> _debugStack;

	public L2Effect(Env env)
	{
		_debugStack = new FastList<String>();
		_state = EffectState.CREATED;
		_skill = env.skill;
		_skillId = _skill.getId();
		_skillLvl = _skill.getLevel();
		_effector = env.character;
		_effected = env.target;
		_env = env;
		_abormalTime = _skill.getAbnormalTime();

		// Check for skill mastery duration time increase
		if(_abormalTime > 0)
		{
			if(_effector.getSkillMastery(getSkillId()) == 2)
				_abormalTime *= 2;

			if(_skill.isBuff() && _abormalTime > 30000 && _skill.getId() != 396 && _skill.getId() != 1374)
				if(_skill.getSkillTargetType() != L2Skill.TargetType.self) // UD, Frenzy, etc. multiply fix
					_abormalTime *= Config.BUFFTIME_MODIFIER;

			if(_skill.isSongDance())
				_abormalTime *= Config.SONGDANCETIME_MODIFIER;

			//if(_env.effectTime < 1)
			//	_abormalTime *= _env.effectTime;
		}
		_effectStartTime = System.currentTimeMillis();
		addToDebugStack("created state: " + _state);
	}

	public void attachEffect(t_effect effect)
	{
		if(effect == null)
			return;
		if(_effects == null)
			_effects = new t_effect[]{ effect };
		else
		{
			int len = _effects.length;
			t_effect[] tmp = new t_effect[len + 1];
			System.arraycopy(_effects, 0, tmp, 0, len);
			tmp[len] = effect;
			_effects = tmp;
		}

		if(effect.getTemplate()._funcTemplates != null)
		{
			if(_funcTemplates == null)
			{
				_funcTemplates = new FuncTemplate[effect.getTemplate()._funcTemplates.length];
				System.arraycopy(effect.getTemplate()._funcTemplates, 0, _funcTemplates, 0, effect.getTemplate()._funcTemplates.length);
			}
			else
			{
				int len = _funcTemplates.length;
				FuncTemplate[] tmp = new FuncTemplate[len + effect.getTemplate()._funcTemplates.length];
				System.arraycopy(_funcTemplates, 0, tmp, 0, len);
				System.arraycopy(effect.getTemplate()._funcTemplates, 0, tmp, len, effect.getTemplate()._funcTemplates.length);
				_funcTemplates = tmp;
			}
		}
	}

	public boolean isEffectAttached()
	{
		return _effects != null && _effects.length > 0;
	}

	public long getAbnormalTime()
	{
		return _abormalTime;
	}

	public void setAbnormalTime(long time)
	{
		_abormalTime = time;
	}

	public long getTimeLeft()
	{
		return _effectEndTask != null ? _effectEndTask.getDelay(TimeUnit.MILLISECONDS) : getAbnormalTime();
	}

	public long getEffectStartTime()
	{
		return _effectStartTime;
	}

	public boolean isInUse()
	{
		return _inUse;
	}

	public void setInUse(boolean inUse)
	{
		_inUse = inUse;
		if(_inUse)
		{
			addToDebugStack("setInUse state: " + _state + " inUse " + inUse);
			scheduleEffect();
		}
		else if(_state != EffectState.FINISHED)
		{
			_state = EffectState.FINISHING;
			addToDebugStack("setInUse state: " + _state + " inUse " + inUse);
		}
	}

	public L2Skill getSkill()
	{
		return _skill;
	}

	public int getSkillId()
	{
		return _skillId;
	}

	public int getSkillLevel()
	{
		return _skillLvl;
	}

	public L2Character getEffector()
	{
		return _effector;
	}

	public L2Character getEffected()
	{
		return _effected;
	}

	private synchronized void startEffectTask(long duration)
	{
		if(_effectEndTask != null)
		{
			addToDebugStack("startEffectTask state: " + _state + " _effectEndTask not null");
			// Cancel the task
			_effectEndTask.cancel(false);
			_effectEndTask = null;
		}

		if(duration > 0)
			_effectEndTask = ThreadPoolManager.getInstance().scheduleEffect(new EffectEndTask(), duration);

		addToDebugStack("startEffectTask state: " + _state + " for " + duration);
		updateEffects();
	}

	/**
	 * Stop the L2Effect task and send Server->Client update packet.<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Cancel the effect in the the abnormal effect map of the L2Character </li>
	 * <li>Stop the task of the L2Effect, remove it and update client magic icon </li><BR><BR>
	 */
	public void exit()
	{
		//if(_next != null)
		//	_next.exit();
		//_next = null;
		addToDebugStack("exit state: " + _state);

		if(_state == EffectState.FINISHED)
			return;
		if(_state != EffectState.CREATED)
		{
			_state = EffectState.FINISHING;
			addToDebugStack("exit set state to: " + _state);
			scheduleEffect();
		}
		else
		{
			_state = EffectState.FINISHING;
			addToDebugStack("exit set state to: " + _state);
		}
	}

	public void takeScheduledNext(L2Effect source)
	{
		if(source.getNext() != null)
		{
			_next = source.getNext();
			source._next = null;
		}
	}

	/**
	 * Stop the task of the L2Effect, remove it and update client magic icon.<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Cancel the task </li>
	 * <li>Stop and remove L2Effect from L2Character and update client magic icon </li><BR><BR>
	 */
	private void stopEffectTask(boolean finished)
	{
		try
		{
			addToDebugStack("stopEffectTask state: " + _state + " call removeEffect");
			_effected.removeEffect(this);

			updateEffects();
			_skill.notifyEffectRemoved(_effected, this, finished);

			if(_effectEndTask != null)
			{
				// Cancel the task
				_effectEndTask.cancel(true);
				_effectEndTask = null;
			}
			else
				addToDebugStack("stopEffectTask: but no task!");
		}
		catch(NullPointerException e)
		{
		}
	}

	public final void scheduleEffect()
	{
		if(_state == EffectState.CREATED)
		{
			_state = EffectState.ACTING;
			addToDebugStack("scheduleEffect set state: " + _state);

			for(t_effect effect : _effects)
				effect.onStart();

			// Fake Death и Silent Move не отображаются
			// Отображать сообщение только для первого эффекта скилла
			if(_skill.getId() != 60 && _skill.getId() != 221)
				getEffected().sendPacket(new SystemMessage(SystemMessage.YOU_CAN_FEEL_S1S_EFFECT).addSkillName(_skill.getId(), _skill.getDisplayLevel()));

			_effected.updateStats(); // Обрабатываем отображение статов

			startEffectTask(_abormalTime);
			return;
		}

		if(_state == EffectState.FINISHING)
		{
			_state = EffectState.FINISHED;
			addToDebugStack("scheduleEffect set state: " + _state);

			// Для ускоренной "остановки" эффекта
			_inUse = false;

			// Cancel the effect in the the abnormal effect map of the L2Character
			for(t_effect effect : _effects)
				effect.onExit();

			// If the time left is equal to zero, send the message
			// Отображать сообщение только для последнего оставшегося эффекта скилла
			if(_finished)
				getEffected().sendPacket(new SystemMessage(SystemMessage.THE_EFFECT_OF_S1_HAS_WORN_OFF).addSkillName(_skill.getId(), _skill.getDisplayLevel()));
			else
				getEffected().sendPacket(new SystemMessage(SystemMessage.THE_EFFECT_OF_S1_HAS_BEEN_REMOVED).addSkillName(_skill.getId(), _skill.getDisplayLevel()));

			// Stop the task of the L2Effect, remove it and update client magic icon
			stopEffectTask(_finished);
		}
	}

	public void updateEffects()
	{
		_effected.updateStats();
	}

	public Func[] getStatFuncs()
	{
		if(_funcTemplates == null)
			return _emptyFunctionSet;
		Func[] funcs = new Func[_funcTemplates.length];
		for(int i = 0; i < funcs.length; i++)
		{
			Env env = new Env(_effector, _effected, _skill);
			Func f = _funcTemplates[i].getFunc(env, this); // effect is owner
			funcs[i] = f;
		}
		return funcs;
	}

	public static void addIcon(L2Effect effect, AbstractAbnormalStatus as)
	{
		if(effect._state != EffectState.ACTING)
			return;

		if((as instanceof PartySpelled || as instanceof ExOlympiadSpelledInfo) && (effect._skill.isToggle() || effect._skill.isPotion()) || effect.getSkillId() == 5041)
			return;

		int duration = effect._effectEndTask == null ? -1 : (int) effect._effectEndTask.getDelay(TimeUnit.SECONDS);
		as.addEffect(effect._skill.getDisplayId(), effect._skill.getDisplayLevel(), duration);
	}
	
    //public void addIcon(IconEffectPacket ps) 
    //{ 
    //    if(_state == EffectState.ACTING) 
    //        return;
    //    int duration = _skill.isToggle() ? AbnormalStatusUpdate.INFINITIVE_EFFECT : (int) getTimeLeft(); 
    //    ps.addIconEffect(_skillId, _skillLvl, duration); 
    //}  

	protected int getLevel()
	{
		return _skill.getLevel();
	}

	public void scheduleNext(L2Effect e)
	{
		if(_next != null)
			_next.exit();
		_next = e;
	}

	public L2Effect getNext()
	{
		return _next;
	}

	public boolean containsEffect(String name)
	{
		for(t_effect effect : _effects)
			if(effect.getTemplate()._name.equals(name))
				return true;
		return false;
	}

	public void addToDebugStack(String string)
	{
		try
		{
			_debugStack.add(_format.format(System.currentTimeMillis()) + " L2Effect: " + _effected + " " + string + " " + this);
		}
		catch(NullPointerException npe)
		{
		}
	}

	public void printDebugStack()
	{
		for(String string : _debugStack)
			System.out.println(string);
	}

	public boolean isSuccess(boolean skillSuccess)
	{
		for(t_effect effect : _effects)
			if(!effect.isSuccess(skillSuccess))
				return false;

		return skillSuccess;
	}

	public boolean isStackable()
	{
		return !getSkill().isToggle() && !getSkill().getAbnormalTypes().contains("none");
	}

	public boolean isStackable(L2Effect effect)
	{
		for(String abnormal : getSkill().getAbnormalTypes())
			if(effect.getSkill().getAbnormalTypes().contains(abnormal))
				return true;

		return false;
	}

	public int getForce()
	{
		if(_effects[0] instanceof c_force_buff)
			return ((c_force_buff) _effects[0])._forces;

		return 0;
	}

	public void increaseForce()
	{
		if(_effects[0] instanceof c_force_buff)
			((c_force_buff) _effects[0]).increaseForce();
	}

	public void decreaseForce()
	{
		if(_effects[0] instanceof c_force_buff)
			((c_force_buff) _effects[0]).decreaseForce();
	}

	public int getAbnormalLevel()
	{
		return _skill.getAbnormalLevel();
	}

	private String getEffectNames()
	{
		String ret = "";
		if(_effects != null)
			for(t_effect effect : _effects)
				ret += effect.getClass().getSimpleName() + ";";

		return ret;
	}

	private final class EffectEndTask implements Runnable
	{
		public void run()
		{
			try
			{
				_finished = true;
				_state = EffectState.FINISHING;
				scheduleEffect();
			}
			catch(Throwable e)
			{
				_log.warn("", e);
			}
		}
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + getEffectNames() + "ab_lv=" + getSkill().getAbnormalLevel() + ";ab_types=" + getSkill().getAbnormals() + "ctime=" + new SimpleDateFormat("dd HH:mm:ss").format(_effectStartTime) + "] skill: " + getSkill();
	}
}