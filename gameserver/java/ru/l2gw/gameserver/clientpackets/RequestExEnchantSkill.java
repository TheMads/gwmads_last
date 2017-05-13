package ru.l2gw.gameserver.clientpackets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2ShortCut;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.base.L2EnchantSkillLearn;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.tables.SkillTreeTable;
import ru.l2gw.commons.math.Rnd;

/**
 * Format chdd
 * c: (id) 0xD0
 * h: (subid) 0x0F
 * d: skill id
 * d: skill lvl
 */
public class RequestExEnchantSkill extends L2GameClientPacket
{
	protected int _skillId;
	protected int _skillLvl;
	protected static final Log _enchantLog = LogFactory.getLog("skillEnchant");

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

			int[] cost = sl.getCost();
			int requiredSp = cost[1] * SkillTreeTable.NORMAL_ENCHANT_COST_MULTIPLIER * sl.getCostMult();
			int requiredAdena = cost[0] * SkillTreeTable.NORMAL_ENCHANT_COST_MULTIPLIER * sl.getCostMult();
			int rate = sl.getRate(player);

			if(player.getAdena() < requiredAdena)
			{
				sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
				return;
			}

			if(player.getSp() < requiredSp)
			{
				sendPacket(Msg.SP_REQUIRED_FOR_SKILL_ENCHANT_IS_INSUFFICIENT);
				return;
			}

			if(_skillLvl % 100 == 1) // only first lvl requires book (101, 201, 301 ...)
			{
				L2ItemInstance spb = player.getInventory().getItemByItemId(SkillTreeTable.NORMAL_ENCHANT_BOOK);
				if(spb == null || !player.destroyItem("EnchantSkill", spb.getObjectId(), 1, null, true))
				{
					sendPacket(new SystemMessage(SystemMessage.ITEMS_REQUIRED_FOR_SKILL_ENCHANT_ARE_INSUFFICIENT));
					return;
				}
			}

			if(player.reduceAdena("EnchantSkill", requiredAdena, null, true))
			{
				player.addExpAndSp(0, -1 * requiredSp);
				player.sendPacket(new SystemMessage(SystemMessage.SP_HAS_DECREASED_BY_S1).addNumber(requiredSp));
				if(Rnd.chance(rate))
				{
					_enchantLog.info(player + " enchant skillId: " + _skillId + " level: " + oldLevel + " to " + _skillLvl + " success, sp: " + requiredSp);
					player.addSkill(skill, true);
					player.sendPacket(new SystemMessage(SystemMessage.SUCCEEDED_IN_ENCHANTING_SKILL_S1).addSkillName(_skillId, (short) _skillLvl));
					player.sendPacket(new ExEnchantSkillResult(1));
				}
				else
				{
					_enchantLog.info(player + " enchant skillId: " + _skillId + " level: " + oldLevel + " to " + _skillLvl + " failed set level: " + sl.getBaseLevel() + " sp: " + requiredSp);
					player.addSkill(SkillTable.getInstance().getInfo(_skillId, sl.getBaseLevel()), true);
					player.sendPacket(new SystemMessage(SystemMessage.FAILED_IN_ENCHANTING_SKILL_S1).addSkillName(_skillId, (short) _skillLvl));
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

		//sendPacket(new SkillList(player));
		player.sendChanges();
		updateSkillShortcuts(player);
		player.sendPacket(new ExEnchantSkillInfo(_skillId, player.getSkillDisplayLevel(_skillId)));
		player.sendPacket(new SkillList(player));
	}

	protected void updateSkillShortcuts(L2Player player)
	{
		// update all the shortcuts to this skill
		for(L2ShortCut sc : player.getAllShortCuts())
			if(sc.id == _skillId && sc.type == L2ShortCut.TYPE_SKILL)
			{
				L2ShortCut newsc = new L2ShortCut(sc.slot, sc.page, sc.type, sc.id, player.getSkillDisplayLevel(_skillId));
				player.sendPacket(new ShortCutRegister(newsc));
				player.registerShortCut(newsc);
			}
	}
}
