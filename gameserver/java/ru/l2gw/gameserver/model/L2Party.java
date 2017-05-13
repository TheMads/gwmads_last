package ru.l2gw.gameserver.model;

import ru.l2gw.gameserver.Config;
import ru.l2gw.commons.arrays.GArray;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.cache.Msg;
import ru.l2gw.gameserver.idfactory.IdFactory;
import ru.l2gw.gameserver.instancemanager.PartyRoomManager;
import ru.l2gw.gameserver.instancemanager.RaidBossSpawnManager;
import ru.l2gw.gameserver.model.L2ObjectTasks.SoulIncreaseTask;
import ru.l2gw.gameserver.model.base.Experience;
import ru.l2gw.gameserver.model.entity.DimensionalRift.DimensionalRift;
import ru.l2gw.gameserver.model.entity.olympiad.Olympiad;
import ru.l2gw.gameserver.model.entity.olympiad.OlympiadTeam;
import ru.l2gw.gameserver.model.instances.L2ItemInstance;
import ru.l2gw.gameserver.model.instances.L2MonsterInstance;
import ru.l2gw.gameserver.model.instances.L2NpcInstance;
import ru.l2gw.gameserver.serverpackets.*;
import ru.l2gw.gameserver.skills.Stats;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

public class L2Party
{
	private final List<L2Player> _members = new CopyOnWriteArrayList<>();
	private int _partyLvl = 0;
	private int _itemDistribution = 0;
	private int _itemOrder = 0;
	private DimensionalRift _dr;
	private L2CommandChannel _commandChannel;
	private OlympiadTeam _olympiadTeam;
	private int _partyLeader = 0;

	public static final int ITEM_LOOTER = 0;
	public static final int ITEM_RANDOM = 1;
	public static final int ITEM_RANDOM_SPOIL = 2;
	public static final int ITEM_ORDER = 3;
	public static final int ITEM_ORDER_SPOIL = 4;

	private byte _requestChangeLoot = -1;
	private List<Integer> _changeLootAnswers = null;
	private long _requestChangeLootTimer = 0;
	private static final int[] LOOT_SYSSTRINGS = {487, 488, 798, 799, 800};
	private Future<?> _checkTask = null;
	private int _partyId;

	/**
	 * constructor ensures party has always one member - leader
	 * @param leader создатель парти
	 * @param itemDistribution режим распределения лута
	 */
	public L2Party(L2Player leader, int itemDistribution)
	{
		_itemDistribution = itemDistribution;
		_members.add(leader);
		_partyLvl = leader.getLevel();
		_partyId = IdFactory.getInstance().getNextId();
	}

	/**
	 * @return number of party members
	 */
	public int getMemberCount()
	{
		return getPartyMembers().size();
	}

	/**
	 * @return all party members
	 */
	public List<L2Player> getPartyMembers()
	{
		return _members;
	}

	public List<L2Playable> getPartyMembersWithPets()
	{
		List<L2Playable> result = new ArrayList<>(_members.size());
		for(L2Player member : _members)
		{
			result.add(member);
			if(member.getPet() != null)
				result.add(member.getPet());
		}
		return result;
	}

	public L2Player getRandomMember()
	{
		return getPartyMembers().get(Rnd.get(getPartyMembers().size()));
	}

	/**
	 * @return random member from party
	 */
	private L2Player getRandomMemberInRange(L2Player player, L2ItemInstance item, int range)
	{
		ArrayList<L2Player> result = new ArrayList<>(_members.size());

		for(L2Player member : _members)
			if(member.isInRange(player, range) && !member.isDead() && member.getInventory().validateCapacity(item) && member.getInventory().validateWeight(item))
				result.add(member);

		if(result.size() > 0)
			return result.get(Rnd.get(result.size()));

		return null;
	}

	/**
	 * @return next item looter
	 */
	private L2Player getNextLooterInRange(L2Player player, L2ItemInstance item, int range)
	{
		synchronized(_members)
		{
			int antiloop = _members.size();
			while(--antiloop > 0)
			{
				int looter = _itemOrder;
				_itemOrder++;
				if(_itemOrder > _members.size() - 1)
					_itemOrder = 0;

				L2Player ret = looter < _members.size() ? _members.get(looter) : player;

				if(ret != null && !ret.isDead() && ret.isInRangeZ(player, range) && ret.getInventory().validateCapacity(item) && ret.getInventory().validateWeight(item))
					return ret;
			}
		}
		return player;
	}

