package ru.l2gw.gameserver.model.instances;

import ru.l2gw.gameserver.model.L2Character;
import ru.l2gw.gameserver.model.L2Party;
import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.entity.SevenSignsFestival.FestivalSpawnGroup;
import ru.l2gw.gameserver.model.entity.SevenSignsFestival.SevenSignsFestival;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

/**
 * L2FestivalMonsterInstance
 * This class manages all attackable festival NPCs, spawned during the Festival of Darkness.
 *
 * @author Tempy
 */
public class L2FestivalMonsterInstance extends L2MonsterInstance
{
	protected int _offeringCount = 1;
	private int _festibalId;
	private int _subGroupId;
	private FestivalSpawnGroup _spawnGroup;

	/**
	 * Constructor of L2FestivalMonsterInstance (use L2Character and L2NpcInstance constructor).<BR><BR>
	 * <p/>
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Call the L2Character constructor to set the _template of the L2FestivalMonsterInstance (copy skills from template to object and link _calculators to NPC_STD_CALCULATOR) </li>
	 * <li>Set the name of the L2MonsterInstance</li>
	 * <li>Create a RandomAnimation Task that will be launched after the calculated delay if the server allow it </li><BR><BR>
	 *
	 * @param objectId	  Identifier of the object to initialized
	 * @param Template to apply to the NPC
	 */
	public L2FestivalMonsterInstance(int objectId, L2NpcTemplate template, long bossIndex, long p1, long p2, long p3)
	{
		super(objectId, template, bossIndex, p1, p2, p3);
	}

	public void setOfferingCount(int bonusMultiplier)
	{
		_offeringCount = bonusMultiplier;
	}

	public void setFestivalId(int festId)
	{
		_festibalId = festId;
	}

	public void setSubGroupId(int subGroupId)
	{
		_subGroupId = subGroupId;
	}

	public int getFestivalId()
	{
		return _festibalId;
	}

	public int getSubGroupId()
	{
		return _subGroupId;
	}

	public void setSpawnGroup(FestivalSpawnGroup spawnGroup)
	{
		_spawnGroup = spawnGroup;
	}


	/**
	 * Return True if the attacker is not another L2FestivalMonsterInstance.<BR><BR>
	 */
	@Override
	public boolean isAttackable(L2Character attacker, boolean forceUse, boolean sendMessage)
	{
		return !(attacker instanceof L2FestivalMonsterInstance);
	}

	/**
	 * All mobs in the festival really don't need random animation.
	 */
	@Override
	public boolean hasRandomAnimation()
	{
		return false;
	}

	/**
	 * Actions:
	 * <li>Check if the killing object is a player, and then find the party they belong to.</li>
	 * <li>Add a blood offering item to the leader of the party.</li>
	 * <li>Update the party leader's inventory to show the new item addition.</li>
	 */
	@Override
	public void doItemDrop(L2Player lastAttacker, int levelDiff)
	{
		if(lastAttacker == null)
			return;

		super.doItemDrop(lastAttacker, levelDiff);

		L2Party party = lastAttacker.getParty();

		if(party == null)
			return;

		double levelMod = (double) getLevel() / lastAttacker.getLevel();
		levelMod = Math.min(Math.max(levelMod, 0.5), 1.3);

		L2Player partyLeader = party.getPartyLeader();
		int count = Math.max((int) (_offeringCount * levelMod), 1);

		party.broadcastToPartyMembers(partyLeader, new SystemMessage(SystemMessage.THE_PARTY_LEADER_HAS_OBTAINED_S2_OF_S1).addItemName(SevenSignsFestival.FESTIVAL_OFFERING_ID).addNumber(count));
		partyLeader.addItem("FestivalDrop", SevenSignsFestival.FESTIVAL_OFFERING_ID, count, this, true);
	}

	@Override
	public boolean canMoveToHome()
	{
		return false;
	}

	@Override
	public void onDecay()
	{
		super.onDecay();
		if(_spawnGroup != null)
			_spawnGroup.npcDecayed(this);
	}
}