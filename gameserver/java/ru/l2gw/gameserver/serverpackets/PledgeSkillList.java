package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Clan.SubPledge;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.commons.arrays.GArray;

/**
 * Пример (828 протокол):
 * 0000: fe 3a 00 05 00 00 00 00 00 00 00 72 01 00 00 01    .:.........r....
 * 0010: 00 00 00 79 01 00 00 01 00 00 00 7b 01 00 00 02    ...y.......{....
 * 0020: 00 00 00 86 01 00 00 01 00 00 00 87 01 00 00 01    ................
 * 0030: 00 00 00                                           ...
 */
public class PledgeSkillList extends L2GameServerPacket
{
	private GArray<SkillInfo> clanSkills = new GArray<SkillInfo>();
	private GArray<SkillInfo> subPledgeSkills = new GArray<SkillInfo>();

	public PledgeSkillList(L2Player player)
	{
		if(player == null || player.getClanId() == 0)
			return;

		L2Clan clan = player.getClan();

		for(L2Skill sk : clan.getAllSkills())
			clanSkills.add(new SkillInfo(sk.getId(), sk.getLevel(), 0));

		GArray<L2Skill> skills = clan.getSubPledgeSkills(0);
		if(skills != null && skills.size() > 0)
			for(L2Skill skill : skills)
				subPledgeSkills.add(new SkillInfo(skill.getId(), skill.getLevel(), 0));

		for(SubPledge sp : clan.getAllSubPledges())
		{
			skills = clan.getSubPledgeSkills(sp.getType());
			if(skills != null && skills.size() > 0)
				for(L2Skill skill : skills)
					subPledgeSkills.add(new SkillInfo(skill.getId(), skill.getLevel(), sp.getType()));
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x3A);
		writeD(clanSkills.size());
		writeD(subPledgeSkills.size());
		
		for(SkillInfo info : clanSkills)
		{
			writeD(info.id);
			writeD(info.level);
		}
		for(SkillInfo info : subPledgeSkills)
		{
			writeD(info.subPledge);
			writeD(info.id);
			writeD(info.level);
		}
	}

	static class SkillInfo
	{
		public int id, level, subPledge;

		public SkillInfo(int _id, int _level, int _subPledge)
		{
			id = _id;
			level = _level;
			subPledge = _subPledge;
		}
	}
}