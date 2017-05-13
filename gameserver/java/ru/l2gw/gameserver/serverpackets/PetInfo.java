package ru.l2gw.gameserver.serverpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.L2Summon;
import ru.l2gw.gameserver.tables.PetDataTable;
import ru.l2gw.util.Location;

/**
 * Дамп пакета с оффа, 828 протокол:
 * 0000: b2 01 00 00 00 f9 a1 50 4c 3f 79 0f 00 00 00 00    .......PL?y.....
 * 0010: 00 a7 4e 02 00 71 62 00 00 a8 f7 ff ff 01 a0 00    ..N..qb.........
 * 0020: 00 00 00 00 00 4d 01 00 00 16 01 00 00 a0 00 00    .....M..........
 * 0030: 00 50 00 00 00 a0 00 00 00 50 00 00 00 a0 00 00    .P.......P......
 * 0040: 00 50 00 00 00 a0 00 00 00 50 00 00 00 9a 99 99    .P.......P......
 * 0050: 99 99 99 f1 3f 81 43 a8 52 b3 07 f0 3f 00 00 00    ....?.C.R...?...
 * 0060: 00 00 00 34 40 00 00 00 00 00 00 45 40 00 00 00    ...4@......E@...
 * 0070: 00 00 00 00 00 00 00 00 00 01 01 00 00 01 00 00    ................
 * 0080: 43 00 79 00 63 00 00 00 01 00 00 00 00 00 00 00    C.y.c...........
 * 0090: 00 00 00 00 00 00 00 00 00 00 00 00 a8 13 00 00    ................
 * 00a0: a8 13 00 00 60 07 00 00 60 07 00 00 00 00 00 00    ....`...`.......
 * 00b0: 4a 00 00 00 b9 ef 81 29 00 00 00 00 00 00 00 00    J......)........
 * 00c0: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00    ................
 * 00d0: ee d4 00 00 da 07 00 00 ff 01 00 00 98 03 00 00    ................
 * 00e0: df 01 00 00 74 00 00 00 6f 00 00 00 50 00 00 00    ....t...o...P...
 * 00f0: b0 00 00 00 16 01 00 00 4d 01 00 00 00 00 00 00    ........M.......
 * 0100: 00 00 00 00 00 00 05 00 00 00 02 00 00 00 00 00    ................
 * 0110: 00 00                                              ..
 *
 * rev 828	dddddddddddddddddddffffdddcccccSSdddddddddddQQQddddddddddddd
 */
public class PetInfo extends L2GameServerPacket
{
	private int runSpd, walkSpd, MAtkSpd, PAtkSpd, pvp_flag, karma, rideable, abnormalEffect, abnormalEffect2;
	private int type, obj_id, npc_id, runing, incombat, dead, _sp, level;
	private int curFed, maxFed, curHp, maxHp, curMp, maxMp, curLoad, maxLoad;
	private int pAtk, pDef, mAtk, mDef, accuracy, evasion, crit, team, sps, ss, form, weapon, armor, spawnType;
	private Location _loc;
	private float col_redius, col_height;
	private long exp, exp_this_lvl, exp_next_lvl;
	private String name, title;
	private int nameStringId, titleStringId;

