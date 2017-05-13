package ru.l2gw.gameserver.model;

import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.model.base.Experience;
import ru.l2gw.gameserver.model.base.ItemToDrop;
import ru.l2gw.commons.arrays.GArray;

public class L2DropGroup
{
	private int _groupId;
	private boolean _isAdena = false;
	private boolean _isBossJewel = false;
	private int _groupChance;
	private int _dropType;
	private int _ratedGroupChance;
	private final GArray<L2DropData> _items = new GArray<L2DropData>();

	public L2DropGroup(int id, int groupChance, int dropType)
	{
		_groupId = id;
		_groupChance = groupChance;
		_dropType = dropType;
	}

	public int getId()
	{
		return _groupId;
	}

	public void addDropItem(L2DropData item)
	{
		if(item.getItem().isBossJewel())
			_isBossJewel = true;

		if(item.getItem().isAdena())
			_isAdena = true;

		_items.add(item);

		if(_dropType == 0) // corpse_make_list
		{
			float rateChance = item.getChance() * Config.RATE_DROP_SPOIL;
			float mod = 1;
			if(rateChance > L2Drop.MAX_CHANCE)
				mod = rateChance / L2Drop.MAX_CHANCE;

			float countRate = item.isFixedQty() ? 1 : mod;
			int min = item.getMinDrop() == item.getMaxDrop() ? item.getMinDrop() : Math.round(item.getMinDrop() * countRate);
			int max = Math.round(item.getMaxDrop() * countRate);

			item.setBalanced((int) Math.min(rateChance, L2Drop.MAX_CHANCE), min, max);
			_ratedGroupChance = L2Drop.MAX_CHANCE;
		}
		else if(_dropType == 1) // additional_make_multi_list
		{
			float rateChance = _groupChance * (_isBossJewel ? Config.RATE_DROP_BOSS_JEWEL : item.isRaid() ? Config.RATE_DROP_RAIDBOSS : _isAdena ? Config.RATE_DROP_ADENA : Config.RATE_DROP_ITEMS);
			float mod = 1;
			if(rateChance > L2Drop.MAX_CHANCE)
				mod = rateChance / L2Drop.MAX_CHANCE;

			float countRate = item.isFixedQty() ? 1 : mod;
			int min = item.getMinDrop() == item.getMaxDrop() ? item.getMinDrop() : Math.round(item.getMinDrop() * countRate);
			int max = Math.round(item.getMaxDrop() * countRate);

			if(item.getItem().isEquipment() && max > Config.RATE_EQUIP_LIMIT_MAX)
			{
				min = Config.RATE_EQUIP_LIMIT_MIN;
				max = Config.RATE_EQUIP_LIMIT_MAX;
			}

			item.setBalanced(item.getChance(), min, max);
			_ratedGroupChance = (int) Math.min(rateChance, L2Drop.MAX_CHANCE);
		}
		else if(_dropType == 2) // additional_make_list
		{
			float rateChance = _groupChance * (_isBossJewel ? Config.RATE_DROP_BOSS_JEWEL : item.isRaid() ? Config.RATE_DROP_RAIDBOSS : _isAdena ? Config.RATE_DROP_ADENA : Config.RATE_DROP_ITEMS);
			float mod = 1;
			if(rateChance > L2Drop.MAX_CHANCE)
				mod = rateChance / L2Drop.MAX_CHANCE;

			float countRate = item.isFixedQty() ? 1 : mod;
			int min = item.getMinDrop() == item.getMaxDrop() ? item.getMinDrop() : Math.round(item.getMinDrop() * countRate);
			int max = Math.round(item.getMaxDrop() * countRate);

			if(item.getItem().isEquipment() && max > Config.RATE_EQUIP_LIMIT_MAX)
			{
				min = Config.RATE_EQUIP_LIMIT_MIN;
				max = Config.RATE_EQUIP_LIMIT_MAX;
			}

			item.setBalanced(item.getChance(), min, max);
			_ratedGroupChance = L2Drop.MAX_CHANCE;
		}
		else // ex_item_drop_list
			_ratedGroupChance = _groupChance;
	}

	public GArray<L2DropData> getDropItems(boolean copy)
	{
		if(!copy)
			return _items;
		GArray<L2DropData> temp = new GArray<>(_items.size());
		temp.addAll(_items);
		return temp;
	}

	/**
	 * Эта функция выбирает одну вещь из группы
	 * Используется в основном механизме рассчета дропа
	 */
	public ItemToDrop roll(int diff, boolean isRaidBoss, L2Player player)
	{
		int groupChance = _ratedGroupChance;

		if(Config.DEBUG)
			System.out.println("rollDrop start: groupChance = " + groupChance + " diff: " + diff + " isRaid: " + isRaidBoss + " " + player);

		// Поправка на глублко синих мобов
		double diffMod = 1;
		if(Config.DEEPBLUE_DROP_RULES && diff > 0)
		{
			if(isRaidBoss)
				diffMod = diff == 1 ? 0.90 : Experience.penaltyModifier(diff - 1, 15) - 0.10;
			else
				diffMod = Experience.penaltyModifier(diff, 9);

			if(diffMod < 0)
				diffMod = 0;

			groupChance *= diffMod;
			if(Config.DEBUG)
				System.out.println("rollDrop: diffMod = " + diffMod + " groupChance: " + groupChance);
		}

		if(groupChance < Rnd.get(1, L2Drop.MAX_CHANCE))
			return null;

		int chance = Rnd.get(1, L2Drop.MAX_CHANCE);
		if(Config.DEBUG)
			System.out.println("rollDrop: chance = " + chance + " groupId: " + getId());
		int sum = 0;
		for(L2DropData i : _items)
		{
			sum += i.getBalancedChance();
			if(Config.DEBUG)
				System.out.println("rollDrop: itemId = " + i.getItemId() + " balanced chance: " + i.getBalancedChance() + " sum: " + sum);

			if(sum >= chance)
			{
				ItemToDrop t = new ItemToDrop(i.getItemId());
				if(i.getBalancedMin() < i.getBalancedMax())
					t.count = Rnd.get(i.getBalancedMin(), i.getBalancedMax());
				else
					t.count = i.getBalancedMin();

				if(Config.DEBUG)
					System.out.println("rollDrop: itemId = " + i.getItemId() + " hit! count: " + t.count + " min: " + i.getBalancedMin() + " max: " + i.getBalancedMax());

				return t;
			}
		}

		return null;
	}

	public int getRatedGroupChance()
	{
		return _ratedGroupChance;
	}
}