package ru.l2gw.gameserver.model;

import ru.l2gw.gameserver.Config;
import ru.l2gw.gameserver.serverpackets.ExStorageMaxCount;
import ru.l2gw.gameserver.serverpackets.NickNameChanged;
import ru.l2gw.gameserver.serverpackets.StatusUpdate;
import ru.l2gw.gameserver.serverpackets.UserInfo;
import ru.l2gw.gameserver.skills.Stats;

@SuppressWarnings( { "nls", "unqualified-field-access", "boxing" })
public class StatsChangeRecorder
{
	private L2Player _player;
	private int _accuracy;
	private int _attackSpeed;
	private int _castSpeed;
	private int _criticalHit;
	private int _evasion;
	private int _magicAttack;
	private int _magicDefence;
	private int _maxCp;
	private int _maxHp;
	private int _maxLoad;
	private int _curLoad;
	private int _maxMp;
	private int _physicAttack;
	private int _physicDefence;
	private int[] _attackElement;
	private int _defenceFire;
	private int _defenceWater;
	private int _defenceWind;
	private int _defenceEarth;
	private int _defenceHoly;
	private int _defenceUnholy;

	private int _headObjectId;
	private int _hairObjectId;
	private int _dhairObjectId;
	private int _rhandObjectId;
	private int _chestObjectId;
	private int _lhandObjectId;
	private int _glovesObjectId;
	private int _legsObjectId;
	private int _feetObjectId;
	private int _backObjectId;
	private int _underObjectId;
	private int _lfingerObjectId;
	private int _rfingerObjectId;
	private int _lbrasObjectId;
	private int _rbrasObjectId;
	private int _learObjectId;
	private int _rearObjectId;
	private int _neckObjectId;
	private int _beltObjectId;
	private int _deco1ObjectId;
	private int _deco2ObjectId;
	private int _deco3ObjectId;
	private int _deco4ObjectId;
	private int _deco5ObjectId;
	private int _deco6ObjectId;

	private int _level;
	private long _exp;
	private int _sp;
	private int _karma;
	private int _pk;
	private int _pvp;

	private float _runSpeed;
	private long _abnormalEffects;

	private String _title;
	private int _inventoryLimit;
	private boolean _cloak;
	private int _warehouse;
	private int _privateBuy;
	private int _recipeDwarven;
	private int _recipeCommon;

	public StatsChangeRecorder(L2Player player)
	{
		_player = player;
		refreshSaves();
	}