	public PetInfo(L2Summon summon, int spawn)
	{
		type = summon.getSummonType();
		obj_id = summon.getObjectId();
		npc_id = summon.getTemplate().npcId;
		_loc = summon.getLoc();
		MAtkSpd = summon.getMAtkSpd();
		PAtkSpd = summon.getPAtkSpd();
		runSpd = (int) summon.getRunSpeed();
		walkSpd = (int) summon.getWalkSpeed();
		col_redius = summon.getColRadius();
		col_height = summon.getColHeight();
		runing = summon.isRunning() ? 1 : 0;
		incombat = summon.isInCombat() ? 1 : 0;
		dead = summon.isAlikeDead() ? 1 : 0;
		name = summon.isPet() ? summon.getName() : "";
		title = summon.getTitle();
		nameStringId = summon.getNameFStringId();
		titleStringId = summon.getTitleFStringId();
		pvp_flag = summon.getPvpFlag();
		karma = summon.getKarma();
		curFed = summon.getCurrentFed();
		maxFed = summon.getMaxMeal();
		curHp = (int) summon.getCurrentHp();
		maxHp = summon.getMaxHp();
		curMp = (int) summon.getCurrentMp();
		maxMp = summon.getMaxMp();
		_sp = summon.getSp();
		level = summon.getLevel();
		exp = summon.getExp();
		exp_this_lvl = summon.getExpForThisLevel();
		exp_next_lvl = summon.getExpForNextLevel();
		curLoad = summon.isPet() ? summon.getInventory().getTotalWeight() : 0;
		maxLoad = summon.getMaxLoad();
		pAtk = summon.getPAtk(null);
		pDef = summon.getPDef(null);
		mAtk = summon.getMAtk(null, null);
		mDef = summon.getMDef(null, null);
		accuracy = summon.getAccuracy();
		evasion = summon.getEvasionRate(null);
		crit = summon.getCriticalHit(null, null);
		abnormalEffect = summon.getAbnormalEffect();
		abnormalEffect2 = summon.getAbnormalEffect2();
		weapon = summon.getWeaponItemId();
		armor = summon.getArmorItemId();
		spawnType = spawn;
		// В режиме трансформации значек mount/dismount не отображается
		L2Player owner = summon.getPlayer();
		if(owner != null && owner.getTransformation() != 0)
			rideable = 0; //not rideable
		else
			rideable = (summon.isPet() && summon.isMountable())? 1 : 0;
		team = summon.getTeam();
		ss = summon.getSoulshotConsumeCount();
		sps = summon.getSpiritshotConsumeCount();

		if(summon.getTemplate().getNpcId() == PetDataTable.BLACK_WOLF_ID || summon.getTemplate().getNpcId() == PetDataTable.WGREAT_WOLF_ID)
		{
			if(summon.getLevel() >= 60 && summon.getLevel() < 65)
				form = 1;
			else if(summon.getLevel() >= 65 && summon.getLevel() < 70)
				form = 2;
			else if(summon.getLevel() >= 70)
				form = 3;
		}
		else if(summon.getTemplate().getNpcId() == PetDataTable.FENRIR_WOLF_ID || summon.getTemplate().getNpcId() == PetDataTable.WFENRIR_WOLF_ID)
		{
			if(summon.getLevel() >= 75 && summon.getLevel() < 80)
				form = 1;
			else if(summon.getLevel() >= 80 && summon.getLevel() < 85)
				form = 2;
			else if(summon.getLevel() >= 85)
				form = 3;
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xB2);
		writeD(type);
		writeD(obj_id);
		writeD(npc_id + 1000000);
		writeD(0); // 1=attackable
		writeD(_loc.getX());
		writeD(_loc.getY());
		writeD(_loc.getZ());
		writeD(_loc.getHeading());
		writeD(0);
		writeD(MAtkSpd);
		writeD(PAtkSpd);
		writeD(runSpd);
		writeD(walkSpd);
		writeD(runSpd/*_swimRunSpd*/);
		writeD(walkSpd/*_swimWalkSpd*/);
		writeD(runSpd/*_flRunSpd*/);
		writeD(walkSpd/*_flWalkSpd*/);
		writeD(runSpd/*_flyRunSpd*/);
		writeD(walkSpd/*_flyWalkSpd*/);
		writeF(1/*_cha.getProperMultiplier()*/);
		writeF(1/*_cha.getAttackSpeedMultiplier()*/);
		writeF(col_redius);
		writeF(col_height);
		writeD(weapon); // right hand weapon
		writeD(armor);
		writeD(0); // left hand weapon
		writeC(1); // name above char 1=true ... ??
		writeC(runing); // running=1
		writeC(incombat); // attacking 1=true
		writeC(dead); // dead 1=true
		writeC(spawnType); // 0 - Focus on pet window, 1 - update pet stats, 2 - show spawn animation
		writeD(nameStringId);
		writeS(name);
		writeD(titleStringId);
		writeS(title);
		writeD(1);
		writeD(pvp_flag); //0=white, 1=purple, 2=purpleblink, if its greater then karma = purple
		writeD(karma); // hmm karma ??
		writeD(curFed); // how fed it is
		writeD(maxFed); //max fed it can be
		writeD(curHp); //current hp
		writeD(maxHp); // max hp
		writeD(curMp); //current mp
		writeD(maxMp); //max mp
		writeD(_sp); //sp
		writeD(level);// lvl
		writeQ(exp);
		writeQ(exp_this_lvl); // 0%  absolute value
		writeQ(exp_next_lvl); // 100% absoulte value
		writeD(curLoad); //weight
		writeD(maxLoad); //max weight it can carry
		writeD(pAtk);//patk
		writeD(pDef);//pdef
		writeD(mAtk);//matk
		writeD(mDef);//mdef
		writeD(accuracy);//accuracy
		writeD(evasion);//evasion
		writeD(crit);//critical
		writeD(runSpd);//speed
		writeD(PAtkSpd);//atkspeed
		writeD(MAtkSpd);//casting speed
		writeD(abnormalEffect); //c2  abnormal visual effect... bleed=1; poison=2; bleed?=4;
		writeH(rideable);
		writeC(0); // c2
		writeH(0); // ??
		writeC(team); // team aura (1 = blue, 2 = red)
		writeD(ss);
		writeD(sps);
		writeD(form);
		writeD(abnormalEffect2);
	}
}