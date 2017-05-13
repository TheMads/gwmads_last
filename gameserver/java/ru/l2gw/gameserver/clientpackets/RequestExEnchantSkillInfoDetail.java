package ru.l2gw.gameserver.clientpackets;

import ru.l2gw.gameserver.model.L2Player;
import ru.l2gw.gameserver.model.base.L2EnchantSkillLearn;
import ru.l2gw.gameserver.serverpackets.ExEnchantSkillInfoDetail;
import ru.l2gw.gameserver.tables.SkillTreeTable;
import ru.l2gw.commons.arrays.GArray;

public final class RequestExEnchantSkillInfoDetail extends L2GameClientPacket
{
	private static final int TYPE_NORMAL_ENCHANT = 0;
	private static final int TYPE_SAFE_ENCHANT = 1;
	private static final int TYPE_UNTRAIN_ENCHANT = 2;
	private static final int TYPE_CHANGE_ENCHANT = 3;

	private int _type;
	private int _skillId;
	private int _skillLvl;

	@Override
	protected void readImpl()
	{
		_type = readD();
		_skillId = readD();
		_skillLvl = readD();
	}

	@Override
	protected void runImpl()
	{
		L2Player player = getClient().getPlayer();

		if(player == null)
			return;

		if(player.getTransformation() != 0)
		{
			player.sendMessage("You must leave transformation mode first.");
			return;
		}

		if(player.getLevel() < 76 || player.getClassId().getLevel() < 4)
		{
			player.sendMessage("You must have 3rd class change quest completed.");
			return;
		}

		int bookId = 0;
		int sp = 0;
		int adenaCount = 0;
		float spMult = SkillTreeTable.NORMAL_ENCHANT_COST_MULTIPLIER;

		L2EnchantSkillLearn esd;

		switch(_type)
		{
			case TYPE_NORMAL_ENCHANT:
				if(_skillLvl % 100 == 1)
					bookId = SkillTreeTable.NORMAL_ENCHANT_BOOK;
				esd = SkillTreeTable.getSkillEnchant(_skillId, _skillLvl);
				break;
			case TYPE_SAFE_ENCHANT:
				bookId = SkillTreeTable.SAFE_ENCHANT_BOOK;
				esd = SkillTreeTable.getSkillEnchant(_skillId, _skillLvl);
				spMult = SkillTreeTable.SAFE_ENCHANT_COST_MULTIPLIER;
				break;
			case TYPE_UNTRAIN_ENCHANT:
				bookId = SkillTreeTable.UNTRAIN_ENCHANT_BOOK;
				esd = SkillTreeTable.getSkillEnchant(_skillId, _skillLvl + 1);
				break;
			case TYPE_CHANGE_ENCHANT:
				bookId = SkillTreeTable.CHANGE_ENCHANT_BOOK;
				GArray<L2EnchantSkillLearn> s = SkillTreeTable.getEnchantsForChange(_skillId, _skillLvl);
				esd = s.size() > 0 ? s.get(0) : null;
				spMult = 1f / SkillTreeTable.SAFE_ENCHANT_COST_MULTIPLIER;
				break;
			default:
				_log.warn("Unknown skill enchant type: " + _type);
				return;
		}

		if(esd == null)
			return;

		spMult *= esd.getCostMult();
		int[] cost = esd.getCost();

		sp = (int) (cost[1] * spMult);

		if(_type != TYPE_UNTRAIN_ENCHANT)
			adenaCount = (int) (cost[0] * spMult);

		// send skill enchantment detail
		player.sendPacket(new ExEnchantSkillInfoDetail(_skillId, _skillLvl, sp, esd.getRate(player), bookId, adenaCount));
	}
}