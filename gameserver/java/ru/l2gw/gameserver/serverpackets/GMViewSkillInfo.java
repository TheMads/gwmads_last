package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.tables.SkillTable;

import java.util.Collection;

public class GMViewSkillInfo extends L2GameServerPacket
{
	private String char_name;
	private Collection<L2Skill> _skills;

	public GMViewSkillInfo(L2Player player)
	{
		char_name = player.getName();
		_skills = player.getAllSkills();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x97);
		writeS(char_name);
		writeD(_skills.size());
		for(L2Skill skill : _skills)
		{
			if(skill.getId() > 9000)
				continue; // fake skills to change base stats

			writeD(skill.isPassive() || skill.isOnAttack() || skill.isOnMagicAttack() || skill.isOnUnderAttack() ? 1 : 0);
			writeD(skill.getDisplayLevel());
			writeD(skill.getId());
			writeC(0x00); //c5
			writeC(SkillTable.getInstance().getMaxLevel(skill.getId(), skill.getLevel()) > 100 ? 1 : 0);
		}
	}
}