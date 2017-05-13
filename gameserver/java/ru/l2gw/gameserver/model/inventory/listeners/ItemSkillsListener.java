package ru.l2gw.gameserver.model.inventory.listeners;

import ru.l2gw.gameserver.model.Inventory;
import ru.l2gw.gameserver.model.L2Playable;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.SkillList;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.templates.L2EtcItem;
import ru.l2gw.gameserver.templates.L2Item;

import java.util.List;

/**
 * Добавление\удалениe скилов, прописанных предметам в sql или в xml.
 */
public final class ItemSkillsListener implements PaperdollListener
{
	Inventory _inv;

	public ItemSkillsListener(Inventory inv)
	{
		_inv = inv;
	}

	public void notifyUnequipped(int slot, L2ItemInstance item)
	{
		if(!_inv.isRefreshingListeners() && item.isShadowItem())
			item.notifyEquipped(false);

		if(_inv.getOwner() instanceof L2Playable)
			item.notifyUnEquipped((L2Playable) _inv.getOwner());

		L2Player player;

		if(_inv.getOwner().isPlayer())
			player = (L2Player) _inv.getOwner();
		else
			return;

		L2Skill[] itemSkills;
		L2Skill[] enchant4Skills;

		L2Item it = item.getItem();

		itemSkills = it.getAttachedSkills();

		enchant4Skills = it.getEnchant4Skills();

		if(itemSkills != null)
		{
			if(item.getItemType() == L2EtcItem.EtcItemType.RUNE_SELECT)
			{
				int c = player.getInventory().getRuneCountByType(item.getItem().getDelayShareGroup());
				int level = player.getInventory().getRuneMaxLevelByType(item.getItem().getDelayShareGroup());
				if(level <= c)
				{
					L2Skill rune = null;

					if(c > itemSkills.length)
						rune = itemSkills[itemSkills.length - 1];
					else if(c > 0)
						rune = itemSkills[c - 1];

					if(rune != null)
						player.addSkill(rune, false);
					else
						player.removeSkill(itemSkills[0], false);

					player.updateStats();
				}
			}
			else if(item.getItemType() == L2EtcItem.EtcItemType.RUNE)
			{
				int c = player.getInventory().getRuneCountByType(item.getItem().getDelayShareGroup());
				int level = player.getInventory().getRuneMaxLevelByType(item.getItem().getDelayShareGroup());
				int newLevel = Math.max(c, level);
				if(newLevel > 0)
				{
					int maxLvl = SkillTable.getInstance().getMaxLevel(itemSkills[0].getId(), 0);
					if(newLevel > maxLvl)
						newLevel = maxLvl;

					L2Skill rune = SkillTable.getInstance().getInfo(itemSkills[0].getId(), newLevel);

					if(rune != null)
						player.addSkill(rune, false);
					player.updateStats();
				}
				else
				{
					player.removeSkill(itemSkills[0], false);
					player.updateStats();
				}
			}
			else
				for(L2Skill itemSkill : itemSkills)
					player.removeSkill(itemSkill, false);
		}

		if(enchant4Skills != null)
			for(L2Skill enchantSkill : enchant4Skills)
				player.removeSkill(enchantSkill, false);

		List<L2Skill> enchantSkills = item.getAllEnchantOptionSkills();
		for(L2Skill skill : enchantSkills)
			player.removeSkill(skill, false);

		if(itemSkills != null || enchant4Skills != null || !enchantSkills.isEmpty())
			player.sendPacket(new SkillList(player));
	}

	public void notifyEquipped(int slot, L2ItemInstance item)
	{
		if(_inv.getOwner() instanceof L2Playable)
			item.notifyEquipped((L2Playable) _inv.getOwner());

		L2Player player;
		if(_inv.getOwner().isPlayer())
			player = (L2Player) _inv.getOwner();
		else
			return;

		L2Skill[] itemSkills = null;
		L2Skill[] enchant4Skills = null;

		L2Item it = item.getItem();

		itemSkills = it.getAttachedSkills();

		if(item.getEnchantLevel() >= 4)
			enchant4Skills = it.getEnchant4Skills();


		if(itemSkills != null)
		{
			if(item.getItemType() == L2EtcItem.EtcItemType.RUNE_SELECT)
			{
				int c = player.getInventory().getRuneCountByType(item.getItem().getDelayShareGroup());
				int level = player.getInventory().getRuneMaxLevelByType(item.getItem().getDelayShareGroup());
				if(level <= c)
				{
					L2Skill rune = null;

					if(c > itemSkills.length)
						rune = itemSkills[itemSkills.length - 1];
					else if(c > 0)
						rune = itemSkills[c - 1];

					if(rune != null)
						player.addSkill(rune, false);
					else
						player.removeSkill(itemSkills[0], false);

					player.updateStats();
				}
			}
			else if(item.getItemType() == L2EtcItem.EtcItemType.RUNE)
			{
				int c = player.getInventory().getRuneCountByType(item.getItem().getDelayShareGroup());
				int level = player.getInventory().getRuneMaxLevelByType(item.getItem().getDelayShareGroup());
				if(level >= c && level <= itemSkills[0].getLevel())
				{
					player.addSkill(itemSkills[0], false);
					player.updateStats();
				}
			}
			else
				for(L2Skill itemSkill : itemSkills)
					player.addSkill(itemSkill, false);
		}

		if(enchant4Skills != null)
			for(L2Skill enchantSkill : enchant4Skills)
				player.addSkill(enchantSkill, false);

		List<L2Skill> enchantSkills = item.getEnchantOptionSkills();
		for(L2Skill skill : enchantSkills)
			player.addSkill(skill, false);

		if(itemSkills != null || enchant4Skills != null || !enchantSkills.isEmpty())
			player.sendPacket(new SkillList(player));

		if(!_inv.isRefreshingListeners() && item.isShadowItem())
			item.notifyEquipped(true);
	}
}