	public void refreshSaves()
	{
		if(_player == null)
			return;

		_level = _player.getLevel();
		_accuracy = _player.getAccuracy();
		_attackSpeed = _player.getPAtkSpd();
		_castSpeed = _player.getMAtkSpd();
		_criticalHit = _player.getCriticalHit(null, null);
		_evasion = _player.getEvasionRate(null);
		_magicAttack = _player.getMAtk(null, null);
		_magicDefence = _player.getMDef(null, null);
		_maxCp = _player.getMaxCp();
		_maxHp = _player.getMaxHp();
		_maxLoad = _player.getMaxLoad();
		_curLoad = _player.getCurrentLoad();
		_maxMp = _player.getMaxMp();
		_physicAttack = _player.getPAtk(null);
		_physicDefence = _player.getPDef(null);
		_attackElement = _player.getAttackElement();
		_defenceFire = _player.getDefenceFire();
		_defenceWater = _player.getDefenceWater();
		_defenceWind = _player.getDefenceWind();
		_defenceEarth = _player.getDefenceEarth();
		_defenceHoly = _player.getDefenceHoly();
		_defenceUnholy = _player.getDefenceDark();
		_inventoryLimit = _player.getInventoryLimit();
		_warehouse = _player.getWarehouseLimit();
		_privateBuy = _player.getTradeLimit();
		_recipeDwarven = _player.getDwarvenRecipeLimit();
		_recipeCommon = _player.getCommonRecipeLimit();

		_cloak = _player.isStatActive(Stats.CLOAK);

		_headObjectId = _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_HEAD);
		_hairObjectId = _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_HAIR);
		_dhairObjectId = _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_DHAIR);
		_rhandObjectId = _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RHAND);
		_chestObjectId = _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_CHEST);
		_lhandObjectId = _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND);
		_glovesObjectId = _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_GLOVES);
		_legsObjectId = _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LEGS);
		_feetObjectId = _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_FEET);
		_backObjectId = _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_BACK);

		_underObjectId = _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_UNDER);
		_lfingerObjectId = _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LFINGER);
		_rfingerObjectId = _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RFINGER);
		_lbrasObjectId = _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LBRACELET);
		_rbrasObjectId = _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RBRACELET);
		_learObjectId = _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LEAR);
		_rearObjectId = _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_REAR);
		_neckObjectId = _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_NECK);
		_beltObjectId = _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_BELT);
		_deco1ObjectId = _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_DECO1);
		_deco2ObjectId = _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_DECO2);
		_deco3ObjectId = _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_DECO3);
		_deco4ObjectId = _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_DECO4);
		_deco5ObjectId = _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_DECO5);
		_deco6ObjectId = _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_DECO6);

		_exp = _player.getExp();
		_sp = _player.getSp();
		_karma = _player.getKarma();

		_runSpeed = _player.getRunSpeed();
		_pk = _player.getPkKills();
		_pvp = _player.getPvpKills();
		_abnormalEffects = _player.getAllAbnormalEffects();
		_title = _player.getTitle();
	}

	public void sendChanges()
	{
		if(_player == null)
			return;

		// Броадкаст UserInfo и charInfo();
		if(needsUserInfoBroadcast())
		{
			if(needUserInfoForce() && Config.BROADCAST_STATS_INTERVAL)
				_player.sendPacket(new UserInfo(_player));
			_player.broadcastUserInfo(!Config.BROADCAST_STATS_INTERVAL);
			sendStorageInfo();
			refreshSaves();
			return;
		}                                                                                                                                                    ;

		sendGlobalInfo();
		sendStorageInfo();
		sendPartyInfo();
		sendSelfInfo();

		refreshSaves();
	}

	/**
	 * Отправляет броадкастом инфу всем игрокам
	 */
	private void sendGlobalInfo()
	{
		StatusUpdate globalUpdate = new StatusUpdate(_player.getObjectId());
		/**if(_attackSpeed != _player.getPAtkSpd())
			globalUpdate.addAttribute(StatusUpdate.ATK_SPD, _player.getPAtkSpd());

		if(_castSpeed != _player.getMAtkSpd())
			globalUpdate.addAttribute(StatusUpdate.CAST_SPD, _player.getMAtkSpd());
		 */

		if(_karma != _player.getKarma())
			globalUpdate.addAttribute(StatusUpdate.KARMA, _player.getKarma());

		if(globalUpdate.hasAttributes())
			_player.broadcastPacket(globalUpdate);

		// Проверка тайтла
		if(_title == null && _player.getTitle() != null || _title != null && !_title.equals(_player.getTitle()))
			_player.broadcastPacketToOthers(new NickNameChanged(_player));
	}

	/**
	 * Отправляет инфу парти игрока.
	 * Если парти нет, то отправляет лично игроку
	 */
	private void sendPartyInfo()
	{
		// Эти статы нужно рассылать только для партии игрока
		StatusUpdate partyUpdate = new StatusUpdate(_player.getObjectId());
		if(_maxCp != _player.getMaxCp())
			partyUpdate.addAttribute(StatusUpdate.MAX_CP, _player.getMaxCp());

		if(_maxHp != _player.getMaxHp())
			partyUpdate.addAttribute(StatusUpdate.MAX_HP, _player.getMaxHp());

		if(_maxMp != _player.getMaxMp())
			partyUpdate.addAttribute(StatusUpdate.MAX_MP, _player.getMaxMp());

		L2Party party = _player.getParty();
		if(partyUpdate.hasAttributes())
			if(party != null)
				party.broadcastToPartyMembers(partyUpdate);
			else
				_player.sendPacket(partyUpdate);
	}

	/**
	 * Отправляет инфу только игроку
	 */
	private void sendSelfInfo()
	{
		if(needUserInfoForce())
		{
			_player.sendUserInfo(true);
			return;
		}

		// Количество exp, sp, pk и левел - характеристики о которых другие игроки не обязаны знать
		if(_pk != _player.getPkKills() || _pvp != _player.getPvpKills() || _exp != _player.getExp() || _sp != _player.getSp() || _inventoryLimit != _player.getInventoryLimit() || _cloak != _player.isStatActive(Stats.CLOAK))
		{
			_player.sendUserInfo(false);
			return;
		}

		// Проверка тайтла
		if(_title == null && _player.getTitle() != null)
		{
			_player.sendUserInfo(false);
			return;
		}
		else if(_title != null && !_title.equals(_player.getTitle()))
		{
			_player.sendUserInfo(false);
			return;
		}

		if(_accuracy != _player.getAccuracy())
		{
			_player.sendUserInfo(false);
			return;
		}

		if(_criticalHit != _player.getCriticalHit(null, null))
		{
			_player.sendUserInfo(false);
			return;
		}

		if(_evasion != _player.getEvasionRate(null))
		{
			_player.sendUserInfo(false);
			return;
		}

		if(_magicAttack != _player.getMAtk(null, null))
		{
			_player.sendUserInfo(false);
			return;
		}

		if(_magicDefence != _player.getMDef(null, null))
		{
			_player.sendUserInfo(false);
			return;
		}

		if(_maxLoad != _player.getMaxLoad())
		{
			_player.sendUserInfo(false);
			return;
		}

		if(_curLoad != _player.getCurrentLoad())
			_player.sendPacket(new StatusUpdate(_player._objectId).addAttribute(StatusUpdate.CUR_LOAD, _player.getCurrentLoad()));

		if(_physicAttack != _player.getPAtk(null))
		{
			_player.sendUserInfo(false);
			return;
		}

		if(_physicDefence != _player.getPDef(null))
		{
			_player.sendUserInfo(false);
			return;
		}

		int[] attElement = _player.getAttackElement();
		if(_attackElement[0] != attElement[0] || _attackElement[1] != attElement[1])
		{
			_player.sendUserInfo(false);
			return;
		}

		if(_defenceFire != _player.getDefenceFire() || _defenceWater != _player.getDefenceWater() ||
				_defenceWind != _player.getDefenceWind() || _defenceEarth != _player.getDefenceEarth() ||
				_defenceHoly != _player.getDefenceHoly() || _defenceUnholy != _player.getDefenceDark())
		{
			_player.sendUserInfo(false);
			return;
		}

		if(_level != _player.getLevel())
			_player.sendUserInfo(false);
	}

	public void sendStorageInfo()
	{
		if(_inventoryLimit != _player.getInventoryLimit() || _warehouse != _player.getWarehouseLimit()
				|| _privateBuy != _player.getTradeLimit() || _recipeDwarven != _player.getDwarvenRecipeLimit() || _recipeCommon != _player.getCommonRecipeLimit())
			_player.sendPacket(new ExStorageMaxCount(_player));
	}

	/**
	 * Проверяет нужно ли делать UserInfo broadcast. Дорогостоящая операция.
	 * @return true если нужно.
	 */
	private boolean needsUserInfoBroadcast()
	{
		return _runSpeed != _player.getRunSpeed() || _abnormalEffects != _player.getAllAbnormalEffects() || _attackSpeed != _player.getPAtkSpd() || _castSpeed != _player.getMAtkSpd() ||
				_headObjectId != _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_HEAD) ||
				_hairObjectId != _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_HAIR) ||
				_dhairObjectId != _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_DHAIR) ||
				_rhandObjectId != _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RHAND) ||
				_chestObjectId != _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_CHEST) ||
				_lhandObjectId != _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND) ||
				_glovesObjectId != _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_GLOVES) ||
				_legsObjectId != _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LEGS) ||
				_feetObjectId != _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_FEET) ||
				_backObjectId != _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_BACK);
	}

	private boolean needUserInfoForce()
	{
		return (_deco1ObjectId != _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_DECO1) ||
				_deco2ObjectId != _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_DECO2) ||
				_deco3ObjectId != _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_DECO3) ||
				_deco4ObjectId != _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_DECO4) ||
				_deco5ObjectId != _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_DECO5) ||
				_deco6ObjectId != _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_DECO6) ||
				_lfingerObjectId != _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LFINGER) ||
				_rfingerObjectId != _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RFINGER) ||
				_lbrasObjectId != _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LBRACELET) ||
				_rbrasObjectId != _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RBRACELET) ||
				_learObjectId != _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LEAR) ||
				_rearObjectId != _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_REAR) ||
				_neckObjectId != _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_NECK) ||
				_beltObjectId != _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_BELT) ||
				_underObjectId != _player.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_UNDER));
	}
}