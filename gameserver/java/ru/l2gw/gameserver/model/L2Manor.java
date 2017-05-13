package ru.l2gw.gameserver.model;

import javolution.util.FastMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.instancemanager.CastleManorManager;
import ru.l2gw.gameserver.instancemanager.CastleManorManager.CropProcure;
import ru.l2gw.gameserver.instancemanager.ResidenceManager;
import ru.l2gw.gameserver.instancemanager.TerritoryWarManager;
import ru.l2gw.gameserver.instancemanager.TownManager;
import ru.l2gw.gameserver.model.L2Skill.NextAction;
import ru.l2gw.gameserver.model.entity.siege.SiegeUnit;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2MonsterInstance;
import ru.l2gw.gameserver.model.instances.L2RaidBossInstance;
import ru.l2gw.gameserver.tables.ItemTable;
import ru.l2gw.gameserver.tables.SkillTable;
import ru.l2gw.gameserver.templates.L2Item;

import java.io.*;
import java.util.StringTokenizer;

public class L2Manor
{
	private static Log _log = LogFactory.getLog(L2Manor.class.getName());
	private static L2Manor _instance;

	private static FastMap<Integer, SeedData> _seeds;

	public L2Manor()
	{
		_seeds = new FastMap<Integer, SeedData>().shared();
		parseData();
	}

	public static L2Manor getInstance()
	{
		if(_instance == null)
			_instance = new L2Manor();
		return _instance;
	}

	public GArray<Integer> getAllCrops()
	{
		GArray<Integer> crops = new GArray<Integer>();

		for(SeedData seed : _seeds.values())
			if(!crops.contains(seed.getCrop()) && seed.getCrop() != 0 && !crops.contains(seed.getCrop()))
				crops.add(seed.getCrop());

		return crops;
	}

	public static SeedData getSeedByItemId(int seedId)
	{
		if(_seeds.get(seedId) != null)
			return _seeds.get(seedId);
		return null;
	}

	public int getSeedBasicPrice(int seedId)
	{
		L2Item seedItem = ItemTable.getInstance().getTemplate(seedId);
		if(seedItem != null)
			return seedItem.getReferencePrice();
		return 0;
	}

	public int getSeedBasicPriceByCrop(int cropId)
	{
		for(SeedData seed : _seeds.values())
			if(seed.getCrop() == cropId)
				return getSeedBasicPrice(seed.getId());
		return 0;
	}

	public int getCropBasicPrice(int cropId)
	{
		L2Item cropItem = ItemTable.getInstance().getTemplate(cropId);

		if(cropItem != null)
			return cropItem.getReferencePrice();
		return 0;
	}

	public int getMatureCrop(int cropId)
	{
		for(SeedData seed : _seeds.values())
			if(seed.getCrop() == cropId)
				return seed.getMature();
		return 0;
	}

	/**
	 * Returns price which lord pays to buy one seed
	 * @param seedId
	 * @return seed price
	 */
	public long getSeedBuyPrice(int seedId)
	{
		long buyPrice = getSeedBasicPrice(seedId) / 10;
		return buyPrice > 0 ? buyPrice : 1;
	}

	public int getSeedMinLevel(int seedId)
	{
		SeedData seed = _seeds.get(seedId);

		if(seed != null)
			return seed.getLevel() - 5;
		return -1;
	}

	public int getSeedMaxLevel(int seedId)
	{
		SeedData seed = _seeds.get(seedId);

		if(seed != null)
			return seed.getLevel() + 5;
		return -1;
	}

	public int getSeedLevelByCrop(int cropId)
	{
		for(SeedData seed : _seeds.values())
			if(seed.getCrop() == cropId)
				return seed.getLevel();
		return 0;
	}

	public int getSeedLevel(int seedId)
	{
		SeedData seed = _seeds.get(seedId);

		if(seed != null)
			return seed.getLevel();
		return -1;
	}

	public boolean isAlternative(int seedId)
	{
		for(SeedData seed : _seeds.values())
			if(seed.getId() == seedId)
				return seed.isAlternative();
		return false;
	}

	public int getCropType(int seedId)
	{
		SeedData seed = _seeds.get(seedId);

		if(seed != null)
			return seed.getCrop();
		return -1;
	}

	public synchronized int getRewardItem(int cropId, int type)
	{
		for(SeedData seed : _seeds.values())
			if(seed.getCrop() == cropId)
				return seed.getReward(type); // there can be several
		// seeds with same crop, but
		// reward should be the same for
		// all
		return -1;
	}

