package ru.l2gw.gameserver.skills.effects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.skills.funcs.FuncTemplate;
import ru.l2gw.gameserver.templates.StatsSet;

import java.lang.reflect.Constructor;

@SuppressWarnings("unchecked")
public final class EffectTemplate
{
	private static Log _log = LogFactory.getLog(EffectTemplate.class.getName());

	private Class<?> _clazz;
	public final Constructor<?> _constructor;
	private i_effect i_effect = null;

	public final int _ticks; // in milliseconds
	public FuncTemplate[] _funcTemplates;
	public boolean _hidden;
	public final String _options;
	public final int _activateRate;
	public final double _val;
	public final String _name;
	public final L2Skill _skill;
	public final boolean _applyOnCaster;
	public final boolean _excludeCaster;
	public final StatsSet _attrs;
	public EffectTemplate[] _childTemplates;
	public boolean _updatePet;

	public EffectTemplate(StatsSet set, L2Skill skill)
	{
		_attrs = set;
		_ticks = set.getInteger("ticks", 0);
		_applyOnCaster = set.getBool("applyOnCaster", false);
		_excludeCaster = set.getBool("excludeCaster", false);
		_hidden = set.getBool("hidden", false);
		_options = set.getString("options", null);
		_activateRate = set.getInteger("activateRate", -1);
		_skill = skill;

		_name = set.getString("name", "t_effect");
		_val = set.getDouble("val", 0);

		try
		{
			_clazz = Class.forName("ru.l2gw.gameserver.skills.effects." + _name);
		}
		catch(ClassNotFoundException e)
		{
			try
			{
				_clazz = Class.forName("ru.l2gw.gameserver.skills.effects.Effect" + _name);
			}
			catch(ClassNotFoundException e2)
			{
				throw new RuntimeException(e2);
			}
		}

		try
		{
			if(_name.startsWith("i_"))
			{
				_constructor = _clazz.getConstructor(EffectTemplate.class);
				i_effect = (i_effect) _constructor.newInstance(this);
			}
			else
				_constructor = _clazz.getConstructor(L2Effect.class, EffectTemplate.class);
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	public boolean isInstant()
	{
		return i_effect != null;
	}

	public i_effect getInstantEffect()
	{
		return i_effect;
	}

	public L2Effect getEffect(Env env)
	{
		L2Effect effect = new L2Effect(env);
		createEffect(effect);

		if(_childTemplates != null)
			for(EffectTemplate template : _childTemplates)
				template.createEffect(effect);

		if(effect.isEffectAttached())
			return effect;

		return null;
	}

	private t_effect createEffect(L2Effect effect)
	{
		try
		{
			return (t_effect) _constructor.newInstance(effect, this);
		}
		catch(Throwable t)
		{
			_log.warn("Error creating new instance of Class " + _clazz);
			t.printStackTrace();
		}
		return null;
	}

	public void attach(FuncTemplate f)
	{
		if(_funcTemplates == null)
			_funcTemplates = new FuncTemplate[] { f };
		else
		{
			int len = _funcTemplates.length;
			FuncTemplate[] tmp = new FuncTemplate[len + 1];
			System.arraycopy(_funcTemplates, 0, tmp, 0, len);
			tmp[len] = f;
			_funcTemplates = tmp;
		}

		if(f._stat == Stats.SERVITOR_TRANSFER_PATK || f._stat == Stats.SERVITOR_TRANSFER_PDEF || f._stat == Stats.SERVITOR_TRANSFER_MATK || f._stat == Stats.SERVITOR_TRANSFER_MDEF || f._stat == Stats.SERVITOR_TRANSFER_MAX_HP || f._stat == Stats.SERVITOR_TRANSFER_MAX_MP)
			_updatePet = true;
	}

	public void addChildTemplate(EffectTemplate template)
	{
		if(_childTemplates == null)
			_childTemplates = new EffectTemplate[] { template };
		else
		{
			int len = _childTemplates.length;
			EffectTemplate[] tmp = new EffectTemplate[len + 1];
			System.arraycopy(_childTemplates, 0, tmp, 0, len);
			tmp[len] = template;
			_childTemplates = tmp;
		}
	}
}
