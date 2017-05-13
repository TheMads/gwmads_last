package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.base.ItemData;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;

/**
 * @author: rage
 * @date: 14.07.2010 12:07:13
 */
public class i_restoration_random extends i_effect
{
	private final ItemData[] _items;

	public i_restoration_random(EffectTemplate template)
	{
		super(template);
		_items = ItemData.parseItem(template._options);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		if(!cha.isPlayer())
			return;

		L2Player player = (L2Player) cha;

		int chance = Rnd.get(100000000);
		for(ItemData id : _items)
		{
			if(chance < id.chance)
			{
				for(ItemData item : id.items)
					player.addItem("SkillSummonItem", item.item_id, item.count, null, true);
				return;
			}
			chance -= id.chance;
		}
		player.sendPacket(new SystemMessage(SystemMessage.THERE_WAS_NOTHING_FOUND_INSIDE_OF_THAT));
	}
}
