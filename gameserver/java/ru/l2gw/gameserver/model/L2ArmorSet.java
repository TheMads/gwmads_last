package ru.l2gw.gameserver.model;

import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.commons.arrays.GArray;

public final class L2ArmorSet
{
	private final int _chest;
	private final GArray<Integer> _legs;
	private final GArray<Integer> _head;
	private final GArray<Integer> _gloves;
	private final GArray<Integer> _feet;
	private final GArray<Integer> _shield;

	private final L2Skill _setSkill;
	private final L2Skill _shieldSkill;
	private final L2Skill _enchant6Skill;

	public L2ArmorSet(int chest, String legs, String head, String gloves, String feet, String shield, String skillInfo, String shieldSkillInfo, String enchant6SkillInfo)
	{
		_chest = chest;
		_legs = parseString(legs);
		_head = parseString(head);
		_gloves = parseString(gloves);
		_feet = parseString(feet);
		_shield = parseString(shield);

		_setSkill = SkillTable.parseSkillInfo(skillInfo);
		_shieldSkill = SkillTable.parseSkillInfo(shieldSkillInfo);
		_enchant6Skill = SkillTable.parseSkillInfo(enchant6SkillInfo);
	}

	/**
	 * Checks if player have equipped all items from set (not checking shield)
	 * @param player whose inventory is being checked
	 * @return True if player equips whole set
	 */
	public boolean containAll(L2Player player)
	{
		Inventory inv = player.getInventory();

		int legs = inv.getPaperdollItemId(Inventory.PAPERDOLL_LEGS);
		int head = inv.getPaperdollItemId(Inventory.PAPERDOLL_HEAD);
		int gloves = inv.getPaperdollItemId(Inventory.PAPERDOLL_GLOVES);
		int feet = inv.getPaperdollItemId(Inventory.PAPERDOLL_FEET);

		return containAll(_chest, legs, head, gloves, feet);
	}

	public boolean containAll(int chest, int legs, int head, int gloves, int feet)
	{
		if(_chest != 0 && _chest != chest)
			return false;
		if(_legs.size() > 0 && !_legs.contains(legs))
			return false;
		if(_head.size() > 0 && !_head.contains(head))
			return false;
		if(_gloves.size() > 0 && !_gloves.contains(gloves))
			return false;
		if(_feet.size() > 0 && !_feet.contains(feet))
			return false;

		return true;
	}

	public boolean containItem(int slot, int itemId)
	{
		switch(slot)
		{
			case Inventory.PAPERDOLL_CHEST:
				return _chest == itemId;
			case Inventory.PAPERDOLL_LEGS:
				return _legs.contains(itemId);
			case Inventory.PAPERDOLL_HEAD:
				return _head.contains(itemId);
			case Inventory.PAPERDOLL_GLOVES:
				return _gloves.contains(itemId);
			case Inventory.PAPERDOLL_FEET:
				return _feet.contains(itemId);
			default:
				return false;
		}
	}

	public L2Skill getSkill()
	{
		return _setSkill;
	}

	public boolean containShield(L2Player player)
	{
		L2ItemInstance shieldItem = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		return shieldItem != null && _shield.contains(shieldItem.getItemId());
	}

	public boolean containShield(int shield_id)
	{
		return _shield.size() > 0 && _shield.contains(shield_id);
	}

	public L2Skill getShieldSkill()
	{
		return _shieldSkill;
	}

	public L2Skill getEnchant6Skill()
	{
		return _enchant6Skill;
	}

	/**
	 * Checks if all parts of set are enchanted to +6 or more
	 * @param player
	 * @return
	 */
	public boolean isEnchanted6(L2Player player)
	{
		// Player don't have full set
		if(!containAll(player))
			return false;

		Inventory inv = player.getInventory();

		L2ItemInstance chestItem = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		L2ItemInstance legsItem = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		L2ItemInstance headItem = inv.getPaperdollItem(Inventory.PAPERDOLL_HEAD);
		L2ItemInstance glovesItem = inv.getPaperdollItem(Inventory.PAPERDOLL_GLOVES);
		L2ItemInstance feetItem = inv.getPaperdollItem(Inventory.PAPERDOLL_FEET);

		if(chestItem.getEnchantLevel() < 6)
			return false;
		if(_legs.size() > 0 && legsItem.getEnchantLevel() < 6)
			return false;
		if(_gloves.size() > 0 && glovesItem.getEnchantLevel() < 6)
			return false;
		if(_head.size() > 0 && headItem.getEnchantLevel() < 6)
			return false;
		if(_feet.size() > 0 && feetItem.getEnchantLevel() < 6)
			return false;

		return true;
	}

	private static GArray<Integer> parseString(String string)
	{
		if(string == null || string.isEmpty())
			return new GArray<Integer>(0);

		String[] ar = string.split(",");
		GArray<Integer> result = new GArray<Integer>(ar.length);
		for(String str : ar)
			if(!str.isEmpty())
				result.add(Integer.parseInt(str));

		return result;
	}
}