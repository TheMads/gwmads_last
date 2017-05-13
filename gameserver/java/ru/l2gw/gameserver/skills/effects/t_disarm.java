package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.instancemanager.CursedWeaponsManager;
import ru.l2gw.gameserver.model.L2Effect;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2MonsterInstance;
import ru.l2gw.gameserver.serverpackets.InventoryUpdate;
import ru.l2gw.gameserver.serverpackets.SystemMessage;

final class t_disarm extends t_effect
{
	private int _rhand;

	public t_disarm(L2Effect effect, EffectTemplate template)
	{
		super(effect, template);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		if(getEffected() instanceof L2Player)
		{

			L2Player player = getEffected().getPlayer();

			L2ItemInstance weapon = player.getActiveWeaponInstance();

			// Нельзя снимать/одевать проклятое оружие
			if(weapon == null || CursedWeaponsManager.getInstance().isCursed(weapon.getItemId()) || weapon.isFortFlag() || weapon.isTerritoryWard())
				return;

			GArray<L2ItemInstance> items = player.getInventory().unEquipItemAndRecord(weapon);

			for(L2ItemInstance item : items)
			{
				player.sendDisarmMessage(item);

				if(item.getChargedSpiritshot() != L2ItemInstance.CHARGED_NONE)
					player.sendPacket(new SystemMessage(SystemMessage.POWER_OF_MANA_DISABLED));
				if(item.getChargedSoulshot() != L2ItemInstance.CHARGED_NONE)
					player.sendPacket(new SystemMessage(SystemMessage.POWER_OF_THE_SPIRITS_DISABLED));
				item.setChargedSpiritshot(L2ItemInstance.CHARGED_NONE);
				item.setChargedSoulshot(L2ItemInstance.CHARGED_NONE);
			}
			player.sendPacket(new InventoryUpdate(items));
		}
		else if(getEffected().isMonster() && !getEffected().isRaid())
		{
			L2MonsterInstance monster = (L2MonsterInstance) getEffected();
			_rhand = monster.getRightHandItem();
			if(_rhand > 0)
			{
				monster.setRHandId(0);
				monster.updateAbnormalEffect();
			}
		}
	}

	@Override
	public void onExit()
	{
		super.onExit();
		if(getEffected().isMonster())
		{
			L2MonsterInstance monster = (L2MonsterInstance) getEffected();
			if(_rhand > 0)
			{
				monster.setRHandId(_rhand);
				monster.updateAbnormalEffect();
			}	
		}
	}
}