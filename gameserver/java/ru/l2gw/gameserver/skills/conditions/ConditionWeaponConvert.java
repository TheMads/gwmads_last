package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.KamaelWeaponExchangeInstance;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author: rage
 * @date: 14.07.2010 12:48:05
 */
public class ConditionWeaponConvert extends Condition
{
	@Override
	public boolean testImpl(Env env)
	{
		if(!env.character.isPlayer() || env.character.isOutOfControl() || env.character.isInDuel() || env.character.getActiveWeaponInstance() == null)
			return false;

		L2ItemInstance weapon = env.character.getActiveWeaponInstance();
		if(weapon == null)
			return false;

		if(weapon.isAugmented())
		{
		 	env.character.sendPacket(Msg.THE_AUGMENTED_ITEM_CANNOT_BE_CONVERTED_PLEASE_CONVERT_AFTER_THE_AUGMENTATION_HAS_BEEN_REMOVED);
			return false;
		}

		if(KamaelWeaponExchangeInstance.convertWeaponId(weapon.getItemId()) == 0)
		{
			env.character.sendPacket(Msg.YOU_CANNOT_CONVERT_THIS_ITEM);
			return false;
		}

		return true;
	}
}
