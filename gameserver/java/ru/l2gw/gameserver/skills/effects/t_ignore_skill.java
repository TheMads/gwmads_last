package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Effect;

/**
 * @author: rage
 * @date: 12.10.11 14:50
 */
public class t_ignore_skill extends t_effect
{
	private GArray<Integer> _ignoredSkills;

	public t_ignore_skill(L2Effect effect, EffectTemplate template)
	{
		super(effect, template);
		if(template._options != null && !template._options.isEmpty())
		{
			String[] skills = template._options.split(";");
			_ignoredSkills = new GArray<>(skills.length);
			for(String s : skills)
				if(s != null && !s.isEmpty())
					_ignoredSkills.add(Integer.parseInt(s));
		}
	}

	@Override
	public void onStart()
	{
		super.onStart();
		for(Integer skillId : _ignoredSkills)
			getEffected().addIgnoredSkill(skillId);
	}

	@Override
	public void onExit()
	{
		super.onExit();
		for(Integer skillId : _ignoredSkills)
			getEffected().removeIgnoredSkill(skillId);
	}
}