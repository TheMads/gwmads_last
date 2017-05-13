package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.entity.Hero;
import ru.l2gw.gameserver.templates.StatsSet;

import java.util.Map;

/**
 * Format: (ch) d [SdSdSdd]
 * d: size
 * [
 * S: hero name
 * d: hero class ID
 * S: hero clan name
 * d: hero clan crest id
 * S: hero ally name
 * d: hero Ally id
 * d: count
 * ]
 */
public class ExHeroList extends L2GameServerPacket
{
	private Map<Integer, StatsSet> _heroList;

	public ExHeroList()
	{
		_heroList = Hero.getActiveHeroes();
	}

	@Override
	protected final void writeImpl()
	{
		writeC(EXTENDED_PACKET);
		writeH(0x7A);

		writeD(_heroList.size());
		for(Integer heroId : _heroList.keySet())
		{
			StatsSet hero = _heroList.get(heroId);
			writeS(hero.getString("char_name"));
			writeD(hero.getInteger("class_id"));
			writeS(hero.getString("clan_name", ""));
			writeD(hero.getInteger("clan_crest", 0));
			writeS(hero.getString("ally_name", ""));
			writeD(hero.getInteger("ally_crest", 0));
			writeD(hero.getInteger("count", 1));
		}

	}
}