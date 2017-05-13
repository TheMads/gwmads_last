package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Fishing;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.templates.L2Weapon;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author: rage
 * @date: 15.07.2010 12:17:14
 */
public class i_fishing_pumping extends i_effect
{
	public i_fishing_pumping(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		if(!cha.isPlayer())
			return;

		L2Player player = (L2Player) cha;
		L2Fishing fish = player.getFishCombat();
		L2Weapon weaponItem = player.getActiveWeaponItem();
		int SS = player.getChargedFishShot() ? 2 : 1;
		int pen = 0;
		double gradebonus = 1 + weaponItem.getCrystalType().ordinal() * 0.1;
		int dmg = (int) (getSkill().getPower(cha, null) * gradebonus * SS);

		if(player.getSkillLevel(1315) <= getSkill().getLevel() - 2) // 1315 - Fish Expertise
		{
			// Penalty
			player.sendPacket(new SystemMessage(SystemMessage.SINCE_THE_SKILL_LEVEL_OF_REELING_PUMPING_IS_HIGHER_THAN_THE_LEVEL_OF_YOUR_FISHING_MASTERY_A_PENALTY_OF_S1_WILL_BE_APPLIED));
			pen = 50;
			int penatlydmg = dmg - pen;
			if(player.isGM())
				player.sendMessage("Dmg w/o penalty = " + dmg);
			dmg = penatlydmg;
		}

		if(SS == 2)
			player.unChargeFishShot();

		if(fish != null)
			fish.UsePomping(dmg, pen);
	}
}
