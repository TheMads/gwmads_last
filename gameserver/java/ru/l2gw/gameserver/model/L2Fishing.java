package ru.l2gw.gameserver.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.l2gw.commons.math.Rnd;
import ru.l2gw.gameserver.controllers.ThreadPoolManager;
import ru.l2gw.gameserver.geodata.GeoEngine;
import ru.l2gw.gameserver.model.instances.L2PenaltyMonsterInstance;
import ru.l2gw.gameserver.serverpackets.ExFishingHpRegen;
import ru.l2gw.gameserver.serverpackets.ExFishingStartCombat;
import ru.l2gw.gameserver.serverpackets.SystemMessage;
import ru.l2gw.gameserver.tables.NpcTable;
import ru.l2gw.gameserver.templates.L2NpcTemplate;

import java.util.concurrent.Future;

public class L2Fishing implements Runnable
{
	protected static Log _log = LogFactory.getLog(L2Fishing.class.getName());
	private L2Player _fisher;
	private int _time;
	private int _stop = 0;
	private int _gooduse = 0;
	private int _anim = 0;
	private int _mode = 0;
	private int _deceptiveMode = 0;
	private Future<?> _fishAItask;
	private boolean thinking;
	// Fish datas
	private int _fishID;
	private int _fishMaxHP;
	private int _fishCurHP;
	private double _regenHP;
	private boolean _isUpperGrade;

	public void run()
	{
		if(_fisher == null)
			return;

		if(_fishCurHP >= _fishMaxHP * 2)
		{
			// The fish got away
			_fisher.sendPacket(new SystemMessage(SystemMessage.THE_FISH_GOT_AWAY));
			penaltyMonster(); //Random chance to spawn monster
			doDie(false);
		}
		else if(_time <= 0)
		{
			// Time is up, so that fish got away
			_fisher.sendPacket(new SystemMessage(SystemMessage.TIME_IS_UP_SO_THAT_FISH_GOT_AWAY));
			penaltyMonster(); //Random chance to spawn monster
			doDie(false);
		}
		else
			AiTask();
	}

	// =========================================================
	public L2Fishing(L2Player Fisher, FishData fish, boolean isNoob, boolean isUpperGrade)
	{
		_fisher = Fisher;

		_fishMaxHP = fish.getHP();
		_fishCurHP = _fishMaxHP;
		_regenHP = fish.getHpRegen();
		_fishID = fish.getId();
		_time = fish.getCombatTime() / 1000;
		_isUpperGrade = isUpperGrade;
		int lureType;
		if(isUpperGrade)
		{
			_deceptiveMode = Rnd.chance(10) ? 1 : 0;
			lureType = 2;
		}
		else
		{
			_deceptiveMode = 0;
			lureType = isNoob ? 0 : 1;
		}
		_mode = Rnd.chance(20) ? 1 : 0;

		ExFishingStartCombat efsc = new ExFishingStartCombat(_fisher, _time, _fishMaxHP, _mode, lureType, _deceptiveMode);
		_fisher.broadcastPacket(efsc);

		// Succeeded in getting a bite
		_fisher.sendPacket(new SystemMessage(SystemMessage.SUCCEEDED_IN_GETTING_A_BITE));

		if(_fishAItask == null)
			_fishAItask = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(this, 1000, 1000);

	}

	public void changeHp(int hp, int pen)
	{
		_fishCurHP -= hp;
		if(_fishCurHP < 0)
			_fishCurHP = 0;

		ExFishingHpRegen efhr = new ExFishingHpRegen(_fisher, _time, _fishCurHP, _mode, _gooduse, _anim, pen, _deceptiveMode);
		_fisher.broadcastPacket(efhr);
		_gooduse = 0;
		_anim = 0;
		if(_fishCurHP > _fishMaxHP * 2)
		{
			_fishCurHP = _fishMaxHP * 2;
			penaltyMonster();
			doDie(false);
		}
		else if(_fishCurHP == 0)
			doDie(true);
	}

	public void doDie(boolean win)
	{
		try
		{
			if(_fishAItask != null)
			{
				_fishAItask.cancel(false);
				_fishAItask = null;
			}

			if(_fisher == null)
				return;

			if(win)
				if(Rnd.chance(5))
					penaltyMonster();
				else
				{
					_fisher.sendPacket(new SystemMessage(SystemMessage.SUCCEEDED_IN_FISHING));
					_fisher.addItem("FishItem_add", _fishID, 1, null, true);
				}
			_fisher.endFishing(win);

			_fisher = null;
		}
		catch(NullPointerException e)
		{
		}
	}

	protected void AiTask()
	{
		if(thinking || _fisher == null)
			return;
		thinking = true;
		_time--;

		try
		{
			if(_mode == 1)
			{
				if(_deceptiveMode == 0)
					_fishCurHP += (int) _regenHP;
			}
			else if(_deceptiveMode == 1)
				_fishCurHP += (int) _regenHP;
			if(_stop == 0)
			{
				_stop = 1;
				if(Rnd.chance(30))
					_mode = _mode == 0 ? 1 : 0;

				if(_isUpperGrade)
					if(Rnd.chance(10))
						_deceptiveMode = _deceptiveMode == 0 ? 1 : 0;
			}
			else
				_stop--;
		}
		finally
		{
			thinking = false;
			ExFishingHpRegen efhr = new ExFishingHpRegen(_fisher, _time, _fishCurHP, _mode, 0, _anim, 0, _deceptiveMode);
			if(_anim != 0)
				_fisher.broadcastPacket(efhr);
			else
				_fisher.sendPacket(efhr);
		}
	}

