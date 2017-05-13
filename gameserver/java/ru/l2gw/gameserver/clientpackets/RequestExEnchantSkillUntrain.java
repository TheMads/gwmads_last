package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.base.L2EnchantSkillLearn;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.ExEnchantSkillInfo;
import ru.l2gw.gameserver.serverpackets.ExEnchantSkillResult;
import ru.l2gw.gameserver.serverpackets.SkillList;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.tables.SkillTreeTable;
import ru.l2gw.util.Util;

public final class RequestExEnchantSkillUntrain extends RequestExEnchantSkill
{
	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null)
			return;

		if(player.getTransformation() != 0)
		{
			player.sendMessage("You must leave transformation mode first.");
			return;
		}

		if(player.getLevel() < 76 || player.getClassId().getLevel() < 4)
		{
			player.sendMessage("You must have 3rd class change quest completed.");
			return;
		}

		L2Skill newSkill = null;
		try
		{
			player.skillEnchantLock.lock();
			int oldSkillLevel = player.getSkillDisplayLevel(_skillId);
			if(oldSkillLevel == -1)
				return;

			L2EnchantSkillLearn sl = SkillTreeTable.getSkillEnchant(_skillId, oldSkillLevel);
			if(sl == null)
				return;

			if(_skillLvl != oldSkillLevel - 1)
			{
				Util.handleIllegalPlayerAction(player, "RequestExEnchantSkillUntrain", "tried to use skill enchant exploit", 1);
				return;
			}

			if(_skillLvl % 100 == 0)
			{
				_skillLvl = sl.getBaseLevel();
				newSkill = SkillTable.getInstance().getInfo(_skillId, _skillLvl);
			}
			else
				newSkill = SkillTable.getInstance().getInfo(_skillId, SkillTreeTable.convertEnchantLevel(sl.getBaseLevel(), _skillLvl, sl.getMaxEnchantLevel()));

			if(newSkill == null)
				return;

			int requiredSp = sl.getCost()[1] * sl.getCostMult();

			L2ItemInstance spb = player.getInventory().getItemByItemId(SkillTreeTable.UNTRAIN_ENCHANT_BOOK);

			if(spb == null || !player.destroyItem("EnchantSkillUntrain", spb.getObjectId(), 1, null, true))
			{
				sendPacket(new SystemMessage(SystemMessage.ITEMS_REQUIRED_FOR_SKILL_ENCHANT_ARE_INSUFFICIENT));
				return;
			}

			_enchantLog.info(player + " untrain skillId: " + _skillId + " level: " + oldSkillLevel + " to level: " + _skillLvl + " returned sp: " + requiredSp);
			player.addExpAndSp(0, requiredSp);
			player.addSkill(newSkill, true);

			if(_skillLvl % 100 > 0)
			{
				SystemMessage sm = new SystemMessage(SystemMessage.Untrain_of_enchant_skill_was_successful_Current_level_of_enchant_skill_S1_has_been_decreased_by_1);
				sm.addSkillName(_skillId, (short) _skillLvl);
				player.sendPacket(sm);
			}
			else
			{
				SystemMessage sm = new SystemMessage(SystemMessage.Untrain_of_enchant_skill_was_successful_Current_level_of_enchant_skill_S1_became_0_and_enchant_skill_will_be_initialized);
				sm.addSkillName(_skillId, (short) _skillLvl);
				player.sendPacket(sm);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			player.skillEnchantLock.unlock();
		}

		updateSkillShortcuts(player);

		player.sendPacket(new ExEnchantSkillInfo(_skillId, newSkill.getDisplayLevel()));
		player.sendPacket(new ExEnchantSkillResult(1));
		player.sendPacket(new SkillList(player));
	}
}