	/**
	 * true if player is party leader
	 * @param player
	 * @return
	 */
	public boolean isLeader(L2Player player)
	{
		synchronized(_members)
		{
			return _members.size() > _partyLeader && _members.get(_partyLeader).equals(player);
		}
	}

	/**
	 * Returns the Object ID for the party leader to be used as a unique identifier of this party
	 * @return int
	 */
	public int getPartyLeaderOID()
	{
		return getPartyLeader().getObjectId();
	}

	/**
	 * Возвращает лидера партии
	 * @return L2Player Лидер партии
	 */
	public L2Player getPartyLeader()
	{
		synchronized(_members)
		{
			return _members.get(_partyLeader);
		}
	}

	/**
	 * Broadcasts packet to every party member
	 * @param msg packet to broadcast
	 */
	public void broadcastToPartyMembers(L2GameServerPacket msg)
	{
		for(L2Player member : _members)
			member.sendPacket(msg);
	}

	/**
	 * Рассылает текстовое сообщение всем членам группы
	 * @param msg сообщение
	 */
	public void broadcastMessageToPartyMembers(String msg)
	{
		broadcastToPartyMembers(SystemMessage.sendString(msg));
	}

	/**
	 * Рассылает пакет всем членам группы исключая указанного персонажа<BR><BR>
	 */
	public void broadcastToPartyMembers(L2Player player, L2GameServerPacket msg)
	{
		for(L2Player member : _members)
			if(!member.equals(player))
				member.sendPacket(msg);
	}

	public void broadcastToPartyMembersInRange(L2Player player, L2GameServerPacket msg, int range)
	{
		for(L2Player member : _members)
			if(player.isInRange(member, range))
				member.sendPacket(msg);
	}

	public boolean containsMember(L2Character cha)
	{
		if(cha == null)
			return false;

		L2Player player = cha.getPlayer();
		return player != null && _members.contains(player);
	}

