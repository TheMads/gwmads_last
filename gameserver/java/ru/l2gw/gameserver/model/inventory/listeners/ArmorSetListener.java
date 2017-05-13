package ru.l2gw.gameserver.model.inventory.listeners;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.model.Inventory;
import ru.l2gw.gameserver.model.L2ArmorSet;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.SkillList;
import ru.l2gw.gameserver.tables.ArmorSetsTable;
import ru.l2gw.gameserver.tables.SkillTable;

public final class ArmorSetListener implements PaperdollListener
{
	public static short SET_COMMON_SKILL_ID = 3006;

	protected static final Log _log = LogFactory.getLog(ArmorSetListener.class.getName());

	Inventory _inv;

	public ArmorSetListener(Inventory inv)
	{
		_inv = inv;
	}

	public void notifyEquipped(int slot, L2ItemInstance item)
	{
		if(!_inv.getOwner().isPlayer() || slot < 0)
			return;

		L2Player player = (L2Player) _inv.getOwner();

		// checks if player worns chest item
		L2ItemInstance chestItem = _inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		if(chestItem == null)
			return;

		// checks if there is armorset for chest item that player worns
		L2ArmorSet armorSet = ArmorSetsTable.getInstance().getSet(chestItem.getItemId());
		if(armorSet == null)
			return;

		boolean update = false;
		// checks if equipped item is part of set
		if(armorSet.containItem(slot, item.getItemId()))
		{
			if(armorSet.containAll(player))
			{
				L2Skill commonSetSkill = SkillTable.getInstance().getInfo(SET_COMMON_SKILL_ID, 1);

				if(armorSet.getSkill() != null)
				{
					player.addSkill(armorSet.getSkill(), false);
					player.addSkill(commonSetSkill, false);
					update = true;
				}

				if(armorSet.getShieldSkill() != null && armorSet.containShield(player)) // has shield from set
				{
					player.addSkill(armorSet.getShieldSkill(), false);
					update = true;
				}
				if(armorSet.getEnchant6Skill() != null && armorSet.isEnchanted6(player)) // has all parts of set enchanted to 6 or more
				{
					player.addSkill(armorSet.getEnchant6Skill(), false);
					update = true;
				}
			}
		}
		else if(armorSet.containShield(item.getItemId()) && armorSet.containAll(player) && armorSet.getShieldSkill() != null)
		{
			player.addSkill(armorSet.getShieldSkill(), false);
			update = true;
		}

		if(update)
			player.sendPacket(new SkillList(player));
	}

	public void notifyUnequipped(int slot, L2ItemInstance item)
	{
		if(slot < 0)
			return;

		boolean remove = false;
		L2Skill removeSkillId1 = null; // set skill
		L2Skill removeSkillId2 = null; // shield skill
		L2Skill removeSkillId3 = null; // enchant +6 skill

		if(slot == Inventory.PAPERDOLL_CHEST)
		{
			L2ArmorSet armorSet = ArmorSetsTable.getInstance().getSet(item.getItemId());
			if(armorSet == null)
				return;

			remove = true;
			removeSkillId1 = armorSet.getSkill();
			removeSkillId2 = armorSet.getShieldSkill();
			removeSkillId3 = armorSet.getEnchant6Skill();

		}
		else
		{
			L2ItemInstance chestItem = _inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
			if(chestItem == null)
				return;

			L2ArmorSet armorSet = ArmorSetsTable.getInstance().getSet(chestItem.getItemId());
			if(armorSet == null)
				return;

			if(armorSet.containItem(slot, item.getItemId())) // removed part of set
			{
				remove = true;
				removeSkillId1 = armorSet.getSkill();
				removeSkillId2 = armorSet.getShieldSkill();
				removeSkillId3 = armorSet.getEnchant6Skill();
			}
			else if(armorSet.containShield(item.getItemId())) // removed shield
			{
				remove = true;
				removeSkillId2 = armorSet.getShieldSkill();
			}
		}

		boolean update = false;
		if(remove)
		{
			if(removeSkillId1 != null)
			{
				L2Skill commonSetSkill = SkillTable.getInstance().getInfo(SET_COMMON_SKILL_ID, 1);
				((L2Player) _inv.getOwner()).removeSkill(removeSkillId1, false);
				((L2Player) _inv.getOwner()).removeSkill(commonSetSkill, false);
				update = true;
			}
			if(removeSkillId2 != null)
			{
				_inv.getOwner().removeSkill(removeSkillId2);
				update = true;
			}
			if(removeSkillId3 != null)
			{
				_inv.getOwner().removeSkill(removeSkillId3);
				update = true;
			}
		}
		if(update)
			_inv.getOwner().sendPacket(new SkillList((L2Player) _inv.getOwner()));
	}
}