	public void UseRealing(int dmg, int pen)
	{
		if(_fisher == null)
			return;
		_anim = 2;
		if(Rnd.chance(10))
		{
			_fisher.sendPacket(new SystemMessage(SystemMessage.FISH_HAS_RESISTED));
			_gooduse = 0;
			changeHp(0, pen);
			return;
		}
		if(_mode == 1)
		{
			if(_deceptiveMode == 0)
			{
				// Reeling is successful, Damage: $s1
				_fisher.sendPacket(new SystemMessage(SystemMessage.REELING_IS_SUCCESSFUL_DAMAGE_S1).addNumber(dmg));
				if(pen == 50)
					_fisher.sendPacket(new SystemMessage(SystemMessage.YOUR_REELING_WAS_SUCCESSFUL_MASTERY_PENALTYS1_).addNumber(pen));

				_gooduse = 1;
				changeHp(dmg, pen);
			}
			else
			{
				// Reeling failed, Damage: $s1
				_fisher.sendPacket(new SystemMessage(SystemMessage.REELING_FAILED_DAMAGE_S1).addNumber(dmg));
				_gooduse = 2;
				changeHp(-dmg, pen);
			}
		}
		else if(_deceptiveMode == 0)
		{
			// Reeling failed, Damage: $s1
			_fisher.sendPacket(new SystemMessage(SystemMessage.REELING_FAILED_DAMAGE_S1).addNumber(dmg));
			_gooduse = 2;
			changeHp(-dmg, pen);
		}
		else
		{
			// Reeling is successful, Damage: $s1
			_fisher.sendPacket(new SystemMessage(SystemMessage.REELING_IS_SUCCESSFUL_DAMAGE_S1).addNumber(dmg));
			if(pen == 50)
				_fisher.sendPacket(new SystemMessage(SystemMessage.REELING_IS_SUCCESSFUL_DAMAGE_S1).addNumber(pen));

			_gooduse = 1;
			changeHp(dmg, pen);
		}
	}

	public void UsePomping(int dmg, int pen)
	{
		if(_fisher == null)
			return;

		_anim = 1;
		if(Rnd.chance(10))
		{
			_fisher.sendPacket(new SystemMessage(SystemMessage.FISH_HAS_RESISTED));
			_gooduse = 0;
			changeHp(0, pen);
			return;
		}

		if(_mode == 0)
		{
			if(_deceptiveMode == 0)
			{
				// Pumping is successful. Damage: $s1
				_fisher.sendPacket(new SystemMessage(SystemMessage.PUMPING_IS_SUCCESSFUL_DAMAGE_S1).addNumber(dmg));
				if(pen == 50)
					_fisher.sendPacket(new SystemMessage(SystemMessage.YOUR_PUMPING_WAS_SUCCESSFUL_MASTERY_PENALTYS1_).addNumber(pen));

				_gooduse = 1;
				changeHp(dmg, pen);
			}
			else
			{
				// Pumping failed, Regained: $s1
				_fisher.sendPacket(new SystemMessage(SystemMessage.PUMPING_FAILED_DAMAGE_S1).addNumber(dmg));
				_gooduse = 2;
				changeHp(-dmg, pen);
			}
		}
		else if(_deceptiveMode == 0)
		{
			// Pumping failed, Regained: $s1
			_fisher.sendPacket(new SystemMessage(SystemMessage.PUMPING_FAILED_DAMAGE_S1).addNumber(dmg));
			_gooduse = 2;
			changeHp(-dmg, pen);
		}
		else
		{
			// Pumping is successful. Damage: $s1
			_fisher.sendPacket(new SystemMessage(SystemMessage.PUMPING_IS_SUCCESSFUL_DAMAGE_S1).addNumber(dmg));
			if(pen == 50)
				_fisher.sendPacket(new SystemMessage(SystemMessage.YOUR_PUMPING_WAS_SUCCESSFUL_MASTERY_PENALTYS1_).addNumber(pen));

			_gooduse = 1;
			changeHp(dmg, pen);
		}
	}

	private void penaltyMonster()
	{
		int lvl = (int) Math.round(_fisher.getLevel() * 0.1);
		int npcid;

		_fisher.sendPacket(new SystemMessage(SystemMessage.YOU_HAVE_CAUGHT_A_MONSTER));
		switch(lvl)
		{
			case 0:
			case 1:
				npcid = 18319;
				break;
			case 2:
				npcid = 18320;
				break;
			case 3:
				npcid = 18321;
				break;
			case 4:
				npcid = 18322;
				break;
			case 5:
				npcid = 18323;
				break;
			case 6:
				npcid = 18324;
				break;
			case 7:
				npcid = 18325;
				break;
			case 8:
				npcid = 18326;
				break;
			default:
				npcid = 18319;
				break;
		}

		L2NpcTemplate temp = NpcTable.getTemplate(npcid);
		if(temp != null)
		{
			L2Spawn spawn;
			try
			{
				spawn = new L2Spawn(temp);
				spawn.setLoc(GeoEngine.findPointToStay(_fisher.getX(), _fisher.getY(), _fisher.getZ(), 40, 100, _fisher.getReflection()));
				spawn.setAmount(1);
				spawn.setHeading(_fisher.getHeading() - 32768);
				spawn.stopRespawn();
				L2PenaltyMonsterInstance monster = (L2PenaltyMonsterInstance) spawn.doSpawn(true);
				if(_fisher.getReflection() != 0)
					monster.setReflection(_fisher.getReflection());
				monster.SetPlayerToKill(_fisher);
			}
			catch(Exception e)
			{
				_log.warn("Could not spawn Penalty Monster " + npcid + ", exception: " + e);
			}
		}
	}
}