package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.model.*;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.model.instances.L2VillageMasterInstance;
import ru.l2gw.gameserver.serverpackets.ShortCutRegister;
import ru.l2gw.gameserver.serverpackets.SkillList;
import ru.l2gw.gameserver.serverpackets.ExAcquirableSkillListByClass;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.tables.SkillTreeTable;
import ru.l2gw.util.Util;
import ru.l2gw.extensions.multilang.CustomMessage;

public class RequestAquireSkill extends L2GameClientPacket
{
	// format: cddd(d)
	private int _id;
	private byte _level;
	private int _skillType;
	private int _subPledge = -1;

	@Override
	public void readImpl()
	{
		_id = readD();
		_level = (byte) readD();
		_skillType = readD();
		if(_skillType == 3)
			_subPledge = readD();
	}

	@Override
	public void runImpl()
	{
		L2Player player = getClient().getPlayer();
		if(player == null || player.getTransformation() != 0)
			return;
		L2NpcInstance trainer = player.getLastNpc();
		// В ГоДе можно учить скилы персонажа без тренера
		//if((trainer == null || !player.isInRange(trainer, player.getInteractDistance(trainer))) && !player.isGM())
		//	return;

		if((_skillType == SkillTreeTable.SKILL_TYPE_CLAN || _skillType == SkillTreeTable.SKILL_TYPE_CLAN_SUB_PLEDGE) && !player.isClanLeader())
		{
			player.sendPacket(Msg.ONLY_THE_CLAN_LEADER_IS_ENABLED);
			return;
		}

		player.setSkillLearningClassId(player.getClassId());

		L2Skill skill = SkillTable.getInstance().getInfo(_id, _level);

		if(_skillType != SkillTreeTable.SKILL_TYPE_CLAN_SUB_PLEDGE && player.getSkillLevel(_id) >= _level)
			return; // already knows the skill with this level

		L2SkillLearn skillLearn = SkillTreeTable.getSkillLearn(_id, _level, player.getClassId(), _skillType == SkillTreeTable.SKILL_TYPE_CLAN ? player.getClan() : null, player);

		if(skillLearn == null)
		{
			System.out.println("RequestAquireSkill: no skill learn for skillId:" + _id + " lvl: " + _level);
			return;
		}

		if(_skillType != SkillTreeTable.SKILL_TYPE_CLAN_SUB_PLEDGE && _level > 1 && player.getSkillLevel(_id) != _level - 1 && !skillLearn.isTransferSkill())
		{
			Util.handleIllegalPlayerAction(player, "RequestAquireSkill[58]", "tried to increase skill " + _id + " level to " + _level + " while having it's level " + player.getSkillLevel(_id), 2);
			player.sendMessage(new CustomMessage("Incorrect skill level or Skill is transfer! ", player));
			return;
		}

		if(!(skill.isCommon() || SkillTreeTable.getInstance().isSkillPossible(player, _id, _level)))
		{
				Util.handleIllegalPlayerAction(player, "RequestAquireSkill[64]", "tried to learn skill " + _id + " while on class " + player.getActiveClass(), 2);
				player.sendMessage(new CustomMessage("Skill is common or not for you class! ", player));
			return;
		}

		if(_skillType == SkillTreeTable.SKILL_TYPE_CLAN || _skillType == SkillTreeTable.SKILL_TYPE_CLAN_SUB_PLEDGE)
			learnClanSkill(skill, player.getClan(), _subPledge);
		else
		{
			if(skillLearn.getMinLevel() > player.getLevel())
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_DONT_MEET_SKILL_LEVEL_REQUIREMENTS));
				return;
			}

			int _requiredSp = skillLearn.getSpCost();

			if(player.getSp() >= _requiredSp || skillLearn.isCommon() || skillLearn.isTransformation() || skillLearn.isSubclass())
			{
				if(skillLearn.getItemId() > 0 && (player.getInventory().getItemByItemId(skillLearn.getItemId()) == null || player.getInventory().getItemByItemId(skillLearn.getItemId()).getCount() < skillLearn.getItemCount()))
				{
					player.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_ENOUGH_ITEMS_TO_LEARN_SKILLS));
					return;
				}

				if(skillLearn.isSubclass() && player.isSubClassActive())
				{
					player.sendPacket(new SystemMessage(SystemMessage.SKILL_NOT_FOR_SUBCLASS));
					return;
				}

				player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S1).addSkillName(skill.getId()));

				if(skillLearn.getItemId() > 0 && !player.destroyItemByItemId("AquireSkill", skillLearn.getItemId(), skillLearn.getItemCount(), trainer, true))
					return;

				player.addSkill(skill, true);

				if(_requiredSp > 0)
					player.setSp(player.getSp() - _requiredSp);

				player.updateStats();
				player.sendUserInfo(true);

				//update all the shortcuts to this skill
				if(_level > 1)
					for(L2ShortCut sc : player.getAllShortCuts())
						if(sc.id == _id && sc.type == L2ShortCut.TYPE_SKILL)
						{
							L2ShortCut newsc = new L2ShortCut(sc.slot, sc.page, sc.type, sc.id, _level);
							player.sendPacket(new ShortCutRegister(newsc));
							player.registerShortCut(newsc);
						}
			}
			else
			{
				player.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_ENOUGH_SP_TO_LEARN_SKILLS));
				return;
			}
		}

		player.sendPacket(new SkillList(player));

		switch(_skillType)
		{
			case SkillTreeTable.SKILL_TYPE_NORMAL:
				//trainer.showSkillList(player);
				player.sendPacket(new ExAcquirableSkillListByClass(player));
				break;
			case SkillTreeTable.SKILL_TYPE_FISHING:
				if(trainer != null)
					trainer.showFishingSkillList(player);
				break;
			case SkillTreeTable.SKILL_TYPE_COLLECTION:
				if(trainer != null)
					trainer.showCollectionSkillList(player);
				break;
			case SkillTreeTable.SKILL_TYPE_CLAN:
				if(SkillTreeTable.getInstance().isSubPledgeSkill(skill.getId()))
					if(trainer != null)
						trainer.showClanSubPledgeSkillList(player);
				else
					if(trainer != null)
						trainer.showClanSkillList(player);
				break;
			case SkillTreeTable.SKILL_TYPE_SUBCLASS:
				if(trainer != null)
					trainer.showSubclassSkillList(player);
				break;
			case SkillTreeTable.SKILL_TYPE_TRANSFORM:
				if(trainer != null)
					trainer.showTransformationSkillList(player);
				break;
			case SkillTreeTable.SKILL_TYPE_TRANSFER:
				if(trainer != null)
					trainer.showTransferSkillList(player);
		}
	}

	private void learnClanSkill(L2Skill skill, L2Clan clan, int subPledge)
	{
		L2Player player = getClient().getPlayer();
		if(player == null || skill == null || clan == null)
			return;

		L2NpcInstance trainer = player.getLastNpc();

		if(trainer == null)
			return;

		if(!(trainer instanceof L2VillageMasterInstance) && !SkillTreeTable.getInstance().isSubPledgeSkill(skill.getId()))
		{
			System.out.println("RequestAquireSkill.learnClanSkill, trainer isn't L2VillageMasterInstance");
			System.out.println(trainer.getName() + "[" + trainer.getNpcId() + "] Loc: " + trainer.getLoc());
			return;
		}

		if(!player.isClanLeader())
		{
			player.sendPacket(Msg.ONLY_THE_CLAN_LEADER_IS_ENABLED);
			return;
		}

		L2SkillLearn skillLearn = SkillTreeTable.getSkillLearn(_id, _level, null, clan, player);

		if(subPledge >= 0 && skill.getLevel() > 1)
		{
			GArray<L2Skill> skills  = clan.getSubPledgeSkills(subPledge);
			if(skills == null)
				return;
			for(L2Skill subSkill : skills)
				if(subSkill.getId() == skill.getId())
				{
					if(subSkill.getLevel() != skill.getLevel() - 1)
						return;
					break;
				}
		}

		int _requiredRep = skillLearn.getRepCost();
		short itemId = 0;

		if(!Config.ALT_DISABLE_EGGS)
			itemId = skillLearn.getItemId();
		if(skillLearn.getMinLevel() <= clan.getLevel() && clan.getReputationScore() >= _requiredRep)
		{
			if(itemId > 0)
			{
				L2ItemInstance spb = player.getInventory().getItemByItemId(itemId);
				if(spb == null || spb.getCount() < skillLearn.getItemCount())
				{
					// Haven't spellbook
					player.sendPacket(new SystemMessage(SystemMessage.YOU_DO_NOT_HAVE_ENOUGH_ITEMS_TO_LEARN_SKILLS));
					return;
				}

				player.destroyItemByItemId("AquireSkill", itemId, skillLearn.getItemCount(), trainer, true);
			}
			clan.incReputation(-_requiredRep, false, "AquireSkill");
			clan.addNewSkill(skill, player, subPledge);
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S1).addSkillName(_id, (short) _level));

			if(subPledge >= 0)
				trainer.showClanSubPledgeSkillList(player);
			else
				trainer.showClanSkillList(player); //Maybe we shoud add a check here...
		}
		else
		{
			player.sendMessage("Your clan doesn't have enough reputation points to learn this skill");
			//sm = null;
			return;
		}

		//update all the shortcuts to this skill
		if(_level > 1)
			for(L2ShortCut sc : player.getAllShortCuts())
				if(sc.id == _id && sc.type == L2ShortCut.TYPE_SKILL)
				{
					L2ShortCut newsc = new L2ShortCut(sc.slot, sc.page, sc.type, sc.id, _level);
					player.sendPacket(new ShortCutRegister(newsc));
					player.registerShortCut(newsc);
				}
	}
}