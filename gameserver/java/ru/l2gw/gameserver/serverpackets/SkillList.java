package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.tables.SkillTreeTable;
import ru.l2gw.commons.arrays.GArray;

/**
 * format   d (dddc)
 */
public class SkillList extends L2GameServerPacket
{
	private GArray<L2Skill> _skills;
	private L2Player _player;
	private boolean allowEnchant;
	private int _learnedSkill;

	public SkillList(L2Player player)
	{
		_skills = new GArray<>();
		_learnedSkill = 0;
		allowEnchant = player.getLevel() >= 76 && player.getClassId().getLevel() >= 4;
		for(L2Skill sk : player.getAllSkills())
			if(sk != null && !sk.isHidden())
				_skills.add(sk);
	}
	
	public SkillList(L2Player player, int learnedSkillId)
	{
		_skills = new GArray<>();
		_player = player;
		_learnedSkill = learnedSkillId;
		for(L2Skill sk : player.getAllSkills())
			if(sk != null && !sk.isHidden())
				_skills.add(sk);
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x5F);
		writeD(_skills.size());
		for(L2Skill temp : _skills)
		{
			writeD(temp.isActive() || temp.isToggle() ? 0 : 1); // deprecated? клиентом игнорируется
			if(temp.getChainIndex() != -1 && temp.getChainSkillId() != 0 && _player.getSkillChainDetails().containsKey(temp.getChainIndex()))
			{
				writeD(0x01);
				writeD(14612 + temp.getChainIndex());
			}
			else
			{
				writeD(temp.getDisplayLevel());
				writeD(temp.getDisplayId());
			}
			writeD(temp.getReuseSkillId());
			writeC(temp.isActive() ? 0x00 : 0x01); // иконка скилла серая если не 0
			writeC(allowEnchant ? SkillTreeTable.isEnchantable(temp) : 0);
		}
		writeD(_learnedSkill);
	}
}