package ru.l2gw.gameserver.serverpackets;

//import ru.l2gw.commons.arrays.GArray;
//import ru.l2gw.gameserver.data.xml.holder.SkillAcquireHolder;
import ru.l2gw.gameserver.model.L2Clan;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.base.ClassId;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.L2SkillLearn;
import ru.l2gw.gameserver.tables.SkillTreeTable;
import ru.l2gw.gameserver.tables.SkillTable;

import java.util.Collection;

/**
/* @author VISTALL
/* @date 22:22/25.05.2011
/*/
public class ExAcquirableSkillListByClass extends L2GameServerPacket
{
	private Collection<L2SkillLearn> _skills;

	public ExAcquirableSkillListByClass(L2Player player)
	{
		_skills = SkillTreeTable.getInstance().getAcquirableSkillListByClass(player);
	}

	@Override
	protected void writeImpl()
	{
		writeEx(0xFA);
		writeD(_skills.size());
		for(L2SkillLearn s : _skills)
		{
			L2Skill skill = SkillTable.getInstance().getInfo(s.getId(), s.getLevel());
			if(skill == null)
				continue;

			writeD(s.getId());
			writeD(s.getLevel());
			writeD(s.getSpCost());
			writeH(s.getMinLevel());
			writeH(0x00); // Dual-class min level.
			boolean haveItem = s.getItemId() > 0;
			writeD(haveItem ? 1 : 0); //getRequiredItems()
			if(haveItem)
			{
				writeD(s.getItemId());
				writeQ(s.getItemCount());
			}
			writeD(0);
		}
	}
}