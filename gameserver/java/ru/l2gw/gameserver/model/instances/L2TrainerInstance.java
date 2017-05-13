package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.templates.L2NpcTemplate;

public final class L2TrainerInstance extends L2NpcInstance // deprecated?
{
	//private static Log _log = LogFactory.getLog(L2TrainerInstance.class.getName());

	public L2TrainerInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public String getHtmlPath(int npcId, int val, int karma)
	{
		String pom = "";
		if(val == 0)
			pom = "" + npcId;
		else
			pom = npcId + "-" + val;

		return "data/html/trainer/" + pom + ".htm";
	}
}
