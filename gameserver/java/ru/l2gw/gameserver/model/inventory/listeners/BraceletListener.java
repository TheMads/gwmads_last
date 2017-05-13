package ru.l2gw.gameserver.model.inventory.listeners;


import ru.l2gw.gameserver.model.Inventory;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.templates.L2Item;

public final class BraceletListener implements PaperdollListener
{
	private Inventory _inv;

	public BraceletListener(Inventory inv)
	{
		_inv = inv;
	}

	public void notifyUnequipped(int slot, L2ItemInstance item)
	{
		if(!(_inv.getOwner() != null && _inv.getOwner().isPlayer()) || slot < 0)
			return;

		L2Player owner = (L2Player) _inv.getOwner();

		if(item.getBodyPart() == L2Item.SLOT_L_BRACELET)
		{
			// При снятии браслета с агнишеном, удаляем агришена
			if(owner.getAgathionId() != 0)
				owner.setAgathion(0);

			// При снятии браслета с маунтом, ставим чара на землю если он на этом маунте
			if(owner.getMountEngine().getMountNpcId() > 0)
				for(L2Skill skill : item.getItem().getAttachedSkills())
					if(skill.getNpcId() == owner.getMountEngine().getMountNpcId())
					{
						owner.getMountEngine().dismount();
						break;
					}
		}
		else if(item.getBodyPart() == L2Item.SLOT_R_BRACELET)
			_inv.setAllowedTalismans(0);
	}

	public void notifyEquipped(int slot, L2ItemInstance item)
	{
		if(!(_inv.getOwner() != null && _inv.getOwner().isPlayer()) || slot < 0)
			return;

		if(item.getBodyPart() == L2Item.SLOT_R_BRACELET)
		{
			//Чтобы скажем ввести новый браслет с 5тью дырками дайте ему скилл id = 3326
			//Чтобы ввести учёт чего либо например если будет влиять заточка точим браслет с 1 дыркой в нём станет две под циклом прибавьте getEnchantlevel()
			int Tcount = item.getItem().getFirstSkill().getId() - 3321;
			_inv.setAllowedTalismans(Tcount);
		}
	}
}