	/**
	 * adds new member to party
	 * @param player L2Player to add
	 */
	public void addPartyMember(L2Player player)
	{
		if(_requestChangeLoot != -1)
			finishLootRequest(false); // cancel on invite

		//sends new member party window for all members
		//we do all actions before adding member to a list, this speeds things up a little
		player.sendPacket(new PartySmallWindowAll(_members));

		for(L2Player partyMember : getPartyMembers())
			if(partyMember.getPet() != null)
				player.sendPacket(new ExPartyPetWindowAdd(partyMember.getPet()));

		player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_JOINED_S1S_PARTY).addString(_members.get(_partyLeader).getName()));
		broadcastToPartyMembers(new SystemMessage(SystemMessage.S1_HAS_JOINED_THE_PARTY).addString(player.getName()));
		broadcastToPartyMembers(new PartySmallWindowAdd(player));

		if(player.isPetSummoned())
			broadcastToPartyMembers(new ExPartyPetWindowAdd(player.getPet()));

		synchronized(_members)
		{
			_members.add(player);
		}

		recalculatePartyData();

		player.updateEffectIcons();
		if(player.isPetSummoned())
			player.getPet().updateEffectIcons();

		// Если партия уже в СС, то вновь прибывшем посылаем пакет открытия окна СС
		if(isInCommandChannel())
			player.sendPacket(Msg.ExMPCCOpen);
		
		if(player.getPartyRoom() > 0)
		{
			PartyRoom room = PartyRoomManager.getInstance().getRooms().get(player.getPartyRoom());
			if(room != null)
				room.updateInfo();
		}

		player.broadcastRelation();

		if(_olympiadTeam != null && Olympiad.isInTeamList(_olympiadTeam))
			Olympiad.unRegisterTeam(_olympiadTeam);
	}

	/**
	 * removes player from party
	 * @param player L2Player to remove
	 */
	public void removePartyMember(L2Player player)
	{
		if(player == null || !_members.contains(player))
			return;

		int memberIndex = _members.indexOf(player);
		
		synchronized(_members)
		{
			_members.remove(player);
		}

		recalculatePartyData();

		// Отсылаемы вышедшему пакет закрытия СС
		if(isInCommandChannel())
			player.sendPacket(Msg.ExMPCCClose);

		player.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_WITHDRAWN_FROM_THE_PARTY));
		player.sendPacket(Msg.PartySmallWindowDeleteAll);
		player.setParty(null);

		broadcastToPartyMembers(new SystemMessage(SystemMessage.S1_HAS_LEFT_THE_PARTY).addString(player.getName()));
		broadcastToPartyMembers(new PartySmallWindowDelete(player));
		if(player.isPetSummoned())
			broadcastToPartyMembers(new ExPartyPetWindowDelete(player.getPet()));

		if(_members.size() == 1)
		{
			_partyLeader = 0;
			L2Player lastMember = _members.get(0);

			// Если в партии остался 1 человек, то удаляем ее из СС
			if(isInCommandChannel())
			{
				if(_commandChannel.getChannelLeader() == player || _commandChannel.getChannelLeader() == lastMember)
					_commandChannel.disbandChannel();
				else
					_commandChannel.removeParty(this);
			}

			lastMember.sendPacket(new SystemMessage(SystemMessage.THE_PARTY_HAS_DISPERSED));
			lastMember.setParty(null);

			if(isInDimensionalRift())
				_dr.manualExit();

			IdFactory.getInstance().releaseId(_partyId);
			if(_checkTask != null)
			{
				_checkTask.cancel(true);
				_checkTask = null;
			}
		}
		else if(_partyLeader == memberIndex)
		{
			_partyLeader = 0;
			broadcastToPartyMembers(Msg.PartySmallWindowDeleteAll);

			for(L2Player member : getPartyMembers())
				member.sendPacket(new PartySmallWindowAll(_members));
		}
		else if(_partyLeader > memberIndex)
		{
			_partyLeader--;
			broadcastToPartyMembers(new PartySmallWindowDelete(player));
		}
		else
			broadcastToPartyMembers(new PartySmallWindowDelete(player));
		
		if(player.getPartyRoom() > 0)
		{
			PartyRoom room = PartyRoomManager.getInstance().getRooms().get(player.getPartyRoom());
			if(room != null)
				room.updateInfo();
		}

		player.broadcastRelation();

		if(_olympiadTeam != null && Olympiad.isInTeamList(_olympiadTeam))
			Olympiad.unRegisterTeam(_olympiadTeam);
	}

	/**
	 * Change party leader (used for string arguments)
	 * @param name имя нового лидера парти
	 */
	public void changePartyLeader(String name)
	{
		L2Player new_leader = getPlayerByName(name);
		L2Player current_leader = _members.get(_partyLeader);

		if(new_leader == null)
			return;

		if(current_leader.equals(new_leader))
		{
			current_leader.sendPacket(new SystemMessage(SystemMessage.YOU_CANNOT_TRANSFER_RIGHTS_TO_YOURSELF));
			return;
		}

		synchronized(_members)
		{
			if(!_members.contains(new_leader))
			{
				current_leader.sendPacket(new SystemMessage(SystemMessage.YOU_CAN_TRANSFER_RIGHTS_ONLY_TO_ANOTHER_PARTY_MEMBER));
				return;
			}

			_partyLeader = _members.indexOf(new_leader);

			//обновляем инфо о пати у всех пати мемберов и сообщаем о новом лидере
			SystemMessage msg = new SystemMessage(SystemMessage.S1_HAS_BECOME_A_PARTY_LEADER);
			msg.addString(new_leader.getName());
			for(L2Player member : getPartyMembers())
			{
				member.sendPacket(Msg.PartySmallWindowDeleteAll);
				member.sendPacket(new PartySmallWindowAll(_members));
				member.sendPacket(msg);
			}
			for(L2Player member : getPartyMembers())
			{
				broadcastToPartyMembers(member, new PartySpelled(member, true));
			}
		}

		if(isInCommandChannel())
		{
			if(_members.contains(_commandChannel.getChannelLeader()))
				_commandChannel.setChannelLeader(new_leader);
			_commandChannel.broadcastToChannelMembers(new ExMultiPartyCommandChannelInfo(_commandChannel));
		}

		new_leader.broadcastRelation();
		current_leader.broadcastRelation();

		if(_olympiadTeam != null && Olympiad.isInTeamList(_olympiadTeam))
			Olympiad.unRegisterTeam(_olympiadTeam);
	}

	/**
	 * finds a player in the party by name
	 * @param name имя для поиска
	 * @return найденый L2Player или null если не найдено
	 */
	private L2Player getPlayerByName(String name)
	{
		for(L2Player member : _members)
			if(member.getName().equalsIgnoreCase(name))
				return member;
		return null;
	}

	/**
	 * Oust player from party
	 * @param player L2Player которого выгоняют
	 */
	public void oustPartyMember(L2Player player)
	{
		if(!getPartyMembers().contains(player))
			return;

		if(isLeader(player))
		{
			removePartyMember(player);
			if(getPartyMembers().size() > 1)
			{
				if(isInCommandChannel())
				{
					if(player == _commandChannel.getChannelLeader())
						_commandChannel.setChannelLeader(getPartyMembers().get(_partyLeader));
					_commandChannel.broadcastToChannelMembers(new ExMultiPartyCommandChannelInfo(_commandChannel));
				}
				SystemMessage msg = new SystemMessage(SystemMessage.S1_HAS_BECOME_A_PARTY_LEADER);
				msg.addString(getPartyMembers().get(_partyLeader).getName());
				L2GameServerPacket del_pkt = new PartySmallWindowDeleteAll();
				for(L2Player member : _members)
				{
					member.sendPacket(del_pkt);
					member.sendPacket(new PartySmallWindowAll(_members));
					member.sendPacket(msg);
				}
			}
		}
		else
			removePartyMember(player);
	}

	/**
	 * Oust player from party Overloaded method that takes player's name as
	 * parameter
	 *
	 * @param name имя игрока для изгнания
	 */
	public void oustPartyMember(String name)
	{
		oustPartyMember(getPlayerByName(name));
	}

	/**
	 * distribute item(s) to party members
	 * @param player
	 * @param item
	 */
	public void distributeItem(L2Player player, L2ItemInstance item)
	{
		distributeItem(player, item, null);
	}

	public void distributeItem(L2Player player, L2ItemInstance item, L2NpcInstance fromNpc)
	{

		if(item.getItemId() == 57)
		{
			distributeAdena(item, player);
			return;
		}

		L2Player target;

		switch(_itemDistribution)
		{
			case ITEM_RANDOM:
			case ITEM_RANDOM_SPOIL:
				target = getRandomMemberInRange(player, item, Config.ALT_PARTY_DISTRIBUTION_RANGE);
				break;
			case ITEM_ORDER:
			case ITEM_ORDER_SPOIL:
				target = getNextLooterInRange(player, item, Config.ALT_PARTY_DISTRIBUTION_RANGE);
				break;
			case ITEM_LOOTER:
			default:
				target = player;
				break;
		}

		if(target == null)
		{
			item.dropToTheGround(player, fromNpc);
			return;
		}

		if(!target.getInventory().validateWeight(item))
		{
			target.sendActionFailed();
			target.sendPacket(Msg.YOU_HAVE_EXCEEDED_THE_WEIGHT_LIMIT);
			item.dropToTheGround(target, fromNpc);
			return;
		}

		if(!target.getInventory().validateCapacity(item))
		{
			target.sendActionFailed();
			target.sendPacket(Msg.YOUR_INVENTORY_IS_FULL);
			item.dropToTheGround(player, fromNpc);
			return;
		}

		if(item.getCount() == 1)
		{
			SystemMessage smsg;
			if(item.getEnchantLevel() > 0)
			{
				smsg = new SystemMessage(SystemMessage.S1_HAS_OBTAINED__S2S3);
				smsg.addString(target.getName());
				smsg.addNumber(item.getEnchantLevel());
				smsg.addItemName(item.getItemId());
			}
			else
			{
				smsg = new SystemMessage(SystemMessage.S1_HAS_OBTAINED_S2);
				smsg.addString(target.getName());
				smsg.addItemName(item.getItemId());
			}

			broadcastToPartyMembers(target, smsg);
		}
		else
		{
			SystemMessage smsg = new SystemMessage(SystemMessage.S1_HAS_OBTAINED_S3_S2);
			smsg.addString(target.getName());
			smsg.addItemName(item.getItemId());
			smsg.addNumber(item.getCount());
			broadcastToPartyMembers(target, smsg);
		}

		target.addItem("Party", item, player, true);
		target.sendChanges();
	}

	/**
	 * distribute adena to party members
	 * @param adena инстанс адены для распределения
	 * @param player кто поднял
	 */
	public void distributeAdena(L2ItemInstance adena, L2Player player)
	{
		long totalAdena = adena.getCount();

		ArrayList<L2Player> _membersInRange = new ArrayList<L2Player>();

		if(adena.getCount() < _members.size())
			_membersInRange.add(player);
		else
			for(L2Player p : _members)
				if(p.equals(player) || player.isInRange(p, Config.ALT_PARTY_DISTRIBUTION_RANGE) && !p.isDead())
					_membersInRange.add(p);

		long amount = totalAdena / _membersInRange.size();
		long ost = totalAdena % _membersInRange.size();

		for(L2Player member : _membersInRange)
			member.addAdena("Party", member.equals(player) ? amount + ost : amount, player, true);
	}

	/**
	 * Distribute Experience and SP rewards to L2Player Party members in the known area of the last attacker.<BR><BR>
	 *
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Get the L2Player owner of the L2SummonInstance (if necessary) </li>
	 * <li>Calculate the Experience and SP reward distribution rate </li>
	 * <li>Add Experience and SP to the L2Player </li><BR><BR>
	 *
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : This method DOESN'T GIVE rewards to L2PetInstance</B></FONT><BR><BR>
	 *
	 * @param xpReward The Experience reward to distribute
	 * @param spReward The SP reward to distribute
	 * @param rewardedMembers The list of L2Player to reward and LSummonInstance whose owner must be reward
	 * @param lastAttacker The L2Character that has killed the L2NpcInstance
	 *
	 */
	public void distributeXpAndSp(long xpReward, long spReward, long rpReward, float partyMul, GArray<L2Player> rewardedMembers, L2Character lastAttacker, L2NpcInstance target)
	{
		recalculatePartyData();

		GArray<L2Player> mtr = new GArray<L2Player>();
		int minPartyLevel = -1;
		int maxPartyLevel = -1;
		double partyLvlSum = 0;

		// создаём список тех кто рядом
		for(L2Player member : rewardedMembers)
		{
			if(!lastAttacker.isInRange(member, Config.ALT_PARTY_DISTRIBUTION_RANGE))
				continue;

			if(minPartyLevel == -1 || member.getLevel() < minPartyLevel)
				minPartyLevel = member.getLevel();

			if(maxPartyLevel == -1 || member.getLevel() > maxPartyLevel)
				maxPartyLevel = member.getLevel();

			partyLvlSum += member.getLevel();

			mtr.add(member);
		}

		if(maxPartyLevel - minPartyLevel > 15)
		{
			for(L2Player member : rewardedMembers)
				if(member.getLevel() < maxPartyLevel - 15)
					mtr.remove(member);
			partyLvlSum = 0;
			for(L2Player member : mtr)
				partyLvlSum += member.getLevel();
		}

		// бонус за пати
		double bonus = getExpSpBonus(mtr.size());

		// количество эксп и сп для раздачи на всех
		double XP = xpReward * bonus;
		double SP = spReward * bonus;
		double RP = rpReward * bonus;

		byte targetLevel = target.getLevel();

		for(L2Player member : mtr)
		{
			double lvlPenalty = 1;
			double partyLvlPenalty = 1;
			if(maxPartyLevel - member.getLevel() >= 10 && maxPartyLevel - member.getLevel() <= 14)
				partyLvlPenalty = 0.3;

			if(member.getLevel() - 9 > targetLevel)
				lvlPenalty = Experience.penaltyModifier(member.getLevel() - targetLevel - 7, 9);

			// отдаем его часть с учетом пенальти
			double memberXp = XP * lvlPenalty * member.getLevel() / partyLvlSum * partyLvlPenalty;
			double memberSp = SP * lvlPenalty * member.getLevel() / partyLvlSum * partyLvlPenalty;
			double memberRp = RP * lvlPenalty * member.getLevel() / partyLvlSum * partyLvlPenalty;

			// больше чем соло не дадут
			if(memberXp > xpReward)
				memberXp = xpReward;
			if(memberSp > spReward)
				memberSp = spReward;
			if(memberRp > rpReward)
				memberRp = rpReward;

			// Vitality
			double bonusXp = 0;
			double bonusSp = 0;
			if(target.getAIParams() == null || !target.getAIParams().getBool("no_vitality", false))
			{
				int baseXp = (int) (target.getTemplate().revardExp * lvlPenalty * member.getLevel() * partyMul / partyLvlSum);
				bonusXp += (long) target.calcStat(Stats.EXP_SP, target.getTemplate().revardExp, null, null) * lvlPenalty * member.getLevel() / partyLvlSum * Config.VIT_RATE * member.getVitality().getRate();
				bonusSp += (long) target.calcStat(Stats.EXP_SP, target.getTemplate().revardSp, null, null) * lvlPenalty * member.getLevel() / partyLvlSum * Config.VIT_RATE * member.getVitality().getRate();
				member.getVitality().updatePointsByExp(baseXp, target.getLevel(), target.isRaid(), target.isRaid());
			}

			if(member.isPremiumEnabled())
			{
				bonusXp += memberXp * Config.PREMIUM_RATE_EXPSP / 100.;
				bonusSp += memberSp * Config.PREMIUM_RATE_EXPSP / 100.;
			}

			bonusXp += memberXp * member.calcStat(Stats.EXP_MODIFY, 1, null, null);
			bonusSp += memberXp * member.calcStat(Stats.SP_MODIFY, 1, null, null);

			if(Config.ALT_FLOATING_RATE_ENABLE)
			{
				memberXp *= member.getFloatingRate().rateEXPSP;
				memberSp *= member.getFloatingRate().rateEXPSP;
			}

			memberXp += bonusXp;
			memberSp += bonusSp;

			memberXp *= partyMul;
			memberSp *= partyMul;
			bonusXp *= partyMul;
			bonusSp *= partyMul;

			long petXP = 0;
			long petSP = 0;
			L2Summon pet = member.getPet();
			// отдаем часть пету или саммону
			if(pet != null && !pet.isDead() && member.isInRange(pet, Config.ALT_PARTY_DISTRIBUTION_RANGE))
			{
				float penalty = pet.getExpPenalty();
				petXP = (long) (memberXp * penalty);
				memberXp -= petXP;
				petSP = (long) (memberSp * penalty);
				memberSp -= petSP;
			}

			if(memberXp < 0)
				memberXp = 0;
			if(memberSp < 0)
				memberSp = 0;
			if(bonusXp < 0)
				bonusXp = 0;
			if(bonusSp < 0)
				bonusSp = 0;

			member.getHuntingBonus().addPoints(Rnd.get(5));
			member.addExpAndSp((long) memberXp, (long) memberSp, (long) bonusXp, (long) bonusSp, true);
			if(petXP > 0)
				pet.addExpAndSp(petXP, petSP);

			if(memberRp > 0)
			{
				RaidBossSpawnManager.getInstance().addPoints(member.getObjectId(), target.getNpcId(), (int) memberRp);
				member.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_EARNED_S1_RAID_POINTS).addNumber((int) memberRp));
				L2MonsterInstance._logBoss.info(member + " got raid points: " + (int) memberRp + " for killing: " + target);
			}
			// Начисление душ камаэлянам
			double neededExp = member.calcStat(Stats.SOULS_CONSUME_EXP, 0, null, null);
			if(neededExp > 0 && memberXp > neededExp)
			{
				target.broadcastPacket(new SpawnEmitter(target, member));
				ThreadPoolManager.getInstance().scheduleAi(new SoulIncreaseTask(member), 1000, true);
				//member.setConsumedSouls(member.getConsumedSouls() + 1, target);
			}
		}

		recalculatePartyData();
	}

	public void recalculatePartyData()
	{
		_partyLvl = 0;
		for(L2Player member : _members)
			if(member.getLevel() > _partyLvl)
				_partyLvl = member.getLevel();
	}

	private static final double[] bonuses = { 1.00, 1.00, 1.10, 1.20, 1.30, 1.40, 1.50, 2.00, 2.10, 2.20 };

	private double getExpSpBonus(int size)
	{
		return bonuses[size];
	}

	public int getLevel()
	{
		return _partyLvl;
	}

	public int getLootDistribution()
	{
		return _itemDistribution;
	}

	public boolean isDistributeSpoilLoot()
	{
		boolean rv = false;

		if(_itemDistribution == ITEM_RANDOM_SPOIL || _itemDistribution == ITEM_ORDER_SPOIL)
			rv = true;

		return rv;
	}

	public boolean isInDimensionalRift()
	{
		return _dr != null;
	}

	public void setDimensionalRift(DimensionalRift dr)
	{
		_dr = dr;
	}

	public DimensionalRift getDimensionalRift()
	{
		return _dr;
	}

	public void setOlympiadTeam(OlympiadTeam ot)
	{
		_olympiadTeam = ot;
	}

	public OlympiadTeam getOlympiadTeam()
	{
		return _olympiadTeam;
	}

	public boolean isInCommandChannel()
	{
		return _commandChannel != null;
	}

	public L2CommandChannel getCommandChannel()
	{
		return _commandChannel;
	}

	public void setCommandChannel(L2CommandChannel channel)
	{
		_commandChannel = channel;
	}

	public boolean isAutoLoot()
	{
		for(L2Player player : _members)
			if(player != null && !player.isAutoLoot())
				return false;

		return true;
	}

	@Override
	public String toString()
	{
		return "Party[leader=" + getPartyLeader() + ";members=" + getPartyMembers().size() + "]";
	}

	public void requestLootChange(byte type)
	{
		if(_requestChangeLoot != -1)
			if(System.currentTimeMillis() > _requestChangeLootTimer)
				finishLootRequest(false);
			else
				return;

		int additionalTime = 45000; // timeout 45sec, guess

		_requestChangeLoot = type;
		_requestChangeLootTimer = System.currentTimeMillis() + additionalTime;
		_changeLootAnswers = new ArrayList<>(_members.size());
		_checkTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new ChangeLootCheck(), additionalTime + 1000, 5000);

		broadcastToPartyMembers(getPartyLeader(), new ExAskModifyPartyLooting(getPartyLeader().getName(), type));
		getPartyLeader().sendPacket(new SystemMessage(SystemMessage.REQUESTING_APPROVAL_CHANGE_PARTY_LOOT_S1).addSystemString(LOOT_SYSSTRINGS[type]));
	}

	public synchronized void answerLootChangeRequest(L2Player member, boolean answer)
	{
		if(_requestChangeLoot == -1)
			return;
		if(_changeLootAnswers.contains(member.getObjectId()))
			return;
		if(!answer)
		{
			finishLootRequest(false);
			return;
		}
		_changeLootAnswers.add(member.getObjectId());
		if(_changeLootAnswers.size() >= getMemberCount() - 1)
			finishLootRequest(true);
	}

	private synchronized void finishLootRequest(boolean success)
	{
		if(_requestChangeLoot == -1)
			return;
		if(_checkTask != null)
		{
			_checkTask.cancel(false);
			_checkTask = null;
		}
		if(success)
		{
			_itemDistribution = _requestChangeLoot;
			broadcastToPartyMembers(new ExSetPartyLooting(1, _requestChangeLoot));
			broadcastToPartyMembers(new SystemMessage(SystemMessage.PARTY_LOOT_CHANGED_S1).addSystemString(LOOT_SYSSTRINGS[_requestChangeLoot]));
		}
		else
		{
			broadcastToPartyMembers(new ExSetPartyLooting(0, (byte) 0));
			broadcastToPartyMembers(new SystemMessage(SystemMessage.PARTY_LOOT_CHANGE_CANCELLED));
		}
		_requestChangeLoot = -1;
		_requestChangeLootTimer = 0;
		_changeLootAnswers = null;
	}

	public int getPartyId()
	{
		return _partyId;
	}

	private class ChangeLootCheck implements Runnable
	{
		@Override
		public void run()
		{
			if(System.currentTimeMillis() > L2Party.this._requestChangeLootTimer)
				L2Party.this.finishLootRequest(false);
		}
	}
}