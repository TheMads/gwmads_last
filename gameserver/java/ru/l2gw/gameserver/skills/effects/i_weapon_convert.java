package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.instancemanager.KamaelWeaponExchangeInstance;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.serverpackets.ExAutoSoulShot;
import ru.l2gw.gameserver.serverpackets.InventoryUpdate;
import ru.l2gw.gameserver.serverpackets.ShortCutInit;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;

/**
 * @author: rage
 * @date: 14.07.2010 12:54:18
 */
public class i_weapon_convert extends i_effect
{
	public i_weapon_convert(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		if(!cha.isPlayer())
			return;

		L2Player player = (L2Player) cha;
		L2ItemInstance item = player.getActiveWeaponInstance();
		if(item == null)
			return;

		int id = item.getItemId();
		int itemtoexchange = KamaelWeaponExchangeInstance.convertWeaponId(id);

		if(itemtoexchange == 0)
			return;

		GArray<L2ItemInstance> items = player.getInventory().unEquipItemAndRecord(item);
		player.sendPacket(new SystemMessage(SystemMessage.S1__HAS_BEEN_DISARMED).addItemName(id));

		InventoryUpdate iu = new InventoryUpdate();
		for(L2ItemInstance it : items)
			iu.addItem(it);

		iu.addRemovedItem(item);
		player.sendPacket(iu);

		iu = new InventoryUpdate();
		item.setItemId(itemtoexchange);
		iu.addNewItem(item);
		items = player.getInventory().equipItemAndRecord(item);
		for(L2ItemInstance it : items)
			iu.addItem(it);

		player.sendChanges();
		player.sendPacket(iu);
		if(player.getActiveWeaponInstance() == item)
			player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EQUIPPED_YOUR_S1).addItemName(itemtoexchange));

		player.sendPacket(new ShortCutInit(player));
		for(int shotId : player.getAutoSoulShot())
			player.sendPacket(new ExAutoSoulShot(shotId, true));
	}
}
