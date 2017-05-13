package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2PetInstance;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.tables.PetDataTable;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author: rage
 * @date: 15.07.2010 13:09:22
 */
public class i_food_for_pet extends i_effect
{
	private int normalFeed, rideFeed, wyvernFeed;

	public i_food_for_pet(EffectTemplate template)
	{
		super(template);
		String[] feed = template._options.split(";");
		normalFeed = Integer.parseInt(feed[0]);
		if(feed.length > 1)
			rideFeed = Integer.parseInt(feed[1]);
		if(feed.length > 2)
			wyvernFeed = Integer.parseInt(feed[2]);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		for(Env env : targets)
		{
			if(env.target instanceof L2PetInstance)
			{
				L2PetInstance pet =  (L2PetInstance) env.target;
				if(pet.destroyItemByItemId("Consume", env.item.getItemId(), 1, null, true))
				{
					if(pet.getNpcId() == PetDataTable.WYVERN_ID)
						pet.setCurrentFed(pet.getCurrentFed() + wyvernFeed);
					else if(cha.getPlayer() != null && cha.isRiding() && cha.getPlayer().getMountEngine().getMountNpcId() == pet.getNpcId())
						pet.setCurrentFed(pet.getCurrentFed() + rideFeed);
					else
						pet.setCurrentFed(pet.getCurrentFed() + normalFeed);

					pet.sendPetInfo();
				}
			}
			else if(env.target instanceof L2Player)
			{
				L2Player player = (L2Player) env.target;

				if(player.getMountEngine().isMounted() && player.getMountEngine().getPetTemplate().food.contains(env.item.getItemId()) && player.destroyItemByItemId("Consume", env.item.getItemId(), 1, null, true))
				{
					if(player.isFlying())
						player.getMountEngine().addMeal(rideFeed);
					else
						player.getMountEngine().addMeal(rideFeed);
				}
			}
		}
	}
}