	public synchronized long getRewardAmountPerCrop(int castle, int cropId, int type)
	{
		final CropProcure cs = ResidenceManager.getInstance().getCastleById(castle).getCropProcure(CastleManorManager.PERIOD_CURRENT).get(cropId);

		for(SeedData seed : _seeds.values())
			if(seed.getCrop() == cropId)
				return cs.getPrice() / getCropBasicPrice(seed.getReward(type));
		return -1;
	}

	public synchronized int getRewardItemBySeed(int seedId, int type)
	{
		SeedData seed = _seeds.get(seedId);

		if(seed != null)
			return seed.getReward(type);
		return 0;
	}

	/**
	* Return all crops which can be purchased by given castle
	*
	* @param castleId
	* @return
	*/
	public GArray<Integer> getCropsForCastle(int castleId)
	{
		GArray<Integer> crops = new GArray<Integer>();

		for(SeedData seed : _seeds.values())
			if(seed.getManorId() == castleId && !crops.contains(seed.getCrop()))
				crops.add(seed.getCrop());

		return crops;
	}

	/**
	 * Return list of seed ids, which belongs to castle with given id
	 * @param castleId - id of the castle
	 * @return seedIds - list of seed ids
	 */
	public GArray<Integer> getSeedsForCastle(int castleId)
	{
		GArray<Integer> seedsID = new GArray<Integer>();
		for(SeedData seed : _seeds.values())
			if(seed.getManorId() == castleId && !seedsID.contains(seed.getId()))
				seedsID.add(seed.getId());
		return seedsID;
	}

	/**
	 * Returns castle id where seed can be sowned<br>
	 * @param seedId
	 * @return castleId
	 */
	public int getCastleIdForSeed(int seedId)
	{
		SeedData seed = _seeds.get(seedId);

		if(seed != null)
			return seed.getManorId();
		return 0;
	}

	public int getSeedSaleLimit(int seedId)
	{
		SeedData seed = _seeds.get(seedId);

		if(seed != null)
			return seed.getSeedLimit();
		return 0;
	}

	public int getCropPuchaseLimit(int cropId)
	{
		for(SeedData seed : _seeds.values())
			if(seed.getCrop() == cropId)
				return seed.getCropLimit();
		return 0;
	}

	private class SeedData
	{
		private int _id;
		private int _level; // seed level
		private int _crop; // crop type
		private int _mature; // mature crop type
		private int _type1;
		private int _type2;
		private int _manorId; // id of manor (castle id) where seed can be farmed
		private int _isAlternative;
		private int _limitSeeds;
		private int _limitCrops;

		public SeedData(int level, int crop, int mature)
		{
			_level = level;
			_crop = crop;
			_mature = mature;
		}

		public void setData(int id, int t1, int t2, int manorId, int isAlt, int lim1, int lim2)
		{
			_id = id;
			_type1 = t1;
			_type2 = t2;
			_manorId = manorId;
			_isAlternative = isAlt;
			_limitSeeds = lim1;
			_limitCrops = lim2;
		}

		public int getManorId()
		{
			return _manorId;
		}

		public int getId()
		{
			return _id;
		}

		public int getCrop()
		{
			return _crop;
		}

		public int getMature()
		{
			return _mature;
		}

		public int getReward(int type)
		{
			return type == 1 ? _type1 : _type2;
		}

		public int getLevel()
		{
			return _level;
		}

		public int getMinLevel()
		{
			return _level - 5;
		}

		public int getMaxLevel()
		{
			return _level + 5;
		}

		public boolean isAlternative()
		{
			return _isAlternative == 1;
		}

		public int getSeedLimit()
		{
			return TerritoryWarManager.getTerritoryById(_manorId + 80).hasLord() ? (int) (_limitSeeds * 1.1) : _limitSeeds;
		}

		public int getCropLimit()
		{
			return TerritoryWarManager.getTerritoryById(_manorId + 80).hasLord() ? (int) (_limitCrops * 1.1) : _limitCrops;
		}
	}

