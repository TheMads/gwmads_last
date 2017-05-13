package ru.l2gw.gameserver.model.base;

import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.skills.funcs.FuncTemplate;

/**
 * @author: rage
 * @date: 27.10.11 0:00
 */
public class EnchantOption
{
	private static final FuncTemplate[] emptyFunc = new FuncTemplate[0];
	private final int optionId;
	private final L2Skill skill;
	private FuncTemplate[] functions;

	public EnchantOption(int id, L2Skill skill)
	{
		optionId = id;
		this.skill = skill;
	}

	public int getOptionId()
	{
		return optionId;
	}

	public void addFuncTemplate(FuncTemplate func)
	{
		if(functions == null)
			functions = new FuncTemplate[1];
		else
		{
			FuncTemplate[] tmp = new FuncTemplate[functions.length + 1];
			System.arraycopy(functions, 0, tmp, 0, functions.length);
			functions = tmp;
		}

		functions[functions.length - 1] = func;
	}

	public L2Skill getSkill()
	{
		return skill;
	}

	public FuncTemplate[] getFunctions()
	{
		if(functions == null)
			return emptyFunc;

		return functions;
	}
}
