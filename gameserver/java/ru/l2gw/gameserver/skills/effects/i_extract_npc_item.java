package ru.l2gw.gameserver.skills.effects;

import javolution.util.FastMap;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.ai.CtrlEvent;
import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.skills.Env;
import ru.l2gw.gameserver.templates.StatsSet;
import ru.l2gw.commons.math.Rnd;

/**
 * @author rage
 * @date 07.09.2010 12:05:04
 */
public class i_extract_npc_item extends i_effect
{
	private final FastMap<Integer, StatsSet> _items;
	private final double _chance;

	public i_extract_npc_item(EffectTemplate template)
	{
		super(template);
		String[] items = _template._attrs.getString("options", "").split(";");
		_chance = _template._attrs.getDouble("chance", 100);
		_items = new FastMap<Integer, StatsSet>(items.length / 4);
		for(int i = 0; i < items.length; i += 4)
			if(items[i] != null && !items[i].isEmpty())
				try
				{
					int npcId = Integer.parseInt(items[i]);
					StatsSet set = new StatsSet();
					set.set("item_id", items[i + 1]);
					set.set("min", items[i + 2]);
					set.set("max", items[i + 3]);
					_items.put(npcId, set);
				}
				catch(Exception e)
				{
					_log.warn(this + " parse error: " + _template._attrs.getString("options", "") + " " + e);
				}
	}

	@Override
	public void doEffect(L2Character cha, GArray<Env> targets, int ss, boolean counter)
	{
		if(!cha.isPlayer())
			return;

		L2Player player = (L2Player) cha;

		for(Env env : targets)
			if(env.target instanceof L2NpcInstance && _items.containsKey(env.target.getNpcId()))
			{
				StatsSet set = _items.get(env.target.getNpcId());
				int count = Rnd.get(set.getInteger("min"), set.getInteger("max"));
				if(!env.target.isDead() && !((L2NpcInstance) env.target).isDecayed() && Rnd.chance(_chance) && count > 0)
				{
					player.addItem("ExtractNpc", set.getInteger("item_id"), count, env.target, true);
					env.target.getAI().notifyEvent(CtrlEvent.EVT_SCRIPT_EVENT, 20110602, player);
				}
				else
					player.sendPacket(new SystemMessage(SystemMessage.THE_COLLECTION_HAS_FAILED));

				env.target.onDecay();
			}
	}
}
