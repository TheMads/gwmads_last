package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.commons.math.Rnd;
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
import ru.l2gw.util.Util;

public final class RequestExEnchantSkillRouteChange extends RequestExEnchantSkill
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

		L2EnchantSkillLearn sl = SkillTreeTable.getSkillEnchant(_skillId, (short) _skillLvl);

		if(sl == null)
			return;

		try
		{
			player.skillEnchantLock.lock();
			int oldSkillLevel = player.getSkillDisplayLevel(_skillId);
			if(oldSkillLevel == -1)
				return;

			if(oldSkillLevel < sl.getBaseLevel())
				return;

			if(_skillLvl % 100 != oldSkillLevel % 100 || _skillLvl / 100 == oldSkillLevel / 100)
			{
				Util.handleIllegalPlayerAction(player, "RequestExEnchantSkillRouteChange", "tried to use skill enchant exploit", 1);
				return;
			}

			int[] cost = sl.getCost();
			int requiredSp = cost[1] * sl.getCostMult() / SkillTreeTable.SAFE_ENCHANT_COST_MULTIPLIER;
			int requiredAdena = cost[0] * sl.getCostMult() / SkillTreeTable.SAFE_ENCHANT_COST_MULTIPLIER;

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

			L2ItemInstance spb = player.getInventory().getItemByItemId(SkillTreeTable.CHANGE_ENCHANT_BOOK);
			if(spb == null)
			{
				sendPacket(new SystemMessage(SystemMessage.ITEMS_REQUIRED_FOR_SKILL_ENCHANT_ARE_INSUFFICIENT));
				return;
			}

			if(player.destroyItem("SkillRouteChange", spb.getObjectId(), 1, null, true) && player.reduceAdena("SkillRouteChange", requiredAdena, null, true))
			{
				player.addExpAndSp(0, -1 * requiredSp);

				int levelPenalty = Rnd.get(Math.min(4, _skillLvl % 100));

				_skillLvl -= levelPenalty;
				if(_skillLvl % 100 == 0)
					_skillLvl = sl.getBaseLevel();

				L2Skill skill = SkillTable.getInstance().getInfo(_skillId, SkillTreeTable.convertEnchantLevel(sl.getBaseLevel(), _skillLvl, sl.getMaxEnchantLevel()));
				_enchantLog.info(player + " route change skillId: " + _skillId + " level: " + oldSkillLevel + " to level: " + _skillLvl + " sp: " + requiredSp);

				if(skill != null)
					player.addSkill(skill, true);

				if(levelPenalty == 0)
				{
					SystemMessage sm = new SystemMessage(SystemMessage.Enchant_skill_route_change_was_successful_Lv_of_enchant_skill_S1_will_remain);
					sm.addSkillName(_skillId, (short) _skillLvl);
					player.sendPacket(sm);
				}
				else
				{
					SystemMessage sm = new SystemMessage(SystemMessage.Enchant_skill_route_change_was_successful_Lv_of_enchant_skill_S1_has_been_decreased_by_S2);
					sm.addSkillName(_skillId, (short) _skillLvl);
					sm.addNumber(levelPenalty);
					player.sendPacket(sm);
				}

				updateSkillShortcuts(player);
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

		player.sendPacket(new ExEnchantSkillInfo(_skillId, player.getSkillDisplayLevel(_skillId)));
		player.sendPacket(new ExEnchantSkillResult(1));
		player.sendPacket(new SkillList(player));
	}
}
