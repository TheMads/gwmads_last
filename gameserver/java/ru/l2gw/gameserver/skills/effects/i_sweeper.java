package ru.l2gw.gameserver.skills.effects;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2MonsterInstance;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.commons.arrays.GArray;

/**
 * @author: rage
 * @date: 13.07.2010 14:57:53
 */
public class i_sweeper extends i_effect
{
	public i_sweeper(EffectTemplate template)
	{
		super(template);
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
	 	if(!cha.isPlayer())
			 return;

		L2Player player = (L2Player) cha;
		for(Env env : targets)
		{
			if(!env.target.isMonster() || !env.target.isDead())
				continue;

			L2MonsterInstance monster = (L2MonsterInstance) env.target;

			if(!monster.isSpoiled() || !monster.isSweepActive())
				continue;

			monster.setSpoiled(false);
			L2ItemInstance[] items = monster.takeSweep();

			if(items == null || items.length == 0) // и такое бывает o_O
				continue;

			for(L2ItemInstance item : items)
			{
				if(player.isInParty() && player.getParty().isDistributeSpoilLoot())
				{
					player.getParty().distributeItem(player, item);
					continue;
				}

				long itemCount = item.getCount();
				player.addItem("Sweep", item, monster, true);

				SystemMessage smsg;
				if(player.isInParty())
					if(itemCount == 1)
					{
						smsg = new SystemMessage(SystemMessage.S1_HAS_OBTAINED_S2_BY_USING_SWEEPER);
						smsg.addString(player.getName());
						smsg.addItemName(item.getItemId());
						player.getParty().broadcastToPartyMembers(player, smsg);
					}
					else
					{
						smsg = new SystemMessage(SystemMessage.S1_HAS_OBTAINED_3_S2_S_BY_USING_SWEEPER);
						smsg.addString(player.getName());
						smsg.addItemName(item.getItemId());
						smsg.addNumber(itemCount);
						player.getParty().broadcastToPartyMembers(player, smsg);
					}
			}
		}
	}
}