	private void parseData()
	{
		LineNumberReader lnr = null;
		try
		{
			File seedData = new File(Config.DATAPACK_ROOT, "data/seeds.csv");
			lnr = new LineNumberReader(new BufferedReader(new FileReader(seedData)));

			String line = null;
			while((line = lnr.readLine()) != null)
			{
				if(line.trim().length() == 0 || line.startsWith("#"))
					continue;
				SeedData seed = parseList(line);
				_seeds.put(seed.getId(), seed);
			}

			_log.info("ManorManager: Loaded " + _seeds.size() + " seeds");
		}
		catch(FileNotFoundException e)
		{
			_log.info("seeds.csv is missing in data folder");
		}
		catch(Exception e)
		{
			_log.info("error while loading seeds: " + e.getMessage());
		}
		finally
		{
			try
			{
				if(lnr != null)
					lnr.close();
			}
			catch(Exception e1)
			{}
		}
	}

	private SeedData parseList(String line)
	{
		StringTokenizer st = new StringTokenizer(line, ";");

		int seedId = Integer.parseInt(st.nextToken()); // seed id
		int level = Integer.parseInt(st.nextToken()); // seed level
		int cropId = Integer.parseInt(st.nextToken()); // crop id
		int matureId = Integer.parseInt(st.nextToken()); // mature crop id
		int type1R = Integer.parseInt(st.nextToken()); // type I reward
		int type2R = Integer.parseInt(st.nextToken()); // type II reward
		int manorId = Integer.parseInt(st.nextToken()); // id of manor, where seed can be farmed
		int isAlt = Integer.parseInt(st.nextToken()); // alternative seed
		int limitSeeds = Integer.parseInt(st.nextToken()); // limit for seeds
		int limitCrops = Integer.parseInt(st.nextToken()); // limit for crops

		SeedData seed = new SeedData(level, cropId, matureId);
		seed.setData(seedId, type1R, type2R, manorId, isAlt, limitSeeds, limitCrops);

		return seed;
	}

	public static boolean useHandler(L2Playable playable, L2ItemInstance item)
	{
		if(playable == null || !playable.isPlayer() || item == null)
			return false;

		SeedData seed = getSeedByItemId(item.getItemId());

		if(seed == null)
			return false;

		L2Player player = (L2Player) playable;

		// Цель не выбрана
		if(playable.getTarget() == null)
		{
			player.sendActionFailed();
			return true;
		}

		// Цель не моб, РБ или миньон
		if(!player.getTarget().isMonster() || player.getTarget() instanceof L2RaidBossInstance || player.getTarget() != null && player.getTarget().isMinion())
		{
			player.sendPacket(Msg.THE_TARGET_IS_UNAVAILABLE_FOR_SEEDING);
			return true;
		}

		L2MonsterInstance target = (L2MonsterInstance) playable.getTarget();

		if(target == null)
		{
			player.sendPacket(Msg.INVALID_TARGET);
			return true;
		}

		// Моб мертв
		if(target.isDead())
		{
			player.sendPacket(Msg.INVALID_TARGET);
			return true;
		}

		// Уже посеяно
		if(target.isSeeded())
		{
			player.sendPacket(Msg.THE_SEED_HAS_BEEN_SOWN);
			return true;
		}

		int _seedId = item.getItemId();
		if(_seedId == 0 || player.getInventory().getItemByItemId(item.getItemId()) == null)
		{
			player.sendPacket(Msg.INCORRECT_ITEM_COUNT);
			return true;
		}

		int castleId = TownManager.getInstance().getBuildingByObject(player).getCastleId();
		if(castleId < 0)
			castleId = 1; // gludio manor dy default
		else
		{
			SiegeUnit castle = ResidenceManager.getInstance().getBuildingById(castleId);
			if(castle != null)
				castleId = castle.getId();
		}

		// Несовпадение зоны
		if(L2Manor.getInstance().getCastleIdForSeed(_seedId) != castleId)
		{
			player.sendPacket(Msg.THIS_SEED_MAY_NOT_BE_SOWN_HERE);
			return true;
		}

		//Тут нада проверять уровни мобов, но пока не знаю как на офф сервере, коминтирую
		//if(target.getLevel() < seed.getMinLevel())
		//{
		//			return true;
		//}
		//if(target.getLevel() > seed.getMaxLevel())
		//{
		//			return true;
		//}

		// use Sowing skill, id 2097
		L2Skill skill = SkillTable.getInstance().getInfo(2097, 1);
		if(skill == null)
		{
			player.sendActionFailed();
			return true;
		}

		if(skill.checkCondition(player, target, item, false, true))
		{
			player.setUseSeed(_seedId);
			player.getAI().Cast(skill, target);
		}
		else if(skill.getNextAction() == NextAction.ATTACK && target.isAttackable(player, false, false))
			player.getAI().Attack(target, false, false);
		else
			player.sendActionFailed();
		return true;
	}
}