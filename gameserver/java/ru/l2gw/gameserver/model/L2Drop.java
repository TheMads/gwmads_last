package ru.l2gw.gameserver.model;

import ru.l2gw.gameserver.model.base.ItemToDrop;
import ru.l2gw.commons.arrays.GArray;

public class L2Drop
{
	public static final int MAX_CHANCE = 1000000;
	private static final GArray<L2DropGroup> _emptyDrop = new GArray<>(0);
	private GArray<L2DropGroup> _drop = _emptyDrop;
	private GArray<L2DropGroup> _additionalDrop = _emptyDrop;
	private GArray<L2DropGroup> _exItemDrop = _emptyDrop;
	private GArray<L2DropGroup> _spoil = _emptyDrop;

	public void addData(L2DropData d, int groupChance, int dropType)
	{
		if(dropType == 0)
		{
			if(_spoil == _emptyDrop)
				_spoil = new GArray<>(1);
			L2DropGroup temp = new L2DropGroup(_spoil.size(), groupChance, dropType);
			temp.addDropItem(d);
			_spoil.add(temp);
		}
		else if(dropType == 1)
		{
			if(_drop == _emptyDrop)
				_drop = new GArray<>(1);

			int gid = d.getGroupId();
			if(_drop.size() > 0)
				for(L2DropGroup g : _drop)
					if(g.getId() == gid)
					{
						g.addDropItem(d);
						return;
					}

			L2DropGroup temp = new L2DropGroup(d.getGroupId(), groupChance, dropType);
			temp.addDropItem(d);
			_drop.add(temp);
		}
		else if(dropType == 2)
		{
			if(_additionalDrop == _emptyDrop)
				_additionalDrop = new GArray<>(1);

			L2DropGroup temp = new L2DropGroup(_additionalDrop.size(), groupChance, dropType);
			temp.addDropItem(d);
			_additionalDrop.add(temp);
		}
		else if(dropType == 3)
		{
			if(_exItemDrop == _emptyDrop)
				_exItemDrop = new GArray<>(1);

			int gid = d.getGroupId();
			if(_exItemDrop.size() > 0)
				for(L2DropGroup g : _exItemDrop)
					if(g.getId() == gid)
					{
						g.addDropItem(d);
						return;
					}

			L2DropGroup temp = new L2DropGroup(d.getGroupId(), groupChance, dropType);
			temp.addDropItem(d);
			_exItemDrop.add(temp);
		}
	}

	public GArray<ItemToDrop> rollDrop(int diff, boolean isRaidBoss, L2Player player, boolean sweep)
	{
		GArray<ItemToDrop> temp = new GArray<>();

		if(sweep)
		{
			for(L2DropGroup g : _spoil)
			{
				ItemToDrop itd = g.roll(diff, isRaidBoss, player);
				if(itd != null)
					temp.add(itd);
			}
			return temp;
		}

		for(L2DropGroup g : _drop)
		{
			ItemToDrop itd = g.roll(diff, isRaidBoss, player);
			if(itd != null)
				temp.add(itd);
		}

		for(L2DropGroup g : _additionalDrop)
		{
			ItemToDrop itd = g.roll(diff, isRaidBoss, player);
			if(itd != null)
				temp.add(itd);
		}

		for(L2DropGroup g : _exItemDrop)
		{
			ItemToDrop itd = g.roll(diff, isRaidBoss, player);
			if(itd != null)
				temp.add(itd);
		}

		return temp;
	}

	public GArray<L2DropGroup> getSpoil()
	{
		return _spoil;
	}

	public GArray<L2DropGroup> getNormal()
	{
		return _drop;
	}

	public GArray<L2DropGroup> getAdditional()
	{
		return _additionalDrop;
	}

	public GArray<L2DropGroup> getExDrop()
	{
		return _exItemDrop;
	}
}