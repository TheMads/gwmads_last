package ru.l2gw.gameserver.skills.funcs;

import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.skills.Stats;
import ru.l2gw.gameserver.templates.L2Item;
import ru.l2gw.gameserver.templates.L2Weapon;

/**
 * @author: rage
 * @date: 19.01.12 15:55
 */
public class FuncEnchantNpc extends Func
{
	private final Stats _stat;

	public FuncEnchantNpc(Stats stat, int order, Object owner, @SuppressWarnings("unused") double value)
	{
		super(stat, order, owner);
		_stat = stat;
	}

	@Override
	public void calc(Env env)
	{
		if(_cond != null && !_cond.test(env))
			return;

		L2NpcInstance owner = (L2NpcInstance) _funcOwner;
		L2Item item = owner.getActiveWeaponItem();
		if(item == null)
			return;
		
		int enchant = owner.getWeaponEnchant();
		int overenchant = Math.max(0, enchant - 3);

		if(_stat == Stats.MAGIC_DEFENCE || _stat == Stats.POWER_DEFENCE)
		{
			env.value += enchant + overenchant * 2;
			return;
		}

		if(_stat == Stats.MAGIC_ATTACK)
		{
			switch(item.getCrystalType().cry)
			{
				case L2Item.CRYSTAL_S:
					env.value += 4 * (enchant + overenchant);
					break;
				case L2Item.CRYSTAL_A:
					env.value += 3 * (enchant + overenchant);
					break;
				case L2Item.CRYSTAL_B:
					env.value += 3 * (enchant + overenchant);
					break;
				case L2Item.CRYSTAL_C:
					env.value += 3 * (enchant + overenchant);
					break;
				case L2Item.CRYSTAL_D:
					env.value += 2 * (enchant + overenchant);
					break;
				case L2Item.CRYSTAL_NONE:
					env.value += 2 * (enchant + overenchant);
					break;
			}
			return;
		}

		Enum<?> itemType = item.getItemType();
		switch(item.getCrystalType().cry)
		{
			case L2Item.CRYSTAL_S:
				if(itemType == L2Weapon.WeaponType.BOW)
					env.value += 10 * (enchant + overenchant);
				else if(itemType == L2Weapon.WeaponType.CROSSBOW)
					env.value += 7 * (enchant + overenchant);
				else if(itemType == L2Weapon.WeaponType.DUALFIST || itemType == L2Weapon.WeaponType.DUAL || itemType == L2Weapon.WeaponType.BIGSWORD || itemType == L2Weapon.WeaponType.SWORD || itemType == L2Weapon.WeaponType.ANCIENTSWORD && item.getBodyPart() == L2Item.SLOT_LR_HAND)
					env.value += 6 * (enchant + overenchant);
				else
					env.value += 5 * (enchant + overenchant);
				break;
			case L2Item.CRYSTAL_A:
				if(itemType == L2Weapon.WeaponType.BOW)
					env.value += 8 * (enchant + overenchant);
				else if(itemType == L2Weapon.WeaponType.CROSSBOW)
					env.value += 6 * (enchant + overenchant);
				else if(itemType == L2Weapon.WeaponType.DUALFIST || itemType == L2Weapon.WeaponType.DUAL || itemType == L2Weapon.WeaponType.BIGSWORD || itemType == L2Weapon.WeaponType.SWORD || itemType == L2Weapon.WeaponType.ANCIENTSWORD && item.getBodyPart() == L2Item.SLOT_LR_HAND)
					env.value += 5 * (enchant + overenchant);
				else
					env.value += 4 * (enchant + overenchant);
				break;
			case L2Item.CRYSTAL_B:
				if(itemType == L2Weapon.WeaponType.BOW)
					env.value += 6 * (enchant + overenchant);
				else if(itemType == L2Weapon.WeaponType.CROSSBOW)
					env.value += 5 * (enchant + overenchant);
				else if(itemType == L2Weapon.WeaponType.DUALFIST || itemType == L2Weapon.WeaponType.DUAL || itemType == L2Weapon.WeaponType.BIGSWORD || itemType == L2Weapon.WeaponType.SWORD || itemType == L2Weapon.WeaponType.ANCIENTSWORD && item.getBodyPart() == L2Item.SLOT_LR_HAND)
					env.value += 4 * (enchant + overenchant);
				else
					env.value += 3 * (enchant + overenchant);
				break;
			case L2Item.CRYSTAL_C:
				if(itemType == L2Weapon.WeaponType.BOW || itemType == L2Weapon.WeaponType.CROSSBOW)
					env.value += 6 * (enchant + overenchant);
				else if(itemType == L2Weapon.WeaponType.DUALFIST || itemType == L2Weapon.WeaponType.DUAL || itemType == L2Weapon.WeaponType.BIGSWORD || itemType == L2Weapon.WeaponType.SWORD || itemType == L2Weapon.WeaponType.ANCIENTSWORD && item.getBodyPart() == L2Item.SLOT_LR_HAND)
					env.value += 4 * (enchant + overenchant);
				else
					env.value += 3 * (enchant + overenchant);
				break;
			case L2Item.CRYSTAL_D:
				if(itemType == L2Weapon.WeaponType.BOW)
					env.value += 4 * (enchant + overenchant);
				else if(itemType == L2Weapon.WeaponType.CROSSBOW)
					env.value += 3 * (enchant + overenchant);
				else
					env.value += 2 * (enchant + overenchant);
				break;
			case L2Item.CRYSTAL_NONE:
				if(itemType == L2Weapon.WeaponType.BOW)
					env.value += 4 * (enchant + overenchant);
				else if(itemType == L2Weapon.WeaponType.CROSSBOW)
					env.value += 3 * (enchant + overenchant);
				else
					env.value += 2 * (enchant + overenchant);
				break;
		}
	}
}
