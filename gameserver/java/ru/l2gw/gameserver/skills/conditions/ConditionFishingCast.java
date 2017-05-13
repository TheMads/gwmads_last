package ru.l2gw.gameserver.skills.conditions;

import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.instancemanager.ZoneManager;
import ru.l2gw.gameserver.model.Inventory;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Skill;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.zone.L2Zone;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.templates.L2Weapon;
import ru.l2gw.util.Location;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.util.Util;

/**
 * @author: rage
 * @date: 15.07.2010 12:09:41
 */
public class ConditionFishingCast extends Condition
{
	@Override
	public boolean testImpl(Env env)
	{
		if(!env.character.isPlayer())
			return false;

		L2Player player = (L2Player) env.character;

		if(player.getSkillLevel(L2Skill.SKILL_FISHING_MASTERY) == -1)
			return false;

		if(!player.isInZone(L2Zone.ZoneType.fishing))
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANT_FISH_HERE));
			return false;
		}

		if(player.isFishing())
		{
			if(player.getFishCombat() != null)
				player.getFishCombat().doDie(false);
			else
				player.endFishing(false);
			player.sendPacket(new SystemMessage(SystemMessage.CANCELS_FISHING));
			return false;
		}

		if(player.isInBoat())
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANT_FISH_WHILE_YOU_ARE_ON_BOARD));
			return false;
		}

		if(player.getPrivateStoreType() != L2Player.STORE_PRIVATE_NONE)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_FISH_WHILE_USING_A_RECIPE_BOOK_PRIVATE_MANUFACTURE_OR_PRIVATE_STORE));
			return false;
		}

		if(player.isInZone(L2Zone.ZoneType.water))
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANT_FISH_IN_WATER));
			return false;
		}

		Location loc = Util.getPointInRadius(player.getLoc(), Rnd.get(50) + 150, (int) Util.convertHeadingToDegree(player.getHeading()));
		L2Zone water = ZoneManager.getInstance().isInsideZone(L2Zone.ZoneType.water, loc.getX(), loc.getY());

		// float must be in water
		if(water == null || water.getMaxZ() < loc.getZ() - 500)
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANT_FISH_HERE));
			return false;
		}

		if(GeoEngine.canSeeCoord(player, loc.getX(), loc.getY(), loc.getZ() + 32, true) && GeoEngine.getHeight(loc, player.getReflection()) < player.getZ() - 16)
			player.setFishLoc(loc.setZ(water.getMaxZ() - 20));
		else
		{
			player.sendPacket(new SystemMessage(SystemMessage.YOU_CANT_FISH_HERE));
			return false;
		}

		L2Weapon weaponItem = player.getActiveWeaponItem();
		if(weaponItem == null || weaponItem.getItemType() != L2Weapon.WeaponType.ROD)
		{
			//Fishing poles are not installed
			player.sendPacket(new SystemMessage(SystemMessage.FISHING_POLES_ARE_NOT_INSTALLED));
			return false;
		}

		L2ItemInstance lure = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if(lure == null || lure.getCount() < 1)
		{
			player.sendPacket(new SystemMessage(SystemMessage.BAITS_ARE_NOT_PUT_ON_A_HOOK));
			return false;
		}

		return true;
	}
}
