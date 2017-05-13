package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;
import ru.l2gw.commons.arrays.GArray;

import java.io.File;
import java.util.StringTokenizer;

public class L2OlympiadBufferInstance extends L2NpcInstance
{
	private static final int MAX_BUFF_COUNT = 4;

	private GArray<Integer> buffs = new GArray<Integer>();

	public L2OlympiadBufferInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	@Override
	public void onBypassFeedback(L2Player player, String command)
	{
		if(buffs.size() > MAX_BUFF_COUNT)
		{
			showChatWindow(player, 1);
			deleteMe();
		}

		if(command.startsWith("Buff"))
		{
			int id = 0;
			int lvl = 0;
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			id = Integer.parseInt(st.nextToken());
			lvl = Integer.parseInt(st.nextToken());
			L2Skill skill = SkillTable.getInstance().getInfo(id, lvl);
			ThreadPoolManager.getInstance().scheduleGeneral(new doBuff(this, skill, player), 100);

			if(!buffs.contains(id))
				buffs.add(id);

			if(buffs.size() > MAX_BUFF_COUNT)
			{
				showChatWindow(player, 1);
				ThreadPoolManager.getInstance().scheduleGeneral(new Runnable(){ public void run(){ deleteMe(); } }, 500);
			}
			else
				showChatWindow(player, 0);
		}
		else
			showChatWindow(player, 0);
	}

	@Override
	public String getHtmlPath(int npcId, int val, int karma)
	{
		String pom;

		if(buffs.size() > MAX_BUFF_COUNT)
			val = 1;

		if(val == 0)
			pom = "buffer";
		else
			pom = "buffer-" + val;
		String temp = "data/html/olympiad/" + pom + ".htm";
		File mainText = new File(temp);
		if(mainText.exists())
			return temp;

		// If the file is not found, the standard message "I have nothing to say to you" is returned
		return "data/html/npcdefault.htm";
	}

	private class doBuff implements Runnable
	{
		L2Character _actor;
		L2Skill _skill;
		L2Player _target;

		doBuff(L2Character actor, L2Skill skill, L2Player target)
		{
			_actor = actor;
			_skill = skill;
			_target = target;
		}

		public void run()
		{
			_actor.doCast(_skill, _target, null, true);
		}
	}
}