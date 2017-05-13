package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.base.L2EnchantSkillLearn;
import ru.l2gw.gameserver.serverpackets.ExEnchantSkillInfo;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.tables.SkillTreeTable;

public class RequestExEnchantSkillInfo extends L2GameClientPacket
{
	private int _skillId;
	private int _skillLvl;

	@Override
	public void readImpl()
	{
		_skillId = readD();
		_skillLvl = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(_skillLvl > 100)
		{
			L2EnchantSkillLearn sl = SkillTreeTable.getSkillEnchant(_skillId, (short) _skillLvl);
			if(sl == null)
			{
				player.sendMessage("Not found enchant info for this skill");
				return;
			}

			L2Skill skill = SkillTable.getInstance().getInfo(_skillId, SkillTreeTable.convertEnchantLevel(sl.getBaseLevel(), _skillLvl, sl.getMaxEnchantLevel()));

			if(skill == null || skill.getId() != _skillId)
			{
				player.sendMessage("This skill doesn't yet have enchant info in Datapack");
				return;
			}

			if(player.getSkillLevel(_skillId) != skill.getLevel())
			{
				player.sendMessage("Skill not found");
				return;
			}
		}
		else if(player.getSkillLevel(_skillId) != _skillLvl)
		{
			player.sendMessage("Skill not found");
			return;
		}

		player.sendPacket(new ExEnchantSkillInfo(_skillId, _skillLvl));
	}
}