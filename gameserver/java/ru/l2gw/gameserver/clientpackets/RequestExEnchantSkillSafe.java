package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.cache.Msg;
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
import ru.l2gw.commons.math.Rnd;

/**
 * Format (ch) dd
 * c: (id) 0xD0
 * h: (subid) 0x32
 * d: skill id
 * d: skill lvl
 */
public final class RequestExEnchantSkillSafe extends RequestExEnchantSkill
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

		L2EnchantSkillLearn sl = SkillTreeTable.getSkillEnchant(_skillId, _skillLvl);

		if(sl == null)
			return;

		try
		{
			player.skillEnchantLock.lock();
			int slevel = player.getSkillLevel(_skillId);
			int oldLevel = player.getSkillDisplayLevel(_skillId);
			if(slevel == -1)
				return;

			int enchantLevel = SkillTreeTable.convertEnchantLevel(sl.getBaseLevel(), _skillLvl, sl.getMaxEnchantLevel());

			// already knows the skill with this level
			if(slevel >= enchantLevel)
				return;

			// Можем ли мы перейти с текущего уровня скилла на данную заточку
			if(slevel == sl.getBaseLevel() ? _skillLvl % 100 != 1 : slevel != enchantLevel - 1)
			{
				player.sendMessage("Incorrect enchant level.");
				return;
			}

			L2Skill skill = SkillTable.getInstance().getInfo(_skillId, enchantLevel);
			if(skill == null)
				return;

			if(!skill.isCommon() && !SkillTreeTable.getInstance().isSkillPossible(player, _skillId, (short) enchantLevel))
				return;

			int[] cost = sl.getCost();
			int requiredSp = cost[1] * SkillTreeTable.SAFE_ENCHANT_COST_MULTIPLIER * sl.getCostMult();
			int requiredAdena = cost[0] * SkillTreeTable.SAFE_ENCHANT_COST_MULTIPLIER * sl.getCostMult();

			int rate = sl.getRate(player);

			if(player.getSp() < requiredSp)
			{
				sendPacket(Msg.SP_REQUIRED_FOR_SKILL_ENCHANT_IS_INSUFFICIENT);
				return;
			}

			if(player.getAdena() < requiredAdena)
			{
				sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
				return;
			}

			L2ItemInstance spb = player.getInventory().getItemByItemId(SkillTreeTable.SAFE_ENCHANT_BOOK);
			if(spb == null || !player.destroyItem("EnchantSkillSafe", spb.getObjectId(), 1, null, true))
			{
				sendPacket(new SystemMessage(SystemMessage.ITEMS_REQUIRED_FOR_SKILL_ENCHANT_ARE_INSUFFICIENT));
				return;
			}

			if(player.reduceAdena("EnchantSkillSafe", requiredAdena, null, true))
			{
				player.addExpAndSp(0, -1 * requiredSp);
				player.sendPacket(new SystemMessage(SystemMessage.SP_HAS_DECREASED_BY_S1).addNumber(requiredSp));

				if(Rnd.chance(rate))
				{
					_enchantLog.info(player + " enchant safe skillId: " + _skillId + " level: " + oldLevel + " to " + _skillLvl + " success, sp: " + requiredSp);
					player.addSkill(skill, true);
					player.sendPacket(new SystemMessage(SystemMessage.SUCCEEDED_IN_ENCHANTING_SKILL_S1).addSkillName(_skillId, (short) _skillLvl));
					player.sendPacket(new ExEnchantSkillResult(1));
				}
				else
				{
					_enchantLog.info(player + " enchant safe skillId: " + _skillId + " level: " + oldLevel + " to " + _skillLvl + " failed, sp: " + requiredSp);
					player.sendPacket(new SystemMessage(SystemMessage.SKILL_ENCHANT_FAILED_CURRENT_LEVEL_OF_ENCHANT_SKILL_S1_WILL_REMAIN_UNCHANGED).addSkillName(_skillId, (short) _skillLvl));
					player.sendPacket(new ExEnchantSkillResult(0));
				}
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

		sendPacket(new SkillList(player));
		updateSkillShortcuts(player);
		player.sendPacket(new ExEnchantSkillInfo(_skillId, player.getSkillDisplayLevel(_skillId)));
		player.sendPacket(new SkillList(player));
	}
}