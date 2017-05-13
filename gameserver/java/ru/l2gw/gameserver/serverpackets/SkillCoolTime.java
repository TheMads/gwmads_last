package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.skills.TimeStamp;

public class SkillCoolTime extends L2GameServerPacket
{
	/**
	 * Example (C4, 656)
	 * C1 01 00 00 00 6E 00 00 00 02 00 00 00 9D 05 00 00 83 05 00 00 - Ultimate Defence level 2
	 *
	 * possible structure
	 * c - packet number
	 * d - size of skills ???
	 * now cycle?????
	 * d - skill id
	 * d - skill level
	 * d - 1437, total reuse delay
	 * d - 1411, remaining reuse delay
	 */

	private final GArray<ReuseInfo> reuseList;

	public SkillCoolTime(L2Player player)
	{
		reuseList = new GArray<>();

		for(TimeStamp timeStamp : player.getDisabledSkills())
		{
			int level;
			if(timeStamp.getReuseCurrent() / 1000 > 0 && (level = player.getSkillLevel(timeStamp.getSkillId())) > 0)
			{
				ReuseInfo reuseInfo = new ReuseInfo();
				reuseInfo.skillId = timeStamp.getSkillId();
				reuseInfo.level = level;
				reuseInfo.reuseTotal = (int) (timeStamp.getReuseTotal() / 1000);
				reuseInfo.reuseCurrent = (int) (timeStamp.getReuseCurrent() / 1000);
				reuseList.add(reuseInfo);
			}
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xC7); //packet type
		writeD(reuseList.size()); //Size of list

		for(ReuseInfo reuseInfo : reuseList)
		{
			writeD(reuseInfo.skillId); //Skill Id
			writeD(reuseInfo.level); //Skill Level
			writeD(reuseInfo.reuseTotal); //Total reuse delay, seconds
			writeD(reuseInfo.reuseCurrent); //Time remaining, seconds
		}
	}

	private static class ReuseInfo
	{
		public int skillId, level, reuseTotal, reuseCurrent;
	}
}